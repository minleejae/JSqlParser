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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.select.PlainSelect;

/** A structured option following a column data type. */
public class ColumnOption implements Serializable {

    public enum Kind {
        SERIAL_DEFAULT_VALUE, REFERENCE, IDENTITY, CONSTRAINT, DEFAULT, OTHER
    }

    private Kind kind = Kind.OTHER;
    private List<String> tokens;
    private ForeignKeyReference foreignKeyReference;
    private IdentityDefinition identityDefinition;
    private Index constraint;
    private Expression defaultExpression;

    /** Creates a DEFAULT option. Use a NullValue expression for SQL NULL. */
    public static ColumnOption defaultValue(Expression expression) {
        ColumnOption option = new ColumnOption();
        option.kind = Kind.DEFAULT;
        option.setDefaultExpression(expression);
        return option;
    }

    public Expression getDefaultExpression() {
        return defaultExpression;
    }

    /** Replaces the expression of a DEFAULT option created by {@link #defaultValue(Expression)}. */
    public void setDefaultExpression(Expression expression) {
        defaultExpression = Objects.requireNonNull(expression, "defaultExpression");
    }

    public static ColumnOption identity(IdentityDefinition definition) {
        ColumnOption option = new ColumnOption();
        option.kind = Kind.IDENTITY;
        option.identityDefinition = definition;
        return option;
    }

    public static ColumnOption constraint(Index constraint) {
        ColumnOption option = new ColumnOption();
        option.kind = Kind.CONSTRAINT;
        option.constraint = constraint;
        return option;
    }

    public IdentityDefinition getIdentityDefinition() {
        return identityDefinition;
    }

    public Index getConstraint() {
        return constraint;
    }

    public static ColumnOption raw(List<String> tokens) {
        ColumnOption option = new ColumnOption();
        option.tokens = tokens;
        return option;
    }

    public static ColumnOption raw(String... tokens) {
        return raw(Arrays.asList(tokens));
    }

    public static ColumnOption serialDefaultValue() {
        ColumnOption option = raw("SERIAL", "DEFAULT", "VALUE");
        option.kind = Kind.SERIAL_DEFAULT_VALUE;
        return option;
    }

    public static ColumnOption reference(ForeignKeyReference reference) {
        ColumnOption option = new ColumnOption();
        option.kind = Kind.REFERENCE;
        option.foreignKeyReference = reference;
        return option;
    }

    public Kind getKind() {
        return kind;
    }

    public List<String> getTokens() {
        if (kind == Kind.DEFAULT) {
            return Arrays.asList("DEFAULT", String.valueOf(defaultExpression));
        }
        return kind == Kind.OTHER || kind == Kind.SERIAL_DEFAULT_VALUE ? tokens
                : Collections.singletonList(toString());
    }

    public ForeignKeyReference getForeignKeyReference() {
        return foreignKeyReference;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        appendTo(builder, builder::append);
        return builder.toString();
    }

    /** Appends the option using the supplied printer for structured expressions. */
    public void appendTo(StringBuilder builder, Consumer<Expression> expressionPrinter) {
        switch (kind) {
            case DEFAULT:
                builder.append("DEFAULT ");
                expressionPrinter.accept(defaultExpression);
                break;
            case REFERENCE:
                builder.append(foreignKeyReference);
                break;
            case IDENTITY:
                builder.append(identityDefinition);
                break;
            case CONSTRAINT:
                constraint.appendTo(builder, expressionPrinter);
                break;
            default:
                builder.append(PlainSelect.getStringList(tokens, false, false));
                break;
        }
    }
}
