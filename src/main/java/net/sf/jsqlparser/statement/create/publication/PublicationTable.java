/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.publication;

import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.schema.Column;
import java.io.Serializable;

public class PublicationTable implements Serializable {
    private Table table;
    private boolean only;
    private boolean includeDescendants;
    private ExpressionList<Column> columns;
    private Expression where;

    public Table getTable() {
        return table;
    }

    public void setTable(Table table) {
        this.table = table;
    }

    public boolean isOnly() {
        return only;
    }

    public void setOnly(boolean only) {
        this.only = only;
    }

    public boolean isIncludeDescendants() {
        return includeDescendants;
    }

    public void setIncludeDescendants(boolean includeDescendants) {
        this.includeDescendants = includeDescendants;
    }

    public ExpressionList<Column> getColumns() {
        return columns;
    }

    public void setColumns(ExpressionList<Column> columns) {
        this.columns = columns;
    }

    public Expression getWhere() {
        return where;
    }

    public void setWhere(Expression where) {
        this.where = where;
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> expressions) {
        if (only) {
            sql.append("ONLY ");
        }
        sql.append(table);
        if (includeDescendants) {
            sql.append(" *");
        }
        if (columns != null) {
            sql.append(" (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                expressions.accept(columns.get(i));
            }
            sql.append(')');
        }
        if (where != null) {
            sql.append(" WHERE (");
            expressions.accept(where);
            sql.append(')');
        }
    }

    public void visit(Consumer<Table> tables, Consumer<Expression> expressions) {
        tables.accept(table);
        if (columns != null) {
            columns.forEach(expressions);
        }
        if (where != null) {
            expressions.accept(where);
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
