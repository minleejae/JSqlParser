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
import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static net.sf.jsqlparser.util.validation.ValidationTestAsserts.validateNoErrors;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.StmtFeature;
import net.sf.jsqlparser.statement.alter.AlterPublication;
import net.sf.jsqlparser.statement.alter.AlterSubscription;
import net.sf.jsqlparser.statement.create.publication.CreatePublication;
import net.sf.jsqlparser.statement.create.publication.PublicationOption;
import net.sf.jsqlparser.statement.create.publication.PublicationTable;
import net.sf.jsqlparser.statement.create.publication.PublicationTarget;
import net.sf.jsqlparser.statement.create.subscription.CreateSubscription;
import net.sf.jsqlparser.statement.create.subscription.SubscriptionOption;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.feature.DatabaseType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class PostgreSqlReplicationDdlTest {
    static Stream<String> statements() throws Exception {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                PostgreSqlReplicationDdlTest.class
                        .getResourceAsStream("/postgresql/replication-ddl.sql"),
                StandardCharsets.UTF_8))) {
            return reader.lines()
                    .filter(line -> !line.trim().isEmpty() && !line.trim().startsWith("--"))
                    .collect(Collectors.toList()).stream();
        }
    }

    @ParameterizedTest
    @MethodSource("statements")
    void testRoundTripAndValidation(String sql) throws Exception {
        Statement statement = assertSqlCanBeParsedAndDeparsed(sql);
        assertThat(statement).isInstanceOfAny(CreatePublication.class, AlterPublication.class,
                CreateSubscription.class, AlterSubscription.class);
        assertThat(CCJSqlParserUtil.parse(statement.toString()).toString())
                .isEqualTo(statement.toString());
        assertThat(statement.getFeatures().getCertain()).contains(StmtFeature.MODIFIES_SCHEMA)
                .doesNotContain(StmtFeature.RETURNS_RESULT_SET);
        validateNoErrors(sql, 1, DatabaseType.POSTGRESQL);
    }

    @Test
    void testPublicationTargets() throws Exception {
        CreatePublication publication = (CreatePublication) CCJSqlParserUtil.parse(
                "CREATE PUBLICATION mixed FOR TABLE ONLY app.users WHERE (id > 0), orders *, TABLES IN SCHEMA app, audit, TABLE events");
        assertThat(publication.isAllTables()).isFalse();
        assertThat(publication.getTargets()).extracting(PublicationTarget::getKind)
                .containsExactly(PublicationTarget.Kind.TABLE,
                        PublicationTarget.Kind.TABLES_IN_SCHEMA, PublicationTarget.Kind.TABLE);
        PublicationTable first = publication.getTargets().get(0).getTables().get(0);
        assertThat(first.isOnly()).isTrue();
        assertThat(first.getWhere()).isNotNull();
        assertThat(publication.getTargets().get(0).getTables().get(1).isIncludeDescendants())
                .isTrue();
        assertThat(publication.getTargets().get(1).getSchemas()).containsExactly("app", "audit");
        assertThat(TablesNamesFinder.findTables(publication.toString()))
                .containsExactlyInAnyOrder("app.users", "orders", "events");
        assertThat(TablesNamesFinder.findTables("CREATE PUBLICATION p FOR ALL TABLES")).isEmpty();
        assertThat(TablesNamesFinder.findTables("CREATE PUBLICATION p FOR TABLES IN SCHEMA app"))
                .isEmpty();
    }

    @Test
    void testPublicationOptions() throws Exception {
        CreatePublication publication = (CreatePublication) CCJSqlParserUtil.parse(
                "CREATE PUBLICATION p FOR ALL TABLES WITH (publish = 'insert, update', publish_via_partition_root, publish_generated_columns = stored)");
        assertThat(publication.getOptions().get(0).getPublishOperations())
                .containsExactlyInAnyOrder(PublicationOption.Operation.INSERT,
                        PublicationOption.Operation.UPDATE);
        assertThat(publication.getOptions().get(1).getBooleanValue()).isTrue();
        assertThat(publication.getOptions().get(1).getValue()).isNull();
        assertThat(publication.getOptions().get(2).getGeneratedColumns())
                .isEqualTo(PublicationOption.GeneratedColumns.STORED);
        assertThat(
                ((CreatePublication) CCJSqlParserUtil.parse("CREATE PUBLICATION p")).getOptions())
                .isEmpty();
        publication.setName("new_publication");
        assertThat(CCJSqlParserUtil.parse(publication.toString()).toString())
                .startsWith("CREATE PUBLICATION new_publication");
    }

    @Test
    void testSubscriptionOptionsAndNames() throws Exception {
        CreateSubscription subscription = (CreateSubscription) CCJSqlParserUtil.parse(
                "CREATE SUBSCRIPTION sub CONNECTION 'dbname=app' PUBLICATION first_pub, second_pub WITH (connect = false, streaming = parallel, slot_name = NONE)");
        assertThat(subscription.getConnection().getValue()).isEqualTo("dbname=app");
        assertThat(subscription.getPublications()).containsExactly("first_pub", "second_pub");
        assertThat(subscription.getOptions().get(0).getBooleanValue()).isFalse();
        assertThat(subscription.getOptions().get(1).getStreaming())
                .isEqualTo(SubscriptionOption.Streaming.PARALLEL);
        assertThat(subscription.getOptions().get(2).isSlotNameNone()).isTrue();
        assertThat(new SubscriptionOption("slot_name", SubscriptionOption.Kind.SLOT_NAME,
                new StringValue("NONE")).isSlotNameNone()).isFalse();
        assertThat(TablesNamesFinder.findTables(subscription.toString())).isEmpty();
        assertThat(subscription.getFeatures().getUncertain())
                .doesNotContain(StmtFeature.MODIFIES_DATA);
    }

    @Test
    void testAlterActions() throws Exception {
        AlterPublication publication =
                (AlterPublication) CCJSqlParserUtil.parse("ALTER PUBLICATION p DROP TABLE users");
        assertThat(publication.getAction()).isEqualTo(AlterPublication.Action.DROP);
        assertThat(publication.getTargets().get(0).getTables().get(0).getColumns()).isNull();
        AlterSubscription subscription = (AlterSubscription) CCJSqlParserUtil
                .parse("ALTER SUBSCRIPTION sub SET PUBLICATION p WITH (refresh = false)");
        assertThat(subscription.getAction()).isEqualTo(AlterSubscription.Action.SET_PUBLICATION);
        assertThat(subscription.getOptions().get(0).getKind())
                .isEqualTo(SubscriptionOption.Kind.REFRESH);
        AlterSubscription skip = (AlterSubscription) CCJSqlParserUtil
                .parse("ALTER SUBSCRIPTION sub SKIP (lsn = '0/123')");
        assertThat(skip.getAction()).isEqualTo(AlterSubscription.Action.SKIP);
        assertThat(skip.getOptions().get(0).getValue()).isInstanceOf(StringValue.class);
    }

    @Test
    void testVisitorAndCustomDeparser() throws Exception {
        String sql = "CREATE PUBLICATION p FOR TABLE users (id) WHERE (id > 0)";
        Statement statement = CCJSqlParserUtil.parse(sql);
        List<String> columns = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                assertThat(context).isEqualTo("publication");
                columns.add(column.getColumnName());
                return null;
            }
        };
        statement.accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)),
                "publication");
        assertThat(columns).containsExactly("id", "id");

        StringBuilder output = new StringBuilder();
        ExpressionDeParser deparser = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                return getBuilder().append("new_").append(column.getColumnName());
            }
        };
        statement.accept(new StatementDeParser(deparser, new SelectDeParser(), output));
        assertThat(output.toString()).contains("users (new_id) WHERE (new_id > 0)");
        assertThat(statement.toString()).doesNotContain("new_id");
        CCJSqlParserUtil.parse(output.toString());
    }

    @Test
    void testProgrammaticConstruction() throws Exception {
        CreateSubscription subscription = new CreateSubscription();
        subscription.setName("sub");
        subscription.setConnection(new StringValue("dbname=app"));
        subscription.getPublications().add("pub");
        subscription.getOptions()
                .add(new SubscriptionOption("connect", SubscriptionOption.Kind.CONNECT,
                        CCJSqlParserUtil.parseExpression("false")));
        assertThat(CCJSqlParserUtil.parse(subscription.toString()).toString())
                .isEqualTo(subscription.toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"ALTER SUBSCRIPTION sub SKIP (lsn)",
            "ALTER SUBSCRIPTION sub SKIP (copy_data = false)", "CREATE OR REPLACE PUBLICATION p",
            "CREATE OR REPLACE SUBSCRIPTION sub CONNECTION 'dbname=app' PUBLICATION p",
            "CREATE PUBLICATION p FOR TABLE", "CREATE PUBLICATION p FOR TABLE users ()",
            "CREATE PUBLICATION p FOR TABLE users WHERE id > 0",
            "CREATE PUBLICATION p FOR TABLE ONLY users *",
            "ALTER PUBLICATION p DROP TABLE users (id)",
            "ALTER PUBLICATION p DROP TABLE users WHERE (id > 0)",
            "CREATE SUBSCRIPTION sub PUBLICATION p",
            "CREATE SUBSCRIPTION sub CONNECTION 'dbname=app' PUBLICATION",
            "ALTER SUBSCRIPTION sub SET ()", "CREATE PUBLICATION p WITH (unknown = true)"})
    void testInvalidSyntax(String sql) {
        assertThatThrownBy(() -> CCJSqlParserUtil.parse(sql))
                .isInstanceOf(JSQLParserException.class);
    }
}
