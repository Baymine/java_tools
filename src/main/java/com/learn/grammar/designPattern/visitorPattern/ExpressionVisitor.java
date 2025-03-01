package com.learn.grammar.designPattern.visitorPattern;

import com.learn.grammar.designPattern.visitorPattern.implement.BinaryOperation;
import com.learn.grammar.designPattern.visitorPattern.implement.Literal;
import com.learn.grammar.designPattern.visitorPattern.implement.SysDate;

public interface ExpressionVisitor<R, C> {
    R visitSysDate(SysDate expression, C context);

    R visitLiteral(Literal expression, C context);

    R visitBinaryOperation(BinaryOperation expression, C context);
}
