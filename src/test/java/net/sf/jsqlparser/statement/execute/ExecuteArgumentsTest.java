/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.execute;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.UserVariable;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ExecuteArgumentsTest {
    private Execute roundtrip(String sql) throws JSQLParserException {
        Execute execute = (Execute) CCJSqlParserUtil.parse(sql,
                p -> p.withSquareBracketQuotation(true));
        StringBuilder text = new StringBuilder();
        execute.accept(new StatementDeParser(text), null);
        assertThat(text.toString()).isEqualTo(execute.toString());
        assertThat(CCJSqlParserUtil.parse(text.toString(), p -> p.withSquareBracketQuotation(true))
                .toString()).isEqualTo(execute.toString());
        return execute;
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXEC", "EXECUTE", "CALL"})
    void namedBindsRemainArguments(String command) throws JSQLParserException {
        for (String name : List.of("PCK_ACTION_BY", "dbo.p", "db..p", "srv.db.dbo.p",
                "\"a.b\".\"p\"", "[dbo].[p]")) {
            Execute execute = roundtrip(command + " " + name + " :USER_ID, :GROUP_ID");
            assertThat(execute.getName()).isEqualTo(name);
            assertThat(execute.getExprList()).hasSize(2);
            assertThat(execute.getExprList().get(0)).isInstanceOf(JdbcNamedParameter.class);
            assertThat(((JdbcNamedParameter) execute.getExprList().get(0)).getName())
                    .isEqualTo("USER_ID");
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXECUTE myProc 'foo', @outputVar OUTPUT",
            "EXEC p @result = @value OUTPUT, :other", "EXEC p @value OUT",
            "EXEC p :value OUTPUT", "EXEC p ? OUTPUT"})
    void outputArgumentsHaveTheirOwnModel(String sql) throws JSQLParserException {
        Execute execute = roundtrip(sql);
        assertThat(execute.getExprList()).anySatisfy(expression -> {
            assertThat(expression).isInstanceOf(ExecuteArgument.class);
            assertThat(((ExecuteArgument) expression).isOutput()).isTrue();
        });
        assertThat(Validation.validate(List.of(new FeaturesAllowed(Feature.values())), sql))
                .isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"CALL p ()", "CALL p (:a, :b)", "EXEC p (1 + 2) * 3",
            "EXEC p @param = 'foo', @param2 = 'bar'", "CALL dbo.p", "EXEC p -1, NULL"})
    void legacyArgumentFormsStillRoundtrip(String sql) throws JSQLParserException {
        roundtrip(sql);
    }

    @Test
    void preservesFollowingStatementsAndVisitsOutputVariables() throws JSQLParserException {
        Statements statements = CCJSqlParserUtil.parseStatements(
                "EXEC p @a OUTPUT, @b = @c OUTPUT; SELECT 42;");
        assertThat(statements).hasSize(2);
        assertThat(statements.get(1).toString()).isEqualTo("SELECT 42");
        Execute execute = (Execute) statements.get(0);
        List<String> variables = new ArrayList<>();
        Object marker = new Object();
        execute.getExprList().accept(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(UserVariable variable, S context) {
                assertThat(context).isSameAs(marker);
                variables.add(variable.getName());
                return null;
            }
        }, marker);
        assertThat(variables).containsExactly("a", "b", "c");
        variables.clear();
        execute.getExprList().accept(new net.sf.jsqlparser.util.TablesNamesFinder<Void>() {
            @Override
            public <S> Void visit(UserVariable variable, S context) {
                assertThat(context).isSameAs(marker);
                variables.add(variable.getName());
                return null;
            }
        }, marker);
        assertThat(variables).containsExactly("a", "b", "c");
    }

    @Test
    void customDeparserRetainsTheOutputModifierAndLegacyMutation() throws JSQLParserException {
        Execute execute = roundtrip("EXEC p @a OUTPUT");
        StringBuilder text = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(UserVariable variable, S context) {
                getBuilder().append('@').append(variable.getName()).append("_rewritten");
                return getBuilder();
            }
        };
        expressions.setBuilder(text);
        new net.sf.jsqlparser.util.deparser.ExecuteDeParser(expressions, text).deParse(execute);
        assertThat(text.toString()).isEqualTo("EXEC p @a_rewritten OUTPUT");

        ExecuteArgument argument = (ExecuteArgument) execute.getExprList().get(0);
        argument.setOutput(false);
        argument.setExpression(new JdbcNamedParameter().withName("replacement"));
        execute.setExprList(new ExpressionList<>(argument));
        execute.setName(Arrays.asList("db", null, "renamed"));
        assertThat(execute.toString()).isEqualTo("EXEC db..renamed :replacement");
    }

    @ParameterizedTest
    @ValueSource(strings = {"EXEC p 1 OUTPUT", "EXEC p 'x' OUT", "EXEC p @x = 1 OUTPUT",
            "EXEC p @x OUTPUT OUTPUT", "EXEC p :id,"})
    void rejectsMalformedOutputArguments(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
    }
}
