/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.extension;

import net.sf.jsqlparser.statement.RoutineReference;
import net.sf.jsqlparser.statement.create.table.ColDataType;
import java.io.Serializable;

public class ExtensionObject implements Serializable {
    private Kind kind;
    private String name;
    private RoutineReference routine;
    private ColDataType sourceType;
    private ColDataType targetType;
    private String accessMethod;
    private String language;
    private boolean procedural;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public RoutineReference getRoutine() {
        return routine;
    }

    public void setRoutine(RoutineReference routine) {
        this.routine = routine;
    }

    public ColDataType getSourceType() {
        return sourceType;
    }

    public void setSourceType(ColDataType sourceType) {
        this.sourceType = sourceType;
    }

    public ColDataType getTargetType() {
        return targetType;
    }

    public void setTargetType(ColDataType targetType) {
        this.targetType = targetType;
    }

    public String getAccessMethod() {
        return accessMethod;
    }

    public void setAccessMethod(String accessMethod) {
        this.accessMethod = accessMethod;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public boolean isProcedural() {
        return procedural;
    }

    public void setProcedural(boolean procedural) {
        this.procedural = procedural;
    }

    public enum Kind {
        ACCESS_METHOD, AGGREGATE, CAST, COLLATION, CONVERSION, DOMAIN, EVENT_TRIGGER, FOREIGN_DATA_WRAPPER, FOREIGN_TABLE, FUNCTION, MATERIALIZED_VIEW, OPERATOR, OPERATOR_CLASS, OPERATOR_FAMILY, LANGUAGE, PROCEDURE, ROUTINE, SCHEMA, SEQUENCE, SERVER, TABLE, TEXT_SEARCH_CONFIGURATION, TEXT_SEARCH_DICTIONARY, TEXT_SEARCH_PARSER, TEXT_SEARCH_TEMPLATE, TRANSFORM, TYPE, VIEW
    }

    public boolean isTable() {
        return kind == Kind.TABLE || kind == Kind.FOREIGN_TABLE || kind == Kind.VIEW
                || kind == Kind.MATERIALIZED_VIEW;
    }

    @Override
    public String toString() {
        String prefix = (procedural ? "PROCEDURAL " : "") + kind.name().replace('_', ' ') + " ";
        if (kind == Kind.CAST) {
            return prefix + "(" + sourceType + " AS " + targetType + ")";
        }
        if (kind == Kind.TRANSFORM) {
            return prefix + "FOR " + sourceType + " LANGUAGE " + language;
        }
        if (kind == Kind.OPERATOR) {
            return prefix + name + " (" + (sourceType == null ? "NONE" : sourceType) + ", "
                    + (targetType == null ? "NONE" : targetType) + ")";
        }
        return prefix + (routine == null ? name : routine)
                + (accessMethod == null ? "" : " USING " + accessMethod);
    }
}
