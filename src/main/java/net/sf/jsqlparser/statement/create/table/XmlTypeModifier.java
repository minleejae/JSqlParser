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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/** A SQL Server XML schema collection reference, with an optional CONTENT/DOCUMENT facet. */
public final class XmlTypeModifier implements Serializable {
    public enum Kind {
        CONTENT, DOCUMENT
    }

    private final Kind kind;
    private final List<String> schemaCollection;

    public XmlTypeModifier(Kind kind, List<String> schemaCollection) {
        if (schemaCollection == null || schemaCollection.isEmpty() || schemaCollection.size() > 2
                || schemaCollection.stream().anyMatch(Objects::isNull)) {
            throw new IllegalArgumentException(
                    "An XML schema collection needs a name and optional schema");
        }
        this.kind = kind;
        this.schemaCollection = Collections.unmodifiableList(new ArrayList<>(schemaCollection));
    }

    /** Null preserves an omitted facet, which SQL Server interprets as CONTENT. */
    public Kind getKind() {
        return kind;
    }

    public Kind getEffectiveKind() {
        return kind == null ? Kind.CONTENT : kind;
    }

    /** Name parts retain their original identifier quoting. */
    public List<String> getSchemaCollection() {
        return schemaCollection;
    }

    @Override
    public String toString() {
        return "(" + (kind == null ? "" : kind + " ") + String.join(".", schemaCollection) + ")";
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof XmlTypeModifier)) {
            return false;
        }
        XmlTypeModifier that = (XmlTypeModifier) other;
        return kind == that.kind && schemaCollection.equals(that.schemaCollection);
    }

    @Override
    public int hashCode() {
        return Objects.hash(kind, schemaCollection);
    }
}
