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

import java.util.AbstractList;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.StatementVisitor;

public class Grant implements Statement {
    private PrivilegeClause clause = new PrivilegeClause();
    private GrantOption option;

    public PrivilegeClause getClause() {
        return clause;
    }

    public void setClause(PrivilegeClause clause) {
        this.clause = clause;
    }

    public GrantOption getOption() {
        return option;
    }

    public void setOption(GrantOption option) {
        this.option = option;
    }

    public List<Privilege> getPrivilegeItems() {
        return clause.getPrivileges();
    }

    public void setPrivilegeItems(List<Privilege> privileges) {
        clause.setPrivileges(privileges);
    }

    public PrivilegeTarget getTarget() {
        return clause.getTarget();
    }

    public void setTarget(PrivilegeTarget target) {
        clause.setTarget(target);
    }

    public List<String> getRoles() {
        return clause.getRoles();
    }

    public void setRoles(List<String> roles) {
        clause.setRoles(roles);
    }

    public String getRole() {
        return clause.getRoles().isEmpty() ? null : clause.getRoles().get(0);
    }

    public void setRole(String role) {
        clause.setRoles(new ArrayList<>());
        if (role != null) {
            clause.getRoles().add(role);
            clause.setPrivileges(null);
        }
    }

    /** Mutable legacy string view over the typed privileges. */
    public List<String> getPrivileges() {
        if (clause.getPrivileges() == null) {
            return null;
        }
        return new AbstractList<String>() {
            @Override
            public int size() {
                return clause.getPrivileges().size();
            }

            @Override
            public String get(int index) {
                return clause.getPrivileges().get(index).toString();
            }

            @Override
            public String set(int index, String value) {
                return clause.getPrivileges().set(index, new Privilege(value)).toString();
            }

            @Override
            public void add(int index, String value) {
                clause.getPrivileges().add(index, new Privilege(value));
            }

            @Override
            public String remove(int index) {
                return clause.getPrivileges().remove(index).toString();
            }
        };
    }

    public void setPrivileges(List<String> privileges) {
        List<Privilege> items = null;
        if (privileges != null) {
            items = new ArrayList<>();
            for (String privilege : privileges) {
                items.add(new Privilege(privilege));
            }
        }
        clause.setPrivileges(items);
    }

    public String getObjectName() {
        if (clause.getTarget().getNames().isEmpty()
                || clause.getTarget().getNames().get(0).isEmpty()) {
            return null;
        }
        List<String> parts = clause.getTarget().getNames().get(0);
        return parts.stream().map(part -> part == null ? "" : part)
                .collect(Collectors.joining("."));
    }

    public void setObjectName(String objectName) {
        setObjectName(Collections.singletonList(objectName));
    }

    public void setObjectName(List<String> objectName) {
        List<String> parts = new ArrayList<>(objectName);
        getObjectNameParts().clear();
        getObjectNameParts().addAll(parts);
    }

    /** Mutable parts of the first named target, retained for compatibility. */
    public List<String> getObjectNameParts() {
        if (clause.getTarget().getNames().isEmpty()) {
            clause.getTarget().getNames().add(new ArrayList<>());
        }
        return clause.getTarget().getNames().get(0);
    }

    public List<String> getUsers() {
        return clause.getGrantees();
    }

    public void setUsers(List<String> users) {
        clause.setGrantees(users);
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        sql.append("GRANT ");
        clause.appendTo(sql, false, visitor);
        if (option != null) {
            sql.append(" WITH ").append(option);
        }
        if (clause.getGrantedBy() != null) {
            sql.append(" GRANTED BY ").append(clause.getGrantedBy());
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

    public Grant withRole(String role) {
        this.setRole(role);
        return this;
    }

    public Grant withPrivileges(List<String> privileges) {
        this.setPrivileges(privileges);
        return this;
    }

    public Grant withObjectName(String objectName) {
        this.setObjectName(objectName);
        return this;
    }

    public Grant withObjectName(List<String> objectName) {
        this.setObjectName(objectName);
        return this;
    }

    public Grant withUsers(List<String> users) {
        this.setUsers(users);
        return this;
    }

    public Grant addPrivileges(String... privileges) {
        return addPrivileges(Arrays.asList(privileges));
    }

    public Grant addPrivileges(Collection<String> privileges) {
        List<String> additions = new ArrayList<>(privileges);
        if (clause.getPrivileges() == null) {
            clause.setPrivileges(new ArrayList<>());
        }
        for (String privilege : additions) {
            clause.getPrivileges().add(new Privilege(privilege));
        }
        return this;
    }

    public Grant addUsers(String... users) {
        List<String> collection = Optional.ofNullable(getUsers()).orElseGet(ArrayList::new);
        Collections.addAll(collection, users);
        return this.withUsers(collection);
    }

    public Grant addUsers(Collection<String> users) {
        List<String> collection = Optional.ofNullable(getUsers()).orElseGet(ArrayList::new);
        collection.addAll(users);
        return this.withUsers(collection);
    }
}
