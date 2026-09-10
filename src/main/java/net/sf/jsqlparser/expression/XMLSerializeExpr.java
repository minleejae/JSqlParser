/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.expression;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import static java.util.stream.Collectors.joining;

import net.sf.jsqlparser.parser.ASTNodeAccessImpl;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.select.OrderByElement;

public class XMLSerializeExpr extends ASTNodeAccessImpl implements Expression {

    public enum SerializationMode {
        CONTENT, DOCUMENT
    }

    private Expression expression;
    private List<OrderByElement> orderByElements;
    private ColDataType dataType;
    private SerializationMode serializationMode;
    private StringValue encoding;
    private StringValue version;
    private Boolean indent;
    private LongValue indentSize;
    private Boolean showDefaults;

    @Override
    public <T, S> T accept(ExpressionVisitor<T> expressionVisitor, S context) {
        return expressionVisitor.visit(this, context);
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public List<OrderByElement> getOrderByElements() {
        return orderByElements;
    }

    public void setOrderByElements(List<OrderByElement> orderByElements) {
        this.orderByElements = orderByElements;
    }

    public ColDataType getDataType() {
        return dataType;
    }

    public void setDataType(ColDataType dataType) {
        this.dataType = dataType;
    }

    /** Null retains the legacy XMLAGG(XMLTEXT(...)) form and its existing expression getter. */
    public SerializationMode getSerializationMode() {
        return serializationMode;
    }

    public void setSerializationMode(SerializationMode serializationMode) {
        this.serializationMode = serializationMode;
    }

    public StringValue getEncoding() {
        return encoding;
    }

    public void setEncoding(StringValue encoding) {
        this.encoding = encoding;
    }

    public StringValue getVersion() {
        return version;
    }

    public void setVersion(StringValue version) {
        this.version = version;
    }

    /** Null preserves omission, true is INDENT, and false is NO INDENT. */
    public Boolean getIndent() {
        return indent;
    }

    public void setIndent(Boolean indent) {
        this.indent = indent;
    }

    public LongValue getIndentSize() {
        return indentSize;
    }

    public void setIndentSize(LongValue indentSize) {
        this.indentSize = indentSize;
    }

    /** Null preserves omission, true is SHOW DEFAULTS, and false is HIDE DEFAULTS. */
    public Boolean getShowDefaults() {
        return showDefaults;
    }

    public void setShowDefaults(Boolean showDefaults) {
        this.showDefaults = showDefaults;
    }

    /** Shared child discovery for expression, table-name and validation visitors. */
    public List<Expression> getExpressions() {
        List<Expression> result = new ArrayList<>();
        if (expression != null) {
            result.add(expression);
        }
        if (orderByElements != null) {
            for (OrderByElement orderBy : orderByElements) {
                result.add(orderBy.getExpression());
            }
        }
        if (encoding != null) {
            result.add(encoding);
        }
        if (version != null) {
            result.add(version);
        }
        if (indentSize != null) {
            result.add(indentSize);
        }
        return result;
    }

    /** Render both forms without bypassing custom expression or ORDER BY deparsers. */
    public StringBuilder appendTo(StringBuilder sql, Consumer<Expression> expressionWriter,
            Consumer<List<OrderByElement>> orderByWriter) {
        validateOptions();
        sql.append("xmlserialize(");
        if (serializationMode == null) {
            sql.append("xmlagg(xmltext(");
            expressionWriter.accept(expression);
            sql.append(")");
            if (orderByElements != null) {
                orderByWriter.accept(orderByElements);
            }
            sql.append(") AS ").append(dataType);
        } else {
            sql.append(serializationMode).append(" ");
            expressionWriter.accept(expression);
            if (dataType != null) {
                sql.append(" AS ").append(dataType);
            }
            if (encoding != null) {
                sql.append(" ENCODING ");
                expressionWriter.accept(encoding);
            }
            if (version != null) {
                sql.append(" VERSION ");
                expressionWriter.accept(version);
            }
            if (indent != null) {
                sql.append(indent ? " INDENT" : " NO INDENT");
                if (indent && indentSize != null) {
                    sql.append(" SIZE = ");
                    expressionWriter.accept(indentSize);
                }
            }
            if (showDefaults != null) {
                sql.append(showDefaults ? " SHOW DEFAULTS" : " HIDE DEFAULTS");
            }
        }
        return sql.append(")");
    }

    public void validateOptions() {
        if (indentSize != null && (!Boolean.TRUE.equals(indent) || indentSize.getValue() < 0)) {
            throw new IllegalArgumentException(
                    "An indentation size requires INDENT and must be nonnegative");
        }
        if (serializationMode == null && (encoding != null || version != null || indent != null
                || indentSize != null || showDefaults != null)) {
            throw new IllegalArgumentException("Serialization options require CONTENT or DOCUMENT");
        }
        if (serializationMode != null && orderByElements != null && !orderByElements.isEmpty()) {
            throw new IllegalArgumentException(
                    "ORDER BY belongs inside the serialized XMLAGG expression");
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        return appendTo(sql, sql::append, orderBy -> sql.append(" ORDER BY ")
                .append(orderBy.stream().map(OrderByElement::toString).collect(joining(", "))))
                .toString();
    }
}
