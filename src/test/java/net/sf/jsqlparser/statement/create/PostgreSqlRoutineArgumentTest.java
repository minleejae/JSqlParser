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
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.alter.AlterExtension;
import net.sf.jsqlparser.statement.grant.Grant;
import net.sf.jsqlparser.statement.RoutineReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class PostgreSqlRoutineArgumentTest {
    @ParameterizedTest
    @ValueSource(strings = {"integer", "text[]", "IN value integer", "OUT result text",
            "INOUT state app.custom_type", "VARIADIC items text[]",
            "\"argument name\" double precision"})
    void privilegeAndExtensionArgumentsHaveEquivalentAst(String argument)
            throws JSQLParserException {
        Grant grant = (Grant) CCJSqlParserUtil.parse(
                "GRANT EXECUTE ON FUNCTION app.f(" + argument + ") TO reader");
        AlterExtension extension = (AlterExtension) CCJSqlParserUtil.parse(
                "ALTER EXTENSION example ADD FUNCTION app.f(" + argument + ")");
        RoutineReference.Argument access =
                grant.getTarget().getRoutines().get(0).getArguments().get(0);
        RoutineReference.Argument member = extension.getMember().getRoutine().getArguments().get(0);
        assertEquals(access.getMode(), member.getMode());
        assertEquals(access.getName(), member.getName());
        assertEquals(access.getDataType().toString(), member.getDataType().toString());
        assertRoundTrip(grant);
        assertRoundTrip(extension);
    }

    @Test
    void aggregateKeepsOrderByArgumentBoundary() throws JSQLParserException {
        AlterExtension extension = (AlterExtension) CCJSqlParserUtil.parse(
                "ALTER EXTENSION example ADD AGGREGATE app.percentile(double precision ORDER BY integer)");
        RoutineReference routine = extension.getMember().getRoutine();
        assertEquals(1, routine.getArguments().size());
        assertEquals(1, routine.getOrderByArguments().size());
        assertEquals("double precision", routine.getArguments().get(0).getDataType().toString());
        assertRoundTrip(extension);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "GRANT EXECUTE ON FUNCTION app.f(integer ORDER BY integer) TO reader",
            "GRANT EXECUTE ON FUNCTION app.f(*) TO reader",
            "ALTER EXTENSION example ADD FUNCTION app.f(integer ORDER BY integer)",
            "ALTER EXTENSION example ADD FUNCTION app.f(*)",
            "GRANT EXECUTE ON FUNCTION app.f(IN) TO reader"
    })
    void aggregateSyntaxDoesNotLeakIntoOtherRoutineContexts(String sql) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse(sql));
    }

    private static void assertRoundTrip(Statement statement) throws JSQLParserException {
        assertEquals(statement.toString(), CCJSqlParserUtil.parse(statement.toString()).toString());
    }
}
