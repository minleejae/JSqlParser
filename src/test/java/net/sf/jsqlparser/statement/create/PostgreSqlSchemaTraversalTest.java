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

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static net.sf.jsqlparser.util.validation.ValidationTestAsserts.validateNoErrors;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.StmtFeature;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.feature.DatabaseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostgreSqlSchemaTraversalTest {
    @Test
    void testTableFinderIncludesLikeAndReferenceSources() throws JSQLParserException {
        assertThat(TablesNamesFinder.findTables(
                "CREATE TABLE copy (LIKE source INCLUDING ALL, parent_id integer REFERENCES parent (id))"))
                .containsExactlyInAnyOrder("copy", "source", "parent");
        assertThat(TablesNamesFinder.findTables("CREATE TABLE employees OF employee_type"))
                .containsExactly("employees");
        assertThat(TablesNamesFinder.findTables("ALTER SEQUENCE seq OWNED BY app.counters.id"))
                .containsExactly("app.counters");
        assertThat(TablesNamesFinder.findTables("CREATE SEQUENCE seq OWNED BY NONE")).isEmpty();
    }

    @Test
    void testExpressionVisitorReceivesContext() throws JSQLParserException {
        List<String> columns = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                assertThat(context).isEqualTo("context");
                columns.add(column.getColumnName());
                return null;
            }
        };
        StatementVisitorAdapter<Void> visitor =
                new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions));
        CCJSqlParserUtil.parse(
                "CREATE TABLE bookings (room text, EXCLUDE USING gist ((lower(room)) WITH =) WHERE (room <> ''))")
                .accept(visitor, "context");
        CCJSqlParserUtil.parse("ALTER TABLE bookings ALTER COLUMN id TYPE bigint USING id + 1")
                .accept(visitor, "context");
        CCJSqlParserUtil.parse("CREATE VIEW v AS SELECT email FROM contacts")
                .accept(visitor, "context");
        CCJSqlParserUtil.parse("ALTER SEQUENCE seq OWNED BY bookings.id").accept(visitor,
                "context");
        assertThat(columns).containsExactly("room", "room", "id", "email", "id");
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE bookings (room text, EXCLUDE USING gist ((lower(room)) WITH =) WHERE (room <> ''))",
            "ALTER TABLE bookings ALTER COLUMN room TYPE text USING lower(room)",
            "CREATE VIEW v AS SELECT room FROM bookings",
            "CREATE TABLE bookings (room text, CHECK (room <> ''))"})
    void testCustomExpressionDeparser(String sql) throws JSQLParserException {
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                return getBuilder().append("new_").append(column.getColumnName());
            }
        };
        Statement statement = CCJSqlParserUtil.parse(sql);
        statement.accept(new StatementDeParser(expressions, new SelectDeParser(), output));
        assertThat(output.toString()).contains("new_room");
        assertThat(statement.toString()).doesNotContain("new_room");
        assertThat(CCJSqlParserUtil.parse(output.toString()).getClass())
                .isEqualTo(statement.getClass());
    }

    @ParameterizedTest
    @ValueSource(strings = {"CREATE VIEW v AS SELECT * FROM source WITH NO DATA",
            "CREATE MATERIALIZED VIEW v AS SELECT * FROM source WITH CHECK OPTION",
            "CREATE RECURSIVE VIEW v AS SELECT 1",
            "CREATE RECURSIVE VIEW v(n) AS SELECT 1 WITH CHECK OPTION",
            "CREATE TABLE copy (LIKE source INCLUDING UNKNOWN)",
            "CREATE TABLE t (id integer, UNIQUE (id) INITIALLY UNKNOWN)"})
    void testInvalidClauseCombinations(String sql) {
        assertThatThrownBy(() -> CCJSqlParserUtil.parse(sql))
                .isInstanceOf(JSQLParserException.class);
    }

    @Test
    void testMaterializedViewReadEffects() throws JSQLParserException {
        Statement populated =
                CCJSqlParserUtil.parse("CREATE MATERIALIZED VIEW v AS SELECT * FROM source");
        assertThat(populated.getFeatures().getCertain())
                .contains(StmtFeature.MODIFIES_SCHEMA, StmtFeature.READS_DATA)
                .doesNotContain(StmtFeature.RETURNS_RESULT_SET);
        Statement empty = CCJSqlParserUtil
                .parse("CREATE MATERIALIZED VIEW v AS SELECT * FROM source WITH NO DATA");
        assertThat(empty.getFeatures().getCertain()).contains(StmtFeature.MODIFIES_SCHEMA)
                .doesNotContain(StmtFeature.READS_DATA);
        Statement view = CCJSqlParserUtil.parse("CREATE VIEW v AS SELECT * FROM source");
        assertThat(view.getFeatures().getCertain()).doesNotContain(StmtFeature.READS_DATA);
    }

    @Test
    void testPostgreSqlValidation() throws JSQLParserException {
        validateNoErrors("CREATE MATERIALIZED VIEW v AS SELECT * FROM source WITH NO DATA", 1,
                DatabaseType.POSTGRESQL);
        validateNoErrors(
                "CREATE TABLE bookings (room integer, EXCLUDE USING gist ((room + 1) WITH =) WHERE (room > 0))",
                1, DatabaseType.POSTGRESQL);
        validateNoErrors("ALTER TABLE bookings ALTER COLUMN room TYPE bigint USING room + 1", 1,
                DatabaseType.POSTGRESQL);
        validateNoErrors("CREATE SEQUENCE seq OWNED BY bookings.room", 1, DatabaseType.POSTGRESQL);
    }
}
