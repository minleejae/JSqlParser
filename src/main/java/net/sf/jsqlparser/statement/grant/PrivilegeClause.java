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
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;

/** Shared, editable GRANT/REVOKE payload. Null privileges denote role membership. */
public class PrivilegeClause implements Serializable {
    private List<Privilege> privileges;
    private List<String> grantees;
    private String grantedBy;
    private List<String> roles = new ArrayList<>();
    private PrivilegeTarget target = new PrivilegeTarget();

    public List<Privilege> getPrivileges() {
        return privileges;
    }

    public void setPrivileges(List<Privilege> privileges) {
        this.privileges = privileges;
    }

    public List<String> getRoles() {
        return roles;
    }

    public void setRoles(List<String> roles) {
        this.roles = roles;
    }

    public PrivilegeTarget getTarget() {
        return target;
    }

    public void setTarget(PrivilegeTarget target) {
        this.target = target;
    }

    public List<String> getGrantees() {
        return grantees;
    }

    public void setGrantees(List<String> grantees) {
        this.grantees = grantees;
    }

    public String getGrantedBy() {
        return grantedBy;
    }

    public void setGrantedBy(String grantedBy) {
        this.grantedBy = grantedBy;
    }

    public void appendTo(StringBuilder sql, boolean revoke, Consumer<Expression> visitor) {
        if (privileges == null) {
            sql.append(String.join(", ", roles));
        } else {
            for (int i = 0; i < privileges.size(); i++) {
                if (i > 0) {
                    sql.append(", ");
                }
                privileges.get(i).appendTo(sql, visitor);
            }
            sql.append(" ON ").append(target);
        }
        sql.append(revoke ? " FROM " : " TO ").append(String.join(", ", grantees));
    }

    public void visit(Consumer<Table> tables, Consumer<Expression> expressions) {
        if (privileges != null) {
            target.visitTables(tables);
            for (Privilege privilege : privileges) {
                if (privilege.getColumns() != null) {
                    privilege.getColumns().forEach(expressions);
                }
            }
        }
    }
}
