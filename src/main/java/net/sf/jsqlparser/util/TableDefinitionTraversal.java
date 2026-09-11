/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.util;

import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.schema.Table;
import net.sf.jsqlparser.statement.LikeClause;
import net.sf.jsqlparser.statement.alter.AlterExpression;
import net.sf.jsqlparser.statement.alter.AlterExpressionPrimaryKey;
import net.sf.jsqlparser.statement.create.index.CreateIndex;
import net.sf.jsqlparser.statement.create.table.CheckConstraint;
import net.sf.jsqlparser.statement.create.table.ColumnDefinition;
import net.sf.jsqlparser.statement.create.table.ColumnOption;
import net.sf.jsqlparser.statement.create.table.CreateTable;
import net.sf.jsqlparser.statement.create.table.DefaultConstraint;
import net.sf.jsqlparser.statement.create.table.ExcludeConstraint;
import net.sf.jsqlparser.statement.create.table.ForeignKeyIndex;
import net.sf.jsqlparser.statement.create.table.Index;
import net.sf.jsqlparser.statement.create.table.TableElement;

/** Traverses structured table definitions without interpreting legacy raw column options. */
public final class TableDefinitionTraversal {
    private TableDefinitionTraversal() {}

    public static void visit(CreateIndex createIndex, Consumer<Expression> expressions,
            Consumer<Table> tables) {
        accept(createIndex.getTable(), tables);
        if (createIndex.getIndex() != null) {
            visit(createIndex.getIndex(), expressions, tables);
        }
        visitOptions(createIndex.getStorageParameters(), expressions);
        accept(createIndex.getWhere(), expressions);
    }

    /** Visits the structured definitions and expressions belonging to a single ALTER action. */
    public static void visit(AlterExpression action, Consumer<Expression> expressions,
            Consumer<Table> tables) {
        if (action.getColDataTypeList() != null) {
            action.getColDataTypeList().forEach(column -> visit(column, expressions, tables));
        }
        if (action.getIndex() != null) {
            visit(action.getIndex(), expressions, tables);
        }
        if (action instanceof AlterExpressionPrimaryKey) {
            AlterExpressionPrimaryKey primaryKey = (AlterExpressionPrimaryKey) action;
            if (primaryKey.isUsingHash()) {
                accept(primaryKey.getBucketCount(), expressions);
            }
        }
    }

    public static void visit(CreateTable table, Consumer<Expression> expressions,
            Consumer<Table> tables) {
        if (table.getTableElements() != null) {
            table.getTableElements().forEach(element -> visit(element, expressions, tables));
        } else {
            if (table.getColumnDefinitions() != null) {
                table.getColumnDefinitions().forEach(column -> visit(column, expressions, tables));
            }
            if (table.getIndexes() != null) {
                table.getIndexes().forEach(index -> visit(index, expressions, tables));
            }
        }
        accept(table.getTrailingLikeTable(), tables);
        accept(table.getPartitionOf(), tables);
    }

    public static void visit(TableElement element, Consumer<Expression> expressions,
            Consumer<Table> tables) {
        if (element instanceof LikeClause) {
            accept(((LikeClause) element).getTable(), tables);
        } else if (element instanceof ColumnDefinition) {
            ColumnDefinition column = (ColumnDefinition) element;
            if (column.getColumnOptions() != null) {
                for (ColumnOption option : column.getColumnOptions()) {
                    if (option.getForeignKeyReference() != null) {
                        accept(option.getForeignKeyReference().getTable(), tables);
                    }
                    if (option.getConstraint() != null) {
                        visit(option.getConstraint(), expressions, tables);
                    }
                }
            }
            if (column instanceof AlterExpression.ColumnDataType) {
                accept(((AlterExpression.ColumnDataType) column).getUsingExpression(), expressions);
            }
        } else if (element instanceof Index) {
            Index index = (Index) element;
            if (index.getColumns() != null) {
                for (Index.ColumnParams column : index.getColumns()) {
                    accept(column.getExpression(), expressions);
                    visitOptions(column.getOperatorClassParameters(), expressions);
                }
            }
            visitOptions(index.getStorageParameters(), expressions);
            if (index instanceof CheckConstraint) {
                accept(((CheckConstraint) index).getExpression(), expressions);
            }
            if (index instanceof DefaultConstraint) {
                DefaultConstraint constraint = (DefaultConstraint) index;
                accept(constraint.getExpression(), expressions);
                accept(constraint.getColumn(), expressions);
            }
            if (index instanceof ExcludeConstraint) {
                accept(((ExcludeConstraint) index).getExpression(), expressions);
            }
            if (index instanceof ForeignKeyIndex) {
                accept(((ForeignKeyIndex) index).getTable(), tables);
            }
        }
    }

    private static void visitOptions(List<Index.Option> options, Consumer<Expression> expressions) {
        if (options != null) {
            options.forEach(option -> accept(option.getValue(), expressions));
        }
    }

    private static <T> void accept(T value, Consumer<T> consumer) {
        if (value != null) {
            consumer.accept(value);
        }
    }
}
