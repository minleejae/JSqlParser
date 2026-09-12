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
import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitorAdapter;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PartitionTraversalTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "ALTER TABLE parent ATTACH PARTITION child FOR VALUES FROM (0) TO (10)",
            "ALTER TABLE parent ATTACH PARTITION child DEFAULT",
            "ALTER TABLE parent DETACH PARTITION child CONCURRENTLY",
            "ALTER TABLE parent EXCHANGE PARTITION p0 WITH TABLE child WITHOUT VALIDATION"
    })
    void finderIncludesPartitionTables(String sql) throws JSQLParserException {
        assertThat(TablesNamesFinder.findTables(sql)).containsExactlyInAnyOrder("parent", "child");
    }

    @Test
    void visitorReceivesPartitionKeysBoundsAndContext() throws JSQLParserException {
        List<String> visited = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                assertEquals("context", context);
                visited.add(column.getColumnName());
                return null;
            }

            @Override
            public <S> Void visit(LongValue value, S context) {
                assertEquals("context", context);
                visited.add(value.toString());
                return null;
            }
        };
        StatementVisitorAdapter<Void> visitor =
                new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions));
        CCJSqlParserUtil.parse("CREATE TABLE t (id INT) PARTITION BY RANGE (id) "
                + "SUBPARTITION BY HASH (id + 1) SUBPARTITIONS 2")
                .accept(visitor, "context");
        CCJSqlParserUtil
                .parse("ALTER TABLE parent ATTACH PARTITION child FOR VALUES FROM (0) TO (10)")
                .accept(visitor, "context");
        assertThat(visited).containsExactly("id", "id", "1", "0", "10");
    }

    @Test
    void rangeMarkersAreNotVisitedAsColumnReferences() throws JSQLParserException {
        List<String> columns = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                columns.add(column.getColumnName());
                return null;
            }
        };
        CCJSqlParserUtil.parse("ALTER TABLE parent ATTACH PARTITION child "
                + "FOR VALUES FROM (MINVALUE) TO (MAXVALUE)")
                .accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)),
                        null);
        assertThat(columns).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "CREATE TABLE t (id INT) PARTITION BY HASH (id + 1) PARTITIONS 2",
            "ALTER TABLE t PARTITION BY RANGE COLUMNS (id)",
            "CREATE TABLE child PARTITION OF parent FOR VALUES FROM (0) TO (10)",
            "CREATE TABLE child PARTITION OF parent FOR VALUES FROM (MINVALUE) TO (MAXVALUE)",
            "ALTER TABLE parent ATTACH PARTITION child FOR VALUES IN (1, 2)",
            "ALTER TABLE parent ATTACH PARTITION child FOR VALUES WITH (MODULUS 4, REMAINDER 1)",
            "ALTER TABLE parent ATTACH PARTITION child DEFAULT"
    })
    void deparserUsesPartitionExpressionVisitor(String sql) throws JSQLParserException {
        Statement statement = CCJSqlParserUtil.parse(sql);
        StringBuilder output = new StringBuilder();
        ExpressionDeParser expressions = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                return getBuilder().append("mapped_").append(column.getColumnName());
            }

            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                return getBuilder().append(value.getValue() + 100);
            }
        };
        statement.accept(new StatementDeParser(expressions, new SelectDeParser(), output), null);
        String expected = statement.toString()
                .replace("HASH (id + 1)", "HASH (mapped_id + 101)")
                .replace("COLUMNS (id)", "COLUMNS (mapped_id)")
                .replace("FROM (0) TO (10)", "FROM (100) TO (110)")
                .replace("IN (1, 2)", "IN (101, 102)")
                .replace("MODULUS 4, REMAINDER 1", "MODULUS 104, REMAINDER 101");
        assertEquals(expected, output.toString());
        StringBuilder plain = new StringBuilder();
        statement.accept(new StatementDeParser(plain), null);
        assertEquals(statement.toString(), plain.toString());
        assertEquals(expected, CCJSqlParserUtil.parse(expected).toString());
    }
}
