package com.learn.grammar.designPattern.visitorPattern.implement;

import com.learn.grammar.designPattern.visitorPattern.Expression;
import com.learn.grammar.designPattern.visitorPattern.ExpressionVisitor;

public class SysDate implements Expression {

    @Override
    public <R, C> R accept(ExpressionVisitor<R, C> visitor, C context) {
        return visitor.visitSysDate(this, context);
    }
}
