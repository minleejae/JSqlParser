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

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import java.io.Serializable;

public class PublicationTarget implements Serializable {
    private Kind kind;
    private List<PublicationTable> tables = new ArrayList<>();
    private List<String> schemas = new ArrayList<>();

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public List<PublicationTable> getTables() {
        return tables;
    }

    public void setTables(List<PublicationTable> tables) {
        this.tables = tables;
    }

    public List<String> getSchemas() {
        return schemas;
    }

    public void setSchemas(List<String> schemas) {
        this.schemas = schemas;
    }

    public enum Kind {
        TABLE, TABLES_IN_SCHEMA
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> expressions) {
        if (kind == Kind.TABLES_IN_SCHEMA) {
            sql.append("TABLES IN SCHEMA ").append(String.join(", ", schemas));
        } else {
            sql.append("TABLE ");
            for (int i = 0; i < tables.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                tables.get(i).appendTo(sql, expressions);
            }
        }
    }

    public void visit(Consumer<Table> tableVisitor, Consumer<Expression> expressions) {
        if (kind == Kind.TABLE) {
            tables.forEach(table -> table.visit(tableVisitor, expressions));
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
