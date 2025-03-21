/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2019 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.select;

import net.sf.jsqlparser.expression.Alias;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.expression.OracleHierarchicalExpression;
import net.sf.jsqlparser.expression.OracleHint;
import net.sf.jsqlparser.expression.PreferringClause;
import net.sf.jsqlparser.expression.WindowDefinition;
import net.sf.jsqlparser.schema.Table;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Optional;

import static java.util.stream.Collectors.joining;

@SuppressWarnings({"PMD.CyclomaticComplexity"})
public class PlainSelect extends Select {

    private Distinct distinct = null;
    private BigQuerySelectQualifier bigQuerySelectQualifier = null;
    private List<SelectItem<?>> selectItems;
    private List<Table> intoTables;
    private FromItem fromItem;
    private List<LateralView> lateralViews;
    private List<Join> joins;
    private Expression where;
    private GroupByElement groupBy;
    private Expression having;
    private Expression qualify;
    private OptimizeFor optimizeFor;
    private Skip skip;
    private boolean mySqlHintStraightJoin;
    private First first;
    private Top top;
    private OracleHierarchicalExpression oracleHierarchical = null;
    private PreferringClause preferringClause = null;
    private OracleHint oracleHint = null;
    private boolean mySqlSqlCalcFoundRows = false;
    private MySqlSqlCacheFlags mySqlCacheFlag = null;
    private String forXmlPath;
    private KSQLWindow ksqlWindow = null;
    private boolean emitChanges = false;
    private List<WindowDefinition> windowDefinitions;
    /**
     * @see <a href=
     *      'https://clickhouse.com/docs/en/sql-reference/statements/select/from#final-modifier'>Clickhouse
     *      FINAL</a>
     */
    private boolean isUsingFinal = false;
    private boolean isUsingOnly = false;
    private boolean useWithNoLog = false;
    private Table intoTempTable = null;

    public PlainSelect() {}

    public PlainSelect(FromItem fromItem) {
        addSelectItem(new AllColumns());
        setFromItem(fromItem);
    }

    public PlainSelect(FromItem fromItem, Expression whereExpressions) {
        addSelectItem(new AllColumns());
        setFromItem(fromItem);
        setWhere(whereExpressions);
    }

    public PlainSelect(FromItem fromItem, Collection<Expression> orderByExpressions) {
        addSelectItem(new AllColumns());
        setFromItem(fromItem);
        addOrderByExpressions(orderByExpressions);
    }

    public PlainSelect(FromItem fromItem, Expression whereExpressions,
            Collection<Expression> orderByExpressions) {
        addSelectItem(new AllColumns());
        setFromItem(fromItem);
        setWhere(whereExpressions);
        addOrderByExpressions(orderByExpressions);
    }

    public PlainSelect(Collection<Expression> selectExpressions, FromItem fromItem) {
        addSelectExpressions(selectExpressions);
        setFromItem(fromItem);
    }

    public PlainSelect(Collection<Expression> selectExpressions, FromItem fromItem,
            Expression whereExpressions) {
        addSelectExpressions(selectExpressions);
        setFromItem(fromItem);
        setWhere(whereExpressions);
    }

    public PlainSelect(Collection<Expression> selectExpressions, FromItem fromItem,
            Collection<Expression> orderByExpressions) {
        addSelectExpressions(selectExpressions);
        setFromItem(fromItem);
        addOrderByExpressions(orderByExpressions);
    }

    public PlainSelect(Collection<Expression> selectExpressions, FromItem fromItem,
            Expression whereExpressions, Collection<Expression> orderByExpressions) {
        addSelectExpressions(selectExpressions);
        setFromItem(fromItem);
        setWhere(whereExpressions);
        addOrderByExpressions(orderByExpressions);
    }

    @Deprecated
    public boolean isUseBrackets() {
        return false;
    }

    public FromItem getFromItem() {
        return fromItem;
    }

    public void setFromItem(FromItem item) {
        fromItem = item;
    }

    public List<Table> getIntoTables() {
        return intoTables;
    }

    public void setIntoTables(List<Table> intoTables) {
        this.intoTables = intoTables;
    }

    public List<SelectItem<?>> getSelectItems() {
        return selectItems;
    }

