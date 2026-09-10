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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.select.FromItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.PivotVisitorAdapter;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.Select;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectItemVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class DistinctVisitorTest {
    @ParameterizedTest
    @ValueSource(strings = {"SELECT DISTINCT id FROM foo",
            "SELECT * FROM (SELECT DISTINCT id FROM foo) x",
            "WITH x AS (SELECT DISTINCT id FROM foo) SELECT * FROM x",
            "SELECT DISTINCT ON (id) id FROM foo", "SELECT ALL id FROM foo"})
    void analysisPreservesSelectSyntax(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        String before = statement.toString();
        StatementFeatures features = statement.getFeatures();
        assertThat(features.returnsResultSet()).isTrue();
        assertThat(features.mayModifyData()).isFalse();
        assertThat(features.getUnresolvedReferences()).isEmpty();
        statement.accept(new StatementVisitorAdapter<>(), null);
        assertThat(statement.toString()).isEqualTo(before);
        StringBuilder deparsed = new StringBuilder();
        statement.accept(new StatementDeParser(deparsed), null);
        assertThat(CCJSqlParserUtil.parse(deparsed.toString()).toString()).isEqualTo(before);
    }

    @Test
    void plainDistinctKeepsItsAbsentOnList() throws JSQLParserException {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse("SELECT DISTINCT id FROM foo");
        select.accept(new SelectVisitorAdapter<>(), null);
        assertThat(select.getDistinct().getOnSelectItems()).isNull();
        assertThat(select.toString()).isEqualTo("SELECT DISTINCT id FROM foo");
    }

    @ParameterizedTest
    @ValueSource(strings = {"SELECT DISTINCT unproven(id) FROM foo",
            "SELECT * FROM (SELECT DISTINCT unproven(id) FROM foo) x",
            "WITH x AS (SELECT DISTINCT unproven(id) FROM foo) SELECT * FROM x"})
    void plainDistinctStillReportsUnprovenFunctions(String sql) throws JSQLParserException {
        StatementFeatures features = CCJSqlParserUtil.parse(sql).getFeatures(n -> false);
        assertThat(features.getUnresolvedReferences()).containsExactly("unproven");
        assertThat(features.mayModifyData()).isTrue();
        assertThat(features.modifiesData()).isFalse();
    }

    @Test
    void selectAndOutputItemsKeepCallbackOrderAndContext() throws JSQLParserException {
        List<String> seen = new ArrayList<>();
        Object marker = new Object();
        SelectItemVisitorAdapter<Void> items = new SelectItemVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(SelectItem<? extends Expression> item, S context) {
                assertThat(context).isSameAs(marker);
                seen.add(item.getExpression().toString());
                return null;
            }
        };
        SelectVisitorAdapter<Void> visitor = new SelectVisitorAdapter<>(
                new ExpressionVisitorAdapter<>(), new PivotVisitorAdapter<>(), items,
                new FromItemVisitorAdapter<>());
        Select select = (Select) CCJSqlParserUtil
                .parse("SELECT DISTINCT ON (key_fn(id)) value_fn(id), id FROM foo");
        select.accept(visitor, marker);
        assertThat(seen).containsExactly("key_fn(id)", "value_fn(id)", "id");
        assertThat(select.getFeatures().getUnresolvedReferences())
                .containsExactly("key_fn", "value_fn");

        seen.clear();
        PlainSelect output = (PlainSelect) CCJSqlParserUtil.parse("SELECT a, b");
        visitor.visitOutputClause(new OutputClause(output.getSelectItems(), null, null, null),
                marker);
        assertThat(seen).containsExactly("a", "b");
    }
}
