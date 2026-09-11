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

import java.util.Arrays;
import java.util.Collections;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.SetStatement.OnOffOption;
import net.sf.jsqlparser.statement.SetStatement.OnOffOptions;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SetStatementDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.ValidationContext;
import net.sf.jsqlparser.util.validation.ValidationTestAsserts;
import net.sf.jsqlparser.util.validation.feature.PostgresqlVersion;
import net.sf.jsqlparser.util.validation.feature.SqlServerVersion;
import net.sf.jsqlparser.util.validation.validator.SetStatementValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class SqlServerSetOptionsTest {
    private SetStatement parse(String sql) throws JSQLParserException {
        return (SetStatement) CCJSqlParserUtil.parse(sql,
                p -> p.withDialect(Dialect.SQLSERVER).withUnsupportedStatements(false));
    }

    private void assertRoundTrip(SetStatement set, String sql) throws Exception {
        assertEquals(sql, set.toString());
        StringBuilder buffer = new StringBuilder();
        set.accept(new StatementDeParser(buffer), null);
        assertEquals(sql, buffer.toString());
        assertEquals(sql, parse(buffer.toString()).toString());
    }

    @ParameterizedTest
    @EnumSource(OnOffOption.class)
    void parsesBothStatesAsOptions(OnOffOption option) throws Exception {
        for (boolean on : new boolean[] {true, false}) {
            String sql = "SET " + option + (on ? " ON" : " OFF");
            SetStatement set = parse(sql);
            assertEquals(Collections.singletonList(option), set.getOnOffOptions().getOptions());
            assertEquals(on, set.getOnOffOptions().isOn());
            assertEquals(0, set.getCount());
            assertRoundTrip(set, sql);
        }
    }

    @Test
    void retainsGroupedOptionsAndEditsTheirSharedValue() throws Exception {
        SetStatement set = parse("set quoted_identifier, /* group */ ansi_nulls on");
        assertEquals(Arrays.asList(OnOffOption.QUOTED_IDENTIFIER, OnOffOption.ANSI_NULLS),
                set.getOnOffOptions().getOptions());
        set.getOnOffOptions().getOptions().set(0, OnOffOption.NOCOUNT);
        set.getOnOffOptions().setOn(false);
        assertRoundTrip(set, "SET NOCOUNT, ANSI_NULLS OFF");
        assertTrue(new TablesNamesFinder().getTables(set).isEmpty());
        assertEquals(3, CCJSqlParserUtil.parseStatements(
                "SET NOCOUNT ON; SET XACT_ABORT OFF; SELECT 1;",
                p -> p.withDialect(Dialect.SQLSERVER)).size());
        Object context = new Object();
        assertSame(context, set.accept(new StatementVisitorAdapter<Object>() {
            @Override
            public <S> Object visit(SetStatement statement, S value) {
                assertSame(set, statement);
                return value;
            }
        }, context));
    }

    @ParameterizedTest
    @ValueSource(strings = {"SET NOCOUNT", "SET NOCOUNT TRUE", "SET NOCOUNT 1",
            "SET NOCOUNT, ON", "SET NOCOUNT,, ANSI_NULLS ON", "SET NOCOUNT, unknown ON",
            "SET NOCOUNT ON OFF", "SET NOCOUNT ON, ANSI_NULLS OFF"})
    void rejectsIncompleteOrMixedOptionGroups(String sql) {
        assertThrows(JSQLParserException.class, () -> parse(sql));
    }

    @Test
    void retainsGenericAssignmentsAndDialectIsolation() throws Exception {
        for (String sql : new String[] {"SET NOCOUNT = 1", "SET NOCOUNT.value = 1",
                "SET v = 1, c = 3", "SET v = 1, 3", "SET @Flag = 1",
                "SET LOCAL Time Zone 'UTC'", "SET standard_conforming_strings = on"}) {
            SetStatement set = parse(sql);
            assertNull(set.getOnOffOptions());
            assertEquals(CCJSqlParserUtil.parse(sql).toString(), set.toString());
            assertRoundTrip(set, set.toString());
        }
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse("SET NOCOUNT ON"));
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse("SET NOCOUNT ON",
                p -> p.withDialect(Dialect.POSTGRESQL)));
        SetStatement generic = (SetStatement) CCJSqlParserUtil.parse("SET NOCOUNT OFF");
        assertNull(generic.getOnOffOptions());
        assertEquals(1, generic.getCount());
    }

    @Test
    void sharesPunctuationWithoutBypassingCustomExpressionDeparsers() throws Exception {
        SetStatement set = parse("SET v = 1, 3, c = 5");
        StringBuilder buffer = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 10);
            }
        };
        expressions.setBuilder(buffer);
        new SetStatementDeParser(expressions, buffer).deParse(set);
        assertEquals("SET v = 11, 13, c = 15", buffer.toString());
    }

    @Test
    void switchesBetweenAssignmentAndOptionForms() throws Exception {
        SetStatement set = parse("SET LOCAL x = 1");
        set.setOnOffOptions(new OnOffOptions(Collections.singleton(OnOffOption.NOCOUNT), true));
        assertNull(set.getEffectParameter());
        assertRoundTrip(set, "SET NOCOUNT ON");
        set.add("x", new ExpressionList<>(new LongValue(2)), true);
        assertNull(set.getOnOffOptions());
        assertRoundTrip(set, "SET x = 2");
        set.setOnOffOptions(new OnOffOptions(Collections.singleton(OnOffOption.XACT_ABORT), false));
        set.clear();
        assertNull(set.getOnOffOptions());
        assertEquals(0, set.getCount());
        assertThrows(IllegalArgumentException.class,
                () -> new OnOffOptions(Collections.emptyList(), true));
    }

    @Test
    void validatesSqlServerCapabilitySeparatelyFromGenericSet() throws Exception {
        SetStatement set = parse("SET ANSI_NULLS, QUOTED_IDENTIFIER ON");
        SetStatementValidator validator = new SetStatementValidator();
        validator.setContext(new ValidationContext().setCapabilities(
                Arrays.asList(SqlServerVersion.V2019, PostgresqlVersion.V10)));
        validator.validate(set);
        assertFalse(validator.getValidationErrors().containsKey(SqlServerVersion.V2019));
        ValidationTestAsserts.assertNotSupported(
                validator.getValidationErrors().get(PostgresqlVersion.V10),
                Feature.sqlServerSetOptions);
    }
}
