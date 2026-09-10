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

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import net.sf.jsqlparser.expression.StringValue;

public class EnumTypeDefinition implements TypeDefinition {
    private List<StringValue> labels = new ArrayList<>();

    public List<StringValue> getLabels() {
        return labels;
    }

    public void setLabels(List<StringValue> labels) {
        this.labels = labels;
    }

    @Override
    public String toString() {
        return "ENUM (" + labels.stream().map(Object::toString).collect(Collectors.joining(", "))
                + ")";
    }
}
