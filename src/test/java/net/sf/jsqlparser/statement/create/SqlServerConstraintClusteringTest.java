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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.Alter;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.NamedConstraint;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class SqlServerConstraintClusteringTest {
    private static Index constraint(Statement statement) {
        return statement instanceof CreateTable
                ? ((CreateTable) statement).getIndexes().get(0)
                : ((Alter) statement).getAlterExpressions().get(0).getIndex();
    }

    private static Statement parse(String sql) throws JSQLParserException {
        return CCJSqlParserUtil.parse(sql, parser -> parser.withDialect(Dialect.SQLSERVER));
    }

    @Test
    void parsesSakilaReproducerIssue1589() throws Exception {
        String sql = "CREATE TABLE actor (actor_id INT NOT NULL IDENTITY, "
                + "first_name VARCHAR (45) NOT NULL, last_name VARCHAR (45) NOT NULL, "
                + "last_update DATETIME NOT NULL, PRIMARY KEY NONCLUSTERED (actor_id))";
        Index index = constraint(TestUtils.assertSqlCanBeParsedAndDeparsed(sql, true,
                parser -> parser.withDialect(Dialect.SQLSERVER)));
        assertEquals(Index.Clustering.NONCLUSTERED, index.getClustering());
        assertNull(((NamedConstraint) index).getIndexName());
        assertEquals(List.of("actor_id"), index.getColumnsNames());
    }

    @ParameterizedTest
    @EnumSource(Index.Clustering.class)
    void sharesClusteringAcrossCreateAndAlter(Index.Clustering clustering) throws Exception {
        for (String type : List.of("PRIMARY KEY", "UNIQUE")) {
            for (String name : List.of("", "CONSTRAINT [key name] ")) {
                String definition = name + type + " " + clustering + " (id)";
                for (String sql : List.of("CREATE TABLE t (id INT, " + definition + ")",
                        "ALTER TABLE t ADD " + definition)) {
                    Statement statement = TestUtils.assertSqlCanBeParsedAndDeparsed(sql, true,
                            parser -> parser.withDialect(Dialect.SQLSERVER));
                    Index index = constraint(statement);
                    assertEquals(clustering, index.getClustering(), sql);
                    assertEquals(type, index.getType());
                    assertEquals(name.isEmpty() ? null : "[key name]", index.getName());
                    if (index instanceof NamedConstraint) {
                        assertNull(((NamedConstraint) index).getIndexName());
                    }
                    StringBuilder output = new StringBuilder();
                    statement.accept(new StatementDeParser(output), null);
                    assertEquals(clustering, constraint(parse(output.toString())).getClustering());
                    assertEquals(clustering,
                            constraint(parse(statement.toString())).getClustering());
                }
            }
        }
    }

    @Test
    void preservesNamesOutsideSqlServerAndQuotedNames() throws Exception {
        for (String type : List.of("PRIMARY KEY", "UNIQUE")) {
            String sql = "CREATE TABLE t (id INT, " + type + " NONCLUSTERED (id))";
            NamedConstraint defaultIndex =
                    (NamedConstraint) constraint(CCJSqlParserUtil.parse(sql));
            assertNull(defaultIndex.getClustering());
            assertEquals("NONCLUSTERED", defaultIndex.getIndexName());
            for (Dialect dialect : Dialect.values()) {
                if (dialect != Dialect.SQLSERVER) {
                    NamedConstraint index = (NamedConstraint) constraint(CCJSqlParserUtil.parse(sql,
                            parser -> parser.withDialect(dialect)));
                    assertNull(index.getClustering(), dialect.name());
                    assertEquals("NONCLUSTERED", index.getIndexName());
                }
            }
            NamedConstraint quoted = (NamedConstraint) constraint(
                    parse("CREATE TABLE t (id INT, " + type + " [NONCLUSTERED] (id))"));
            assertNull(quoted.getClustering());
            assertEquals("[NONCLUSTERED]", quoted.getIndexName());
        }
    }

    @Test
    void supportsMutationAndLeavesOmittedOptionUnspecified() throws Exception {
        Statement statement = parse("CREATE TABLE t (id INT, PRIMARY KEY (id))");
        Index index = constraint(statement);
        assertNull(index.getClustering());
        index.setClustering(Index.Clustering.CLUSTERED);
        TestUtils.assertDeparse(statement, "CREATE TABLE t (id INT, PRIMARY KEY CLUSTERED (id))");
        index.setClustering(null);
        TestUtils.assertDeparse(statement, "CREATE TABLE t (id INT, PRIMARY KEY (id))");
        NamedConstraint built = new NamedConstraint().withType("UNIQUE").withName("uq_t")
                .withClustering(Index.Clustering.NONCLUSTERED).withColumnsNames(List.of("id"));
        assertEquals("CONSTRAINT uq_t UNIQUE NONCLUSTERED (id)", built.toString());
    }

    @Test
    void keepsFollowingConstraintsAndRejectsDuplicateModifiers() throws Exception {
        CreateTable table = (CreateTable) parse("CREATE TABLE t (id INT, other_id INT, "
                + "PRIMARY KEY NONCLUSTERED (id), UNIQUE (other_id))");
        assertNull(table.getIndexes().get(1).getClustering());
        for (String sql : List.of(
                "CREATE TABLE t (id INT, PRIMARY KEY NONCLUSTERED CLUSTERED (id))",
                "ALTER TABLE t ADD CONSTRAINT pk PRIMARY KEY CLUSTERED NONCLUSTERED (id)",
                "ALTER TABLE t ADD UNIQUE NONCLUSTERED")) {
            assertThrows(JSQLParserException.class, () -> parse(sql));
        }
    }
}
