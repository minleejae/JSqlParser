/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.function;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import net.sf.jsqlparser.statement.create.table.TableElement;

/** A SQL Server scalar, inline table or declared table return type. */
public class FunctionReturnType implements Serializable {
    private ColDataType dataType;
    private boolean table;
    private String tableVariable;
    private List<TableElement> tableElements;

    public ColDataType getDataType() {
        return dataType;
    }

    public void setDataType(ColDataType dataType) {
        this.dataType = dataType;
    }

    public boolean isTable() {
        return table;
    }

    public void setTable(boolean table) {
        this.table = table;
    }

    public String getTableVariable() {
        return tableVariable;
    }

    public void setTableVariable(String variable) {
        this.tableVariable = variable;
    }

    /** Null for an inline return table; otherwise columns and constraints in source order. */
    public List<TableElement> getTableElements() {
        return tableElements;
    }

    public void setTableElements(List<TableElement> elements) {
        this.tableElements = elements;
    }

    public <T extends TableElement> List<T> getTableElements(Class<T> type) {
        List<T> result = new ArrayList<>();
        if (tableElements != null) {
            for (TableElement element : tableElements) {
                if (type.isInstance(element)) {
                    result.add(type.cast(element));
                }
            }
        }
        return result;
    }

    public StringBuilder appendTo(StringBuilder builder, Consumer<TableElement> printer) {
        builder.append("RETURNS ");
        if (!table) {
            return builder.append(dataType);
        }
        if (tableVariable != null) {
            builder.append(tableVariable).append(' ');
        }
        builder.append("TABLE");
        if (tableElements != null) {
            builder.append(" (");
            for (int i = 0; i < tableElements.size(); i++) {
                if (i > 0) {
                    builder.append(", ");
                }
                printer.accept(tableElements.get(i));
            }
            builder.append(')');
        }
        return builder;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        return appendTo(builder, builder::append).toString();
    }
}