    public void setSelectItems(List<SelectItem<?>> list) {
        selectItems = list;
    }

    public SelectItem<?> getSelectItem(int index) {
        return selectItems.get(index);
    }

    public Expression getWhere() {
        return where;
    }

    public void setWhere(Expression where) {
        this.where = where;
    }

    public PlainSelect withFromItem(FromItem item) {
        this.setFromItem(item);
        return this;
    }

    public PlainSelect withSelectItems(List<SelectItem<?>> list) {
        this.setSelectItems(list);
        return this;
    }

    public PlainSelect withSelectItems(SelectItem<?>... selectItems) {
        return this.withSelectItems(Arrays.asList(selectItems));
    }

    public PlainSelect addSelectItems(SelectItem<?>... items) {
        selectItems = Optional.ofNullable(selectItems).orElseGet(ArrayList::new);
        selectItems.addAll(Arrays.asList(items));
        return this;
    }

    public PlainSelect addSelectExpressions(Collection<Expression> expressions) {
        selectItems = Optional.ofNullable(selectItems).orElseGet(ArrayList::new);
        for (Expression expression : expressions) {
            selectItems.add(SelectItem.from(expression));
        }
        return this;
    }

    public PlainSelect addSelectItems(Expression... expressions) {
        return this.addSelectExpressions(Arrays.asList(expressions));
    }

    public PlainSelect addSelectItem(Expression expression, Alias alias) {
        selectItems = Optional.ofNullable(selectItems).orElseGet(ArrayList::new);
        selectItems.add(new SelectItem<>(expression, alias));
        return this;
    }

    public PlainSelect addSelectItem(Expression expression) {
        return addSelectItem(expression, null);
    }

    public List<LateralView> getLateralViews() {
        return lateralViews;
    }

    public void setLateralViews(Collection<LateralView> lateralViews) {
        if (this.lateralViews == null) {
            this.lateralViews = new ArrayList<>();
        } else {
            this.lateralViews.clear();
        }

        if (lateralViews != null) {
            this.lateralViews.addAll(lateralViews);
        } else {
            this.lateralViews = null;
        }
    }

    public PlainSelect addLateralView(LateralView lateralView) {
        if (this.lateralViews == null) {
            this.lateralViews = new ArrayList<>();
        }

        this.lateralViews.add(lateralView);
        return this;
    }

    public PlainSelect withLateralViews(Collection<LateralView> lateralViews) {
        this.setLateralViews(lateralViews);
        return this;
    }

    /**
     * The list of {@link Join}s
     *
     * @return the list of {@link Join}s
     */
    public List<Join> getJoins() {
        return joins;
    }

    public void setJoins(List<Join> list) {
        joins = list;
    }

    public Join getJoin(int index) {
        return joins.get(index);
    }

    public PlainSelect addJoins(Join... joins) {
        List<Join> list = Optional.ofNullable(getJoins()).orElseGet(ArrayList::new);
        Collections.addAll(list, joins);
        return withJoins(list);
    }

    public PlainSelect withJoins(List<Join> joins) {
        this.setJoins(joins);
        return this;
    }

    public boolean isUsingFinal() {
        return isUsingFinal;
    }

    public void setUsingFinal(boolean usingFinal) {
        this.isUsingFinal = usingFinal;
    }

    public PlainSelect withUsingFinal(boolean usingFinal) {
        this.setUsingFinal(usingFinal);
        return this;
    }

    public boolean isUsingOnly() {
        return isUsingOnly;
    }

    public void setUsingOnly(boolean usingOnly) {
        isUsingOnly = usingOnly;
    }

    public PlainSelect withUsingOnly(boolean usingOnly) {
        this.setUsingOnly(usingOnly);
        return this;
    }

    public boolean isUseWithNoLog() {
        return useWithNoLog;
    }

    public void setUseWithNoLog(boolean useWithNoLog) {
        this.useWithNoLog = useWithNoLog;
    }

    public PlainSelect withUseWithNoLog(boolean useWithNoLog) {
        this.setUseWithNoLog(useWithNoLog);
        return this;
    }

    public Table getIntoTempTable() {
        return intoTempTable;
    }

    public void setIntoTempTable(Table intoTempTable) {
        this.intoTempTable = intoTempTable;
    }

