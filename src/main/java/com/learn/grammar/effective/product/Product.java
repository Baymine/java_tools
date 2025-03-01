package com.learn.grammar.effective.product;

import java.math.BigDecimal;

// 应用第17条：使可变性最小化
// 应用第2条：遇到多个构造器参数时要考虑使用构建器
// 应用第68条：遵守普遍接受的命名惯例
public final class Product {
    private final String id;
    private final String name;
    private final BigDecimal price;
    private final String description;

    private Product(Builder builder) {
        this.id = builder.id;
        this.name = builder.name;
        this.price = builder.price;
        this.description = builder.description;
    }

    public String getId() { return id; }
    public String getName() { return name; }
    public BigDecimal getPrice() { return price; }
    public String getDescription() { return description; }

    public static class Builder {
        private final String id;
        private final String name;
        private final BigDecimal price;
        private String description = "";

        public Builder(String id, String name, BigDecimal price) {
            this.id = id;
            this.name = name;
            this.price = price;
        }

        public Builder description(String description) {
            this.description = description;
            return this;
        }

        public Product build() {
            return new Product(this);
        }
    }

    @Override
    public String toString() {
        return "Product{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", price=" + price +
                ", description='" + description + '\'' +
                '}';
    }
}
