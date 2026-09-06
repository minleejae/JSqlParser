/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.replication;

import java.io.Serializable;
import java.util.Locale;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;

/**
 * Common spelling and literal handling; publication and subscription keys remain distinct enums.
 */
public abstract class ReplicationOption<K extends Enum<K>> implements Serializable {
    private final String name;
    private final K kind;
    private final Expression value;

    protected ReplicationOption(String name, K kind, Expression value) {
        this.name = name;
        this.kind = kind;
        this.value = value;
    }

    public String getName() {
        return name;
    }

    public K getKind() {
        return kind;
    }

    public Expression getValue() {
        return value;
    }

    public String getValueText() {
        return value instanceof StringValue ? ((StringValue) value).getValue()
                : value == null ? null : value.toString();
    }

    protected Boolean booleanValue() {
        String text = getValueText();
        if (text == null) {
            return true;
        }
        switch (text.toLowerCase(Locale.ROOT)) {
            case "true":
            case "on":
            case "yes":
            case "1":
                return true;
            case "false":
            case "off":
            case "no":
            case "0":
                return false;
            default:
                return null;
        }
    }

    protected <E extends Enum<E>> E enumValue(Class<E> type) {
        String text = getValueText();
        if (text == null) {
            return null;
        }
        try {
            return Enum.valueOf(type, text.toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException exception) {
            return null;
        }
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> expressions) {
        sql.append(name);
        if (value != null) {
            sql.append(" = ");
            expressions.accept(value);
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
