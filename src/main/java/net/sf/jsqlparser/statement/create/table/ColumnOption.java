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
import net.sf.jsqlparser.statement.select.PlainSelect;

/** A structured option following a column data type. */
public class ColumnOption implements Serializable {

    public enum Kind {
        SERIAL_DEFAULT_VALUE, REFERENCE, IDENTITY, CONSTRAINT, OTHER
    }

    private Kind kind = Kind.OTHER;
    private List<String> tokens;
    private ForeignKeyReference foreignKeyReference;
    private IdentityDefinition identityDefinition;
    private Index constraint;

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
        return kind == Kind.OTHER || kind == Kind.SERIAL_DEFAULT_VALUE ? tokens
                : Collections.singletonList(toString());
    }

    public ForeignKeyReference getForeignKeyReference() {
        return foreignKeyReference;
    }

    @Override
    public String toString() {
        switch (kind) {
            case REFERENCE:
                return foreignKeyReference.toString();
            case IDENTITY:
                return identityDefinition.toString();
            case CONSTRAINT:
                return constraint.toString();
            default:
                return PlainSelect.getStringList(tokens, false, false);
        }
    }
}
