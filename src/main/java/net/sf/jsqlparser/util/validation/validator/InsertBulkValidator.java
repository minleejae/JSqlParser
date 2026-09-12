/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util.validation.validator;

import java.util.stream.Collectors;
import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.insert.InsertBulk;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.util.validation.ValidationCapability;
import net.sf.jsqlparser.util.validation.metadata.NamedObject;

public class InsertBulkValidator extends AbstractValidator<InsertBulk> {
    @Override
    public void validate(InsertBulk statement) {
        validateFeature(Feature.insertBulk);
        validateOptionalFromItem(statement.getTable());
        for (ValidationCapability capability : getCapabilities()) {
            validateOptionalColumnNames(capability, statement.getColumns().stream()
                    .map(ColumnDefinition::getColumnName).collect(Collectors.toList()),
                    NamedObject.table);
        }
        statement.visitExpressions(this::validateOptionalExpression);
    }
}
