/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.create.table;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import net.sf.jsqlparser.schema.Sequence;
import net.sf.jsqlparser.statement.select.PlainSelect;

/** The generation mode and sequence parameters of a GENERATED ... AS IDENTITY clause. */
public class IdentityDefinition implements Serializable {
    public enum GenerationMode {
        ALWAYS, BY_DEFAULT
    }

    private GenerationMode generationMode;
    private List<Sequence.Parameter> parameters;

    public IdentityDefinition(GenerationMode generationMode) {
        this.generationMode = generationMode;
    }

    public GenerationMode getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(GenerationMode generationMode) {
        this.generationMode = generationMode;
    }

    /** Returns null when no parenthesized sequence options were specified. */
    public List<Sequence.Parameter> getParameters() {
        return parameters;
    }

    public void setParameters(List<Sequence.Parameter> parameters) {
        this.parameters = parameters == null ? null : new ArrayList<>(parameters);
    }

    @Override
    public String toString() {
        StringBuilder sql = new StringBuilder("GENERATED ");
        sql.append(generationMode == GenerationMode.ALWAYS ? "ALWAYS" : "BY DEFAULT")
                .append(" AS IDENTITY");
        if (parameters != null) {
            sql.append(" (").append(PlainSelect.getStringList(parameters, false, false))
                    .append(')');
        }
        return sql.toString();
    }
}
