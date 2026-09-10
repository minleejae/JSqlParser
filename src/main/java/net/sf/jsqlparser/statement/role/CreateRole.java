/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.role;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;

public class CreateRole implements Statement {
    public enum Command {
        ROLE, USER, GROUP
    }

    private Command command = Command.ROLE;
    private String name;
    private boolean useWith;
    private List<RoleOption> options = new ArrayList<>();

    public Command getCommand() {
        return command;
    }

    public void setCommand(Command command) {
        this.command = command;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isUseWith() {
        return useWith;
    }

    public void setUseWith(boolean useWith) {
        this.useWith = useWith;
    }

    public List<RoleOption> getOptions() {
        return options;
    }

    public void setOptions(List<RoleOption> options) {
        this.options = options;
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        sql.append("CREATE ").append(command).append(' ').append(name);
        if (useWith) {
            sql.append(" WITH");
        }
        for (RoleOption option : options) {
            sql.append(' ');
            option.appendTo(sql, visitor);
        }
    }

    @Override
    public <T, S> T accept(StatementVisitor<T> visitor, S context) {
        return visitor.visit(this, context);
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
