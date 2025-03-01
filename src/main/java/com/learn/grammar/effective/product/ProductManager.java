package com.learn.grammar.effective.product;

import java.util.HashMap;
import java.util.Map;
import java.math.BigDecimal;

// 应用第3条：用私有构造器或者枚举类型强化Singleton属性
public class ProductManager {
    private static final ProductManager INSTANCE = new ProductManager();
    private final Map<String, Product> products = new HashMap<>();

    private ProductManager() {} // 私有构造器

    public static ProductManager getInstance() {
        return INSTANCE;
    }

    // 应用第1条：用静态工厂方法代替构造器
    public static Product createProduct(String id, String name, BigDecimal price) {
        return new Product.Builder(id, name, price).build();
    }

    public static Product createProduct(String id, String name, BigDecimal price, String description) {
        return new Product.Builder(id, name, price)
                .description(description)
                .build();
    }

    public void addProduct(Product product) {
        products.put(product.getId(), product);
    }

    public Product getProduct(String id) {
        return products.get(id);
    }

    public void updateProduct(Product product) {
        if (products.containsKey(product.getId())) {
            products.put(product.getId(), product);
        }
    }

    public void removeProduct(String id) {
        products.remove(id);
    }
}
