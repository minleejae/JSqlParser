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

import java.util.function.Consumer;
import net.sf.jsqlparser.parser.ASTNodeAccessImpl;

/** A method applied to an expression, such as a SQL Server XML subquery result. */
public class MethodCallExpression extends ASTNodeAccessImpl implements Expression {
    private Expression expression;
    private Function method;

    public MethodCallExpression(Expression expression, Function method) {
        this.expression = expression;
        this.method = method;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public Function getMethod() {
        return method;
    }

    public void setMethod(Function method) {
        this.method = method;
    }

    public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> printer) {
        printer.accept(expression);
        builder.append('.');
        printer.accept(method);
        return builder;
    }

    @Override
    public <T, S> T accept(ExpressionVisitor<T> visitor, S context) {
        return visitor.visit(this, context);
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return appendTo(builder, value -> builder.append(value)).toString();
    }
}
