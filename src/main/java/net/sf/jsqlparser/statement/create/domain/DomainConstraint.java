/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.domain;

import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import java.io.Serializable;

public class DomainConstraint implements Serializable {
    private String name;
    private Kind kind;
    private Expression expression;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Expression getExpression() {
        return expression;
    }

    public void setExpression(Expression expression) {
        this.expression = expression;
    }

    public enum Kind {
        NULL, NOT_NULL, CHECK
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> expressions) {
        if (name != null) {
            sql.append("CONSTRAINT ").append(name).append(' ');
        }
        if (kind == Kind.CHECK) {
            sql.append("CHECK (");
            expressions.accept(expression);
            sql.append(')');
        } else {
            sql.append(kind == Kind.NULL ? "NULL" : "NOT NULL");
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
