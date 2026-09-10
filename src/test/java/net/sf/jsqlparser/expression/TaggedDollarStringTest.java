/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.stream.Stream;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserConstants;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.Token;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class TaggedDollarStringTest {
    static Stream<String> tags() {
        return Stream.of("", "tag", "Tag_123", "_", "한글", "étiquette");
    }

    @ParameterizedTest
    @MethodSource("tags")
    void preservesLiteralBodiesAndDelimiters(String tag) throws Exception {
        String delimiter = "$" + tag + "$";
        for (String body : List.of("", "abc", "a\nb\r\nc\t  ", "x 'one' ''two'' \\ end",
                "/* comment */ -- more\n#hash", "[ {\"some\":\"json\",\"with\":\"properties$\"} ]",
                "$1 $other$ こんにちは")) {
            String literal = delimiter + body + delimiter;
            String sql = "SELECT " + literal + " AS value, 2 FROM t";
            PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sql);
            StringValue value =
                    assertInstanceOf(StringValue.class, select.getSelectItem(0).getExpression());
            assertEquals(body, value.getValue());
            assertEquals(body, value.getNotExcapedValue());
            assertEquals(delimiter, value.getQuoteStr());
            assertEquals(literal, value.toString());
            StringBuilder builder = new StringBuilder();
            select.accept(new StatementDeParser(builder), null);
            PlainSelect again = (PlainSelect) CCJSqlParserUtil.parse(builder.toString());
            assertEquals(body, again.getSelectItem(0).getExpression(StringValue.class).getValue());
            assertEquals(select.toString(), builder.toString());
        }
    }

    @Test
    void keepsDifferentTagsAndDollarSignsInsideBody() throws Exception {
        String body = "$other$ text $Tag$ $$ $1 $t";
        PlainSelect select =
                (PlainSelect) CCJSqlParserUtil.parse("SELECT $tag$" + body + "$tag$::text, $1");
        CastExpression cast = select.getSelectItem(0).getExpression(CastExpression.class);
        assertEquals(body, ((StringValue) cast.getLeftExpression()).getValue());
        assertInstanceOf(JdbcParameter.class, select.getSelectItem(1).getExpression());
    }

    @Test
    void retainsIdentifiersAndSupportsOptOut() throws Exception {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil
                .parse("SELECT $parameter, foo$bar, \"$tag$abc$tag$\", $1 FROM t");
        for (int i = 0; i < 3; i++) {
            assertInstanceOf(Column.class, select.getSelectItem(i).getExpression());
        }
        assertInstanceOf(JdbcParameter.class, select.getSelectItem(3).getExpression());
        for (String identifier : List.of("$tag$abc$tag$", "$tag$identifier")) {
            PlainSelect legacy = (PlainSelect) CCJSqlParserUtil.parse("SELECT " + identifier,
                    parser -> parser.withDollarQuotedStringTags(false));
            assertEquals(identifier,
                    legacy.getSelectItem(0).getExpression(Column.class).getColumnName());
        }
        PlainSelect untagged = (PlainSelect) CCJSqlParserUtil.parse("SELECT $$text$$",
                parser -> parser.withDollarQuotedStringTags(false));
        assertEquals("text", untagged.getSelectItem(0).getExpression(StringValue.class).getValue());
    }

    @Test
    void retainsBodyWithOtherLexerOptions() throws Exception {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(
                "SELECT $t$#hash\n\\text't$tag$ \"q\"$t$",
                parser -> parser
                        .withDialect(net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect.MYSQL));
        assertEquals("#hash\n\\text't$tag$ \"q\"",
                select.getSelectItem(0).getExpression(StringValue.class).getValue());
    }

    @Test
    void keepsLineColumnAndAbsoluteTokenPositions() {
        String literal = "$tag$a\nb$tag$";
        CCJSqlParser parser = CCJSqlParserUtil.newParser("SELECT " + literal + ", 2");
        parser.getNextToken();
        Token value = parser.getNextToken();
        Token comma = parser.getNextToken();
        assertEquals(CCJSqlParserConstants.S_CHAR_LITERAL, value.kind);
        assertEquals(literal, value.image);
        assertEquals(1, value.beginLine);
        assertEquals(8, value.beginColumn);
        assertEquals(2, value.endLine);
        assertEquals(6, value.endColumn);
        assertEquals(8, value.absoluteBegin);
        assertEquals(8 + literal.length(), value.absoluteEnd);
        assertEquals(value.absoluteEnd, comma.absoluteBegin);
        assertEquals(7, comma.beginColumn);
    }

    @Test
    void recognizesFunctionBodyAndFollowingStatement() throws Exception {
        String body = "SELECT 'a;''b'::text;\n";
        String sql =
                "CREATE FUNCTION f() RETURNS text AS $fn$" + body + "$fn$ LANGUAGE SQL; SELECT 42;";
        Statements statements = CCJSqlParserUtil.parseStatements(sql);
        assertEquals(2, statements.size());
        assertEquals("SELECT 42", statements.get(1).toString());
        org.junit.jupiter.api.Assertions
                .assertTrue(statements.get(0).toString().contains("$fn$" + body + "$fn$"));
        assertEquals(2, CCJSqlParserUtil.parseStatements(statements.toString()).size());
    }

    @Test
    @Timeout(10)
    void handlesLongBodiesAndOverlappingDelimiterPrefixes() throws Exception {
        String body = "$ta$tagX $tagtagX\n".repeat(12000);
        String sql = "SELECT $tagtag$" + body + "$tagtag$";
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(new StringReader(sql));
        assertEquals(body, select.getSelectItem(0).getExpression(StringValue.class).getValue());
        PlainSelect streamed = (PlainSelect) CCJSqlParserUtil.parse(
                new java.io.ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)), "UTF-8");
        assertEquals(body, streamed.getSelectItem(0).getExpression(StringValue.class).getValue());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT $tag$missing", "SELECT $Tag$wrong$tag$", "SELECT $t$ends$t",
            "SELECT $a$text$b$", "SELECT $$missing"})
    void rejectsUnterminatedOrMismatchedTags(String sql) {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse(sql, parser -> parser.withTimeOut(1000)));
    }
}
