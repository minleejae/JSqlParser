/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.merge;

import java.util.List;
import java.util.ArrayList;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.Function;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;

import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.ReturningReferenceType;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class PostgreSQLMergeReturningTest {
    private static final String MERGE = "MERGE INTO products p USING incoming i ON p.id = i.id "
            + "WHEN MATCHED THEN UPDATE SET price = i.price "
            + "WHEN NOT MATCHED THEN INSERT (id, price) VALUES (i.id, i.price) ";

    @ParameterizedTest
    @ValueSource(strings = {"RETURNING *", "RETURNING merge_action(), p.id, old.price, new.price",
            "RETURNING WITH (OLD AS o, NEW AS n) o.price, n.price, n.*"})
    void parsesAndReparsesReturning(String returning) throws Exception {
        Merge merge = (Merge) TestUtils.assertSqlCanBeParsedAndDeparsed(MERGE + returning, true);
        assertNotNull(merge.getReturningClause());
        assertEquals(merge.toString(), CCJSqlParserUtil.parse(merge.toString()).toString());
    }

    @Test
    void normalizesOldAndNewReferences() throws Exception {
        Merge merge = (Merge) CCJSqlParserUtil.parse(MERGE
                + "RETURNING WITH (OLD AS o, NEW AS n) o.price, n.price");
        Column oldPrice = merge.getReturningClause().get(0).getExpression(Column.class);
        assertEquals(ReturningReferenceType.OLD, oldPrice.getReturningReferenceType());
        assertNull(oldPrice.getTable());
        assertEquals(ReturningReferenceType.NEW,
                merge.getReturningClause().get(1).getExpression(Column.class)
                        .getReturningReferenceType());
    }

    @Test
    void statementVisitorTraversesReturningExpressions() throws Exception {
        Merge merge = (Merge) CCJSqlParserUtil.parse(MERGE + "RETURNING merge_action()");
        List<String> functions = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions =
                new ExpressionVisitorAdapter<Void>() {
                    @Override
                    public <S> Void visit(Function function, S context) {
                        functions.add(function.getName());
                        return super.visit(function, context);
                    }
                };
        merge.accept(new StatementVisitorAdapter<Void>(
                new SelectVisitorAdapter<>(expressions)), null);
        assertEquals(List.of("merge_action"), functions);
    }

    @Test
    void discoversTablesInReturningSubquery() throws Exception {
        assertTrue(TablesNamesFinder.findTables(MERGE
                + "RETURNING (SELECT max(price) FROM history)").contains("history"));
    }
}