    public PlainSelect withIntoTempTable(Table intoTempTable) {
        this.setIntoTempTable(intoTempTable);
        return this;
    }

    @Override
    public <T, S> T accept(SelectVisitor<T> selectVisitor, S context) {
        return selectVisitor.visit(this, context);
    }

    @Override
    public <T, S> T accept(FromItemVisitor<T> fromItemVisitor, S context) {
        return fromItemVisitor.visit(this, context);
    }

    @Override
    public SampleClause getSampleClause() {
        return null;
    }

    @Override
    public FromItem setSampleClause(SampleClause sampleClause) {
        return null;
    }

    public OptimizeFor getOptimizeFor() {
        return optimizeFor;
    }

    public void setOptimizeFor(OptimizeFor optimizeFor) {
        this.optimizeFor = optimizeFor;
    }

    public Top getTop() {
        return top;
    }

    public void setTop(Top top) {
        this.top = top;
    }

    public Skip getSkip() {
        return skip;
    }

    public void setSkip(Skip skip) {
        this.skip = skip;
    }

    public boolean getMySqlHintStraightJoin() {
        return this.mySqlHintStraightJoin;
    }

    public void setMySqlHintStraightJoin(boolean mySqlHintStraightJoin) {
        this.mySqlHintStraightJoin = mySqlHintStraightJoin;
    }

    public First getFirst() {
        return first;
    }

    public void setFirst(First first) {
        this.first = first;
    }

    public Distinct getDistinct() {
        return distinct;
    }

    public void setDistinct(Distinct distinct) {
        this.distinct = distinct;
    }

    public BigQuerySelectQualifier getBigQuerySelectQualifier() {
        return bigQuerySelectQualifier;
    }

    public PlainSelect setBigQuerySelectQualifier(BigQuerySelectQualifier bigQuerySelectQualifier) {
        this.bigQuerySelectQualifier = bigQuerySelectQualifier;
        return this;
    }

    public Expression getHaving() {
        return having;
    }

    public void setHaving(Expression expression) {
        having = expression;
    }

    public Expression getQualify() {
        return qualify;
    }

    public PlainSelect setQualify(Expression qualify) {
        this.qualify = qualify;
        return this;
    }

    /**
     * A list of {@link Expression}s of the GROUP BY clause. It is null in case there is no GROUP BY
     * clause
     *
     * @return a list of {@link Expression}s
     */
    public GroupByElement getGroupBy() {
        return this.groupBy;
    }

    public void setGroupByElement(GroupByElement groupBy) {
        this.groupBy = groupBy;
    }

    public PlainSelect addGroupByColumnReference(Expression expr) {
        this.groupBy = Optional.ofNullable(groupBy).orElseGet(GroupByElement::new);
        this.groupBy.addGroupByExpression(expr);
        return this;
    }

    public OracleHierarchicalExpression getOracleHierarchical() {
        return oracleHierarchical;
    }

    public void setOracleHierarchical(OracleHierarchicalExpression oracleHierarchical) {
        this.oracleHierarchical = oracleHierarchical;
    }

    public PreferringClause getPreferringClause() {
        return preferringClause;
    }

    public void setPreferringClause(PreferringClause preferringClause) {
        this.preferringClause = preferringClause;
    }

    public OracleHint getOracleHint() {
        return oracleHint;
    }

    public void setOracleHint(OracleHint oracleHint) {
        this.oracleHint = oracleHint;
    }

    public String getForXmlPath() {
        return forXmlPath;
    }

    public void setForXmlPath(String forXmlPath) {
        this.forXmlPath = forXmlPath;
    }

    public KSQLWindow getKsqlWindow() {
        return ksqlWindow;
    }

    public void setKsqlWindow(KSQLWindow ksqlWindow) {
        this.ksqlWindow = ksqlWindow;
    }

    public boolean isEmitChanges() {
        return emitChanges;
    }

    public void setEmitChanges(boolean emitChanges) {
        this.emitChanges = emitChanges;
    }

    public List<WindowDefinition> getWindowDefinitions() {
        return windowDefinitions;
    }

