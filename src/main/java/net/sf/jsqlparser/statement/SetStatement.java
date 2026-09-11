/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement;

import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

public final class SetStatement implements Statement {

    private final List<NameExpr> values = new ArrayList<>();
    private String effectParameter;
    private OnOffOptions onOffOptions;

    /** SQL Server options that share the SET option [, option] ON | OFF syntax. */
    public enum OnOffOption {
        QUOTED_IDENTIFIER, CONCAT_NULL_YIELDS_NULL, CURSOR_CLOSE_ON_COMMIT, ARITHABORT, ARITHIGNORE, FMTONLY, NOCOUNT, NOEXEC, NUMERIC_ROUNDABORT, PARSEONLY, ANSI_DEFAULTS, ANSI_NULL_DFLT_OFF, ANSI_NULL_DFLT_ON, ANSI_NULLS, ANSI_PADDING, ANSI_WARNINGS, FORCEPLAN, SHOWPLAN_ALL, SHOWPLAN_TEXT, SHOWPLAN_XML, IMPLICIT_TRANSACTIONS, REMOTE_PROC_TRANSACTIONS, XACT_ABORT;

        public static OnOffOption fromName(String name) {
            for (OnOffOption option : values()) {
                if (option.name().equalsIgnoreCase(name)) {
                    return option;
                }
            }
            return null;
        }
    }

    /** A group of options sharing one ON/OFF value; separate from assignment expressions. */
    public static final class OnOffOptions implements Serializable {
        private final List<OnOffOption> options;
        private boolean on;

        public OnOffOptions(Collection<OnOffOption> options, boolean on) {
            this.options = new ArrayList<>(options);
            if (this.options.isEmpty() || this.options.contains(null)) {
                throw new IllegalArgumentException("At least one non-null SET option is required");
            }
            this.on = on;
        }

        public List<OnOffOption> getOptions() {
            return options;
        }

        public boolean isOn() {
            return on;
        }

        public void setOn(boolean on) {
            this.on = on;
        }

        private StringBuilder appendTo(StringBuilder builder) {
            if (options.isEmpty() || options.contains(null)) {
                throw new IllegalStateException("Invalid SQL Server SET option group");
            }
            for (int i = 0; i < options.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                builder.append(options.get(i));
            }
            return builder.append(on ? " ON" : " OFF");
        }
    }

    public OnOffOptions getOnOffOptions() {
        return onOffOptions;
    }

    /** Selects SQL Server option syntax, clearing any existing assignments and scope. */
    public void setOnOffOptions(OnOffOptions onOffOptions) {
        Objects.requireNonNull(onOffOptions, "onOffOptions");
        clear();
        this.onOffOptions = onOffOptions;
    }

    public SetStatement() {
        // empty constructor
    }

    public SetStatement(Object name, ExpressionList<?> value) {
        add(name, value, true);
    }

    public void add(Object name, ExpressionList<?> value, boolean useEqual) {
        onOffOptions = null;
        values.add(new NameExpr(name, value, useEqual));
    }

    public void remove(int idx) {
        values.remove(idx);
    }

    public int getCount() {
        return values.size();
    }

    public boolean isUseEqual(int idx) {
        return values.get(idx).useEqual;
    }

    public boolean isUseEqual() {
        return isUseEqual(0);
    }

    public SetStatement setUseEqual(boolean useEqual) {
        return setUseEqual(0, useEqual);
    }

    public SetStatement withUseEqual(int idx, boolean useEqual) {
        this.setUseEqual(idx, useEqual);
        return this;
    }

    public SetStatement setUseEqual(int idx, boolean useEqual) {
        values.get(idx).useEqual = useEqual;
        return this;
    }

    public SetStatement withUseEqual(boolean useEqual) {
        this.setUseEqual(useEqual);
        return this;
    }

    public Object getName() {
        return getName(0);
    }

    public void setName(String name) {
        setName(0, name);
    }

    public Object getName(int idx) {
        return values.get(idx).name;
    }

    public void setName(int idx, String name) {
        values.get(idx).name = name;
    }

    public List<Expression> getExpressions(int idx) {
        return values.get(idx).expressions;
    }

    public List<Expression> getExpressions() {
        return getExpressions(0);
    }

    public void setExpressions(ExpressionList<?> expressions) {
        setExpressions(0, expressions);
    }

    public void setExpressions(int idx, ExpressionList<?> expressions) {
        values.get(idx).expressions = expressions;
    }

    /** Shares statement punctuation with deparsers while allowing expression visitors. */
    public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> expressionRenderer) {
        builder.append("SET ");
        if (onOffOptions != null) {
            if (!values.isEmpty() || effectParameter != null) {
                throw new IllegalStateException(
                        "SET options cannot be combined with assignments or scope");
            }
            return onOffOptions.appendTo(builder);
        }
        if (effectParameter != null) {
            builder.append(effectParameter).append(" ");
        }
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                builder.append(", ");
            }
            appendAssignment(builder, values.get(i), expressionRenderer);
        }
        return builder;
    }

    private static void appendAssignment(StringBuilder builder, NameExpr value,
            Consumer<Expression> expressionRenderer) {
        builder.append(value.name).append(value.useEqual ? " = " : " ");
        if (value.expressions != null) {
            for (int i = 0; i < value.expressions.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                expressionRenderer.accept((Expression) value.expressions.get(i));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return appendTo(builder, builder::append).toString();
    }

    public List<NameExpr> getKeyValuePairs() {
        return values;
    }

    public void addKeyValuePairs(Collection<NameExpr> keyValuePairs) {
        onOffOptions = null;
        values.addAll(keyValuePairs);
    }

    public void addKeyValuePairs(NameExpr... keyValuePairs) {
        addKeyValuePairs(Arrays.asList(keyValuePairs));
    }

    public void clear() {
        onOffOptions = null;
        values.clear();
        effectParameter = null;
    }

    @Override
    public <T, S> T accept(StatementVisitor<T> statementVisitor, S context) {
        return statementVisitor.visit(this, context);
    }

    public String getEffectParameter() {
        return effectParameter;
    }

    public void setEffectParameter(String effectParameter) {
        this.effectParameter = effectParameter;
    }

    public SetStatement withEffectParameter(String effectParameter) {
        this.effectParameter = effectParameter;
        return this;
    }

    static class NameExpr implements Serializable {
        Object name;
        ExpressionList expressions;
        boolean useEqual;

        public NameExpr(Object name, ExpressionList<?> expressions, boolean useEqual) {
            this.name = name;
            this.expressions = expressions;
            this.useEqual = useEqual;
        }

        public Object getName() {
            return name;
        }

        public void setName(Object name) {
            this.name = name;
        }

        public ExpressionList<?> getExpressions() {
            return expressions;
        }

        public void setExpressions(ExpressionList<?> expressions) {
            this.expressions = expressions;
        }

        public boolean isUseEqual() {
            return useEqual;
        }

        public void setUseEqual(boolean useEqual) {
            this.useEqual = useEqual;
        }
    }
}
