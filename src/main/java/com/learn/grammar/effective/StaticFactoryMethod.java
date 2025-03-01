package com.learn.grammar.effective;

// 第1条：用静态工厂方法代替构造器
public class StaticFactoryMethod {
    private final String name;
    private final int value;

    private StaticFactoryMethod(String name, int value) {
        this.name = name;
        this.value = value;
    }

    // 静态工厂方法
    public static StaticFactoryMethod createInstance(String name, int value) {
        // 这里可以添加额外的逻辑，例如验证或缓存
        return new StaticFactoryMethod(name, value);
    }

    // 另一个静态工厂方法，展示可以有多个不同名称的工厂方法
    public static StaticFactoryMethod of(String name, int value) {
        return new StaticFactoryMethod(name, value);
    }

    // Getter方法
    public String getName() {
        return name;
    }

    public int getValue() {
        return value;
    }

    @Override
    public String toString() {
        return "StaticFactoryMethod{name='" + name + "', value=" + value + "}";
    }
}
