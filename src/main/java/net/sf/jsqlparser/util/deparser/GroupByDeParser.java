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
import net.sf.jsqlparser.statement.select.GroupByElement;

public class GroupByDeParser extends AbstractDeParser<GroupByElement> {

    private final ExpressionListDeParser<?> expressionListDeParser;
    private final ExpressionVisitor<StringBuilder> expressionVisitor;

    public GroupByDeParser(ExpressionVisitor<StringBuilder> expressionVisitor,
            StringBuilder buffer) {
        super(buffer);
        this.expressionVisitor = expressionVisitor;
        this.expressionListDeParser = new ExpressionListDeParser<>(expressionVisitor, buffer);
        this.builder = buffer;
    }

    @Override
    public void deParse(GroupByElement groupBy) {
        groupBy.appendTo(builder, expressionListDeParser::deParse,
                expression -> expression.accept(expressionVisitor, null));
    }
}
