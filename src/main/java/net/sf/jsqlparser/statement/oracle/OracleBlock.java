/*-
 * #%L
 * JSQLParser library
 * %%
 * Copyright (C) 2004 - 2026 JSQLParser
 * %%
 * Dual licensed under GNU LGPL 2.1 or Apache License 2.0
 * #L%
 */
package net.sf.jsqlparser.statement.oracle;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.sf.jsqlparser.expression.Expression;
import net.sf.jsqlparser.statement.Block;
import net.sf.jsqlparser.statement.Statement;
import net.sf.jsqlparser.statement.Statements;
import net.sf.jsqlparser.statement.StatementVisitor;
import net.sf.jsqlparser.statement.create.table.ColDataType;

/** An Oracle anonymous block with variable declarations and exception handlers. */
public class OracleBlock extends Block {
    private final List<VariableDeclaration> declarations = new ArrayList<>();
    private final List<ExceptionHandler> exceptionHandlers = new ArrayList<>();

    public List<VariableDeclaration> getDeclarations() {
        return declarations;
    }

    public List<ExceptionHandler> getExceptionHandlers() {
        return exceptionHandlers;
    }

    @Override
    public <T, S> T accept(StatementVisitor<T> visitor, S context) {
        return visitor.visit(this, context);
    }

    public void visitChildren(Consumer<Expression> expressions, Consumer<Statement> statements) {
        for (VariableDeclaration declaration : declarations) {
            if (declaration.getInitializer() != null) {
                expressions.accept(declaration.getInitializer());
            }
        }
        if (getStatements() != null) {
            getStatements().forEach(statements);
        }
        exceptionHandlers.forEach(handler -> handler.getStatements().forEach(statements));
    }

    public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> expressions,
            Consumer<Statement> statements) {
        if (!declarations.isEmpty()) {
            builder.append("DECLARE\n");
            declarations.forEach(declaration -> {
                declaration.appendTo(builder, expressions);
                builder.append(";\n");
            });
        }
        builder.append("BEGIN\n");
        appendStatements(builder, getStatements(), statements);
        if (!exceptionHandlers.isEmpty()) {
            builder.append("EXCEPTION\n");
            for (ExceptionHandler handler : exceptionHandlers) {
                builder.append("WHEN ").append(String.join(" OR ", handler.getExceptions()))
                        .append(" THEN\n");
                appendStatements(builder, handler.getStatements(), statements);
            }
        }
        builder.append("END");
        if (hasSemicolonAfterEnd()) {
            builder.append(';');
        }
        return builder;
    }

    private static void appendStatements(StringBuilder builder, Statements body,
            Consumer<Statement> printer) {
        if (body == null) {
            return;
        }
        for (Statement statement : body) {
            printer.accept(statement);
            if (!(statement instanceof Block) || !((Block) statement).hasSemicolonAfterEnd()) {
                builder.append(';');
            }
            builder.append('\n');
        }
    }

    @Override
    public StringBuilder appendTo(StringBuilder builder) {
        return appendTo(builder, builder::append, builder::append);
    }

    public static class VariableDeclaration implements java.io.Serializable {
        private String name;
        private ColDataType dataType;
        private boolean constant;
        private boolean notNull;
        private String initializerOperator = ":=";
        private Expression initializer;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public ColDataType getDataType() {
            return dataType;
        }

        public void setDataType(ColDataType type) {
            dataType = type;
        }

        public boolean isConstant() {
            return constant;
        }

        public void setConstant(boolean value) {
            constant = value;
        }

        public boolean isNotNull() {
            return notNull;
        }

        public void setNotNull(boolean value) {
            notNull = value;
        }

        public String getInitializerOperator() {
            return initializerOperator;
        }

        public void setInitializerOperator(String operator) {
            initializerOperator = operator;
        }

        public Expression getInitializer() {
            return initializer;
        }

        public void setInitializer(Expression expression) {
            initializer = expression;
        }

        public StringBuilder appendTo(StringBuilder builder, Consumer<Expression> printer) {
            builder.append(name).append(' ');
            if (constant) {
                builder.append("CONSTANT ");
            }
            builder.append(dataType);
            if (notNull) {
                builder.append(" NOT NULL");
            }
            if (initializer != null) {
                builder.append(' ').append(initializerOperator).append(' ');
                printer.accept(initializer);
            }
            return builder;
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder();
            return appendTo(builder, builder::append).toString();
        }
    }
    public static class ExceptionHandler implements java.io.Serializable {
        private final List<String> exceptions = new ArrayList<>();
        private Statements statements;

        public List<String> getExceptions() {
            return exceptions;
        }

        public Statements getStatements() {
            return statements;
        }

        public void setStatements(Statements statements) {
            this.statements = statements;
        }
    }
}
