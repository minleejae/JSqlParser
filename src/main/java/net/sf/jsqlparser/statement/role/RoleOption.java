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

import java.io.Serializable;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;

/** Ordered role attributes; absent attributes are not replaced by server defaults. */
public class RoleOption implements Serializable {
    public enum Kind {
        SUPERUSER, NOSUPERUSER, CREATEDB, NOCREATEDB, CREATEROLE, NOCREATEROLE, INHERIT, NOINHERIT, LOGIN, NOLOGIN, REPLICATION, NOREPLICATION, BYPASSRLS, NOBYPASSRLS, CONNECTION_LIMIT, PASSWORD, VALID_UNTIL, IN_ROLE, IN_GROUP, ROLE, USER, ADMIN, SYSID
    }

    private Kind kind;
    private Expression value;
    private boolean encrypted;
    private List<String> roles;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Expression getValue() {
        return value;
    }

    public void setValue(Expression value) {
        this.value = value;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public RoleOption(Kind kind) {
        this.kind = kind;
    }

    public void appendTo(StringBuilder sql, Consumer<Expression> visitor) {
        if (encrypted) {
            sql.append("ENCRYPTED ");
        }
        sql.append(kind.name().replace('_', ' '));
        if (value != null) {
            sql.append(' ');
            visitor.accept(value);
        }
        if (roles != null) {
            sql.append(' ').append(String.join(", ", roles));
        }
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder();
        appendTo(sql, sql::append);
        return sql.toString();
    }
}
