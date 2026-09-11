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

import net.sf.jsqlparser.parser.feature.Feature;
import net.sf.jsqlparser.statement.SetIdentityInsertStatement;
import net.sf.jsqlparser.util.validation.metadata.NamedObject;

public class SetIdentityInsertValidator extends AbstractValidator<SetIdentityInsertStatement> {
    @Override
    public void validate(SetIdentityInsertStatement statement) {
        validateFeatureAndName(Feature.setIdentityInsert, NamedObject.table,
                statement.getTable().getFullyQualifiedName());
    }
}
