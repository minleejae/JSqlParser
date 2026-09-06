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
import net.sf.jsqlparser.expression.operators.relational.ExpressionList;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;

public class AlterRole implements Statement {
    public enum Action {
        OPTIONS, RENAME, SET, RESET, ADD_USER, DROP_USER
    }

    private CreateRole.Command command = CreateRole.Command.ROLE;

    public CreateRole.Command getCommand() {
        return command;
    }

    public void setCommand(CreateRole.Command command) {
        this.command = command;
    }

    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    private Action action;

    public Action getAction() {
        return action;
    }

    public void setAction(Action action) {
        this.action = action;
    }

    private boolean useWith;

    public boolean isUseWith() {
        return useWith;
    }

    public void setUseWith(boolean useWith) {
        this.useWith = useWith;
    }

    private List<RoleOption> options = new ArrayList<>();

    public List<RoleOption> getOptions() {
        return options;
    }

    public void setOptions(List<RoleOption> options) {
        this.options = options;
    }

    private String newName;

    public String getNewName() {
        return newName;
    }

    public void setNewName(String newName) {
        this.newName = newName;
    }

    private String database;

    public String getDatabase() {
        return database;
    }

    public void setDatabase(String database) {
        this.database = database;
    }

    private String parameter;

    public String getParameter() {
        return parameter;
    }

    public void setParameter(String parameter) {
        this.parameter = parameter;
    }

    private boolean useEquals;

    public boolean isUseEquals() {
        return useEquals;
    }

    public void setUseEquals(boolean useEquals) {
        this.useEquals = useEquals;
    }

    private boolean fromCurrent;

    public boolean isFromCurrent() {
        return fromCurrent;
    }

    public void setFromCurrent(boolean fromCurrent) {
        this.fromCurrent = fromCurrent;
    }

    private boolean useDefault;

    public boolean isUseDefault() {
        return useDefault;
    }

    public void setUseDefault(boolean useDefault) {
        this.useDefault = useDefault;
    }

    private ExpressionList<Expression> values;

    public ExpressionList<Expression> getValues() {
        return values;
    }

    public void setValues(ExpressionList<Expression> values) {
        this.values = values;
    }

    private List<String> users;

    public List<String> getUsers() {
        return users;
    }

    public void setUsers(List<String> users) {
        this.users = users;
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        sql.append("ALTER ").append(command).append(' ').append(name);
        if (database != null) {
            sql.append(" IN DATABASE ").append(database);
        }
        switch (action) {
            case OPTIONS:
                if (useWith) {
                    sql.append(" WITH");
                }
                for (RoleOption option : options) {
                    sql.append(' ');
                    option.appendTo(sql, visitor);
                }
                break;
            case RENAME:
                sql.append(" RENAME TO ").append(newName);
                break;
            case RESET:
                sql.append(" RESET ").append(parameter);
                break;
            case SET:
                sql.append(" SET ").append(parameter);
                if (fromCurrent) {
                    sql.append(" FROM CURRENT");
                } else {
                    sql.append(useEquals ? " = " : " TO ");
                    if (useDefault) {
                        sql.append("DEFAULT");
                    } else {
                        for (int i = 0; i < values.size(); i++) {
                            if (i > 0) {
                                sql.append(", ");
                            }
                            visitor.accept(values.get(i));
                        }
                    }
                }
                break;
            case ADD_USER:
            case DROP_USER:
                sql.append(action == Action.ADD_USER ? " ADD USER " : " DROP USER ")
                        .append(String.join(", ", users));
                break;
            default:
                throw new IllegalStateException("Unknown role action: " + action);
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
