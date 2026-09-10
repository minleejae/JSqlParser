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
import net.sf.jsqlparser.statement.UnsupportedStatement;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

class ParseStatementsFailureTest {
    @Test
    void reportsFailureWhenComplexParsingIsDisabled() {
        JSQLParserException exception = assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parseStatements("SELECT FROM",
                        parser -> parser.withAllowComplexParsing(false)));
        assertNotNull(exception.getCause());
    }

    @Test
    void reportsFailureWhenNestingPreventsRetry() {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parseStatements("SELECT (( FROM",
                        parser -> parser.withAllowComplexParsing(true).withAllowedNestingDepth(0)));
    }

    @Test
    void leavesCallerExecutorUsableAfterFailure() throws Exception {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        try {
            assertThrows(JSQLParserException.class,
                    () -> CCJSqlParserUtil.parseStatements("SELECT FROM", executor,
                            parser -> parser.withAllowComplexParsing(false)));
            assertFalse(executor.isShutdown());
            assertEquals(2, CCJSqlParserUtil.parseStatements("SELECT 1; SELECT 2", executor,
                    parser -> parser.withAllowComplexParsing(false)).size());
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void reportsTimeoutWhenComplexParsingIsDisabled() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        CountDownLatch release = new CountDownLatch(1);
        executor.submit(() -> release.await(30, TimeUnit.SECONDS));
        try {
            JSQLParserException exception = assertThrows(JSQLParserException.class,
                    () -> CCJSqlParserUtil.parseStatements("SELECT 1", executor,
                            parser -> parser.withAllowComplexParsing(false).withTimeOut(50)));
            assertInstanceOf(TimeoutException.class, exception.getCause());
        } finally {
            release.countDown();
            executor.shutdownNow();
        }
    }

    @Test
    void preservesEmptyInputAndUnsupportedStatementContracts() throws Exception {
        assertNull(CCJSqlParserUtil.parseStatements((String) null));
        assertNull(CCJSqlParserUtil.parseStatements(""));
        assertInstanceOf(UnsupportedStatement.class,
                CCJSqlParserUtil.parseStatements("SELECT 1; WHATEVER !",
                        parser -> parser.withAllowComplexParsing(false)
                                .withUnsupportedStatements(true))
                        .get(1));
    }
}
