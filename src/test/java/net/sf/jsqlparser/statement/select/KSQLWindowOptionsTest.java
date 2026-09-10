/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.select;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.select.KSQLWindow.Duration;
import net.sf.jsqlparser.statement.select.KSQLWindow.TimeUnit;
import net.sf.jsqlparser.statement.select.PlainSelect.EmitMode;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

class KSQLWindowOptionsTest {
    private PlainSelect roundTrip(String sql) throws Exception {
        PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(sql);
        StringBuilder builder = new StringBuilder();
        select.accept(new StatementDeParser(builder), null);
        assertEquals(select.toString(), builder.toString());
        assertEquals(select.toString(), CCJSqlParserUtil.parse(builder.toString()).toString());
        return select;
    }

    @ParameterizedTest
    @EnumSource(TimeUnit.class)
    void joinsAcceptAllTimeUnitsAndPreserveBrackets(TimeUnit unit) throws Exception {
        for (boolean brackets : List.of(false, true)) {
            String within = brackets ? "(1 " + unit + ")" : "1 " + unit;
            PlainSelect select = roundTrip("SELECT a.id FROM a INNER JOIN b WITHIN " + within
                    + " GRACE PERIOD 0 SECONDS ON a.id = b.id EMIT CHANGES");
            KSQLJoinWindow window = select.getJoins().get(0).getJoinWindow();
            assertEquals(1, window.getDuration());
            assertEquals(unit, window.getTimeUnit());
            assertEquals(brackets, window.isUsingBrackets());
            assertEquals(0, window.getGracePeriod().getValue());
            assertEquals(TimeUnit.SECONDS, window.getGracePeriod().getTimeUnit());
            assertTrue(select.isEmitChanges());
        }
    }

    @ParameterizedTest
    @EnumSource(TimeUnit.class)
    void aggregationWindowsShareTimeUnitsAndGrace(TimeUnit unit) throws Exception {
        for (String shape : List.of("TUMBLING (SIZE 20 ", "SESSION (20 ",
                "HOPPING (SIZE 20 ")) {
            String spec = shape + unit
                    + (shape.startsWith("HOPPING") ? ", ADVANCE BY 5 " + unit : "")
                    + ", GRACE PERIOD 2 " + unit + ")";
            PlainSelect select =
                    roundTrip("SELECT item_id, SUM(quantity) FROM orders WINDOW " + spec
                            + " GROUP BY item_id EMIT FINAL LIMIT 10");
            KSQLWindow window = select.getKsqlWindow();
            assertEquals(20, window.getSizeDuration());
            assertEquals(unit, window.getSizeTimeUnit());
            assertEquals(2, window.getGracePeriod().getValue());
            assertEquals(unit, window.getGracePeriod().getTimeUnit());
            assertEquals(EmitMode.FINAL, select.getEmitMode());
            assertFalse(select.isEmitChanges());
            if (shape.startsWith("HOPPING")) {
                assertEquals(5, window.getAdvanceDuration());
                assertEquals(unit, window.getAdvanceTimeUnit());
            }
        }
    }

    @Test
    void preservesAsymmetricWindowsAndFollowingJoins() throws Exception {
        PlainSelect select = roundTrip(
                "SELECT a.id FROM a JOIN b WITHIN (1 HOUR, 5 MINUTES) GRACE PERIOD 2 SECONDS ON a.id = b.id JOIN c WITHIN 10 SECONDS ON c.id = a.id EMIT CHANGES");
        assertEquals(2, select.getJoins().size());
        KSQLJoinWindow window = select.getJoins().get(0).getJoinWindow();
        assertTrue(window.isBeforeAfterWindow());
        assertEquals(1, window.getBeforeDuration());
        assertEquals(TimeUnit.HOUR, window.getBeforeTimeUnit());
        assertEquals(5, window.getAfterDuration());
        assertEquals(TimeUnit.MINUTES, window.getAfterTimeUnit());
        assertNull(select.getJoins().get(1).getJoinWindow().getGracePeriod());
    }

    @Test
    void parsesOriginalReproducers() throws Exception {
        roundTrip(
                "select count(*) from log_data_testtopic_1b138324 t1 inner join TestTopic t2 WITHIN 1 MINUTES on t1.email = t2.email EMIT CHANGES");
        PlainSelect select = roundTrip(
                "SELECT count(*), sum('id'), avg('id') FROM TestTopic WINDOW TUMBLING (SIZE 60 SECONDS) EMIT FINAL");
        assertEquals(EmitMode.FINAL, select.getEmitMode());
    }

    @Test
    void retainsLegacyBuilderSemanticsAndAllowsMutation() throws Exception {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse("SELECT * FROM t");
        assertEquals(EmitMode.NONE, select.getEmitMode());
        select.setEmitChanges(true);
        assertEquals(EmitMode.CHANGES, select.getEmitMode());
        assertTrue(select.toString().endsWith("EMIT CHANGES"));
        select.setEmitMode(EmitMode.FINAL);
        assertTrue(select.toString().endsWith("EMIT FINAL"));
        select.setEmitChanges(false);
        assertEquals("SELECT * FROM t", select.toString());
        KSQLJoinWindow window = new KSQLJoinWindow().withDuration(5).withTimeUnit(TimeUnit.MINUTES);
        assertEquals("(5 MINUTES)", window.toString());
        window.withUsingBrackets(false).withGracePeriod(new Duration(0, TimeUnit.SECONDS));
        assertEquals("5 MINUTES GRACE PERIOD 0 SECONDS", window.toString());
        window.setGracePeriod(null);
        assertEquals("5 MINUTES", window.toString());
        KSQLWindow aggregate =
                new KSQLWindow().withSizeDuration(10).withSizeTimeUnit(TimeUnit.SECONDS)
                        .withGracePeriod(new Duration(2, TimeUnit.MINUTES));
        assertEquals("TUMBLING (SIZE 10 SECONDS, GRACE PERIOD 2 MINUTES)", aggregate.toString());
        aggregate.setGracePeriod(null);
        assertEquals("TUMBLING (SIZE 10 SECONDS)", aggregate.toString());
        assertThrows(IllegalArgumentException.class, () -> new Duration(-1, TimeUnit.SECONDS));
    }

    @Test
    void keepsFollowingStatementsAndKeywordIdentifiers() throws Exception {
        Statements statements = CCJSqlParserUtil.parseStatements(
                "SELECT item_id, COUNT(*) FROM orders WINDOW TUMBLING (SIZE 1 HOUR) GROUP BY item_id EMIT FINAL; SELECT grace, period FROM t;");
        assertEquals(2, statements.size());
        assertEquals("SELECT grace, period FROM t", statements.get(1).toString());
        roundTrip("SELECT * FROM a JOIN b within ON a.id = within.id");
        roundTrip("SELECT * FROM a JOIN b AS within ON a.id = within.id");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT * FROM a JOIN b WITHIN 1 BANANAS ON a.id = b.id",
            "SELECT * FROM a JOIN b WITHIN 1 HOURS GRACE 2 MINUTES ON a.id = b.id",
            "SELECT * FROM a JOIN b WITHIN 1 HOURS, 2 HOURS ON a.id = b.id",
            "SELECT * FROM t WINDOW TUMBLING (SIZE 1 HOUR, GRACE PERIOD -1 SECONDS)",
            "SELECT * FROM t WINDOW TUMBLING (SIZE 1 HOUR, GRACE PERIOD 1 SECOND, GRACE PERIOD 2 SECONDS)",
            "SELECT * FROM t EMIT FINAL CHANGES"})
    void rejectsMalformedWindowOptions(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
    }
}
