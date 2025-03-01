package com.learn.grammar.designPattern.visitorPattern.client;

import com.learn.grammar.designPattern.visitorPattern.implement.*;

public class Main {
    public static void main(String[] args) {
        SysDate sysDate = new SysDate();
        Literal literal = new Literal("expression");
        BinaryOperation binaryOperation = new BinaryOperation(
                new Literal("10"),
                new Literal("5"),
                "+"
        );

        Evaluator evaluator = new Evaluator();
        PrettyPrinter prettyPrinter = new PrettyPrinter();

        System.out.println("Evaluation: ");
        System.out.println(sysDate.accept(evaluator, null));
        System.out.println(literal.accept(evaluator, null));
        System.out.println(binaryOperation.accept(evaluator, null));
    }
}
