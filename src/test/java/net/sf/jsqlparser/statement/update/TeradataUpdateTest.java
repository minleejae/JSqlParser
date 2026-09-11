/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.update;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TeradataUpdateTest {
    private static Update parse(String sql) throws JSQLParserException {
        return (Update) CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.TERADATA));
    }

    @Test
    void parsesIssue891AndFindsSourceTables() throws Exception {
        String sql = "UPDATE a FROM db1.table1 a, db2.tabl2 b SET a.column1 = b.column1 "
                + "WHERE a.column2 = b.column2";
        Update update = (Update) assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.TERADATA));
        assertTrue(update.isFromBeforeSet());
        assertTrue(update.isTargetTableAlias());
        assertEquals("a", update.getTable().getName());
        assertEquals(1, update.getJoins().size());
        assertEquals(Set.of("db1.table1", "db2.tabl2"), new TablesNamesFinder().getTables(update));
        assertEquals(sql, parse(update.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "UPDATE t FROM source s SET id = s.id",
            "UPDATE b FROM source a, target b SET b.id = a.id WHERE a.id > 1",
            "UPDATE \"target\" FROM \"schema\".\"table\" AS \"target\" SET id = 1",
            "UPDATE t SET id = 1"})
    void roundTripsTargetsAndOptionalFrom(String sql) throws Exception {
        Statement statement = assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.TERADATA));
        assertEquals(statement.toString(), parse(statement.toString()).toString());
    }

    @Test
    void gatesClauseOrderAndRejectsDuplicates() throws Exception {
        String sql = "UPDATE t FROM s SET id = 1";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.TERADATA) {
                assertThrows(JSQLParserException.class,
                        () -> CCJSqlParserUtil.parse(sql, p -> p.withDialect(dialect)));
            }
        }
        assertThrows(JSQLParserException.class, () -> parse(sql + " FROM other"));
        assertThrows(JSQLParserException.class, () -> parse("UPDATE t FROM SET id = 1"));
        Update legacy = (Update) assertSqlCanBeParsedAndDeparsed("UPDATE t SET id = 1 FROM s");
        assertFalse(legacy.isFromBeforeSet());
        legacy.setFromBeforeSet(true);
        assertEquals(sql, legacy.toString());
        assertEquals(2, CCJSqlParserUtil.parseStatements(sql + "; SELECT 1;",
                p -> p.withDialect(Dialect.TERADATA)).size());
    }

    @Test
    void customDeparserKeepsFromPosition() throws Exception {
        Update update = parse("UPDATE t FROM s SET id = 1 WHERE s.id > 2");
        StringBuilder builder = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 10);
            }
        };
        update.accept(new StatementDeParser(expressions, new SelectDeParser(), builder));
        assertEquals("UPDATE t FROM s SET id = 11 WHERE s.id > 12", builder.toString());
        assertEquals(builder.toString(), parse(builder.toString()).toString());
        update.withFromBeforeSet(false);
        assertEquals("UPDATE t SET id = 1 FROM s WHERE s.id > 2", update.toString());
    }
}
