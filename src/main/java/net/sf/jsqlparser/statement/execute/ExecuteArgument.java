/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.execute;

import java.util.Objects;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.parser.ASTNodeAccessImpl;

/** An EXEC argument with a call-specific OUTPUT modifier. */
public class ExecuteArgument extends ASTNodeAccessImpl implements Expression {
    private Expression expression;
    private boolean output;

    public ExecuteArgument(Expression expression, boolean output) {
        setExpression(expression);
        this.output = output;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = Objects.requireNonNull(expression, "expression");
    }

    public boolean isOutput() {
        return output;
    }

    public void setOutput(boolean output) {
        this.output = output;
    }

    @Override
    public <T, S> T accept(ExpressionVisitor<T> visitor, S context) {
        return visitor.visit(this, context);
    }

    public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> expressionPrinter) {
        expressionPrinter.accept(expression);
        if (output) {
            builder.append(" OUTPUT");
        }
        return builder;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return appendTo(builder, builder::append).toString();
    }
}
