/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.table;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import net.sf.jsqlparser.JSQLParserException;
import net.sf.jsqlparser.parser.AbstractJSqlParser.Dialect;
import net.sf.jsqlparser.parser.CCJSqlParserUtil;
import net.sf.jsqlparser.test.TestUtils;
import net.sf.jsqlparser.util.TablesNamesFinder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class SqlServerXmlTypeTest {
    @ParameterizedTest
    @ValueSource(strings = {
            "XML(CONTENT dbo.OrderSchema)",
            "XML(DOCUMENT dbo.OrderSchema)",
            "XML(dbo.OrderSchema)",
            "XML(OrderSchema)",
            "[xml](CONTENT [Person].[AdditionalContactInfoSchemaCollection])",
            "\"xml\"(DOCUMENT \"Person\".\"Order Schema\")",
            "XML(CONTENT)",
            "XML(DOCUMENT.Collection)",
            "XML([schema.with.dot].[collection.with.dot])"
    })
    void roundTripTypedXml(String typeSql) throws JSQLParserException {
        CreateTable table = (CreateTable) TestUtils.assertSqlCanBeParsedAndDeparsed(
                "CREATE TABLE dbo.t (payload " + typeSql + " NULL)", true,
                parser -> parser.withDialect(Dialect.SQLSERVER));
        ColDataType type = table.getColumnDefinitions().get(0).getColDataType();
        assertNotNull(type.getXmlTypeModifier());
        assertNull(type.getArgumentsStringList());
        assertNull(type.getPrecision());
        assertNull(type.getScale());
        assertEquals(List.of("dbo.t"), new TablesNamesFinder().getTableList(table));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "ALTER TABLE t ADD payload XML(CONTENT dbo.OrderSchema)",
            "ALTER TABLE t ALTER COLUMN payload XML(DOCUMENT dbo.OrderSchema)",
            "DECLARE @payload XML(CONTENT dbo.OrderSchema)",
            "CREATE TABLE t (untyped XML, n DECIMAL(10, 2), s VARCHAR(20))"
    })
    void reuseTypeParsingAcrossStatements(String sql) throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed(sql, true,
                parser -> parser.withDialect(Dialect.SQLSERVER));
    }

    @Test
    void reproduceIssue1567() throws JSQLParserException {
        TestUtils.assertSqlCanBeParsedAndDeparsed("CREATE TABLE [Person].[Person] ("
                + "[BusinessEntityID] [int] NOT NULL, [PersonType] [nchar](2) NOT NULL, "
                + "[NameStyle] [dbo].[NameStyle] NOT NULL, [Title] [nvarchar](8) NULL, "
                + "[FirstName] [dbo].[Name] NOT NULL, [MiddleName] [dbo].[Name] NULL, "
                + "[LastName] [dbo].[Name] NOT NULL, [Suffix] [nvarchar](10) NULL, "
                + "[EmailPromotion] [int] NOT NULL, "
                + "[AdditionalContactInfo] [xml](CONTENT [Person].[AdditionalContactInfoSchemaCollection]) NULL, "
                + "[Demographics] [xml](CONTENT [Person].[IndividualSurveySchemaCollection]) NULL, "
                + "[rowguid] [uniqueidentifier] ROWGUIDCOL NOT NULL, [ModifiedDate] [datetime] NOT NULL)",
                true, parser -> parser.withDialect(Dialect.SQLSERVER));
    }

    @ParameterizedTest
    @ValueSource(strings = {"XML()", "XML(10)", "XML(OTHER dbo.c)",
            "XML(CONTENT dbo.c, dbo.d)", "XML(CONTENT db.dbo.c)", "XML(CONTENT dbo.c"})
    void rejectInvalidModifier(String typeSql) {
        assertThrows(JSQLParserException.class,
                () -> CCJSqlParserUtil.parse("CREATE TABLE t (x " + typeSql + ")",
                        parser -> parser.withDialect(Dialect.SQLSERVER)
                                .withUnsupportedStatements(false)));
    }

    @Test
    void structuredValuesAndEquality() {
        XmlTypeModifier omitted = new XmlTypeModifier(null, List.of("dbo", "c"));
        assertNull(omitted.getKind());
        assertEquals(XmlTypeModifier.Kind.CONTENT, omitted.getEffectiveKind());
        assertThrows(UnsupportedOperationException.class,
                () -> omitted.getSchemaCollection().clear());
        ColDataType lower = new ColDataType("xml").withXmlTypeModifier(omitted);
        ColDataType upper = new ColDataType("XML").withXmlTypeModifier(
                new XmlTypeModifier(null, List.of("dbo", "c")));
        assertEquals(lower, upper);
        assertEquals(lower.hashCode(), upper.hashCode());
        assertNotEquals(lower, new ColDataType("xml"));
        assertNotEquals(omitted,
                new XmlTypeModifier(XmlTypeModifier.Kind.CONTENT, List.of("dbo", "c")));
        assertNotEquals(omitted, new XmlTypeModifier(null, List.of("dbo", "other")));
        lower.setXmlTypeModifier(null);
        assertEquals("xml", lower.toString());
    }

    @Test
    void caseInsensitiveTypeHashAlsoHandlesUnicode() {
        ColDataType dotted = new ColDataType("\u0130");
        ColDataType plain = new ColDataType("i");
        assertEquals(dotted, plain);
        assertEquals(dotted.hashCode(), plain.hashCode());
    }
}
