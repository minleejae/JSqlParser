/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.ValidationContext;
import net.sf.jsqlparser.util.validation.ValidationTestAsserts;
import net.sf.jsqlparser.util.validation.feature.PostgresqlVersion;
import net.sf.jsqlparser.util.validation.feature.SqlServerVersion;
import net.sf.jsqlparser.util.validation.metadata.DatabaseMetaDataValidation;
import net.sf.jsqlparser.util.validation.metadata.Named;
import net.sf.jsqlparser.util.validation.metadata.NamedObject;
import net.sf.jsqlparser.util.validation.validator.StatementValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SetIdentityInsertTest {
    private SetIdentityInsertStatement parse(String sql) throws JSQLParserException {
        return (SetIdentityInsertStatement) CCJSqlParserUtil.parse(sql,
                p -> p.withDialect(Dialect.SQLSERVER).withUnsupportedStatements(false));
    }

    private void assertRoundTrip(SetIdentityInsertStatement set, String sql) throws Exception {
        assertEquals(sql, set.toString());
        StringBuilder buffer = new StringBuilder();
        set.accept(new StatementDeParser(buffer), null);
        assertEquals(sql, buffer.toString());
        SetIdentityInsertStatement reparsed = parse(buffer.toString());
        assertEquals(set.isOn(), reparsed.isOn());
        assertEquals(set.getTable().getFullyQualifiedName(),
                reparsed.getTable().getFullyQualifiedName());
    }

    @ParameterizedTest
    @ValueSource(strings = {"language", "country", "city", "address", "actor", "staff",
            "store", "category", "film", "inventory", "customer", "rental", "payment"})
    void parsesSakilaIdentityDirectives(String table) throws Exception {
        for (boolean on : new boolean[] {true, false}) {
            String sql = "SET IDENTITY_INSERT " + table + (on ? " ON" : " OFF");
            SetIdentityInsertStatement set = parse(sql);
            assertEquals(table, set.getTable().getName());
            assertEquals(on, set.isOn());
            assertRoundTrip(set, sql);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"dbo.actor", "sakila.dbo.actor", "sakila..actor",
            "[sakila].[dbo].[order]", "[table with spaces]", "#staging"})
    void reusesQualifiedTableNames(String table) throws Exception {
        SetIdentityInsertStatement set = parse("set identity_insert " + table + " on");
        assertEquals(table, set.getTable().getFullyQualifiedName());
        assertEquals(Collections.singleton(table), new TablesNamesFinder().getTables(set));
        assertRoundTrip(set, "SET IDENTITY_INSERT " + table + " ON");
    }

    @Test
    void exposesEditableTargetAndStateWithoutClaimingDataChanges() throws Exception {
        SetIdentityInsertStatement set = parse("SET IDENTITY_INSERT dbo.actor ON");
        set.getTable().setName("customer");
        set.setOn(false);
        assertRoundTrip(set, "SET IDENTITY_INSERT dbo.customer OFF");
        set.setTable(new Table("replacement"));
        assertRoundTrip(set, "SET IDENTITY_INSERT replacement OFF");
        assertEquals(Collections.singleton(StmtFeature.MODIFIES_SESSION),
                set.getFeatures().getCertain());
        assertTrue(set.getFeatures().getUncertain().isEmpty());
        assertEquals(3, CCJSqlParserUtil.parseStatements(
                "SET IDENTITY_INSERT dbo.actor ON; INSERT INTO dbo.actor VALUES (1); "
                        + "SET IDENTITY_INSERT dbo.actor OFF;",
                p -> p.withDialect(Dialect.SQLSERVER)).size());
    }

    @Test
    void traversesTheTargetWithVisitorContext() throws Exception {
        SetIdentityInsertStatement set = parse("SET IDENTITY_INSERT dbo.actor ON");
        Object context = new Object();
        List<Table> visited = new ArrayList<>();
        FromItemVisitorAdapter<Void> tables = new FromItemVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Table table, S value) {
                assertSame(context, value);
                visited.add(table);
                return null;
            }
        };
        set.accept(new StatementVisitorAdapter<Void>(null, null, null, tables, null, null),
                context);
        assertEquals(Collections.singletonList(set.getTable()), visited);

        StringBuilder buffer = new StringBuilder();
        SelectDeParser selects = new SelectDeParser() {
            @Override
            public <S> StringBuilder visit(Table table, S value) {
                assertSame(context, value);
                return getBuilder().append("mapped_table");
            }
        };
        set.accept(new StatementDeParser(new ExpressionDeParser(), selects, buffer), context);
        assertEquals("SET IDENTITY_INSERT mapped_table ON", buffer.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"SET IDENTITY_INSERT", "SET IDENTITY_INSERT ON",
            "SET IDENTITY_INSERT dbo.actor", "SET IDENTITY_INSERT dbo.actor TRUE",
            "SET IDENTITY_INSERT dbo.actor = ON", "SET IDENTITY_INSERT dbo.actor ON OFF",
            "SET IDENTITY_INSERT server.sakila.dbo.actor ON",
            "SET IDENTITY_INSERT dbo.actor AS a ON", "SET IDENTITY_INSERT @actor ON"})
    void rejectsMalformedDirectives(String sql) {
        assertThrows(JSQLParserException.class, () -> parse(sql));
    }

    @Test
    void preservesGenericSetAndRequiresSqlServerDialect() throws Exception {
        String sql = "SET IDENTITY_INSERT dbo.actor ON";
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.POSTGRESQL)));
        for (String generic : new String[] {"SET IDENTITY_INSERT = 1",
                "SET IDENTITY_INSERT.value = 1", "SET @flag = 1"}) {
            assertInstanceOf(SetStatement.class, CCJSqlParserUtil.parse(generic,
                    p -> p.withDialect(Dialect.SQLSERVER)));
        }
    }

    @Test
    void validatesTheCapabilityAndExistingTargetTable() throws Exception {
        SetIdentityInsertStatement set = parse("SET IDENTITY_INSERT dbo.actor OFF");
        List<Named> visited = new ArrayList<>();
        DatabaseMetaDataValidation metadata = name -> {
            visited.add(name);
            return true;
        };
        StatementValidator validator = new StatementValidator();
        validator.setContext(new ValidationContext().setCapabilities(
                Arrays.asList(SqlServerVersion.V2019, PostgresqlVersion.V10, metadata)));
        set.accept(validator, null);
        assertFalse(validator.getValidationErrors().containsKey(SqlServerVersion.V2019));
        assertFalse(validator.getValidationErrors().containsKey(metadata));
        ValidationTestAsserts.assertNotSupported(
                validator.getValidationErrors().get(PostgresqlVersion.V10),
                Feature.setIdentityInsert);
        assertEquals(1, visited.size());
        assertEquals(NamedObject.table, visited.get(0).getNamedObject());
        assertEquals("dbo.actor", visited.get(0).getFqn());
    }
}
