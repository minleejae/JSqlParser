/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.alter;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.alter.AlterExpression.ColumnSetDefault;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AlterColumnDefaultExpressionTest {
    @ParameterizedTest
    @ValueSource(strings = {"(1 + 2)", "NULL", "CURRENT_TIMESTAMP",
            "nextval('app.counter'::regclass)", "'value'::text", "-1"})
    void parsedDefaultRetainsExpressionAndLegacyText(String value) throws JSQLParserException {
        Alter alter = parse(value);
        ColumnSetDefault column =
                alter.getAlterExpressions().get(0).getColumnSetDefaultList().get(0);
        assertNotNull(column.getDefaultExpression());
        assertEquals(column.getDefaultExpression().toString(), column.getDefaultValue());
        StringBuilder buffer = new StringBuilder();
        alter.accept(new StatementDeParser(buffer), null);
        assertEquals(alter.toString(), buffer.toString());
        assertEquals(alter.toString(), CCJSqlParserUtil.parse(buffer.toString()).toString());
    }

    @Test
    void visitorReachesDefaultsInEachAlterActionWithContext() throws JSQLParserException {
        List<Long> values = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(LongValue value, S context) {
                assertEquals("context", context);
                values.add(value.getValue());
                return null;
            }
        };
        CCJSqlParserUtil.parse("ALTER TABLE t ALTER COLUMN a SET DEFAULT (1 + 2), "
                + "ALTER COLUMN b SET DEFAULT 3")
                .accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)),
                        "context");
        assertThat(values).containsExactly(1L, 2L, 3L);
    }

    @Test
    void customDeparserAndAstEditsUseTheStructuredDefault() throws JSQLParserException {
        Alter alter = parse("(1 + 2)");
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 100);
            }
        };
        alter.accept(new StatementDeParser(expressions, new SelectDeParser(), output), null);
        assertEquals("ALTER TABLE t ALTER COLUMN a SET DEFAULT (101 + 102)", output.toString());
        ColumnSetDefault column =
                alter.getAlterExpressions().get(0).getColumnSetDefaultList().get(0);
        column.setDefaultExpression(new LongValue(42));
        assertEquals("42", column.getDefaultValue());
        assertEquals("ALTER TABLE t ALTER COLUMN a SET DEFAULT 42", alter.toString());
    }

    @Test
    void legacyStringConstructorRemainsOpaqueAndAcceptsNull() {
        ColumnSetDefault column = new ColumnSetDefault("a", "vendor_default()");
        assertNull(column.getDefaultExpression());
        assertEquals("a SET DEFAULT vendor_default()", column.toString());
        assertEquals("a SET DEFAULT null", new ColumnSetDefault("a", null).toString());
        column.setDefaultExpression(new LongValue(1));
        assertEquals("1", column.getDefaultValue());
        column.setDefaultExpression(null);
        assertNull(column.getDefaultValue());
    }

    private static Alter parse(String value) throws JSQLParserException {
        return (Alter) CCJSqlParserUtil.parse("ALTER TABLE t ALTER COLUMN a SET DEFAULT " + value);
    }
}
