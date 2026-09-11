/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement;

import static org.junit.jupiter.api.Assertions.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.ExpressionVisitorAdapter;
import net.sf.jsqlparser.expression.JdbcNamedParameter;
import net.sf.jsqlparser.expression.LongValue;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.execute.Execute;
import net.sf.jsqlparser.statement.oracle.OracleAssignment;
import net.sf.jsqlparser.statement.oracle.OracleBlock;
import net.sf.jsqlparser.statement.select.SelectVisitorAdapter;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.SelectDeParser;
import net.sf.jsqlparser.util.deparser.StatementDeParser;
import net.sf.jsqlparser.util.validation.Validation;
import net.sf.jsqlparser.util.validation.feature.FeaturesAllowed;
import net.sf.jsqlparser.util.validation.feature.OracleVersion;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class OracleBlockTest {
    private static OracleBlock parse(String sql) throws Exception {
        OracleBlock block =
                (OracleBlock) CCJSqlParserUtil.parse(sql, p -> p.withDialect(Dialect.ORACLE));
        StringBuilder output = new StringBuilder();
        block.accept(new StatementDeParser(output));
        assertEquals(block.toString(), output.toString());
        assertEquals(block.toString(), CCJSqlParserUtil
                .parse(output.toString(), p -> p.withDialect(Dialect.ORACLE)).toString());
        return block;
    }

    @Test
    void supportsIssue2007BindCalls() throws Exception {
        OracleBlock block =
                parse("BEGIN modifyParams(:BP_00000_v, :BP_00001_v, :BP_00012_v); END;");
        Execute call = assertInstanceOf(Execute.class, block.getStatements().get(0));
        assertEquals(Execute.ExecType.IMPLICIT, call.getExecType());
        assertEquals("modifyParams", call.getName());
        assertEquals(3, call.getExprList().size());
        List<String> names = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(JdbcNamedParameter parameter, S context) {
                assertEquals("ctx", context);
                names.add(parameter.getName());
                return null;
            }
        };
        block.accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)), "ctx");
        assertEquals(List.of("BP_00000_v", "BP_00001_v", "BP_00012_v"), names);
        assertTrue(block.getFeatures().isOpaque());
        assertTrue(block.getFeatures().getUnresolvedReferences().contains("modifyparams"));
        assertThrows(UnsupportedOperationException.class,
                () -> new TablesNamesFinder().getTables(block));
    }

    @Test
    void supportsIssue1786AndMaintainerNestedExample() throws Exception {
        OracleBlock simple = parse(
                "DECLARE num NUMBER; BEGIN num := 10; dbms_output.put_line('The number is ' || num); END;");
        assertEquals("num", simple.getDeclarations().get(0).getName());
        assertEquals("NUMBER", simple.getDeclarations().get(0).getDataType().toString());
        OracleAssignment assignment =
                assertInstanceOf(OracleAssignment.class, simple.getStatements().get(0));
        assertEquals("num", assignment.getTarget().toString());
        assertEquals("10", assignment.getValue().toString());
        OracleBlock nested = parse(
                "DECLARE l_message VARCHAR2(100) := 'Hello'; BEGIN DECLARE l_message2 VARCHAR2(100) := l_message || ' World!'; BEGIN DBMS_OUTPUT.put_line(l_message2); END; EXCEPTION WHEN OTHERS THEN DBMS_OUTPUT.put_line(DBMS_UTILITY.format_error_stack); END;");
        assertInstanceOf(OracleBlock.class, nested.getStatements().get(0));
        assertEquals(List.of("OTHERS"), nested.getExceptionHandlers().get(0).getExceptions());
        assertEquals("DBMS_OUTPUT.put_line",
                ((Execute) nested.getExceptionHandlers().get(0).getStatements().get(0)).getName());
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "BEGIN p; p(); schema.pkg.p(1, 'text'); p(arg => 1); NULL; END;",
            "DECLARE n CONSTANT NUMBER := 1; s VARCHAR2(10) NOT NULL DEFAULT 'a'; BEGIN n2 := n; END;",
            "BEGIN :out_value := 1; rec.field := 2; END;",
            "BEGIN NULL; EXCEPTION WHEN NO_DATA_FOUND OR TOO_MANY_ROWS THEN NULL; WHEN OTHERS THEN p; END;",
            "BEGIN BEGIN NULL; END; BEGIN NULL; END; END;"
    })
    void supportsBlockForms(String sql) throws Exception {
        parse(sql);
    }

    @Test
    void visitsMutationsAcrossDeclarationsBodyAndHandlers() throws Exception {
        OracleBlock block = parse(
                "DECLARE n NUMBER := 1; BEGIN n := 2; EXCEPTION WHEN OTHERS THEN n := 3; END;");
        List<Long> values = new ArrayList<>();
        ExpressionVisitorAdapter<Void> expressions = new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(LongValue value, S context) {
                assertEquals("ctx", context);
                values.add(value.getValue());
                return null;
            }
        };
        block.accept(new StatementVisitorAdapter<>(new SelectVisitorAdapter<>(expressions)), "ctx");
        assertEquals(List.of(1L, 2L, 3L), values);
        assertFalse(block.getFeatures().modifiesData());
        block.getDeclarations().get(0).setInitializer(new LongValue(10));
        ((OracleAssignment) block.getExceptionHandlers().get(0).getStatements().get(0))
                .setValue(new LongValue(30));
        StringBuilder output = new StringBuilder();
        ExpressionDeParser deparser = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(LongValue value, S context) {
                assertEquals("ctx", context);
                return getBuilder().append(value.getValue() + 1);
            }
        };
        block.accept(new StatementDeParser(deparser, new SelectDeParser(), output), "ctx");
        assertTrue(output.toString().contains("NUMBER := 11"));
        assertTrue(output.toString().contains("n := 31"));
        parse(output.toString());
    }

    @Test
    void findsSqlTablesInBothMainBodyAndHandlers() throws Exception {
        OracleBlock block = parse(
                "DECLARE n NUMBER := (SELECT max(id) FROM source); BEGIN INSERT INTO target SELECT id FROM source; EXCEPTION WHEN OTHERS THEN INSERT INTO audit VALUES (1); END;");
        assertEquals(Set.of("source", "target", "audit"), new TablesNamesFinder().getTables(block));
        assertTrue(block.getFeatures().modifiesData());
        assertFalse(block.getFeatures().returnsResultSet());
    }

    @Test
    void retainsScriptBoundariesAndExistingDialects() throws Exception {
        assertEquals(3, CCJSqlParserUtil.parseStatements("BEGIN p; END; BEGIN q; END; SELECT 1;",
                p -> p.withDialect(Dialect.ORACLE)).size());
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parse("BEGIN p(1); END;"));
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse("DECLARE n NUMBER; BEGIN n := 1; END;",
                        p -> p.withDialect(Dialect.SQLSERVER)));
        assertInstanceOf(DeclareStatement.class, CCJSqlParserUtil.parse("DECLARE @n int = 1",
                p -> p.withDialect(Dialect.SQLSERVER)));
        assertInstanceOf(Block.class, CCJSqlParserUtil.parse("BEGIN SELECT 1; END;"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"BEGIN RAISE; END;", "BEGIN LOOP; END;", "DECLARE BEGIN NULL; END;",
            "DECLARE n NUMBER BEGIN NULL; END;",
            "BEGIN p(1) END;", "BEGIN NULL;", "BEGIN END;", "BEGIN p(1,); END;",
            "DECLARE n CONSTANT NUMBER; BEGIN NULL; END;",
            "DECLARE n NUMBER NOT NULL; BEGIN NULL; END;",
            "BEGIN NULL; EXCEPTION WHEN OTHERS THEN END;"})
    void rejectsIncompleteBlocks(String sql) {
        assertThrows(JSQLParserException.class, () -> parse(sql));
    }

    @Test
    void validatesOracleBlockCapability() {
        String sql = "DECLARE n NUMBER := 1; BEGIN n := 2; p(n); END;";
        assertTrue(new Validation(CCJSqlParserUtil.newParser("SELECT 1").withDialect(Dialect.ORACLE)
                .getConfiguration(), List.of(OracleVersion.values()[0]), sql).validate().isEmpty());
        assertFalse(
                new Validation(
                        CCJSqlParserUtil.newParser("SELECT 1").withDialect(Dialect.ORACLE)
                                .getConfiguration(),
                        List.of(new FeaturesAllowed(Feature.block)), sql).validate().isEmpty());
    }
}
