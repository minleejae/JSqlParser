/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.select;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.expression.MethodCallExpression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SqlServerXmlMethodTest {
    private static final String SIMPLE =
            "SELECT (SELECT body FROM docs FOR XML PATH (''), TYPE).value('.', 'varchar(max)')";

    private static PlainSelect parse(String sql) throws Exception {
        return (PlainSelect) CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.SQLSERVER));
    }

    @Test
    void parsesOriginalStuffQueryIssue386() throws Exception {
        String sql = "SELECT (STUFF((SELECT '|' + person_name FROM person "
                + "JOIN person_group ON person.person_id = person_group.person_id "
                + "WHERE person_group.group_id = 1 FOR XML PATH(''), TYPE)"
                + ".value('.', 'varchar(max)'), 1, 1, '')) AS person_name";
        Statement statement = assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.SQLSERVER));
        assertEquals(Set.of("person", "person_group"),
                new TablesNamesFinder().getTables(statement));
        assertEquals(statement.toString(), parse(statement.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"value('.', 'varchar(max)')", "query('/root')", "exist('/root')",
            "query('/root').value('.', 'int')"})
    void preservesMethodsAndChains(String method) throws Exception {
        String sql = "SELECT (SELECT body FROM docs FOR XML PATH(''), TYPE)." + method;
        PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.SQLSERVER));
        MethodCallExpression call = (MethodCallExpression) select.getSelectItem(0).getExpression();
        assertInstanceOf(ParenthesedSelect.class, call.getExpression());
        assertEquals(method.substring(0, method.indexOf('(')), call.getMethod().getName());
        assertEquals(select.toString(), parse(select.toString()).toString());
        assertEquals(Set.of("docs"), new TablesNamesFinder().getTables((Statement) select));
    }

    @Test
    void visitsReceiverAndMethodArgumentsWithContext() throws Exception {
        List<String> values = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(StringValue value, S context) {
                assertEquals("context", context);
                values.add(value.getValue());
                return null;
            }
        };
        SelectVisitorAdapter<Void> selects = new SelectVisitorAdapter<>(expressions);
        expressions.setSelectVisitor(selects);
        parse(SIMPLE).accept(new StatementVisitorAdapter<>(selects), "context");
        assertTrue(values.contains("."));
        assertTrue(values.contains("varchar(max)"));
        assertEquals(1, values.stream().filter("."::equals).count());
    }

    @Test
    void editsAndDeparsesMethodArguments() throws Exception {
        PlainSelect select = parse(SIMPLE);
        MethodCallExpression call = (MethodCallExpression) select.getSelectItem(0).getExpression();
        call.setMethod(new Function("query", new StringValue("'/root'")));
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(StringValue value, S context) {
                return getBuilder().append("'changed'");
            }
        };
        select.accept(new StatementDeParser(expressions, new SelectDeParser(), output));
        assertTrue(output.toString().endsWith(".query('changed')"));
        assertTrue(select.toString().endsWith(".query('/root')"));
        assertEquals(output.toString(), parse(output.toString()).toString());
    }

    @Test
    void gatesDialectAndRetainsRowNavigation() throws Exception {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(SIMPLE));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.SQLSERVER) {
                assertThrows(JSQLParserException.class,
                        () -> CCJSqlParserUtil.parse(SIMPLE, p -> p.withDialect(dialect)));
            }
        }
        assertSqlCanBeParsedAndDeparsed("SELECT (row_value).field FROM t");
        assertSqlCanBeParsedAndDeparsed("SELECT (row_value).field COLLATE en_US FROM t");
        assertThrows(JSQLParserException.class,
                () -> parse(SIMPLE.substring(0, SIMPLE.length() - 1)));
        assertEquals(2, CCJSqlParserUtil.parseStatements(SIMPLE + "; SELECT 1;",
                p -> p.withDialect(Dialect.SQLSERVER)).size());
    }

    @Test
    void validatesMethodAsFunction() {
        String sql = "SELECT 1 WHERE " + SIMPLE.substring("SELECT ".length()) + " = 'value'";
        FeatureConfiguration config = new FeatureConfiguration().setValue(Feature.dialect,
                Dialect.SQLSERVER.name());
        assertTrue(new Validation(config, List.of(new FeaturesAllowed(Feature.values())), sql)
                .validate().isEmpty());
        assertFalse(new Validation(config,
                List.of(new FeaturesAllowed(Feature.values()).remove(Feature.function)), sql)
                .validate().isEmpty());
    }
}
