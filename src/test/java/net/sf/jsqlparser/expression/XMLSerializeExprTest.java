/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.OrderByElement;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import net.sf.jsqlparser.util.deparser.ExpressionDeParser;
import net.sf.jsqlparser.util.deparser.OrderByDeParser;
import net.sf.jsqlparser.util.validation.validator.ExpressionValidator;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class XMLSerializeExprTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "SELECT XMLSERIALIZE(CONTENT XMLTYPE('<Owner>Grandco</Owner>')) AS xmlserialize_doc FROM DUAL",
            "SELECT XMLSERIALIZE(DOCUMENT payload AS CLOB) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT payload AS VARCHAR2(4000)) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT payload AS BLOB ENCODING 'UTF-8' VERSION '1.0' INDENT SIZE = 2 SHOW DEFAULTS) FROM docs",
            "SELECT XMLSERIALIZE(DOCUMENT payload NO INDENT HIDE DEFAULTS) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT payload INDENT) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT payload INDENT SIZE = 0) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT XMLAGG(XMLELEMENT(NAME e, payload) ORDER BY id) AS CLOB) FROM docs",
            "SELECT XMLSERIALIZE(CONTENT (SELECT payload FROM docs)) FROM DUAL",
            "SELECT XMLSERIALIZE(CONTENT XMLTYPE('<x>it''s XML</x>') VERSION '1.1') FROM DUAL"
    })
    void roundTripOracleSyntax(String sql) throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(sql);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "XMLSERIALIZE(CONTENT)",
            "XMLSERIALIZE(DOCUMENT x AS)",
            "XMLSERIALIZE(CONTENT x NO INDENT SIZE = 2)",
            "XMLSERIALIZE(CONTENT x INDENT SIZE = -1)",
            "XMLSERIALIZE(CONTENT x ENCODING UTF8)",
            "XMLSERIALIZE(CONTENT x VERSION 1)",
            "XMLSERIALIZE(CONTENT x SHOW)",
            "XMLSERIALIZE(CONTENT x NO SOMETHING)",
            "XMLSERIALIZE(CONTENT x INDENT INDENT)"
    })
    void rejectMalformedSyntax(String expression) {
        assertThrows(JSQLParserException.class, () -> CCJSqlParserUtil.parseExpression(expression));
    }

    @Test
    void inspectOptionsAndOmission() throws JSQLParserException {
        XMLSerializeExpr xml = (XMLSerializeExpr) CCJSqlParserUtil.parseExpression(
                "XMLSERIALIZE(CONTENT payload ENCODING 'UTF-8' VERSION '1.0' INDENT SIZE = 4 SHOW DEFAULTS)");
        assertEquals(XMLSerializeExpr.SerializationMode.CONTENT, xml.getSerializationMode());
        assertNull(xml.getDataType());
        assertEquals("UTF-8", xml.getEncoding().getValue());
        assertEquals("1.0", xml.getVersion().getValue());
        assertEquals(Boolean.TRUE, xml.getIndent());
        assertEquals(4, xml.getIndentSize().getValue());
        assertEquals(Boolean.TRUE, xml.getShowDefaults());
        assertEquals(4, xml.getExpressions().size());
        XMLSerializeExpr omitted = (XMLSerializeExpr) CCJSqlParserUtil.parseExpression(
                "XMLSERIALIZE(DOCUMENT payload)");
        assertNull(omitted.getIndent());
        assertNull(omitted.getShowDefaults());
        omitted.setIndentSize(new LongValue(2));
        assertThrows(IllegalArgumentException.class, omitted::validateOptions);
    }

    @Test
    void legacyVisitorsHandleAbsentOrderBy() throws JSQLParserException {
        XMLSerializeExpr xml = (XMLSerializeExpr) CCJSqlParserUtil.parseExpression(
                "XMLSERIALIZE(XMLAGG(XMLTEXT(payload)) AS VARCHAR(100))");
        assertNull(xml.getSerializationMode());
        assertNull(xml.getOrderByElements());
        List<String> seen = new ArrayList<>();
        xml.accept(new ExpressionVisitorAdapter<Void>() {
            @Override
            public <S> Void visit(Column column, S context) {
                seen.add(column.getColumnName() + ":" + context);
                return null;
            }
        }, "context");
        assertEquals(List.of("payload:context"), seen);
    }

    @Test
    void visitAndRewriteLegacyOrderByWithContext() throws JSQLParserException {
        XMLSerializeExpr xml = (XMLSerializeExpr) CCJSqlParserUtil.parseExpression(
                "XMLSERIALIZE(XMLAGG(XMLTEXT(payload) ORDER BY id DESC NULLS LAST) AS VARCHAR(100))");
        StringBuilder sql = new StringBuilder();
        ExpressionDeParser writer = new ExpressionDeParser() {
            @Override
            public <S> StringBuilder visit(Column column, S context) {
                assertEquals("context", context);
                return getBuilder().append("new_").append(column.getColumnName());
            }
        };
        writer.setBuilder(sql);
        xml.accept(writer, "context");
        assertEquals(
                "xmlserialize(xmlagg(xmltext(new_payload) ORDER BY new_id DESC NULLS LAST) AS VARCHAR (100))",
                sql.toString());
        List<String> seen = new ArrayList<>();
        xml.accept(new ExpressionValidator() {
            @Override
            public <S> Void visit(Column column, S context) {
                seen.add(column.getColumnName());
                assertEquals("context", context);
                return null;
            }
        }, "context");
        assertEquals(List.of("payload", "id"), seen);
    }

    @Test
    void discoverTablesInSerializedSubquery() throws JSQLParserException {
        PlainSelect select = (PlainSelect) CCJSqlParserUtil.parse(
                "SELECT XMLSERIALIZE(CONTENT (SELECT payload FROM docs)) FROM outer_source");
        assertTrue(
                new TablesNamesFinder().getTableList((net.sf.jsqlparser.statement.Statement) select)
                        .containsAll(List.of("docs", "outer_source")));
    }

    @Test
    void preserveLegacyOrderByCustomization() {
        StringBuilder sql = new StringBuilder();
        OrderByDeParser writer = new OrderByDeParser(new ExpressionDeParser(), sql) {
            @Override
            public void deParseElement(OrderByElement element) {
                getBuilder().append("custom_order");
            }
        };
        writer.deParse(false, List.of(new OrderByElement().withExpression(new Column("id"))));
        assertEquals(" ORDER BY custom_order", sql.toString());
    }
}
