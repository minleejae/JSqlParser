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

public class CompositeTypeDefinition implements TypeDefinition {
    private List<TypeAttribute> attributes = new ArrayList<>();

    public List<TypeAttribute> getAttributes() {
        return attributes;
    }

    public void setAttributes(List<TypeAttribute> attributes) {
        this.attributes = attributes;
    }

    @Override
    public String toString() {
        return "(" + attributes.stream().map(Object::toString).collect(Collectors.joining(", "))
                + ")";
    }
}
