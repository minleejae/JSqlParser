/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.grant;

import java.io.Serializable;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.schema.Column;

public class Privilege implements Serializable {
    public enum Kind {
        ALL, SELECT, INSERT, UPDATE, DELETE, TRUNCATE, REFERENCES, TRIGGER, MAINTAIN, USAGE, CREATE, CONNECT, TEMPORARY, TEMP, EXECUTE, SET, ALTER_SYSTEM, ALTER, DROP
    }

    private String legacyName;
    private Kind kind;
    private boolean usePrivileges;
    private ExpressionList<Column> columns;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public boolean isUsePrivileges() {
        return usePrivileges;
    }

    public void setUsePrivileges(boolean usePrivileges) {
        this.usePrivileges = usePrivileges;
    }

    public ExpressionList<Column> getColumns() {
        return columns;
    }

    public void setColumns(ExpressionList<Column> columns) {
        this.columns = columns;
    }

    public Privilege(Kind kind) {
        this.kind = kind;
    }

    /** Preserves values supplied through the pre-existing string-based Grant API. */
    public Privilege(String name) {
        try {
            this.kind = Kind.valueOf(name);
        } catch (IllegalArgumentException ex) {
            this.legacyName = name;
        }
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        sql.append(kind == null ? legacyName : kind.name().replace('_', ' '));
        if (usePrivileges) {
            sql.append(" PRIVILEGES");
        }
        if (columns != null) {
            sql.append(" (");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                visitor.accept(columns.get(i));
            }
            sql.append(')');
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
