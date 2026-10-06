package com.inmemdb.server.sql.parser.ast;

/**
 * A binary expression, e.g. {@code id = 1}, {@code age > 18}, {@code a AND b}.
 */
public class BinaryExpr implements Expression {

    public enum Op {
        EQ, NEQ, LT, LTE, GT, GTE, AND, OR, IS, IS_NOT
    }

    private final Expression left;
    private final Op op;
    private final Expression right;

    public BinaryExpr(Expression left, Op op, Expression right) {
        this.left = left;
        this.op = op;
        this.right = right;
    }

    public Expression getLeft() { return left; }
    public Op getOp()           { return op;   }
    public Expression getRight(){ return right; }

    @Override
    public String toString() {
        return "(" + left + " " + op + " " + right + ")";
    }
}