    public void setWindowDefinitions(List<WindowDefinition> windowDefinitions) {
        this.windowDefinitions = windowDefinitions;
    }

    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
            "PMD.NPathComplexity"})
    public StringBuilder appendSelectBodyTo(StringBuilder builder) {
        builder.append("SELECT ");

        if (this.mySqlHintStraightJoin) {
            builder.append("STRAIGHT_JOIN ");
        }

        if (oracleHint != null) {
            builder.append(oracleHint).append(" ");
        }

        if (skip != null) {
            builder.append(skip).append(" ");
        }

        if (first != null) {
            builder.append(first).append(" ");
        }

        if (distinct != null) {
            builder.append(distinct).append(" ");
        }

        if (bigQuerySelectQualifier != null) {
            switch (bigQuerySelectQualifier) {
                case AS_STRUCT:
                    builder.append("AS STRUCT ");
                    break;
                case AS_VALUE:
                    builder.append("AS VALUE ");
                    break;
            }
        }

        if (top != null) {
            builder.append(top).append(" ");
        }
        if (mySqlCacheFlag != null) {
            builder.append(mySqlCacheFlag.name()).append(" ");
        }
        if (mySqlSqlCalcFoundRows) {
            builder.append("SQL_CALC_FOUND_ROWS").append(" ");
        }
        builder.append(getStringList(selectItems));

        if (intoTables != null) {
            builder.append(" INTO ");
            for (Iterator<Table> iter = intoTables.iterator(); iter.hasNext();) {
                builder.append(iter.next().toString());
                if (iter.hasNext()) {
                    builder.append(", ");
                }
            }
        }

        if (fromItem != null) {
            builder.append(" FROM ");
            if (isUsingOnly) {
                builder.append("ONLY ");
            }
            builder.append(fromItem);
            if (lateralViews != null) {
                for (LateralView lateralView : lateralViews) {
                    builder.append(" ").append(lateralView);
                }
            }
            if (joins != null) {
                for (Join join : joins) {
                    if (join.isSimple()) {
                        builder.append(", ").append(join);
                    } else {
                        builder.append(" ").append(join);
                    }
                }
            }

            if (isUsingFinal) {
                builder.append(" FINAL");
            }

            if (ksqlWindow != null) {
                builder.append(" WINDOW ").append(ksqlWindow);
            }
            if (where != null) {
                builder.append(" WHERE ").append(where);
            }
            if (oracleHierarchical != null) {
                builder.append(oracleHierarchical);
            }
            if (preferringClause != null) {
                builder.append(" ").append(preferringClause);
            }
            if (groupBy != null) {
                builder.append(" ").append(groupBy);
            }
            if (having != null) {
                builder.append(" HAVING ").append(having);
            }
            if (qualify != null) {
                builder.append(" QUALIFY ").append(qualify);
            }
            if (windowDefinitions != null) {
                builder.append(" WINDOW ");
                builder.append(windowDefinitions.stream().map(WindowDefinition::toString)
                        .collect(joining(", ")));
            }
            if (emitChanges) {
                builder.append(" EMIT CHANGES");
            }
        } else {
            // without from
            if (where != null) {
                builder.append(" WHERE ").append(where);
            }
        }
        if (intoTempTable != null) {
            builder.append(" INTO TEMP ").append(intoTempTable);
        }
        if (useWithNoLog) {
            builder.append(" WITH NO LOG");
        }
        return builder;
    }

    @Override
    @SuppressWarnings({"PMD.CyclomaticComplexity", "PMD.ExcessiveMethodLength",
            "PMD.NPathComplexity"})
    public String toString() {
        StringBuilder builder = new StringBuilder();
        super.appendTo(builder);

        if (optimizeFor != null) {
            builder.append(optimizeFor);
        }

        if (forXmlPath != null) {
            builder.append(" FOR XML PATH(").append(forXmlPath).append(")");
        }

        return builder.toString();
    }

    public PlainSelect withMySqlSqlCalcFoundRows(boolean mySqlCalcFoundRows) {
        this.setMySqlSqlCalcFoundRows(mySqlCalcFoundRows);
        return this;
    }

    public PlainSelect withMySqlSqlNoCache(MySqlSqlCacheFlags mySqlCacheFlag) {
        this.setMySqlSqlCacheFlag(mySqlCacheFlag);
        return this;
    }

    public boolean getMySqlSqlCalcFoundRows() {
        return this.mySqlSqlCalcFoundRows;
    }

    public void setMySqlSqlCalcFoundRows(boolean mySqlCalcFoundRows) {
        this.mySqlSqlCalcFoundRows = mySqlCalcFoundRows;
    }

    public MySqlSqlCacheFlags getMySqlSqlCacheFlag() {
        return this.mySqlCacheFlag;
    }

    public void setMySqlSqlCacheFlag(MySqlSqlCacheFlags sqlCacheFlag) {
        this.mySqlCacheFlag = sqlCacheFlag;
    }

    public PlainSelect withDistinct(Distinct distinct) {
        this.setDistinct(distinct);
        return this;
    }

    public PlainSelect withIntoTables(List<Table> intoTables) {
        this.setIntoTables(intoTables);
        return this;
    }

    public PlainSelect withWhere(Expression where) {
        this.setWhere(where);
        return this;
    }

    public PlainSelect withOptimizeFor(OptimizeFor optimizeFor) {
        this.setOptimizeFor(optimizeFor);
        return this;
    }

    public PlainSelect withSkip(Skip skip) {
        this.setSkip(skip);
        return this;
    }

    public PlainSelect withMySqlHintStraightJoin(boolean mySqlHintStraightJoin) {
        this.setMySqlHintStraightJoin(mySqlHintStraightJoin);
        return this;
    }

    public PlainSelect withFirst(First first) {
        this.setFirst(first);
        return this;
    }

    public PlainSelect withTop(Top top) {
        this.setTop(top);
        return this;
    }

    public PlainSelect withOracleHierarchical(OracleHierarchicalExpression oracleHierarchical) {
        this.setOracleHierarchical(oracleHierarchical);
        return this;
    }

    public PlainSelect withPreferringClause(PreferringClause preferringClause) {
        this.setPreferringClause(preferringClause);
        return this;
    }

    public PlainSelect withOracleHint(OracleHint oracleHint) {
        this.setOracleHint(oracleHint);
        return this;
    }

    public PlainSelect withOracleSiblings(boolean oracleSiblings) {
        this.setOracleSiblings(oracleSiblings);
        return this;
    }

    public PlainSelect withForXmlPath(String forXmlPath) {
        this.setForXmlPath(forXmlPath);
        return this;
    }

    public PlainSelect withKsqlWindow(KSQLWindow ksqlWindow) {
        this.setKsqlWindow(ksqlWindow);
        return this;
    }

    public PlainSelect withNoWait(boolean noWait) {
        this.setNoWait(noWait);
        return this;
    }

    public PlainSelect withHaving(Expression having) {
        this.setHaving(having);
        return this;
    }

    public PlainSelect addSelectItems(Collection<? extends SelectItem<?>> selectItems) {
        List<SelectItem<?>> collection =
                Optional.ofNullable(getSelectItems()).orElseGet(ArrayList::new);
        collection.addAll(selectItems);
        return this.withSelectItems(collection);
    }

    public PlainSelect addIntoTables(Table... intoTables) {
        List<Table> collection = Optional.ofNullable(getIntoTables()).orElseGet(ArrayList::new);
        Collections.addAll(collection, intoTables);
        return this.withIntoTables(collection);
    }

    public PlainSelect addIntoTables(Collection<? extends Table> intoTables) {
        List<Table> collection = Optional.ofNullable(getIntoTables()).orElseGet(ArrayList::new);
        collection.addAll(intoTables);
        return this.withIntoTables(collection);
    }

    public PlainSelect addJoins(Collection<? extends Join> joins) {
        List<Join> collection = Optional.ofNullable(getJoins()).orElseGet(ArrayList::new);
        collection.addAll(joins);
        return this.withJoins(collection);
    }

    public <E extends FromItem> E getFromItem(Class<E> type) {
        return type.cast(getFromItem());
    }

    public <E extends Expression> E getWhere(Class<E> type) {
        return type.cast(getWhere());
    }

    public <E extends Expression> E getHaving(Class<E> type) {
        return type.cast(getHaving());
    }

    public enum BigQuerySelectQualifier {
        AS_STRUCT, AS_VALUE
    }
}
