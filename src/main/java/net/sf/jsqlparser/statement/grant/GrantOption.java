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

public class GrantOption implements Serializable {
    public enum Kind {
        GRANT, ADMIN, INHERIT, SET
    }
    public enum Value {
        OPTION, TRUE, FALSE
    }

    private Kind kind;
    private Value value = Value.OPTION;

    public Kind getKind() {
        return kind;
    }

    public void setKind(Kind kind) {
        this.kind = kind;
    }

    public Value getValue() {
        return value;
    }

    public void setValue(Value value) {
        this.value = value;
    }

    public GrantOption(Kind kind) {
        this.kind = kind;
    }

    @Override
    public String toString() {
        return kind + " " + value;
    }
}
