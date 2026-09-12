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
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.ArrayList;
import java.util.Arrays;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.TableOption;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import org.junit.jupiter.api.Test;

class TableOptionMutationTest {
    @Test
    void changingTypedOptionUpdatesLegacyTokensAndBothRenderers() throws JSQLParserException {
        CreateTable table = parse();
        TableOption engine = table.getTableOption(TableOption.Kind.ENGINE).orElseThrow();
        engine.setValue("MyISAM");
        engine.setUseEquals(false);
        assertEquals(Arrays.asList("ENGINE", "MyISAM"), table.getTableOptionsStrings());
        assertSql(table, "CREATE TABLE t (id INT) ENGINE MyISAM");
    }

    @Test
    void addingAndClearingTypedOptionsUpdatesDeparser() throws JSQLParserException {
        CreateTable table = parse();
        table.getTableOptions().add(TableOption.raw("ROW_FORMAT", "=", "DYNAMIC"));
        assertSql(table, "CREATE TABLE t (id INT) ENGINE = InnoDB ROW_FORMAT = DYNAMIC");
        table.getTableOptions().remove(0);
        assertSql(table, "CREATE TABLE t (id INT) ROW_FORMAT = DYNAMIC");
        table.getTableOptions().clear();
        assertSql(table, "CREATE TABLE t (id INT)");
    }

    @Test
    void rawOptionsRemainMutableAndCanReplaceTypedOptions() throws JSQLParserException {
        CreateTable table = parse();
        table.setTableOptionsStrings(new ArrayList<>(Arrays.asList("ENGINE", "=", "CSV")));
        assertNull(table.getTableOptions());
        table.getTableOptionsStrings().set(2, "MyISAM");
        assertSql(table, "CREATE TABLE t (id INT) ENGINE = MyISAM");
        table.setTableOptions(null);
        assertNull(table.getTableOptionsStrings());
        assertSql(table, "CREATE TABLE t (id INT)");
    }

    private static CreateTable parse() throws JSQLParserException {
        return (CreateTable) CCJSqlParserUtil.parse("CREATE TABLE t (id INT) ENGINE=InnoDB");
    }

    private static void assertSql(CreateTable table, String expected) throws JSQLParserException {
        assertEquals(expected, table.toString());
        StringBuilder buffer = new StringBuilder();
        table.accept(new StatementDeParser(buffer), null);
        assertEquals(expected, buffer.toString());
        assertEquals(expected, CCJSqlParserUtil.parse(expected).toString());
    }
}
