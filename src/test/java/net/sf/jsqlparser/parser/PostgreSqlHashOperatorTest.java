/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.parser;

import static org.junit.jupiter.api.Assertions.*;

import net.sf.jsqlparser.expression.JsonExpression;
import net.sf.jsqlparser.expression.operators.arithmetic.BitwiseRightShift;
import net.sf.jsqlparser.expression.operators.relational.Intersects;
import net.sf.jsqlparser.expression.operators.relational.NotEqualsTo;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostgreSqlHashOperatorTest {
    private static PlainSelect parse(String sql) throws Exception {
        return (PlainSelect) CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.POSTGRESQL));
    }

    @ParameterizedTest
    @ValueSource(strings = {"js#>>'{a,b,1}'", "js #>> '{a,b,1}'", "js#>> '{a,b,1}'",
            "t.js#>>'{a,b,1}'", "\"js#\"#>>'{a,b,1}'", "js/*comment*/#>>'{a,b,1}'"})
    void preservesJsonPrecedenceWithOrWithoutSpacesIssue2163(String expression) throws Exception {
        PlainSelect select = parse("SELECT * FROM t WHERE " + expression + " <> 'bar'");
        NotEqualsTo condition = (NotEqualsTo) select.getWhere();
        assertInstanceOf(JsonExpression.class, condition.getLeftExpression());
        assertTrue(select.toString().contains(" #>> "));
        StringBuilder output = new StringBuilder();
        select.accept(new StatementDeParser(output));
        assertEquals(select.toString(), output.toString());
        assertEquals(select.toString(), parse(output.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"js#>'{a}'", "js#>'{a}'#>>'{b}'"})
    void recognizesOtherHashJsonOperators(String expression) throws Exception {
        PlainSelect select = parse("SELECT " + expression + " FROM t");
        assertInstanceOf(JsonExpression.class, select.getSelectItem(0).getExpression());
        assertEquals(select.toString(), parse(select.toString()).toString());
    }

    @Test
    void splitsHashXorAndPreservesQuotedIdentifiersAndLiterals() throws Exception {
        PlainSelect select = parse("SELECT a#b, a#2, \"a#b\", '#>>', $$a#b$$ FROM t");
        assertInstanceOf(Intersects.class, select.getSelectItem(0).getExpression());
        assertInstanceOf(Intersects.class, select.getSelectItem(1).getExpression());
        assertEquals("\"a#b\"", ((Column) select.getSelectItem(2).getExpression()).getColumnName());
        assertEquals(select.toString(), parse(select.toString()).toString());
    }

    @Test
    void leavesLegacyAndSqlServerNamesAndMysqlCommentsIntact() throws Exception {
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.POSTGRESQL && dialect != Dialect.MYSQL
                    && dialect != Dialect.MARIADB && dialect != Dialect.BIGQUERY) {
                PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse("SELECT a#b FROM #temp",
                        p -> p.withDialect(dialect));
                assertEquals("a#b",
                        ((Column) select.getSelectItem(0).getExpression()).getColumnName());
            }
        }
        PlainSelect legacy = (PlainSelect) CCJSqlParserUtil.parse("SELECT js#>>'{a}' FROM t");
        assertInstanceOf(BitwiseRightShift.class, legacy.getSelectItem(0).getExpression());
        PlainSelect mysql = (PlainSelect) CCJSqlParserUtil.parse("SELECT a#comment\nFROM t",
                p -> p.withDialect(Dialect.MYSQL));
        assertEquals("SELECT a FROM t", mysql.toString());
    }

    @Test
    void keepsTokenOffsetsAfterSplittingAndAcrossStatements() throws Exception {
        CCJSqlParser parser = CCJSqlParserUtil.newParser("js#>>'{}'")
                .withDialect(Dialect.POSTGRESQL);
        Token name = parser.getNextToken();
        Token operator = parser.getNextToken();
        assertEquals("js", name.image);
        assertEquals(1, name.beginColumn);
        assertEquals(2, name.endColumn);
        assertEquals(1, name.absoluteBegin);
        assertEquals(3, name.absoluteEnd);
        assertEquals("#>>", operator.image);
        assertEquals(3, operator.beginColumn);
        assertEquals(5, operator.endColumn);
        assertEquals(3, operator.absoluteBegin);
        assertEquals(6, operator.absoluteEnd);
        assertEquals(2, CCJSqlParserUtil.parseStatements("SELECT js#>>'{}' FROM t; SELECT 1;",
                p -> p.withDialect(Dialect.POSTGRESQL)).size());
    }
}
