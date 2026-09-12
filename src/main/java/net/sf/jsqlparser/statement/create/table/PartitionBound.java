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

import java.io.Serializable;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

/** A PostgreSQL declarative-partition bound. */
public class PartitionBound implements Serializable {

    public enum Type {
        RANGE, LIST, HASH, DEFAULT
    }

    private Type type;
    private ExpressionList<Expression> fromExpressions;
    private ExpressionList<Expression> toExpressions;
    private ExpressionList<Expression> inExpressions;
    private Expression modulus;
    private Expression remainder;

    public PartitionBound() {}

    public PartitionBound(Type type) {
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public void setType(Type type) {
        this.type = type;
    }

    public ExpressionList<Expression> getFromExpressions() {
        return fromExpressions;
    }

    public void setFromExpressions(ExpressionList<Expression> fromExpressions) {
        this.fromExpressions = fromExpressions;
    }

    public ExpressionList<Expression> getToExpressions() {
        return toExpressions;
    }

    public void setToExpressions(ExpressionList<Expression> toExpressions) {
        this.toExpressions = toExpressions;
    }

    public ExpressionList<Expression> getInExpressions() {
        return inExpressions;
    }

    public void setInExpressions(ExpressionList<Expression> inExpressions) {
        this.inExpressions = inExpressions;
    }

    public Expression getModulus() {
        return modulus;
    }

    public void setModulus(Expression modulus) {
        this.modulus = modulus;
    }

    public Expression getRemainder() {
        return remainder;
    }

    public void setRemainder(Expression remainder) {
        this.remainder = remainder;
    }

    public PartitionBound withType(Type type) {
        setType(type);
        return this;
    }

    public PartitionBound withFromExpressions(ExpressionList<Expression> fromExpressions) {
        setFromExpressions(fromExpressions);
        return this;
    }

    public PartitionBound withToExpressions(ExpressionList<Expression> toExpressions) {
        setToExpressions(toExpressions);
        return this;
    }

    public PartitionBound withInExpressions(ExpressionList<Expression> inExpressions) {
        setInExpressions(inExpressions);
        return this;
    }

    public PartitionBound withModulus(Expression modulus) {
        setModulus(modulus);
        return this;
    }

    public PartitionBound withRemainder(Expression remainder) {
        setRemainder(remainder);
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> expressionPrinter) {
        switch (type) {
            case RANGE:
                sql.append("FOR VALUES FROM (");
                appendRangeValues(sql, fromExpressions, expressionPrinter);
                sql.append(") TO (");
                appendRangeValues(sql, toExpressions, expressionPrinter);
                sql.append(')');
                break;
            case LIST:
                sql.append("FOR VALUES IN (");
                expressionPrinter.accept(inExpressions);
                sql.append(')');
                break;
            case HASH:
                sql.append("FOR VALUES WITH (MODULUS ");
                expressionPrinter.accept(modulus);
                sql.append(", REMAINDER ");
                expressionPrinter.accept(remainder);
                sql.append(')');
                break;
            case DEFAULT:
                sql.append("DEFAULT");
                break;
            default:
                break;
        }
    }

    /** Visits active bound expressions, excluding the MINVALUE/MAXVALUE range markers. */
    public void visitExpressions(Consumer<Expression> expressions) {
        if (type == null) {
            return;
        }
        switch (type) {
            case RANGE:
                visitRangeValues(fromExpressions, expressions);
                visitRangeValues(toExpressions, expressions);
                break;
            case LIST:
                if (inExpressions != null) {
                    expressions.accept(inExpressions);
                }
                break;
            case HASH:
                if (modulus != null) {
                    expressions.accept(modulus);
                }
                if (remainder != null) {
                    expressions.accept(remainder);
                }
                break;
            default:
                break;
        }
    }

    private static void visitRangeValues(ExpressionList<Expression> values,
            Consumer<Expression> expressions) {
        if (values != null) {
            values.stream().filter(value -> !isRangeMarker(value)).forEach(expressions);
        }
    }

    private static void appendRangeValues(StringBuilder sql, ExpressionList<Expression> values,
            Consumer<Expression> expressions) {
        if (values == null) {
            sql.append("null");
            return;
        }
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                sql.append(", ");
            }
            Expression value = values.get(i);
            if (isRangeMarker(value)) {
                sql.append(value);
            } else {
                expressions.accept(value);
            }
        }
    }

    private static boolean isRangeMarker(Expression expression) {
        if (!(expression instanceof Column)) {
            return false;
        }
        Column column = (Column) expression;
        return column.getTable() == null && ("MINVALUE".equalsIgnoreCase(column.getColumnName())
                || "MAXVALUE".equalsIgnoreCase(column.getColumnName()));
    }

}
