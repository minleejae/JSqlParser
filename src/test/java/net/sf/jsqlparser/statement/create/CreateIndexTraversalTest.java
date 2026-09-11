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

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;

class CreateIndexTraversalTest {
    private static final String SQL = "CREATE INDEX ix ON public.t "
            + "((id + 1) public.test_ops (option = 2)) WITH (fillfactor = 80) WHERE id > 3";

    @Test
    void visitsKeyOptionStorageAndPredicateExpressionsOnceWithContext() throws Exception {
        CreateIndex statement = (CreateIndex) CCJSqlParserUtil.parse(SQL,
                parser -> parser.withDialect(Dialect.POSTGRESQL));
        List<Long> values = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(LongValue value, S context) {
                assertEquals("context", context);
                values.add(value.getValue());
                return null;
            }
        };
        statement.accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)),
                "context");
        assertEquals(List.of(1L, 2L, 80L, 3L), values);
        assertEquals(Set.of("public.t"), new TablesNamesFinder().getTables(statement));
    }

    @Test
    void deparsesAllExpressionsThroughTheCustomVisitor() throws Exception {
        CreateIndex statement = (CreateIndex) CCJSqlParserUtil.parse(SQL,
                parser -> parser.withDialect(Dialect.POSTGRESQL));
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 10);
            }
        };
        statement.accept(new StatementDeParser(expressions, new SelectDeParser(), output));
        assertEquals("CREATE INDEX ix ON public.t "
                + "((id + 11) public.test_ops (option = 12)) WITH (fillfactor = 90) WHERE id > 13",
                output.toString());
        assertEquals(SQL, statement.toString());
        assertEquals(output.toString(), CCJSqlParserUtil.parse(output.toString(),
                parser -> parser.withDialect(Dialect.POSTGRESQL)).toString());
    }
}
