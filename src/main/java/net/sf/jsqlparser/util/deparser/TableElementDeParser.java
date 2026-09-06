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

import java.util.Iterator;
import net.sf.jsqlparser.expression.ExpressionVisitor;
import net.sf.jsqlparser.statement.create.table.CheckConstraint;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.ColumnOption;
import net.sf.jsqlparser.statement.create.table.ExcludeConstraint;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.TableElement;

/** Deparses table elements while preserving expression visitor customization. */
public class TableElementDeParser extends AbstractDeParser<TableElement> {
    private final ExpressionVisitor<StringBuilder> expressionVisitor;

    public TableElementDeParser(StringBuilder builder,
            ExpressionVisitor<StringBuilder> expressionVisitor) {
        super(builder);
        this.expressionVisitor = expressionVisitor;
    }

    @Override
    public void deParse(TableElement element) {
        if (element instanceof ExcludeConstraint) {
            deParseExclude((ExcludeConstraint) element);
        } else if (element instanceof CheckConstraint) {
            deParseCheck((CheckConstraint) element);
        } else if (element instanceof ColumnDefinition
                && ((ColumnDefinition) element).getColumnOptions() != null) {
            deParseColumn((ColumnDefinition) element);
        } else {
            builder.append(element);
        }
    }

    private void deParseColumn(ColumnDefinition column) {
        builder.append(column.getColumnName());
        if (column.getColDataType() != null) {
            builder.append(' ').append(column.getColDataType());
        }
        if (column.isWithOptions()) {
            builder.append(" WITH OPTIONS");
        }
        for (ColumnOption option : column.getColumnOptions()) {
            builder.append(' ');
            if (option.getConstraint() != null) {
                deParse(option.getConstraint());
            } else {
                builder.append(option);
            }
        }
    }

    private void deParseExclude(ExcludeConstraint constraint) {
        if (constraint.getName() != null) {
            builder.append("CONSTRAINT ").append(constraint.getName()).append(' ');
        }
        builder.append("EXCLUDE");
        if (constraint.getUsing() != null) {
            builder.append(" USING ").append(constraint.getUsing());
        }
        if (constraint.getColumns() != null) {
            builder.append(" (");
            for (Iterator<Index.ColumnParams> iterator =
                    constraint.getColumns().iterator(); iterator.hasNext();) {
                iterator.next().appendTo(builder,
                        expression -> expression.accept(expressionVisitor, null));
                if (iterator.hasNext()) {
                    builder.append(", ");
                }
            }
            builder.append(')');
        }
        constraint.appendConstraintOptionsTo(builder);
        if (constraint.getExpression() != null) {
            builder.append(" WHERE (");
            constraint.getExpression().accept(expressionVisitor, null);
            builder.append(')');
        }
        constraint.appendConstraintAttributesTo(builder);
    }

    private void deParseCheck(CheckConstraint constraint) {
        if (constraint.getName() != null || constraint.isUseConstraintKeyword()) {
            builder.append("CONSTRAINT");
            if (constraint.getName() != null) {
                builder.append(' ').append(constraint.getName());
            }
            builder.append(' ');
        }
        builder.append("CHECK (");
        if (constraint.getExpression() != null) {
            constraint.getExpression().accept(expressionVisitor, null);
        } else {
            builder.append("null");
        }
        builder.append(')');
        if (constraint.getEnforced() != null) {
            builder.append(constraint.getEnforced() ? " ENFORCED" : " NOT ENFORCED");
        }
        constraint.appendConstraintAttributesTo(builder);
    }
}
