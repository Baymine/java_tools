package com.learn.grammar.designPattern.visitorPattern.implement;

import com.learn.grammar.designPattern.visitorPattern.ExpressionVisitor;

public class PrettyPrinter implements ExpressionVisitor<String, Object> {
    @Override
    public String visitSysDate(SysDate expression, Object context) {
        return "SysDate";
    }

    @Override
    public String visitLiteral(Literal expression, Object context) {
        return "'" + expression.getValue() + "'";
    }

    @Override
    public String visitBinaryOperation(BinaryOperation expression, Object context) {
        return "(" + expression.getLeft().accept(this, context) + " "
                + expression.getOperator() + " "
                + expression.getRight().accept(this, context) + ")";
    }
}
