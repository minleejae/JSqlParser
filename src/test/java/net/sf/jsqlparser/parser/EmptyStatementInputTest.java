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

import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.statement.Statements;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class EmptyStatementInputTest {
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n", "/* nothing */", "-- nothing\n"})
    void rejectsInputWithoutASingleStatement(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
        for (boolean allowComplex : new boolean[] {false, true}) {
            assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql,
                    parser -> parser.withAllowComplexParsing(allowComplex)));
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n", "/* nothing */", "-- nothing\n"})
    void returnsAnEmptyStatementList(String sql) throws Exception {
        assertTrue(CCJSqlParserUtil.parseStatements(sql).isEmpty());
        for (boolean allowComplex : new boolean[] {false, true}) {
            assertTrue(CCJSqlParserUtil.parseStatements(sql,
                    parser -> parser.withAllowComplexParsing(allowComplex)).isEmpty());
        }
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" \t\r\n", "/* nothing */", "-- nothing\n"})
    void preservesTheContractWithACallerExecutor(String sql) throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            for (boolean allowComplex : new boolean[] {false, true}) {
                assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql, executor,
                        parser -> parser.withAllowComplexParsing(allowComplex)));
                assertTrue(CCJSqlParserUtil.parseStatements(sql, executor,
                        parser -> parser.withAllowComplexParsing(allowComplex)).isEmpty());
            }
            assertFalse(executor.isShutdown());
            assertEquals("SELECT 1", CCJSqlParserUtil.parse("SELECT 1", executor, null).toString());
            assertEquals(2,
                    CCJSqlParserUtil.parseStatements("SELECT 1; SELECT 2", executor, null).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " \t\r\n", "/* nothing */", "-- nothing\n"})
    void agreesWithReaderAndStreamParsing(String sql) {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse(new StringReader(sql)));
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(
                new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8))));
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(
                new ByteArrayInputStream(sql.getBytes(StandardCharsets.UTF_8)), "UTF-8"));
    }

    @Test
    void emptyResultsRemainIndependentAndMutable() throws Exception {
        Statements first = CCJSqlParserUtil.parseStatements("");
        first.add(CCJSqlParserUtil.parse("SELECT 1"));
        assertTrue(CCJSqlParserUtil.parseStatements("").isEmpty());
        assertTrue(CCJSqlParserUtil.parseStatements((String) null).isEmpty());
        assertEquals(1, first.size());
    }
}
