/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2025 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.piped;

import static org.junit.jupiter.api.Assertions.assertEquals;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PipeExpressionDeParserTest {
    @Test
    void customPrinterVisitsEveryValueOnceAcrossThePipeline() throws Exception {
        String sql = "FROM t |> SELECT 1 AS a, 'secret' AS b |> EXTEND a + 2 AS c "
                + "|> SET a = 3, b = 'hidden' |> LIMIT 4 OFFSET 5";
        var statement = CCJSqlParserUtil.parse(sql);
        String original = statement.toString();
        List<Long> visited = new ArrayList<>();
        List<String> strings = new ArrayList<>();
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                visited.add(value.getValue());
                return getBuilder().append(value.getValue() + 100);
            }

            @Override
            public <S> StringBuilder visit(StringValue value, S context) {
                strings.add(value.getValue());
                return getBuilder().append("'masked'");
            }
        };
        statement.accept(new StatementDeParser(expressions, new SelectDeParser(), output), null);
        assertEquals(Arrays.asList(1L, 2L, 3L, 4L, 5L), visited);
        assertEquals(Arrays.asList("secret", "hidden"), strings);
        assertEquals("FROM t\n|> SELECT 101 AS a, 'masked' AS b\n"
                + "|> EXTEND a + 102 AS c\n|> SET a = 103, b = 'masked'\n"
                + "|> LIMIT 104 OFFSET 105", output.toString());
        assertEquals(original, statement.toString());
        assertEquals(output.toString(), CCJSqlParserUtil.parse(output.toString()).toString());
    }

    @ParameterizedTest
    @ValueSource(strings = {"FROM t |> SELECT DISTINCT a + 1 AS b, a * 2 AS c",
            "FROM t |> EXTEND COALESCE(a, 1) AS b",
            "FROM t |> SET a = 1, b = (SELECT 2)",
            "FROM t |> SET (a, b) = (1, 2)",
            "FROM t |> LIMIT 1", "FROM t |> LIMIT 1 OFFSET 2"})
    void defaultPrinterPreservesAliasesSubqueriesAndAssignmentBrackets(String sql)
            throws Exception {
        var statement = CCJSqlParserUtil.parse(sql);
        StringBuilder output = new StringBuilder();
        statement.accept(new StatementDeParser(output), null);
        assertEquals(statement.toString(), output.toString());
        assertEquals(statement.toString(), CCJSqlParserUtil.parse(output.toString()).toString());
    }
}
