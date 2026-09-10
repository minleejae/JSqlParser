/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.type;

import net.sf.jsqlparser.statement.create.table.ColDataType;
import java.io.Serializable;

public class TypeAttribute implements Serializable {
    private String name;
    private ColDataType dataType;
    private String collation;

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

    public String getCollation() {
        return collation;
    }

    public void setCollation(String collation) {
        this.collation = collation;
    }

    @Override
    public String toString() {
        return name + " " + dataType + (collation == null ? "" : " COLLATE " + collation);
    }
}
