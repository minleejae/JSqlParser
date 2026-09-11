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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DorisJoinHintTest {
    private static PlainSelect parse(String sql, Dialect dialect) throws JSQLParserException {
        return (PlainSelect) CCJSqlParserUtil.parse(sql, parser -> parser.withDialect(dialect));
    }

    @Test
    void parsesOriginalReproducerIssue1620() throws Exception {
        String sql = "SELECT * FROM uba.events a LEFT JOIN [shuffle] uba.events b "
                + "ON a.event_id = b.event_id";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sql, false,
                parser -> parser.withDialect(Dialect.DORIS));
        Join join = select.getJoins().get(0);
        assertEquals("shuffle", join.getJoinHint().getKeyword());
        assertEquals(JoinHint.Position.AFTER_JOIN, join.getJoinHint().getPosition());
        assertEquals("uba.events", ((Table) join.getFromItem()).getFullyQualifiedName());
        assertEquals("b", join.getFromItem().getAlias().getName());
        assertEquals(Set.of("uba.events"), new TablesNamesFinder<>().getTables((Statement) select));
    }

    @ParameterizedTest
    @ValueSource(strings = {"shuffle", "broadcast", "SHUFFLE", "BROADCAST"})
    void preservesHintPositionAndRoundTrips(String hint) throws Exception {
        for (String kind : List.of("", "INNER ", "LEFT OUTER ", "RIGHT ", "LEFT SEMI ")) {
            String sql = "SELECT a.id FROM a " + kind + "JOIN [" + hint + "] b USING (id)";
            PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sql, false,
                    parser -> parser.withDialect(Dialect.DORIS));
            StringBuilder output = new StringBuilder();
            select.accept(new StatementDeParser(output), null);
            for (String rendered : List.of(select.toString(), output.toString())) {
                JoinHint reparsed = parse(rendered, Dialect.DORIS).getJoins().get(0).getJoinHint();
                assertEquals(hint, reparsed.getKeyword());
                assertEquals(JoinHint.Position.AFTER_JOIN, reparsed.getPosition());
            }
        }
    }

    @Test
    void keepsHintPerJoinAndSupportsAstMutation() throws Exception {
        PlainSelect select = parse("SELECT * FROM a JOIN [shuffle] b ON a.id = b.id "
                + "JOIN [broadcast] c ON b.id = c.id JOIN d ON c.id = d.id", Dialect.DORIS);
        assertEquals("shuffle", select.getJoins().get(0).getJoinHint().getKeyword());
        assertEquals("broadcast", select.getJoins().get(1).getJoinHint().getKeyword());
        assertNull(select.getJoins().get(2).getJoinHint());
        select.getJoins().get(0)
                .setJoinHint(new JoinHint("broadcast", JoinHint.Position.AFTER_JOIN));
        TestUtils.assertDeparse(select, "SELECT * FROM a JOIN [broadcast] b ON a.id = b.id "
                + "JOIN [broadcast] c ON b.id = c.id JOIN d ON c.id = d.id");
        select.getJoins().get(0).setJoinHint(null);
        assertEquals("JOIN b ON a.id = b.id", select.getJoins().get(0).toString());
    }

    @Test
    void keepsSqlServerHintsAndQuotedTableNames() throws Exception {
        for (String keyword : List.of("LOOP", "HASH", "MERGE", "REMOTE")) {
            String sql = "SELECT * FROM a INNER " + keyword + " JOIN b ON a.id = b.id";
            PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sql);
            assertEquals(JoinHint.Position.BEFORE_JOIN,
                    select.getJoins().get(0).getJoinHint().getPosition());
            assertEquals(keyword, new JoinHint(keyword).toString());
        }
        PlainSelect quoted = parse("SELECT * FROM a JOIN [shuffle] b ON a.id = b.id",
                Dialect.SQLSERVER);
        assertNull(quoted.getJoins().get(0).getJoinHint());
        assertEquals("[shuffle]", ((Table) quoted.getJoins().get(0).getFromItem()).getName());
        PlainSelect dorisTable = parse("SELECT * FROM a JOIN `shuffle` b ON a.id = b.id",
                Dialect.DORIS);
        assertNull(dorisTable.getJoins().get(0).getJoinHint());
    }

    @Test
    void requiresDorisDialect() {
        String sql = "SELECT * FROM a JOIN [shuffle] db.b b ON a.id = b.id";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.DORIS) {
                assertThrows(JSQLParserException.class, () -> parse(sql, dialect), dialect.name());
            }
        }
    }

    @Test
    void rejectsUnknownAndDuplicateHints() {
        for (String sql : List.of("SELECT * FROM a JOIN [unknown] b ON a.id = b.id",
                "SELECT * FROM a JOIN [shuffle, broadcast] b ON a.id = b.id",
                "SELECT * FROM a JOIN [shuffle] [broadcast] b ON a.id = b.id",
                "SELECT * FROM a INNER HASH JOIN [shuffle] b ON a.id = b.id",
                "SELECT * FROM a JOIN [shuffle b ON a.id = b.id")) {
            assertThrows(JSQLParserException.class, () -> parse(sql, Dialect.DORIS), sql);
        }
    }
}
