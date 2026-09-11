/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util.deparser;

import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.create.index.CreateIndex;

public class CreateIndexDeParser extends AbstractDeParser<CreateIndex> {
    private final ExpressionVisitor<StringBuilder> expressionVisitor;

    public CreateIndexDeParser(StringBuilder buffer) {
        this(buffer, null);
    }

    public CreateIndexDeParser(StringBuilder buffer,
            ExpressionVisitor<StringBuilder> expressionVisitor) {
        super(buffer);
        this.expressionVisitor = expressionVisitor;
    }

    @Override
    public void deParse(CreateIndex createIndex) {
        if (expressionVisitor == null) {
            createIndex.appendTo(builder);
        } else {
            createIndex.appendTo(builder, expression -> expression.accept(expressionVisitor, null));
        }
    }
}
