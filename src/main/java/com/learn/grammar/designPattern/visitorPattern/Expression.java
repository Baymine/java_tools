package com.learn.grammar.designPattern.visitorPattern;

public interface Expression {
    <R, C> R accept(ExpressionVisitor<R, C> visitor, C context);
}
