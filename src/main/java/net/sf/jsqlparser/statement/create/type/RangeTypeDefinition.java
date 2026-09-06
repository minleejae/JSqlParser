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
import net.sf.jsqlparser.statement.create.table.ColDataType;

public class RangeTypeDefinition implements TypeDefinition {
    private List<Option> options = new ArrayList<>();

    public List<Option> getOptions() {
        return options;
    }

    public void setOptions(List<Option> options) {
        this.options = options;
    }

    public enum OptionKind {
        SUBTYPE, SUBTYPE_OPCLASS, COLLATION, CANONICAL, SUBTYPE_DIFF, MULTIRANGE_TYPE_NAME
    }
    public static class Option implements java.io.Serializable {
        private final OptionKind kind;
        private final ColDataType dataType;
        private final String name;

        public Option(ColDataType dataType) {
            this.kind = OptionKind.SUBTYPE;
            this.dataType = dataType;
            this.name = null;
        }

        public Option(OptionKind kind, String name) {
            this.kind = kind;
            this.name = name;
            this.dataType = null;
        }

        public OptionKind getKind() {
            return kind;
        }

        public ColDataType getDataType() {
            return dataType;
        }

        public String getName() {
            return name;
        }

        @Override
        public String toString() {
            return kind + " = " + (dataType == null ? name : dataType);
        }
    }

    public ColDataType getSubtype() {
        return options.stream().filter(option -> option.getKind() == OptionKind.SUBTYPE)
                .map(Option::getDataType).findFirst().orElse(null);
    }

    @Override
    public String toString() {
        return "RANGE (" + options.stream().map(Object::toString).collect(Collectors.joining(", "))
                + ")";
    }
}
