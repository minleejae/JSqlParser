/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.schema;

import java.io.Serializable;
import java.util.Objects;

/** Distinguishes explicit OWNED BY NONE from an owning column and from an omitted clause. */
public class SequenceOwnership implements Serializable {
    private final Column column;

    private SequenceOwnership(Column column) {
        this.column = column;
    }

    public static SequenceOwnership none() {
        return new SequenceOwnership(null);
    }

    public static SequenceOwnership ownedBy(Column column) {
        return new SequenceOwnership(Objects.requireNonNull(column, "column"));
    }

    public Column getColumn() {
        return column;
    }

    public boolean isNone() {
        return column == null;
    }

    @Override
    public String toString() {
        return "OWNED BY " + (column == null ? "NONE" : column);
    }
}
