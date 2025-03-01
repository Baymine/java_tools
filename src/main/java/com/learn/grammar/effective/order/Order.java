package com.learn.grammar.effective.order;

import com.learn.grammar.effective.product.Product;
import com.learn.grammar.effective.user.User;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 表示电子商务系统中的订单。
 * 这个类是不可变的，使用构建器模式来创建实例。
 * 应用第18条：复合优于继承
 * 应用第68条：遵守普遍接受的命名惯例
 */
public class Order {
    private final String id;
    private final User user;
    private final List<OrderItem> items;
    private final LocalDateTime orderTime;
    private OrderStatus status;

    private Order(Builder builder) {
        this.id = builder.id;
        this.user = builder.user;
        this.items = new ArrayList<>(builder.items);
        this.orderTime = LocalDateTime.now();
        this.status = OrderStatus.PENDING;
    }

    /**
     * 返回订单中的商品列表。
     * 应用第50条：必要时进行保护性拷贝
     * @return 不可修改的商品列表
     */
    public List<OrderItem> getItems() {
        return Collections.unmodifiableList(items);
    }

    public String getId() { return id; }
    public User getUser() { return user; }
    public LocalDateTime getOrderTime() { return orderTime; }
    public OrderStatus getStatus() { return status; }

    public void setStatus(OrderStatus status) {
        this.status = status;
    }

    public BigDecimal getTotalPrice() {
        return items.stream()
                .map(OrderItem::getTotalPrice)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public static class Builder {
        private final String id;
        private final User user;
        private final List<OrderItem> items = new ArrayList<>();

        public Builder(String id, User user) {
            this.id = id;
            this.user = user;
        }

        public Builder addItem(Product product, int quantity) {
            items.add(new OrderItem(product, quantity));
            return this;
        }

        public Order build() {
            return new Order(this);
        }
    }

    public static class OrderItem {
        private final Product product;
        private final int quantity;

        public OrderItem(Product product, int quantity) {
            this.product = product;
            this.quantity = quantity;
        }

        public Product getProduct() { return product; }
        public int getQuantity() { return quantity; }
        public BigDecimal getTotalPrice() {
            return product.getPrice().multiply(BigDecimal.valueOf(quantity));
        }
    }

    public enum OrderStatus {
        PENDING, PROCESSING, SHIPPED, DELIVERED, CANCELLED
    }

    @Override
    public String toString() {
        return "Order{" +
                "id='" + id + '\'' +
                ", user=" + user.getUsername() +
                ", items=" + items.size() +
                ", orderTime=" + orderTime +
                ", status=" + status +
                ", totalPrice=" + getTotalPrice() +
                '}';
    }
}
