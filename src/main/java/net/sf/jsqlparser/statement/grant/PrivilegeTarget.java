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
import java.util.stream.Collectors;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.RoutineReference;

/** Targets have distinct kinds; a routine signature is not a function invocation. */
public class PrivilegeTarget implements Serializable {
    public enum Kind {
        TABLE, SEQUENCE, DATABASE, DOMAIN, FOREIGN_DATA_WRAPPER, FOREIGN_SERVER, FUNCTION, PROCEDURE, ROUTINE, LANGUAGE, LARGE_OBJECT, PARAMETER, SCHEMA, TABLESPACE, TYPE
    }

    private Kind kind = Kind.TABLE;
    private boolean explicitKind;
    private boolean allInSchema;
    private boolean defaultPrivileges;
    private List<List<String>> names = new ArrayList<>();
    private List<RoutineReference> routines = new ArrayList<>();

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public boolean isExplicitKind() {
        return explicitKind;
    }

    public void setExplicitKind(boolean explicitKind) {
        this.explicitKind = explicitKind;
    }

    public boolean isAllInSchema() {
        return allInSchema;
    }

    public void setAllInSchema(boolean allInSchema) {
        this.allInSchema = allInSchema;
    }

    public boolean isDefaultPrivileges() {
        return defaultPrivileges;
    }

    public void setDefaultPrivileges(boolean defaultPrivileges) {
        this.defaultPrivileges = defaultPrivileges;
    }

    public List<List<String>> getNames() {
        return names;
    }

    public void setNames(List<List<String>> names) {
        this.names = names;
    }

    public List<RoutineReference> getRoutines() {
        return routines;
    }

    public void setRoutines(List<RoutineReference> routines) {
        this.routines = routines;
    }

    public void visitTables(Consumer<Table> visitor) {
        if (kind == Kind.TABLE && !allInSchema && !defaultPrivileges) {
            for (List<String> name : names) {
                if (!name.isEmpty()) {
                    visitor.accept(new Table(name));
                }
            }
        }
    }

    @Override
    public String toString() {
        String type = kind.name().replace('_', ' ');
        String targets = routines.isEmpty()
                ? names.stream().map(n -> n.stream().map(part -> part == null ? "" : part)
                        .collect(Collectors.joining("."))).collect(Collectors.joining(", "))
                : routines.stream().map(Object::toString).collect(Collectors.joining(", "));
        if (defaultPrivileges) {
            return type + "S";
        }
        if (allInSchema) {
            return "ALL " + type + "S IN SCHEMA " + targets;
        }
        return (explicitKind ? type + " " : "") + targets;
    }
}
