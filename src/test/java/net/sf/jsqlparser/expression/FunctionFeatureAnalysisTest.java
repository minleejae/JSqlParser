/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementFeatures;
import net.sf.jsqlparser.statement.StmtFeature;
import net.sf.jsqlparser.statement.select.Limit;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.ValueSource;

class FunctionFeatureAnalysisTest {
    static Stream<Arguments> functionForms() {
        return Stream.of(
                Arguments.of("SELECT pg_sleep(1) FROM t", "pg_sleep"),
                Arguments.of("SELECT pg_sleep(1) OVER () FROM t", "pg_sleep"),
                Arguments.of("SELECT SuM(v) OVER () FROM t", "sum"),
                Arguments.of("SELECT analytics.sum(v) OVER () FROM t", "analytics.sum"),
                Arguments.of("SELECT CONVERT(v USING utf8) FROM t", "convert"),
                Arguments.of("SELECT CONVERT(INT, v) FROM t", "convert"),
                Arguments.of("SELECT TRY_CONVERT(INT, v) FROM t", "try_convert"));
    }

    @ParameterizedTest
    @MethodSource("functionForms")
    void appliesTheSamePurityContractToEachFunctionForm(String sql, String name)
            throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        String before = statement.toString();
        StatementFeatures unknown = statement.getFeatures(n -> false);
        assertThat(unknown.getUnresolvedReferences()).containsExactly(name);
        assertThat(unknown.getUncertain())
                .contains(StmtFeature.MODIFIES_DATA, StmtFeature.MODIFIES_SCHEMA);
        assertThat(unknown.modifiesData()).isFalse();
        assertThat(unknown.mayModifyData()).isTrue();

        StatementFeatures pure = statement.getFeatures(name::equals);
        assertThat(pure.getUnresolvedReferences()).isEmpty();
        assertThat(pure.mayModifyData()).isFalse();
        assertThat(statement.toString()).isEqualTo(before);
        assertThat(CCJSqlParserUtil.parse(before).toString()).isEqualTo(before);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT sum(danger(v)) OVER () FROM t",
            "SELECT sum(v) OVER (PARTITION BY danger(k)) FROM t",
            "SELECT sum(v) OVER (ORDER BY danger(k)) FROM t",
            "SELECT sum(v) FILTER (WHERE danger(k) > 0) OVER () FROM t",
            "SELECT array_agg(v ORDER BY danger(k)) OVER () FROM t",
            "SELECT lag(v, danger(k), 0) OVER () FROM t",
            "SELECT lag(v, 1, danger(k)) OVER () FROM t",
            "SELECT sum(v) OVER (ORDER BY k ROWS danger(1) PRECEDING) FROM t",
            "SELECT sum(v) OVER (ORDER BY k ROWS BETWEEN danger(1) PRECEDING AND CURRENT ROW) FROM t",
            "SELECT sum(v) OVER (ORDER BY k ROWS BETWEEN CURRENT ROW AND danger(1) FOLLOWING) FROM t",
            "SELECT sum(v) OVER w FROM t WINDOW w AS (PARTITION BY danger(k))",
            "SELECT sum(v) OVER w FROM t WINDOW w AS (ORDER BY danger(k))",
            "SELECT sum(v) OVER w FROM t WINDOW w AS (ORDER BY k ROWS danger(1) PRECEDING)",
            "SELECT CONVERT(INT, danger(v)) FROM t"})
    void pureOuterFunctionsDoNotHideUnprovenChildren(String sql) throws JSQLParserException {
        StatementFeatures features = CCJSqlParserUtil.parse(sql)
                .getFeatures(Set.of("sum", "array_agg", "lag", "convert")::contains);
        assertThat(features.getUnresolvedReferences()).containsExactly("danger");
        assertThat(features.mayModifyData()).isTrue();
        assertThat(features.modifiesData()).isFalse();
    }

    @Test
    void inlineWindowVisitsBothOrderListsOnceAndKeepsContext() throws JSQLParserException {
        String sql = "SELECT array_agg(arg_fn(v) ORDER BY inner_fn(k)) "
                + "FILTER (WHERE filter_fn(v) > 0) OVER (PARTITION BY part_fn(k) "
                + "ORDER BY outer_fn(k) ROWS BETWEEN start_fn(1) PRECEDING "
                + "AND end_fn(1) FOLLOWING) FROM t";
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(sql);
        List<String> seen = new ArrayList<>();
        Object marker = new Object();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Function function, S context) {
                assertThat(context).isSameAs(marker);
                seen.add(function.getName());
                return super.visit(function, context);
            }

            @Override
            protected <S> Void visitExpressions(Expression expression, S context,
                    Collection<Expression> children) {
                assertThat(context).isSameAs(marker);
                return super.visitExpressions(expression, context, children);
            }
        };
        select.accept(new SelectVisitorAdapter<>(expressions), marker);
        assertThat(seen).containsExactly("arg_fn", "filter_fn", "inner_fn", "part_fn",
                "outer_fn", "start_fn", "end_fn");
    }

    @Test
    void functionModifiersRemainVisibleAfterAnalyticConversion() {
        Function function = new Function().withName("parent").withParameters(new LongValue(1));
        function.setHavingClause(new Function.HavingClause(Function.HavingClause.HavingType.MAX,
                new Function().withName("having_fn")));
        function.setKeywordArguments(List.of(new Function.KeywordArgument("SEPARATOR",
                new Function().withName("keyword_fn"))));
        function.setLimit(new Limit().withRowCount(new Function().withName("limit_fn")));
        for (Expression expression : List.of(function, new AnalyticExpression(function))) {
            PlainSelect select = new PlainSelect();
            select.setSelectItems(List.of(new SelectItem<>(expression)));
            assertThat(select.getFeatures("parent"::equals).getUnresolvedReferences())
                    .containsExactly("having_fn", "keyword_fn", "limit_fn");
        }
    }

    @Test
    void emptyAndUnboundedWindowsContainNoSpuriousFunctions() throws JSQLParserException {
        assertThat(new WindowDefinition().getAllExpressions()).isEmpty();
        Statement statement = CCJSqlParserUtil.parse("SELECT sum(v) OVER "
                + "(ORDER BY k ROWS BETWEEN UNBOUNDED PRECEDING AND CURRENT ROW) FROM t");
        assertThat(statement.getFeatures("sum"::equals).mayModifyData()).isFalse();
    }
}
