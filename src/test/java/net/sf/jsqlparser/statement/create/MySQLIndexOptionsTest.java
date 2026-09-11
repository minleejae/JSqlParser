/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.drop.Drop;
import net.sf.jsqlparser.statement.lock.LockStatement;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.deparser.CreateIndexDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class MySQLIndexOptionsTest {
    @Test
    void parsesAlgorithmAndLockOptionsInEitherOrder() throws Exception {
        for (String algorithm : List.of("DEFAULT", "INPLACE", "COPY")) {
            for (String lock : List.of("DEFAULT", "NONE", "SHARED", "EXCLUSIVE")) {
                for (String equals : List.of(" ", " = ")) {
                    String algorithmOption = "ALGORITHM" + equals + algorithm;
                    String lockOption = "LOCK" + equals + lock;
                    for (String options : List.of(algorithmOption + " " + lockOption,
                            lockOption + " " + algorithmOption)) {
                        for (String statement : List.of("CREATE INDEX idx ON t (id) ",
                                "DROP INDEX idx ON t ")) {
                            TestUtils.assertSqlCanBeParsedAndDeparsed(statement + options, true);
                        }
                    }
                }
            }
        }
    }

    @Test
    void preservesLegacyOptionTokensAndTheirOrder() throws Exception {
        CreateIndex create = (CreateIndex) CCJSqlParserUtil.parse(
                "CREATE INDEX idx ON t (id) LOCK EXCLUSIVE ALGORITHM = DEFAULT");
        assertEquals(List.of("LOCK", "EXCLUSIVE", "ALGORITHM", "=", "DEFAULT"),
                create.getTailParameters());
        Drop drop = (Drop) CCJSqlParserUtil.parse(
                "DROP INDEX idx ON t LOCK = DEFAULT ALGORITHM COPY");
        assertEquals(List.of("ON", "t", "LOCK", "=", "DEFAULT", "ALGORITHM", "COPY"),
                drop.getParameters());
        assertEquals(create.toString(), CCJSqlParserUtil.parse(create.toString()).toString());
        assertEquals(drop.toString(), CCJSqlParserUtil.parse(drop.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATE INDEX idx ON t (id)", "DROP INDEX idx ON t"})
    void keepsFollowingLockStatementSeparate(String index) throws Exception {
        Statements statements = CCJSqlParserUtil.parseStatements(
                index + "; LOCK TABLE t IN SHARE MODE");
        assertEquals(2, statements.size());
        assertInstanceOf(LockStatement.class, statements.get(1));
        net.sf.jsqlparser.parser.CCJSqlParser parser = CCJSqlParserUtil.newParser(
                index + " LOCK TABLE t IN SHARE MODE");
        parser.SingleStatement();
        assertEquals(net.sf.jsqlparser.parser.CCJSqlParserConstants.K_LOCK,
                parser.getNextToken().kind);
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATE INDEX idx ON t (id) LOCK", "DROP INDEX idx ON t LOCK =",
            "CREATE INDEX idx ON t (id) ALGORITHM =", "DROP INDEX idx ON t ALGORITHM"})
    void rejectsMissingOptionValues(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE INDEX idx ON t (id) LOCK EXCLUSIVE ALGORITHM DEFAULT",
            "CREATE INDEX idx USING BTREE ON t (id) KEY_BLOCK_SIZE = 8 COMMENT 'test' INVISIBLE",
            "CREATE INDEX idx ON t (id) parallel compress nologging",
            "CREATE UNIQUE INDEX CONCURRENTLY IF NOT EXISTS idx ON ONLY t USING btree (id DESC) INCLUDE (value) NULLS NOT DISTINCT WITH (fillfactor = 80) TABLESPACE fast WHERE active"
    })
    void sharesRenderingForMySqlPostgreSqlAndLegacyTails(String sql) throws Exception {
        CreateIndex create = (CreateIndex) TestUtils.assertSqlCanBeParsedAndDeparsed(sql, true);
        create.getIndex().setName("renamed");
        StringBuilder deparsed = new StringBuilder("prefix ");
        new CreateIndexDeParser(deparsed).deParse(create);
        assertEquals("prefix " + create, deparsed.toString());
        assertEquals(create.toString(), CCJSqlParserUtil.parse(create.toString()).toString());
    }
}
