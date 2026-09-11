/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression.operators.relational;

import static org.junit.jupiter.api.Assertions.*;

import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.expression.BinaryExpression;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.expression.operators.conditional.AndExpression;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.test.TestUtils;
import org.junit.jupiter.api.Test;

/**
 *
 * @author Tobias Warneke (t.warneke@gmx.net)
 */
public class LikeExpressionTest {

    @Test
    public void testLikeNotIssue660() {
        LikeExpression instance = new LikeExpression();
        assertFalse(instance.isNot());
        assertTrue(instance.withNot(true).isNot());
    }

    @Test
    public void testSetEscapeAndGetStringExpression() throws JSQLParserException {
        LikeExpression instance =
                (LikeExpression) CCJSqlParserUtil.parseExpression("name LIKE 'J%$_%'");
        // escape character should be $
        Expression instance2 = new StringValue("$");
        instance.setEscape(instance2);

        // match all records with names that start with letter ’J’ and have the ’_’ character in
        // them
        assertEquals("name LIKE 'J%$_%' ESCAPE '$'", instance.toString());
    }

    @Test
    void testNotRLikeIssue1553() throws JSQLParserException {
        String sqlStr = "select * from test where id  not rlike '111'";
        TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
    }

    @Test
    void testDuckDBSimuilarTo() throws JSQLParserException {
        String sqlStr = "SELECT v\n"
                + "    FROM strings\n"
                + "    WHERE v SIMILAR TO 'San* [fF].*'\n"
                + "    ORDER BY v;";
        TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
    }

    @Test
    public void testMatchAny() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v MATCH_ANY 'keyword1 keyword2'", true);
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v NOT MATCH_ANY 'keyword1 keyword2'", true);
    }

    @Test
    public void testMatchAll() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v MATCH_ALL 'keyword1 keyword2'", true);
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v NOT MATCH_ALL 'keyword1 keyword2'", true);
    }

    @Test
    public void testMatchPhrase() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v MATCH_PHRASE 'keyword1 keyword2'", true);
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v NOT MATCH_PHRASE 'keyword1 keyword2'", true);
    }

    @Test
    public void testMatchPhrasePrefix() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v MATCH_PHRASE_PREFIX 'keyword1 keyword2'", true);
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v NOT MATCH_PHRASE_PREFIX 'keyword1 keyword2'", true);
    }

    @Test
    public void testMatchRegexp() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v MATCH_REGEXP 'keyword1 keyword2'", true);
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "select * from dual where v NOT MATCH_REGEXP 'keyword1 keyword2'", true);
    }

    @Test
    public void testLikeWithOldOracleJoinSyntaxOnRightOperand() throws JSQLParserException {
        String sqlStr = "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 LIKE t2.col2(+)";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
        LikeExpression like = (LikeExpression) select.getWhere();

        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) like.getRightExpression()).getOldOracleJoinSyntax());
        assertEquals(EqualsTo.NO_ORACLE_JOIN,
                ((Column) like.getLeftExpression()).getOldOracleJoinSyntax());
    }

    @Test
    public void testNotLikeWithOldOracleJoinSyntaxOnRightOperand() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 NOT LIKE t2.col2(+)", true);
    }

    @Test
    public void testSimilarToWithOldOracleJoinSyntaxOnRightOperand() throws JSQLParserException {
        String sqlStr = "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 SIMILAR TO t2.col2(+)";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
        BinaryExpression similarTo = (BinaryExpression) select.getWhere();

        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) similarTo.getRightExpression()).getOldOracleJoinSyntax());
    }

    @Test
    public void testSimilarToOnSeparateTokensWithOldOracleJoinSyntaxOnRightOperand()
            throws JSQLParserException {
        String sqlStr = "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 SIMILAR\nTO t2.col2(+)";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);

        assertTrue(select.getWhere() instanceof SimilarToExpression);
        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) ((SimilarToExpression) select.getWhere()).getRightExpression())
                        .getOldOracleJoinSyntax());
    }

    @Test
    public void testILikeWithOldOracleJoinSyntaxOnRightOperand() throws JSQLParserException {
        String sqlStr = "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 ILIKE t2.col2(+)";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
        LikeExpression like = (LikeExpression) select.getWhere();

        assertEquals(LikeExpression.KeyWord.ILIKE, like.getLikeKeyWord());
        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) like.getRightExpression()).getOldOracleJoinSyntax());
    }

    @Test
    public void testLikeWithOldOracleJoinSyntaxInJoinOnClause() throws JSQLParserException {
        String sqlStr =
                "SELECT * FROM table1 t1 LEFT JOIN table2 t2 ON t1.a = t2.b AND t1.col1 LIKE t2.col2(+)";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
        LikeExpression like = (LikeExpression) ((AndExpression) select.getJoins().get(0)
                .getOnExpression()).getRightExpression();

        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) like.getRightExpression()).getOldOracleJoinSyntax());
    }

    @Test
    public void testSimilarToWithOldOracleJoinSyntaxOnRightOperandAndEscape()
            throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(
                "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1 SIMILAR TO t2.col2(+) ESCAPE '\\'",
                true);
    }

    @Test
    public void testLikeWithOldOracleJoinSyntaxOnLeftOperand() throws JSQLParserException {
        String sqlStr = "SELECT * FROM table1 t1, table2 t2 WHERE t1.col1(+) LIKE t2.col2";
        PlainSelect select = (PlainSelect) TestUtils.assertSqlCanBeParsedAndDeparsed(sqlStr, true);
        LikeExpression like = (LikeExpression) select.getWhere();

        assertEquals(EqualsTo.ORACLE_JOIN_RIGHT,
                ((Column) like.getLeftExpression()).getOldOracleJoinSyntax());
        assertEquals(EqualsTo.NO_ORACLE_JOIN,
                ((Column) like.getRightExpression()).getOldOracleJoinSyntax());
    }
}
