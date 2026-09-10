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

import net.sf.jsqlparser.statement.create.index.CreateIndex;

public class CreateIndexDeParser extends AbstractDeParser<CreateIndex> {

    public CreateIndexDeParser(StringBuilder buffer) {
        super(buffer);
    }

    @Override
    public void deParse(CreateIndex createIndex) {
        createIndex.appendTo(builder);
    }
}
