/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.publication;

import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.replication.ReplicationOption;

public class PublicationOption extends ReplicationOption<PublicationOption.Kind> {
    public enum Kind {
        PUBLISH, PUBLISH_VIA_PARTITION_ROOT, PUBLISH_GENERATED_COLUMNS
    }
    public enum Operation {
        INSERT, UPDATE, DELETE, TRUNCATE
    }
    public enum GeneratedColumns {
        NONE, STORED
    }

    public PublicationOption(String name, Kind kind, Expression value) {
        super(name, kind, value);
    }

    public Boolean getBooleanValue() {
        return getKind() == Kind.PUBLISH_VIA_PARTITION_ROOT ? booleanValue() : null;
    }

    public GeneratedColumns getGeneratedColumns() {
        return getKind() == Kind.PUBLISH_GENERATED_COLUMNS ? enumValue(GeneratedColumns.class)
                : null;
    }

    public Set<Operation> getPublishOperations() {
        if (getKind() != Kind.PUBLISH || getValueText() == null) {
            return null;
        }
        Set<Operation> result = EnumSet.noneOf(Operation.class);
        for (String operation : getValueText().split(",")) {
            try {
                result.add(Operation.valueOf(operation.trim().toUpperCase(Locale.ROOT)));
            } catch (IllegalArgumentException exception) {
                return null;
            }
        }
        return result;
    }
}
