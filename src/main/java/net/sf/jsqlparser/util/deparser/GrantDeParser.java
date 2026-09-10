/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util.deparser;

import net.sf.jsqlparser.statement.grant.Grant;

public class GrantDeParser extends AbstractDeParser<Grant> {
    public GrantDeParser(StringBuilder buffer) {
        super(buffer);
    }

    @Override
    public void deParse(Grant grant) {
        grant.appendTo(builder, builder::append);
    }
}
