package com.learn.grammar.designPattern.visitorPattern.implement;

import com.learn.grammar.designPattern.visitorPattern.Expression;
import com.learn.grammar.designPattern.visitorPattern.ExpressionVisitor;

public class Literal implements Expression {

    private final String value;

    public Literal(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitLiteral(this, context);
    }
}
