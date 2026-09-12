/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.alter;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class AlterKeyAccessorsTest {
    private List<String> columns(AlterExpression action, boolean primary) {
        return primary ? action.getPkColumns() : action.getUkColumns();
    }

    @ParameterizedTest
    @ValueSource(booleans = {false, true})
    void keepsGettersSettersAndMutableListsConnectedToTheIndex(boolean primary) throws Exception {
        String prefix = "ALTER TABLE t ADD " + (primary ? "PRIMARY KEY" : "UNIQUE");
        Alter statement = (Alter) CCJSqlParserUtil.parse(prefix + " (a)");
        AlterExpression action = statement.getAlterExpressions().get(0);
        Index originalIndex = action.getIndex();
        action.getIndex().setColumnsNames(List.of("b"));
        assertEquals(List.of("b"), columns(action, primary));
        if (primary) {
            action.setPkColumns(List.of("c"));
        } else {
            action.setUkColumns(List.of("c"));
        }
        assertEquals(prefix + " (c)", statement.toString());
        columns(action, primary).add("d");
        columns(action, primary).set(0, "e");
        assertEquals(List.of("e", "d"), action.getIndex().getColumnsNames());
        assertEquals("e", columns(action, primary).remove(0));
        if (primary) {
            action.addPkColumns("f");
        } else {
            action.addUkColumns("f");
        }
        assertEquals(prefix + " (d, f)", statement.toString());
        assertSame(originalIndex, action.getIndex());
        StringBuilder output = new StringBuilder();
        statement.accept(new StatementDeParser(output), null);
        assertEquals(statement.toString(), output.toString());
        columns(action, primary).clear();
        assertTrue(action.getIndex().getColumns().isEmpty());
    }

    @Test
    void preservesStructuredElementsAndIndexMetadataForUnchangedKeys() throws Exception {
        Alter statement = (Alter) CCJSqlParserUtil.parse(
                "ALTER TABLE t ADD UNIQUE (a, b) DEFERRABLE",
                parser -> parser.withDialect(
                        net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect.POSTGRESQL));
        AlterExpression action = statement.getAlterExpressions().get(0);
        Index index = action.getIndex();
        index.setName("uq");
        Index.ColumnParams expression =
                new Index.ColumnParams(new net.sf.jsqlparser.schema.Column("a"))
                        .withExpressionParenthesized(false);
        index.getColumns().set(0, expression);
        String original = statement.toString();
        action.setUkColumns(new ArrayList<>(action.getUkColumns()));
        assertSame(expression, index.getColumns().get(0));
        assertEquals(original, statement.toString());
        action.addUkColumns("c");
        assertSame(expression, index.getColumns().get(0));
        assertEquals("uq", index.getName());
        assertEquals(original.replace("(a, b)", "(a, b, c)"), statement.toString());
        assertTrue(statement.toString().endsWith("DEFERRABLE"));
        assertEquals(List.of("a", "b", "c"), action.getUkColumns());
    }

    @Test
    void retainsLegacyOnlyConstructionAndAllowsClearingStructuredKeys() {
        AlterExpression action = new AlterExpression().withOperation(AlterOperation.ADD)
                .withPkColumns(new ArrayList<>(List.of("a")));
        action.addPkColumns("b");
        assertEquals("ADD PRIMARY KEY (a, b)", action.toString());
        action.setIndex(new Index().withType("PRIMARY KEY").withColumnsNames(List.of("c")));
        assertEquals(List.of("c"), action.getPkColumns());
        action.setPkColumns(null);
        assertTrue(action.getPkColumns().isEmpty());
    }
}
