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

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.expression.DateUnitExpression;
import net.sf.jsqlparser.expression.DateUnitExpression.DateUnit;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

class OdbcTimestampIntervalTest {
    @ParameterizedTest
    @ValueSource(strings = {"SQL_TSI_FRAC_SECOND", "SQL_TSI_SECOND", "SQL_TSI_MINUTE",
            "SQL_TSI_HOUR", "SQL_TSI_DAY", "SQL_TSI_WEEK", "SQL_TSI_MONTH",
            "SQL_TSI_QUARTER", "SQL_TSI_YEAR"})
    void recognizesStandardIntervalsForBothFunctions(String unit) throws Exception {
        for (String name : List.of("TIMESTAMPADD", "TIMESTAMPDIFF")) {
            String sql = "SELECT {fn " + name + "(" + unit + ", 2, travel_date)} FROM t";
            for (boolean complex : new boolean[] {false, true}) {
                PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(sql, true,
                        parser -> parser.withAllowComplexParsing(complex));
                Function function = (Function) select.getSelectItem(0).getExpression();
                DateUnitExpression interval = assertInstanceOf(DateUnitExpression.class,
                        function.getParameters().get(0));
                assertEquals(DateUnit.from(unit), interval.getType());
                assertEquals(unit, interval.toString());
                assertNotNull(interval.getASTNode());
                assertEquals(select.toString(),
                        CCJSqlParserUtil.parse(select.toString()).toString());
                List<String> columns = new ArrayList<>();
                function.accept(new ExpressionVisitorAdapter<Void>() {
                    @Override
                    public <S> Void visit(Column column, S context) {
                        columns.add(column.getColumnName());
                        return null;
                    }
                }, null);
                assertEquals(List.of("travel_date"), columns);
            }
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "TIMESTAMPADD(SQL_TSI_YEAR, 2, travel_date)",
            "{fn other(SQL_TSI_YEAR, 2, travel_date)}",
            "{fn schema.TIMESTAMPADD(SQL_TSI_YEAR, 2, travel_date)}",
            "{fn TIMESTAMPADD(t.SQL_TSI_YEAR, 2, travel_date)}",
            "{fn TIMESTAMPADD(\"SQL_TSI_YEAR\", 2, travel_date)}",
            "{fn TIMESTAMPADD(SQL_TSI_UNKNOWN, 2, travel_date)}",
            "{fn TIMESTAMPADD(SQL_TSI_YEAR, travel_date)}"
    })
    void preservesIdentifiersOutsideTheOdbcIntervalPosition(String expression) throws Exception {
        PlainSelect select =
                (PlainSelect) CCJSqlParserUtil.parse("SELECT " + expression + " FROM t");
        Function function = (Function) select.getSelectItem(0).getExpression();
        assertInstanceOf(Column.class, function.getParameters().get(0));
    }

    @Test
    void handlesCaseAndNestedEscapesWithoutChangingOtherArguments() throws Exception {
        String sql = "SELECT {fn timestampdiff(sql_tsi_year, {fn CURDATE()}, SQL_TSI_YEAR)} FROM t";
        PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(sql, true, null);
        Function function = (Function) select.getSelectItem(0).getExpression();
        assertInstanceOf(DateUnitExpression.class, function.getParameters().get(0));
        assertInstanceOf(Function.class, function.getParameters().get(1));
        assertInstanceOf(Column.class, function.getParameters().get(2));
        assertNull(DateUnit.fromOdbcInterval(null));
        assertNull(DateUnit.fromOdbcInterval("YEAR"));
        assertNull(DateUnit.fromOdbcInterval("SQL_TSI_UNKNOWN"));
    }
}
