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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.ValidationError;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class WithCycleClauseTest {
    private static final String CTE = "WITH RECURSIVE walk(id, parent) AS "
            + "(SELECT id, parent FROM nodes UNION ALL "
            + "SELECT n.id, n.parent FROM nodes n JOIN walk w ON n.parent = w.id) ";

    @ParameterizedTest
    @ValueSource(strings = {
            "CYCLE id SET is_cycle USING path",
            "CYCLE id, parent SET is_cycle TO 'Y' DEFAULT 'N' USING path",
            "CYCLE id SET is_cycle TO 1 DEFAULT 0 USING path",
            "CYCLE id SET is_cycle TO true DEFAULT false USING path",
            "SEARCH DEPTH FIRST BY id SET seq CYCLE id SET is_cycle USING path",
            "SEARCH BREADTH FIRST BY id, parent SET seq CYCLE id, parent SET is_cycle USING path"
    })
    void parsesRecursiveCycleVariants(String clause) throws Exception {
        Select select = (Select) TestUtils.assertSqlCanBeParsedAndDeparsed(
                CTE + clause + " SELECT * FROM walk", true);
        assertNotNull(select.getWithItemsList().get(0).getCycleClause());
        assertEquals(select.toString(), CCJSqlParserUtil.parse(select.toString()).toString());
    }

    @Test
    void retainsQuotedNamesAndTypedMarkValues() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse(CTE
                + "CYCLE id, parent SET \"Cycle\" TO 'yes' DEFAULT 'no' USING \"Path\" SELECT * FROM walk");
        WithCycleClause cycle = select.getWithItemsList().get(0).getCycleClause();
        assertEquals(2, cycle.getCycleColumns().size());
        assertEquals("parent", cycle.getCycleColumns().get(1).getColumnName());
        assertEquals("\"Cycle\"", cycle.getMarkColumnName());
        assertEquals("yes", ((StringValue) cycle.getMarkValue()).getValue());
        assertEquals("no", ((StringValue) cycle.getMarkDefault()).getValue());
        assertEquals("\"Path\"", cycle.getPathColumnName());
    }

    @Test
    void preservesImplicitBooleanDefaultsAndMultipleCtes() throws Exception {
        Select select = (Select) TestUtils.assertSqlCanBeParsedAndDeparsed(CTE
                + "CYCLE id SET is_cycle USING path, chosen AS (SELECT * FROM walk) SELECT * FROM chosen",
                true);
        assertNull(select.getWithItemsList().get(0).getCycleClause().getMarkValue());
        assertNull(select.getWithItemsList().get(0).getCycleClause().getMarkDefault());
        assertNull(select.getWithItemsList().get(1).getCycleClause());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CYCLE id SET flag", "CYCLE id SET flag TO 'Y' USING path",
            "CYCLE id SET flag DEFAULT 'N' USING path", "CYCLE SET flag USING path"})
    void rejectsIncompleteCycleClauses(String clause) {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse(CTE + clause + " SELECT * FROM walk"));
    }

    @Test
    void visitsCycleExpressions() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse(CTE
                + "CYCLE id SET flag TO 'Y' DEFAULT 'N' USING path SELECT * FROM walk");
        List<String> values = new ArrayList<>();
        select.accept(new SelectVisitorAdapter<Void>(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(StringValue value, S context) {
                values.add(value.getValue());
                return null;
            }
        }), null);
        assertEquals(List.of("Y", "N"), values);
    }

    @ParameterizedTest
    @ValueSource(strings = {"TO upper('Y') DEFAULT 'N'", "TO 'Y' DEFAULT lower('N')"})
    void validatesCycleMarkExpressionsAlongsideTheCteBody(String markValues) {
        String sql = CTE + "CYCLE id SET flag " + markValues + " USING path SELECT * FROM walk";
        assertTrue(
                Validation.validate(List.of(new FeaturesAllowed(Feature.values())), sql).isEmpty());

        FeaturesAllowed allowed = new FeaturesAllowed(Feature.values()).remove(Feature.function);
        List<ValidationError> errors = Validation.validate(List.of(allowed), sql);
        assertEquals(1, errors.size());
        assertNotNull(errors.get(0).getParsedStatement());
        assertEquals(1, errors.get(0).getErrors().size());
        assertEquals("function not allowed.",
                errors.get(0).getErrors().iterator().next().getMessage());
    }

    @Test
    void sharesSearchAndCycleRenderingWithExpressionDeparser() throws Exception {
        Select select = (Select) CCJSqlParserUtil.parse(CTE
                + "SEARCH DEPTH FIRST BY id SET seq CYCLE id SET flag TO 'Y' DEFAULT 'N' USING path SELECT * FROM walk");
        StringBuilder sql = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                getBuilder().append("changed_").append(column.getColumnName());
                return getBuilder();
            }

            @Override
            public <S> StringBuilder visit(StringValue value, S context) {
                getBuilder().append("'changed_").append(value.getValue()).append("'");
                return getBuilder();
            }
        };
        select.accept(new StatementDeParser(expressions, new SelectDeParser(), sql), null);
        assertTrue(sql.toString().contains(
                "SEARCH DEPTH FIRST BY changed_id SET seq CYCLE changed_id SET flag TO 'changed_Y' DEFAULT 'changed_N' USING path"),
                sql.toString());
    }
}
