/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.schema;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.UnsupportedStatement;
import net.sf.jsqlparser.statement.delete.Delete;
import net.sf.jsqlparser.statement.insert.Insert;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.update.Update;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.feature.DatabaseType;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import net.sf.jsqlparser.util.validation.metadata.DatabaseMetaDataValidation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class TableVariableTest {
    private Statement roundtrip(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql, p -> p.withUnsupportedStatements(false));
        assertThat(statement).isNotInstanceOf(UnsupportedStatement.class);
        StringBuilder text = new StringBuilder();
        statement.accept(new StatementDeParser(text), null);
        assertThat(text.toString()).isEqualTo(statement.toString());
        assertThat(CCJSqlParserUtil.parse(text.toString()).toString())
                .isEqualTo(statement.toString());
        return statement;
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT columnName FROM @table", "SELECT v.* FROM @table AS v",
            "SELECT v.id FROM @table v JOIN dbo.source s ON v.id = s.id",
            "SELECT * FROM (SELECT id FROM @table) t",
            "WITH t AS (SELECT id FROM @table) SELECT * FROM t",
            "INSERT INTO @table VALUES (1)", "INSERT INTO @table (id) SELECT id FROM source",
            "UPDATE @table SET id = 2", "UPDATE t SET id = 3 FROM @table AS t",
            "DELETE FROM @table", "DELETE @table WHERE id = 1",
            "DELETE t FROM @table AS t",
            "MERGE INTO @table AS t USING source AS s ON t.id = s.id "
                    + "WHEN MATCHED THEN UPDATE SET t.id = s.id"})
    void supportsTableVariablesInQueriesAndDml(String sql) throws JSQLParserException {
        roundtrip(sql);
        assertThat(Validation.validate(List.of(new FeaturesAllowed(Feature.values())), sql))
                .isEmpty();
    }

    @Test
    void retainsTypedReferencesInExistingDmlModels() throws JSQLParserException {
        Table from = (Table) ((PlainSelect) roundtrip("SELECT id FROM @t AS v")).getFromItem();
        Table inserted = ((Insert) roundtrip("INSERT INTO @t VALUES (1)")).getTable();
        Table updated = ((Update) roundtrip("UPDATE @t SET id = 2")).getTable();
        Table deleted = ((Delete) roundtrip("DELETE FROM @t")).getTable();
        for (Table table : List.of(from, inserted, updated, deleted)) {
            assertThat(table.isTableVariable()).isTrue();
            assertThat(table.getName()).isEqualTo("@t");
            assertThat(table.getSchemaName()).isNull();
            assertThat(table.getASTNode()).isNotNull();
            Table copy = table.clone();
            assertThat(copy.isTableVariable()).isTrue();
            assertThat(copy.getName()).isEqualTo("@t");
            table.setUnsetCatalogAndSchema("catalog", "schema");
            assertThat(table.getFullyQualifiedName()).isEqualTo("@t");
        }
        assertThat(from.getAlias().getName()).isEqualTo("v");
    }

    @Test
    void separatesLocalSourcesFromCatalogTables() throws JSQLParserException {
        String sql = "SELECT v.id FROM @t v JOIN dbo.source s ON v.id = s.id";
        assertThat(TablesNamesFinder.findTables(sql)).containsExactly("dbo.source");
        assertThat(TablesNamesFinder.findTablesOrOtherSources(sql))
                .containsExactlyInAnyOrder("@t", "dbo.source");
        assertThat(TablesNamesFinder.findTables("INSERT INTO @t SELECT id FROM source"))
                .containsExactly("source");
        assertThat(TablesNamesFinder.findTables("UPDATE @t SET id = 2")).isEmpty();
        assertThat(TablesNamesFinder.findTables("DELETE FROM @t")).isEmpty();
    }

    @Test
    void skipsOnlyLocalTableMetadataLookups() {
        List<String> checkedNames = new ArrayList<>();
        DatabaseMetaDataValidation metadata = named -> {
            checkedNames.add(named.getFqn());
            return true;
        };
        assertThat(Validation.validate(List.of(FeaturesAllowed.SELECT, metadata),
                "SELECT 1 FROM @t CROSS JOIN dbo.source")).isEmpty();
        assertThat(checkedNames).containsExactly("dbo.source");
        checkedNames.clear();
        assertThat(Validation.validate(List.of(new FeaturesAllowed(Feature.values()), metadata),
                "INSERT INTO @t VALUES (1)", "DELETE FROM @t")).isEmpty();
        assertThat(checkedNames).isEmpty();
    }

    @Test
    void validatesTheDialectFeature() {
        assertThat(Validation.validate(List.of(DatabaseType.SQLSERVER), "SELECT id FROM @t"))
                .isEmpty();
        assertThat(Validation.validate(List.of(FeaturesAllowed.SELECT), "SELECT id FROM @t"))
                .isEmpty();
        assertThat(Validation.validate(List.of(new FeaturesAllowed(Feature.select)),
                "SELECT id FROM @t").toString()).contains("tableVariable not allowed");
        assertThat(Validation.validate(List.of(DatabaseType.POSTGRESQL), "SELECT id FROM @t")
                .toString()).contains("tableVariable not supported");
    }

    @Test
    void preservesScalarVariablesDeclarationsAndFollowingStatements() throws JSQLParserException {
        PlainSelect select = (PlainSelect) roundtrip("SELECT @value FROM @t");
        assertThat(select.getSelectItem(0).getExpression()).isInstanceOf(UserVariable.class);
        Statements statements = CCJSqlParserUtil.parseStatements(
                "DECLARE @t TABLE (id INT); INSERT INTO @t VALUES (1); "
                        + "UPDATE @t SET id = 2; SELECT id FROM @t; DELETE FROM @t; SELECT 42;");
        assertThat(statements).hasSize(6);
        assertThat(statements.get(5).toString()).isEqualTo("SELECT 42");
        assertThat(CCJSqlParserUtil.parseStatements(statements.toString())).hasSize(6);
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM dbo.t", "SELECT * FROM #temp",
            "SELECT * FROM \"@catalog_table\"", "UPDATE t USE INDEX (idx) SET id = 1",
            "CREATE INDEX idx ON t (id)", "SELECT id INTO copy FROM source"})
    void preservesCatalogTableGrammar(String sql) throws JSQLParserException {
        Statement statement = roundtrip(sql);
        if (statement instanceof PlainSelect) {
            assertThat(((Table) ((PlainSelect) statement).getFromItem()).isTableVariable())
                    .isFalse();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT * FROM @@global", "SELECT * FROM dbo.@t",
            "SELECT id INTO @t FROM source",
            "CREATE INDEX idx ON @t (id)", "ALTER TABLE @t ADD value INT",
            "DROP TABLE @t", "TRUNCATE TABLE @t", "SELECT @t.* FROM @t"})
    void rejectsCatalogOnlyAndMalformedReferences(String sql) {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse(sql, p -> p.withUnsupportedStatements(false)));
    }

    @Test
    void doesNotAddCreateTableVariableSyntax() throws JSQLParserException {
        // CREATE's existing fallback returns UnsupportedStatement even in strict mode.
        assertThat(CCJSqlParserUtil.parse("CREATE TABLE @t (id INT)",
                p -> p.withUnsupportedStatements(false))).isInstanceOf(UnsupportedStatement.class);
    }
}
