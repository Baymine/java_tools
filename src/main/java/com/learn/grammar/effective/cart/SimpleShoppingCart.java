package com.learn.grammar.effective.cart;

import com.learn.grammar.effective.product.Product;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

// 应用第18条：复合优于继承
public class SimpleShoppingCart implements ShoppingCart {
    private final Map<Product, Integer> items = new HashMap<>();

    @Override
    public void addProduct(Product product, int quantity) {
        items.merge(product, quantity, Integer::sum);
    }

    @Override
    public void removeProduct(Product product) {
        items.remove(product);
    }

    @Override
    public void updateQuantity(Product product, int quantity) {
        if (quantity > 0) {
            items.put(product, quantity);
        } else {
            removeProduct(product);
        }
    }

    // 应用第50条：必要时进行保护性拷贝
    @Override
    public Map<Product, Integer> getItems() {
        return Collections.unmodifiableMap(items);
    }

    @Override
    public BigDecimal getTotalPrice() {
        return items.entrySet().stream()
                .map(entry -> entry.getKey().getPrice().multiply(BigDecimal.valueOf(entry.getValue())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Override
    public void clear() {
        items.clear();
    }
}
