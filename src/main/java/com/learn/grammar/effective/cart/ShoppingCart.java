package com.learn.grammar.effective.cart;

import com.learn.grammar.effective.product.Product;

import java.math.BigDecimal;
import java.util.Map;

// 应用第20条：接口优于抽象类
public interface ShoppingCart {
    void addProduct(Product product, int quantity);
    void removeProduct(Product product);
    void updateQuantity(Product product, int quantity);
    Map<Product, Integer> getItems();
    BigDecimal getTotalPrice();
    void clear();
}
