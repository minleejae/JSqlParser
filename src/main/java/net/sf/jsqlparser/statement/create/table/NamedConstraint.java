/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.table;

import java.util.Collection;
import java.util.List;

import net.sf.jsqlparser.statement.select.PlainSelect;

public class NamedConstraint extends Index {

    private String indexName;
    private boolean useConstraintKeyword;
    private ConstraintNamePosition constraintNamePosition = ConstraintNamePosition.BEFORE;

    /** Position of the constraint symbol relative to its definition. */
    public enum ConstraintNamePosition {
        BEFORE, AFTER
    }

    public ConstraintNamePosition getConstraintNamePosition() {
        return constraintNamePosition;
    }

    public void setConstraintNamePosition(ConstraintNamePosition position) {
        constraintNamePosition = java.util.Objects.requireNonNull(position, "position");
    }

    public NamedConstraint withConstraintNamePosition(ConstraintNamePosition position) {
        setConstraintNamePosition(position);
        return this;
    }

    /** Appends the leading keyword and, for the usual syntax, the constraint name. */
    public void appendConstraintPrefixTo(StringBuilder builder) {
        boolean leadingName = getName() != null
                && constraintNamePosition == ConstraintNamePosition.BEFORE;
        if (useConstraintKeyword || leadingName) {
            builder.append("CONSTRAINT");
            if (leadingName) {
                builder.append(' ').append(getName());
            }
            builder.append(' ');
        }
    }

    /** Appends an Informix constraint name after the complete constraint definition. */
    public void appendConstraintSuffixTo(StringBuilder builder) {
        if (constraintNamePosition == ConstraintNamePosition.AFTER && getName() != null) {
            builder.append(" CONSTRAINT ").append(getName());
        }
    }

    /**
     * Returns the optional index name declared after the constraint type. This is distinct from
     * {@link #getName()}, which represents the optional constraint symbol.
     *
     * @return the index name, or {@code null} when it was omitted
     */
    public String getIndexName() {
        return indexName;
    }

    public void setIndexName(String indexName) {
        this.indexName = indexName;
    }

    public boolean isUseConstraintKeyword() {
        return useConstraintKeyword;
    }

    public void setUseConstraintKeyword(boolean useConstraintKeyword) {
        this.useConstraintKeyword = useConstraintKeyword;
    }

    @Override
    public String toString() {
        String idxSpecText = PlainSelect.getStringList(getIndexSpec(), false, false);
        String keyword = getIndexKeyword() != null
                && !getType().toUpperCase(java.util.Locale.ROOT)
                        .endsWith(getIndexKeyword().toUpperCase(java.util.Locale.ROOT))
                                ? " " + getIndexKeyword()
                                : "";
        String tail = getType()
                + nullsDistinctClause()
                + keyword
                + (indexName != null ? " " + indexName : "")
                + (getUsing() != null ? " USING " + getUsing() : "")
                + (getColumns() == null ? ""
                        : " " + PlainSelect.getStringList(getColumnsNames(), true, true))
                +
                (!"".equals(idxSpecText) ? " " + idxSpecText : "");
        StringBuilder sql = new StringBuilder();
        appendConstraintPrefixTo(sql);
        sql.append(tail);
        appendConstraintOptionsTo(sql);
        if (getKind() != Kind.FOREIGN_KEY) {
            appendConstraintSuffixTo(sql);
            appendConstraintAttributesTo(sql);
        }
        return sql.toString();
    }

    public NamedConstraint withIndexName(String indexName) {
        setIndexName(indexName);
        return this;
    }

    public NamedConstraint withUseConstraintKeyword(boolean useConstraintKeyword) {
        setUseConstraintKeyword(useConstraintKeyword);
        return this;
    }

    @Override
    public NamedConstraint withName(List<String> name) {
        return (NamedConstraint) super.withName(name);
    }

    @Override
    public NamedConstraint withName(String name) {
        return (NamedConstraint) super.withName(name);
    }

    @Override
    public NamedConstraint withType(String type) {
        return (NamedConstraint) super.withType(type);
    }

    @Override
    public NamedConstraint withUsing(String using) {
        return (NamedConstraint) super.withUsing(using);
    }

    @Override
    public NamedConstraint withColumnsNames(List<String> list) {
        return (NamedConstraint) super.withColumnsNames(list);
    }

    @Override
    public NamedConstraint withColumns(List<ColumnParams> columns) {
        return (NamedConstraint) super.withColumns(columns);
    }

    @Override
    public NamedConstraint addColumns(ColumnParams... functionDeclarationParts) {
        return (NamedConstraint) super.addColumns(functionDeclarationParts);
    }

    @Override
    public NamedConstraint addColumns(Collection<? extends ColumnParams> functionDeclarationParts) {
        return (NamedConstraint) super.addColumns(functionDeclarationParts);
    }

    @Override
    public NamedConstraint withIndexSpec(List<String> idxSpec) {
        return (NamedConstraint) super.withIndexSpec(idxSpec);
    }

    @Override
    public NamedConstraint withIndexKeyword(String indexKeyword) {
        return (NamedConstraint) super.withIndexKeyword(indexKeyword);
    }
}
