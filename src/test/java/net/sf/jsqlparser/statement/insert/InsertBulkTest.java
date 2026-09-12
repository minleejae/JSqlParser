/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.insert;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.parser.feature.FeatureConfiguration;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import net.sf.jsqlparser.util.validation.feature.SqlServerVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class InsertBulkTest {
    private static InsertBulk parse(String sql) throws Exception {
        return (InsertBulk) CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.SQLSERVER));
    }

    @Test
    void parsesIssue2033() throws Exception {
        String sql = "INSERT BULK tpch.dbo.order_line ([ol_o_id] int, [ol_d_id] tinyint, "
                + "[ol_w_id] int, [ol_number] tinyint, [ol_i_id] int, [ol_delivery_d] datetime, "
                + "[ol_amount] smallmoney, [ol_supply_w_id] int, [ol_quantity] smallint, "
                + "[ol_dist_info] char(24) COLLATE Chinese_PRC_CI_AS) WITH (ROWS_PER_BATCH = 500000)";
        InsertBulk statement = (InsertBulk) assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.SQLSERVER));
        assertEquals(10, statement.getColumns().size());
        assertEquals("char(24)",
                statement.getColumns().get(9).getColDataType().toString().replace(" ", ""));
        assertEquals(List.of("COLLATE", "Chinese_PRC_CI_AS"),
                statement.getColumns().get(9).getColumnSpecs());
        assertEquals(InsertBulk.Option.Kind.ROWS_PER_BATCH,
                statement.getOptions().get(0).getKind());
        assertEquals("500000", statement.getOptions().get(0).getValue().toString());
        assertEquals(Set.of("tpch.dbo.order_line"), new TablesNamesFinder().getTables(statement));
        assertTrue(statement.getFeatures().modifiesData());
        assertFalse(statement.getFeatures().modifiesSchema());
        assertFalse(statement.getFeatures().returnsResultSet());
        assertEquals(statement.toString(), parse(statement.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " WITH (TABLOCK)",
            " WITH (CHECK_CONSTRAINTS, FIRE_TRIGGERS, KEEP_NULLS, ALLOW_ENCRYPTED_VALUE_MODIFICATIONS)",
            " WITH (ORDER ([id] ASC, [name] DESC), ROWS_PER_BATCH = 100)"})
    void supportsDriverOptionsAndColumnTypes(String options) throws Exception {
        String sql = "INSERT BULK [db].[dbo].[items] ([id] decimal(18, 2), [name] nvarchar(max))"
                + options;
        InsertBulk statement = (InsertBulk) assertSqlCanBeParsedAndDeparsed(sql, true,
                p -> p.withDialect(Dialect.SQLSERVER));
        assertEquals(statement.toString(), parse(statement.toString()).toString());
    }

    @Test
    void exposesMutableOptionsToVisitorsAndDeparsers() throws Exception {
        InsertBulk statement =
                parse("INSERT BULK t (id int) WITH (ROWS_PER_BATCH = 100, ORDER (id ASC))");
        List<Long> values = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(LongValue value, S context) {
                assertEquals("context", context);
                values.add(value.getValue());
                return null;
            }
        };
        statement.accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)),
                "context");
        assertEquals(List.of(100L), values);
        statement.getColumns().get(0).getColDataType().setDataType("bigint");
        statement.getOptions().get(0).setValue(new LongValue(200));
        statement.getOptions().get(1).getOrderByElements().get(0).setAsc(false);
        assertEquals("INSERT BULK t (id bigint) WITH (ROWS_PER_BATCH = 200, ORDER (id DESC))",
                statement.toString());
        StringBuilder output = new StringBuilder();
        ExpressionDeParser deparser = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 1);
            }
        };
        statement.accept(new StatementDeParser(deparser, new SelectDeParser(), output));
        assertEquals(statement.toString().replace("200", "201"), output.toString());
        assertEquals(output.toString(), parse(output.toString()).toString());
    }

    @Test
    void gatesDialectAndRetainsOrdinaryInserts() throws Exception {
        String sql = "INSERT BULK t (id int)";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        for (Dialect dialect : Dialect.values()) {
            if (dialect != Dialect.SQLSERVER) {
                assertThrows(JSQLParserException.class,
                        () -> CCJSqlParserUtil.parse(sql, p -> p.withDialect(dialect)));
            }
        }
        assertSqlCanBeParsedAndDeparsed("INSERT INTO bulk (id) VALUES (1)", true,
                p -> p.withDialect(Dialect.SQLSERVER));
        assertSqlCanBeParsedAndDeparsed("INSERT INTO [bulk] (id) VALUES (1)", true,
                p -> p.withDialect(Dialect.SQLSERVER));
        assertEquals(2, CCJSqlParserUtil.parseStatements(sql + "; SELECT 1;",
                p -> p.withDialect(Dialect.SQLSERVER)).size());
    }

    @ParameterizedTest
    @ValueSource(strings = {"INSERT BULK t ()", "INSERT BULK t (id)",
            "INSERT BULK t (id int) VALUES (1)",
            "INSERT BULK t (id int) WITH ()", "INSERT BULK t (id int) WITH (UNKNOWN_OPTION)",
            "INSERT BULK t (id int) WITH (TABLOCK = 1)",
            "INSERT BULK t (id int) WITH (TABLOCK, TABLOCK)",
            "INSERT BULK t (id int) WITH (ROWS_PER_BATCH)",
            "INSERT BULK t (id int) WITH (ORDER ())"})
    void rejectsMalformedBulkDeclarations(String sql) {
        assertThrows(JSQLParserException.class, () -> parse(sql));
    }

    @Test
    void validatesBulkCapability() {
        FeatureConfiguration config = CCJSqlParserUtil.newParser("SELECT 1")
                .withDialect(Dialect.SQLSERVER).getConfiguration();
        assertTrue(new Validation(config, List.of(SqlServerVersion.values()[0]),
                "INSERT BULK t (id int)")
                .validate().isEmpty());
        assertFalse(new Validation(config, List.of(new FeaturesAllowed(Feature.insert)),
                "INSERT BULK t (id int)").validate().isEmpty());
    }
}
