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

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BooleanValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.create.view.CreateView;
import net.sf.jsqlparser.statement.create.view.ViewOption;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * @see <a href="https://www.postgresql.org/docs/18/sql-createview.html">PostgreSQL CREATE VIEW</a>
 */
class PostgreSqlCreateViewTest {
    @Test
    void testSecurityOptions() throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE VIEW visible_accounts WITH (security_barrier = true, security_invoker = false)"
                        + " AS SELECT * FROM accounts WHERE enabled WITH LOCAL CHECK OPTION");
        assertThat(view.getOptions()).hasSize(2);
        assertThat(view.getOptions().get(0).getKind()).isEqualTo(ViewOption.Kind.SECURITY_BARRIER);
        assertThat(view.getOptions().get(0).getBooleanValue()).isTrue();
        assertThat(view.getOptions().get(1).getBooleanValue()).isFalse();
        assertThat(view.getCheckOption()).isEqualTo(CreateView.CheckOption.LOCAL);
        assertThat(TablesNamesFinder.findTables(view.toString()))
                .containsExactlyInAnyOrder("visible_accounts", "accounts");
    }

    @Test
    void testImplicitOptionValueAndCheckOptionParameter() throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE VIEW visible_accounts WITH (security_barrier, check_option = 'local')"
                        + " AS SELECT * FROM accounts");
        assertThat(view.getOptions().get(0).getValue()).isNull();
        assertThat(view.getOptions().get(0).getBooleanValue()).isTrue();
        assertThat(view.getOptions().get(1).getCheckOption())
                .isEqualTo(CreateView.CheckOption.LOCAL);
        assertThat(view.getCheckOption()).isNull();
    }

    @Test
    void testDefaultCheckOption() throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE VIEW visible_accounts AS SELECT * FROM accounts WITH CHECK OPTION");
        assertThat(view.getCheckOption()).isEqualTo(CreateView.CheckOption.DEFAULT);
        assertThat(view.getEffectiveCheckOption()).isEqualTo(CreateView.CheckOption.CASCADED);
    }

    @Test
    void testRecursiveView() throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE RECURSIVE VIEW numbers(n) AS VALUES (1)"
                        + " UNION ALL SELECT n + 1 FROM numbers WHERE n < 5");
        assertThat(view.isRecursive()).isTrue();
        assertThat(view.getColumnNames()).hasSize(1);
    }

    @Test
    void testMaterializedViewParameters() throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE MATERIALIZED VIEW IF NOT EXISTS account_totals(id) USING heap"
                        + " WITH (fillfactor = 70) TABLESPACE archive"
                        + " AS SELECT id FROM accounts WITH NO DATA");
        assertThat(view.isMaterialized()).isTrue();
        assertThat(view.isIfNotExists()).isTrue();
        assertThat(view.isIfNotExistsAfterViewName()).isFalse();
        assertThat(view.getAccessMethod()).isEqualTo("heap");
        assertThat(view.getStorageParameters().get(0).getName()).isEqualTo("fillfactor");
        assertThat(view.getTableSpace()).isEqualTo("archive");
        assertThat(view.getWithData()).isFalse();
        assertThat(view.getOptions()).isNull();
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " WITH DATA", " WITH NO DATA"})
    void testMaterializedViewDataMode(String suffix) throws JSQLParserException {
        CreateView view = (CreateView) assertSqlCanBeParsedAndDeparsed(
                "CREATE MATERIALIZED VIEW account_totals AS SELECT * FROM accounts" + suffix);
        assertThat(view.getWithData()).isEqualTo(suffix.isEmpty() ? null : !suffix.contains("NO"));
        CreateView reparsed = (CreateView) CCJSqlParserUtil.parse(view.toString());
        assertThat(reparsed.getWithData()).isEqualTo(view.getWithData());
    }

    @Test
    void testProgrammaticConstruction() throws JSQLParserException {
        CreateView view = new CreateView().withView(new Table("visible_accounts"))
                .withSelect((Select) CCJSqlParserUtil.parse("SELECT * FROM accounts"));
        view.setOptions(Collections.singletonList(
                new ViewOption("security_barrier", new BooleanValue(true), true)));
        view.setCheckOption(CreateView.CheckOption.CASCADED);
        CreateView parsed = (CreateView) assertSqlCanBeParsedAndDeparsed(view.toString());
        assertThat(parsed.getOptions().get(0).getBooleanValue()).isTrue();
        assertThat(parsed.getCheckOption()).isEqualTo(CreateView.CheckOption.CASCADED);
    }
}
