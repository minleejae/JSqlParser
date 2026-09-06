/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.trigger;

import java.io.Serializable;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Column;
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;

public class TriggerEvent implements Serializable {
    private CreateTrigger.Event event;
    private ExpressionList<Column> columns;

    public CreateTrigger.Event getEvent() {
        return event;
    }

    public void setEvent(CreateTrigger.Event event) {
        this.event = event;
    }

    public ExpressionList<Column> getColumns() {
        return columns;
    }

    public void setColumns(ExpressionList<Column> columns) {
        this.columns = columns;
    }

    public TriggerEvent(CreateTrigger.Event event) {
        this.event = event;
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        sql.append(event);
        if (columns != null) {
            sql.append(" OF ");
            for (int i = 0; i < columns.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                visitor.accept(columns.get(i));
            }
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
