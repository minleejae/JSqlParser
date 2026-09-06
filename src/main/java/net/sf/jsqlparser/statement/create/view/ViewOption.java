/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.view;

import java.io.Serializable;
import java.util.Locale;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.StringValue;
import net.sf.jsqlparser.schema.MultiPartName;

/** A PostgreSQL view parameter, preserving its spelling, value, and optional equals sign. */
public class ViewOption implements Serializable {
    public enum Kind {
        SECURITY_BARRIER, SECURITY_INVOKER, CHECK_OPTION, OTHER
    }

    private final String name;
    private final Expression value;
    private final boolean useEquals;

    public ViewOption(String name, Expression value, boolean useEquals) {
        this.name = name;
        this.value = value;
        this.useEquals = useEquals;
    }

    public String getName() {
        return name;
    }

    public Kind getKind() {
        switch (MultiPartName.unquote(name).toLowerCase(Locale.ROOT)) {
            case "security_barrier":
                return Kind.SECURITY_BARRIER;
            case "security_invoker":
                return Kind.SECURITY_INVOKER;
            case "check_option":
                return Kind.CHECK_OPTION;
            default:
                return Kind.OTHER;
        }
    }

    public Expression getValue() {
        return value;
    }

    public boolean isUseEquals() {
        return useEquals;
    }

    private String valueText() {
        return value instanceof StringValue ? ((StringValue) value).getValue()
                : value == null ? null : value.toString();
    }

    /** Returns the boolean parameter value, or null for a different kind or an unknown value. */
    public Boolean getBooleanValue() {
        if (getKind() != Kind.SECURITY_BARRIER && getKind() != Kind.SECURITY_INVOKER) {
            return null;
        }
        String text = valueText();
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

    public CreateView.CheckOption getCheckOption() {
        if (getKind() == Kind.CHECK_OPTION) {
            if ("local".equalsIgnoreCase(valueText())) {
                return CreateView.CheckOption.LOCAL;
            }
            if ("cascaded".equalsIgnoreCase(valueText())) {
                return CreateView.CheckOption.CASCADED;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return name + (value == null ? "" : (useEquals ? " = " : " ") + value);
    }
}
