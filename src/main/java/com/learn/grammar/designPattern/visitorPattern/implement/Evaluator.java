package com.learn.grammar.designPattern.visitorPattern.implement;

import com.learn.grammar.designPattern.visitorPattern.ExpressionVisitor;

public class Evaluator implements ExpressionVisitor<Object, Object> {
    @Override
    public Object visitSysDate(SysDate expression, Object context) {
        return java.time.LocalDate.now();
    }

    @Override
    public Object visitLiteral(Literal expression, Object context) {
        return expression.getValue();
    }

    @Override
    public Object visitBinaryOperation(BinaryOperation expression, Object context) {
        Object leftValue = expression.getLeft().accept(this, context);
        Object rightValue = expression.getRight().accept(this, context);

        if (leftValue instanceof Number && rightValue instanceof Number) {
            double left = ((Number)leftValue).doubleValue();
            double right = ((Number)rightValue).doubleValue();
            switch (expression.getOperator()) {
                case "+":
                    return left + right;
                case "-":
                    return left - right;
                case "*":
                    return left * right;
                case "/":
                    if (right == 0) {
                        throw new ArithmeticException("Division by zero");
                    }
                    return left / right;
                default:
                    throw new IllegalArgumentException("Unknown operator: " + expression.getOperator());
            }
        } else {
                throw new IllegalArgumentException("Unsupported operand types");
        }
    }
}
