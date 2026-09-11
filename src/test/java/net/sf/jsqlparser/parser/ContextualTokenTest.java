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

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.alter.AlterSystemStatement;
import net.sf.jsqlparser.statement.alter.AlterSystemOperation;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static net.sf.jsqlparser.test.TestUtils.assertSqlCanBeParsedAndDeparsed;
import static org.junit.jupiter.api.Assertions.*;

class ContextualTokenTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT a FROM t UNION ALL BY NAME MATCHING (a) SELECT a FROM u",
            "SELECT a FROM t UNION DISTINCT BY NAME MATCHING (a, b) SELECT a FROM u",
            "ALTER SYSTEM ENABLE DISTRIBUTED RECOVERY",
            "ALTER SYSTEM DISABLE DISTRIBUTED RECOVERY"
    })
    void parsesPreviouslyShadowedContextualKeywords(String sql) throws Exception {
        for (boolean complex : new boolean[] {false, true}) {
            String parsed = assertSqlCanBeParsedAndDeparsed(sql, true,
                    parser -> parser.withAllowComplexParsing(complex)).toString();
            assertEquals(parsed, CCJSqlParserUtil.parse(parsed).toString());
        }
    }

    @Test
    void preservesAlterSystemOperation() throws Exception {
        assertEquals(AlterSystemOperation.ENABLE_DISTRIBUTED_RECOVERY,
                ((AlterSystemStatement) CCJSqlParserUtil.parse(
                        "alter system enable distributed recovery")).getOperation());
        assertEquals(AlterSystemOperation.DISABLE_DISTRIBUTED_RECOVERY,
                ((AlterSystemStatement) CCJSqlParserUtil.parse(
                        "alter system disable distributed recovery")).getOperation());
    }

    @ParameterizedTest
    @ValueSource(strings = {"MATCHING", "DISTRIBUTED", "RECOVERY"})
    void keepsContextualWordsUsableAsIdentifiers(String word) throws Exception {
        assertEquals(CCJSqlParserConstants.S_IDENTIFIER,
                CCJSqlParserUtil.newParser(word).getNextToken().kind);
        assertSqlCanBeParsedAndDeparsed("SELECT " + word + ", " + word
                + "(a) FROM " + word + " AS x", true);
    }

    @ParameterizedTest
    @ValueSource(
            strings = {"BYTES", "BINARY", "BOOLEAN", "UNSIGNED", "SIGNED", "CHARACTER", "CHAR"})
    void retainsDedicatedTypeTokensAndTypeParsing(String type) throws Exception {
        assertNotEquals(CCJSqlParserConstants.DATA_TYPE,
                CCJSqlParserUtil.newParser(type).getNextToken().kind);
        assertSqlCanBeParsedAndDeparsed("CREATE TABLE t (c " + type + ")", true);
        assertSqlCanBeParsedAndDeparsed("SELECT CAST(c AS " + type + ") FROM t", true);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ALTER SYSTEM ENABLE other RECOVERY",
            "ALTER SYSTEM DISABLE DISTRIBUTED other",
            "ALTER SYSTEM ENABLE DISTRIBUTED",
            "SELECT a FROM t UNION ALL BY NAME other (a) SELECT a FROM u",
            "SELECT a FROM t UNION ALL BY NAME MATCHING () SELECT a FROM u"
    })
    void rejectsWrongContextualKeywords(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
    }
}
