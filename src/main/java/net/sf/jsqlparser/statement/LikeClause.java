/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2022 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement;

import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.imprt.ImportColumn;
import net.sf.jsqlparser.statement.create.table.TableElement;
import net.sf.jsqlparser.statement.select.PlainSelect;
import net.sf.jsqlparser.statement.select.SelectItem;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

/**
 * A LIKE clause used in table definitions and Exasol imports.
 *
 * @see <a href="https://docs.exasol.com/db/latest/sql/create_table.htm">Like Clause in CREATE
 *      TABLE</a>
 * @see <a href="https://docs.exasol.com/db/latest/sql/import.htm">Like Clause in IMPORT</a>
 */
public class LikeClause implements ImportColumn, TableElement, Serializable {
    private Table table;
    private List<SelectItem<Column>> columnsList;

    private List<Option> options = new ArrayList<>();

    public enum OptionKind {
        ALL, COMMENTS, COMPRESSION, CONSTRAINTS, DEFAULTS, GENERATED, IDENTITY, INDEXES, STATISTICS, STORAGE
    }

    public static class Option implements Serializable {
        private final OptionKind kind;
        private final boolean including;

        public Option(OptionKind kind, boolean including) {
            this.kind = kind;
            this.including = including;
        }

        public OptionKind getKind() {
            return kind;
        }

        public boolean isIncluding() {
            return including;
        }

        @Override
        public String toString() {
            return (including ? "INCLUDING " : "EXCLUDING ") + kind;
        }
    }

    /** Options in source order, including repeated options and ALL. */
    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options == null ? new ArrayList<>() : new ArrayList<>(options);
    }

    public void addOption(OptionKind kind, boolean including) {
        options.add(new Option(kind, including));
    }

    /** Resolves ALL and explicit options in declaration order; omission defaults to exclusion. */
    public boolean isIncluding(OptionKind kind) {
        boolean including = false;
        for (Option option : options) {
            if (option.getKind() == kind || option.getKind() == OptionKind.ALL) {
                including = option.isIncluding();
            }
        }
        return including;
    }

    private Boolean explicitIncluding(OptionKind kind) {
        Boolean including = null;
        for (Option option : options) {
            if (option.getKind() == kind) {
                including = option.isIncluding();
            }
        }
        return including;
    }

    private void setIncluding(OptionKind kind, Boolean including) {
        options.removeIf(option -> option.getKind() == kind);
        if (including != null) {
            addOption(kind, including);
        }
    }

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public List<SelectItem<Column>> getColumnsList() {
        return columnsList;
    }

    public void setColumnsList(List<SelectItem<Column>> columnsList) {
        this.columnsList = columnsList;
    }

    public Boolean isIncludingDefaults() {
        return explicitIncluding(OptionKind.DEFAULTS);
    }

    public void setIncludingDefaults(Boolean includingDefaults) {
        setIncluding(OptionKind.DEFAULTS, includingDefaults);
    }

    public Boolean isExcludingDefaults() {
        return isIncludingDefaults() == null ? null : !isIncludingDefaults();
    }

    public void setExcludingDefaults(Boolean excludingDefaults) {
        setIncludingDefaults(excludingDefaults == null ? null : !excludingDefaults);
    }

    public Boolean isIncludingIdentity() {
        return explicitIncluding(OptionKind.IDENTITY);
    }

    public void setIncludingIdentity(Boolean includingIdentity) {
        setIncluding(OptionKind.IDENTITY, includingIdentity);
    }

    public Boolean isExcludingIdentity() {
        return isIncludingIdentity() == null ? null : !isIncludingIdentity();
    }

    public void setExcludingIdentity(Boolean excludingIdentity) {
        setIncludingIdentity(excludingIdentity == null ? null : !excludingIdentity);
    }

    public Boolean isIncludingComments() {
        return explicitIncluding(OptionKind.COMMENTS);
    }

    public void setIncludingComments(Boolean includingComments) {
        setIncluding(OptionKind.COMMENTS, includingComments);
    }

    public Boolean isExcludingComments() {
        return isIncludingComments() == null ? null : !isIncludingComments();
    }

    public void setExcludingComments(Boolean excludingComments) {
        setIncludingComments(excludingComments == null ? null : !excludingComments);
    }

    public StringBuilder appendTo(StringBuilder builder) {
        builder.append(" LIKE ");
        builder.append(table);
        if (columnsList != null) {
            builder.append(" ");
            PlainSelect.appendStringListTo(builder, columnsList, true, true);
        }

        for (Option option : options) {
            builder.append(' ').append(option);
        }

        return builder;
    }

    @Override
    public String toString() {
        return appendTo(new StringBuilder()).toString().trim();
    }
}
