package com.learn.grammar.designPattern.visitorPattern.implement;

import com.learn.grammar.designPattern.visitorPattern.Expression;
import com.learn.grammar.designPattern.visitorPattern.ExpressionVisitor;

public class BinaryOperation implements Expression {
    private final Expression left;

    private final Expression right;

    private final String operator;

    public BinaryOperation(Expression left, Expression right, String operator) {
        this.left = left;
        this.right = right;
        this.operator = operator;
    }


    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitBinaryOperation(this, context);
    }

    public Expression getLeft() {
        return left;
    }

    public Expression getRight() {
        return right;
    }

    public String getOperator() {
        return operator;
    }
}
