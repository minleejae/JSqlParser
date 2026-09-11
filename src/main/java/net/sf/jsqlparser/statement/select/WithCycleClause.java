/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.select;

import java.io.Serializable;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

/** Cycle detection metadata for a recursive common table expression. */
public class WithCycleClause implements Serializable {
    private ExpressionList<Column> cycleColumns;
    private String markColumnName;
    private Expression markValue;
    private Expression markDefault;
    private String pathColumnName;

    public ExpressionList<Column> getCycleColumns() {
        return cycleColumns;
    }

    public void setCycleColumns(ExpressionList<Column> cycleColumns) {
        this.cycleColumns = cycleColumns;
    }

    public WithCycleClause withCycleColumns(ExpressionList<Column> cycleColumns) {
        setCycleColumns(cycleColumns);
        return this;
    }

    public String getMarkColumnName() {
        return markColumnName;
    }

    public void setMarkColumnName(String markColumnName) {
        this.markColumnName = markColumnName;
    }

    public WithCycleClause withMarkColumnName(String markColumnName) {
        setMarkColumnName(markColumnName);
        return this;
    }

    public Expression getMarkValue() {
        return markValue;
    }

    public void setMarkValue(Expression markValue) {
        this.markValue = markValue;
    }

    public WithCycleClause withMarkValue(Expression markValue) {
        setMarkValue(markValue);
        return this;
    }

    public Expression getMarkDefault() {
        return markDefault;
    }

    public void setMarkDefault(Expression markDefault) {
        this.markDefault = markDefault;
    }

    public WithCycleClause withMarkDefault(Expression markDefault) {
        setMarkDefault(markDefault);
        return this;
    }

    public String getPathColumnName() {
        return pathColumnName;
    }

    public void setPathColumnName(String pathColumnName) {
        this.pathColumnName = pathColumnName;
    }

    public WithCycleClause withPathColumnName(String pathColumnName) {
        setPathColumnName(pathColumnName);
        return this;
    }

    public <S> void accept(ExpressionVisitor<?> visitor, S context) {
        if (cycleColumns != null) {
            cycleColumns.forEach(column -> column.accept(visitor, context));
        }
        if (markValue != null) {
            markValue.accept(visitor, context);
        }
        if (markDefault != null) {
            markDefault.accept(visitor, context);
        }
    }

    public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> expressionPrinter) {
        builder.append("CYCLE ");
        if (cycleColumns != null) {
            for (int i = 0; i < cycleColumns.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                expressionPrinter.accept(cycleColumns.get(i));
            }
        }
        builder.append(" SET ").append(markColumnName);
        if (markValue != null) {
            builder.append(" TO ");
            expressionPrinter.accept(markValue);
            builder.append(" DEFAULT ");
            expressionPrinter.accept(markDefault);
        }
        return builder.append(" USING ").append(pathColumnName);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return appendTo(builder, expression -> builder.append(expression)).toString();
    }
}
