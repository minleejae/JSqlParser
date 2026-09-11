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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.StatementFeatureVisitor;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementFeatures;
import net.sf.jsqlparser.statement.StmtFeature;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class MySqlSelectIntoVariablesTest {
    private static PlainSelect parse(String sql, Dialect dialect) throws JSQLParserException {
        return (PlainSelect) CCJSqlParserUtil.parse(sql, parser -> parser.withDialect(dialect));
    }

    @ParameterizedTest
    @EnumSource(value = Dialect.class, names = {"MYSQL", "MARIADB"})
    void parsesOriginalReproducerAndPreservesTargets(Dialect dialect) throws Exception {
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(
                "SELECT COUNT(*) INTO @countTotal FROM employee", false,
                parser -> parser.withDialect(dialect));
        assertNull(select.getIntoTables());
        MySqlSelectIntoClause into = select.getMySqlSelectIntoClause();
        assertEquals(MySqlSelectIntoClause.Type.VARIABLES, into.getType());
        assertEquals(MySqlSelectIntoClause.Position.BEFORE_FROM, into.getPosition());
        assertEquals("countTotal", into.getVariables().get(0).getName());
        assertEquals(Set.of("employee"), new TablesNamesFinder<>().getTables((Statement) select));
    }

    @ParameterizedTest
    @EnumSource(value = Dialect.class, names = {"MYSQL", "MARIADB"})
    void preservesPositionsQuotedNamesAndRoundTrips(Dialect dialect) throws Exception {
        for (String variables : List.of("@a, @b", "@`first name`, @'second name'")) {
            for (String sql : List.of("SELECT a, b INTO " + variables + " FROM t",
                    "SELECT a, b FROM t ORDER BY a LIMIT 1 INTO " + variables,
                    "SELECT a, b FROM t FOR UPDATE INTO " + variables)) {
                PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sql,
                        false, parser -> parser.withDialect(dialect));
                MySqlSelectIntoClause into = select.getMySqlSelectIntoClause();
                assertEquals(sql.endsWith("FROM t") ? MySqlSelectIntoClause.Position.BEFORE_FROM
                        : MySqlSelectIntoClause.Position.TRAILING, into.getPosition());
                assertEquals(variables, into.getVariables().toString());
                StringBuilder output = new StringBuilder();
                select.accept(new StatementDeParser(output), null);
                for (String rendered : List.of(select.toString(), output.toString())) {
                    MySqlSelectIntoClause reparsed =
                            parse(rendered, dialect).getMySqlSelectIntoClause();
                    assertEquals(into.getPosition(), reparsed.getPosition());
                    assertEquals(variables, reparsed.getVariables().toString());
                }
            }
        }
        assertEquals("@value", parse("SELECT 1 INTO @value", dialect)
                .getMySqlSelectIntoClause().getVariables().toString());
    }

    @Test
    void requiresMySqlDialectAndKeepsTableTargets() throws Exception {
        String sql = "SELECT a INTO @value FROM t";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.MYSQL && dialect != Dialect.MARIADB) {
                assertThrows(JSQLParserException.class, () -> parse(sql, dialect), dialect.name());
            }
        }
        PlainSelect tableInto = parse("SELECT a INTO target FROM source", Dialect.SQLSERVER);
        assertNull(tableInto.getMySqlSelectIntoClause());
        assertEquals("target", tableInto.getIntoTables().get(0).getName());
    }

    @Test
    void visitsAndRewritesVariables() throws Exception {
        PlainSelect select = parse("SELECT a, b FROM t INTO @x, @y", Dialect.MYSQL);
        List<String> names = new ArrayList<>();
        select.accept(new SelectVisitorAdapter<Void>(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(UserVariable variable, S context) {
                names.add(variable.getName());
                return null;
            }
        }), null);
        assertEquals(List.of("x", "y"), names);
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(UserVariable variable, S context) {
                return getBuilder().append("@renamed_").append(variable.getName());
            }
        };
        SelectDeParser selects = new SelectDeParser(expressions, output);
        expressions.setSelectVisitor(selects);
        expressions.setBuilder(output);
        select.accept((SelectVisitor<StringBuilder>) selects, null);
        assertEquals("SELECT a, b FROM t INTO @renamed_x, @renamed_y", output.toString());
        select.getMySqlSelectIntoClause().getVariables().get(0).setName("new_x");
        TestUtils.assertDeparse(select, "SELECT a, b FROM t INTO @new_x, @y");
    }

    @Test
    void reportsSessionAssignmentInsteadOfTableCreationOrResultSet() throws Exception {
        StatementFeatures features = StatementFeatureVisitor.analyse(
                parse("SELECT a INTO @value FROM t", Dialect.MYSQL));
        assertTrue(features.is(StmtFeature.READS_DATA));
        assertTrue(features.is(StmtFeature.MODIFIES_SESSION));
        assertFalse(features.modifiesSchema());
        assertFalse(features.modifiesData());
        assertFalse(features.returnsResultSet());
    }

    @Test
    void rejectsSystemVariablesMalformedListsAndDuplicateInto() {
        for (String sql : List.of("SELECT 1 INTO @@sql_mode", "SELECT 1 INTO @",
                "SELECT a, b INTO @x, FROM t", "SELECT a INTO @x + 1 FROM t",
                "SELECT a INTO @x FROM t INTO @y", "SELECT a INTO @x INTO target FROM t",
                "SELECT a INTO target FROM t INTO @x",
                "SELECT a INTO @x FROM t INTO OUTFILE '/tmp/result'")) {
            assertThrows(JSQLParserException.class, () -> parse(sql, Dialect.MYSQL), sql);
        }
    }

    @Test
    void keepsStatementBoundaries() throws Exception {
        assertEquals(2, CCJSqlParserUtil.parseStatements(
                "SELECT a INTO @x FROM t; SELECT @x;",
                parser -> parser.withDialect(Dialect.MYSQL)).size());
    }
}
