/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.parser;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.CastExpression;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.select.PlainSelect;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class LargeObjectTypeTest {
    @ParameterizedTest
    @ValueSource(strings = {"CHARACTER LARGE OBJECT", "CHAR LARGE OBJECT", "NCHAR LARGE OBJECT",
            "NATIONAL CHARACTER LARGE OBJECT", "BINARY LARGE OBJECT", "character large object"})
    void sharesTypeNameAndLengthAcrossTypeFragmentsCastsAndColumns(String name) throws Exception {
        ColDataType type = CCJSqlParserUtil.parseColDataType(name + "(3)");
        assertEquals(name, type.getDataType());
        assertEquals(3, type.getPrecision());
        assertEquals(Arrays.asList("3"), type.getArgumentsStringList());
        assertEquals(name, CCJSqlParserUtil.parseColDataType(name).getDataType());
        PlainSelect select = (PlainSelect) assertSqlCanBeParsedAndDeparsed(
                "SELECT CAST('abc' AS " + name + "(3)) FROM t");
        CastExpression cast = (CastExpression) select.getSelectItem(0).getExpression();
        assertEquals(name, cast.getColDataType().getDataType());
        assertEquals(3, cast.getColDataType().getPrecision());
        assertSqlCanBeParsedAndDeparsed("CREATE TABLE t (payload " + name + "(3) NOT NULL)");
        assertSqlCanBeParsedAndDeparsed("ALTER TABLE t ADD payload " + name + "(3)");
        assertEquals(type.toString(),
                CCJSqlParserUtil.parseColDataType(type.toString()).toString());
    }

    @Test
    void parsesTheDownstreamConditionWithoutRewritingLiteralContents() throws Exception {
        String sql = "(status IN (CAST('hi' AS CHARACTER LARGE OBJECT(2)), "
                + "CAST('low' AS CHARACTER LARGE OBJECT(3))))";
        var condition = CCJSqlParserUtil.parseCondExpression(sql, false);
        List<String> literals = new ArrayList<>();
        condition.accept(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(StringValue value, S context) {
                literals.add(value.getValue());
                return null;
            }
        }, null);
        assertEquals(Arrays.asList("hi", "low"), literals);
        assertEquals(condition.toString(),
                CCJSqlParserUtil.parseCondExpression(condition.toString(), false).toString());
        assertSqlCanBeParsedAndDeparsed("SELECT CAST('CHARACTER LARGE OBJECT' "
                + "AS CHARACTER LARGE OBJECT(22)) FROM t");
    }

    @Test
    void acceptsTokenWhitespaceWithoutChangingIdentifiersOrExistingTypes() throws Exception {
        assertEquals("CHARACTER LARGE OBJECT",
                CCJSqlParserUtil.parseColDataType("CHARACTER /* comment */ LARGE\nOBJECT")
                        .getDataType());
        for (String type : Arrays.asList("CHARACTER VARYING(3)", "NATIONAL CHARACTER(3)",
                "CLOB", "BLOB", "\"CHARACTER LARGE OBJECT\"", "app.large")) {
            ColDataType parsed = CCJSqlParserUtil.parseColDataType(type);
            assertEquals(parsed.toString(),
                    CCJSqlParserUtil.parseColDataType(parsed.toString()).toString());
        }
        assertSqlCanBeParsedAndDeparsed("SELECT large, object FROM t");
    }

    @ParameterizedTest
    @ValueSource(strings = {"CHARACTER LARGE", "CHARACTER LARGE OTHER", "INT LARGE OBJECT"})
    void rejectsIncompleteOrNonCharacterTypeNames(String type) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parseColDataType(type));
    }
}
