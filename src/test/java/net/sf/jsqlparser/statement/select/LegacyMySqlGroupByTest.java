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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParser;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.GroupByDeParser;
import net.sf.jsqlparser.util.validation.ValidationContext;
import net.sf.jsqlparser.util.validation.feature.MySqlVersion;
import net.sf.jsqlparser.util.validation.validator.GroupByValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

class LegacyMySqlGroupByTest {
    private static final Consumer<CCJSqlParser> LEGACY =
            parser -> parser.withDialect(Dialect.MYSQL).withLegacyMySqlGroupBy(true);

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT a FROM b GROUP BY c DESC",
            "SELECT COUNT(*) FROM t GROUP BY a ASC, b, c DESC",
            "SELECT COUNT(*) FROM t GROUP BY (a + b) DESC, LOWER(c) ASC WITH ROLLUP",
            "SELECT COUNT(*) FROM t GROUP BY a DESC HAVING COUNT(*) > 1 ORDER BY a ASC",
            "SELECT COUNT(*) FROM t GROUP BY a, b WITH ROLLUP",
            "SELECT COUNT(*) FROM t GROUP BY (a, b)"
    })
    void parsesAndDeparsesLegacyGrouping(String sql) throws Exception {
        for (boolean complex : new boolean[] {false, true}) {
            Consumer<CCJSqlParser> config = parser -> {
                LEGACY.accept(parser);
                parser.withAllowComplexParsing(complex);
            };
            PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(sql, true, config);
            assertEquals(select.toString(),
                    CCJSqlParserUtil.parse(select.toString(), config).toString());
        }
    }

    @Test
    void requiresBothMysqlDialectAndExplicitOptIn() {
        String sql = "SELECT a FROM b GROUP BY c DESC";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql,
                parser -> parser.withDialect(Dialect.MYSQL)));
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql,
                parser -> parser.withLegacyMySqlGroupBy(true)));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.MYSQL) {
                assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql,
                        parser -> parser.withDialect(dialect).withLegacyMySqlGroupBy(true)));
            }
        }
    }

    @Test
    void keepsExpressionsVisibleAndDirectionsDistinct() throws Exception {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(
                "SELECT COUNT(*) FROM t GROUP BY a ASC, a DESC, b", LEGACY);
        GroupByElement group = select.getGroupBy();
        assertInstanceOf(Column.class, group.getGroupByExpressionList().get(0));
        assertEquals(GroupByElement.SortDirection.ASC, group.getGroupBySortDirection(0));
        assertEquals(GroupByElement.SortDirection.DESC, group.getGroupBySortDirection(1));
        assertNull(group.getGroupBySortDirection(2));
        List<String> columns = new ArrayList<>();
        new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                columns.add(column.getColumnName());
                return null;
            }
        }.visit(group, null);
        assertEquals(List.of("a", "a", "b"), columns);

        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                return getBuilder().append("renamed_").append(column.getColumnName());
            }
        };
        expressions.setBuilder(output);
        new GroupByDeParser(expressions, output).deParse(group);
        assertEquals("GROUP BY renamed_a ASC, renamed_a DESC, renamed_b", output.toString());

        group.addGroupByExpressions(new Column("extra"));
        assertEquals("GROUP BY a ASC, a DESC, b, extra", group.toString());
        group.setGroupByExpressions(new ExpressionList<>(new Column("replacement")));
        assertFalse(group.hasGroupBySortDirections());
        assertEquals("GROUP BY replacement", group.toString());
    }

    @Test
    void validatesOrderingSeparatelyFromOrdinaryGrouping() throws Exception {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(
                "SELECT a FROM t GROUP BY a DESC", LEGACY);
        GroupByValidator<Void> validator = new GroupByValidator<>();
        validator.setContext(new ValidationContext()
                .setCapabilities(Collections.singleton(MySqlVersion.V8_0)));
        validator.validate(select.getGroupBy());
        assertTrue(validator.getValidationErrors().values().stream()
                .flatMap(errors -> errors.stream())
                .anyMatch(error -> error.getMessage().contains("selectGroupByOrdering")));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT a FROM t GROUP BY a DESC ASC",
            "SELECT a FROM t GROUP BY a DESC,",
            "SELECT a FROM t GROUP BY a NULLS FIRST"
    })
    void rejectsMalformedOrdering(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql, LEGACY));
    }
}
