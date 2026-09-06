/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement;

import java.util.List;
import java.util.stream.Collectors;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import java.io.Serializable;

public class RoutineReference implements Serializable {
    private String name;
    private List<Argument> arguments;
    private boolean allArguments;
    private List<Argument> orderByArguments;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<Argument> getArguments() {
        return arguments;
    }

    public void setArguments(List<Argument> arguments) {
        this.arguments = arguments;
    }

    public boolean isAllArguments() {
        return allArguments;
    }

    public void setAllArguments(boolean allArguments) {
        this.allArguments = allArguments;
    }

    public List<Argument> getOrderByArguments() {
        return orderByArguments;
    }

    public void setOrderByArguments(List<Argument> orderByArguments) {
        this.orderByArguments = orderByArguments;
    }

    /** A signature argument is a data type, not an invocation expression. */
    public static class Argument implements Serializable {
        public enum Mode {
            IN, OUT, INOUT, VARIADIC
        }

        private Mode mode;
        private String name;
        private ColDataType dataType;

        public Mode getMode() {
            return mode;
        }

        public void setMode(Mode mode) {
            this.mode = mode;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ColDataType getDataType() {
            return dataType;
        }

        public void setDataType(ColDataType dataType) {
            this.dataType = dataType;
        }

        @Override
        public String toString() {
            return (mode == null ? "" : mode + " ") + (name == null ? "" : name + " ") + dataType;
        }
    }

    @Override
    public String toString() {
        if (arguments == null && !allArguments && orderByArguments == null) {
            return name;
        }
        StringBuilder sql = new StringBuilder(name).append('(');
        if (allArguments) {
            sql.append('*');
        } else if (arguments != null) {
            sql.append(arguments.stream().map(Object::toString).collect(Collectors.joining(", ")));
        }
        if (orderByArguments != null) {
            if (arguments != null && !arguments.isEmpty()) {
                sql.append(' ');
            }
            sql.append("ORDER BY ").append(orderByArguments.stream().map(Object::toString)
                    .collect(Collectors.joining(", ")));
        }
        return sql.append(')').toString();
    }
}
