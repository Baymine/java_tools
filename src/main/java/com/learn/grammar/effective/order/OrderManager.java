package com.learn.grammar.effective.order;

import com.learn.grammar.effective.user.User;
import com.learn.grammar.effective.product.Product;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

// 应用第3条：用私有构造器或者枚举类型强化Singleton属性
public class OrderManager {
    private static final OrderManager INSTANCE = new OrderManager();
    private final Map<String, Order> orders = new HashMap<>();

    private OrderManager() {} // 私有构造器

    public static OrderManager getInstance() {
        return INSTANCE;
    }

    // 应用第1条：用静态工厂方法代替构造器
    public Order createOrder(User user) {
        String orderId = UUID.randomUUID().toString();
        Order order = new Order.Builder(orderId, user).build();
        orders.put(orderId, order);
        return order;
    }

    public void addProductToOrder(String orderId, Product product, int quantity) {
        Order order = orders.get(orderId);
        if (order != null) {
            Order.Builder builder = new Order.Builder(order.getId(), order.getUser());
            order.getItems().forEach(item -> builder.addItem(item.getProduct(), item.getQuantity()));
            builder.addItem(product, quantity);
            orders.put(orderId, builder.build());
        } else {
            // 应用第69条：只针对异常的情况才使用异常
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
    }

    // 应用第55条：谨慎返回optional
    public Optional<Order> getOrder(String orderId) {
        return Optional.ofNullable(orders.get(orderId));
    }

    public void updateOrderStatus(String orderId, Order.OrderStatus status) {
        Order order = orders.get(orderId);
        if (order != null) {
            order.setStatus(status);
        } else {
            throw new IllegalArgumentException("Order not found: " + orderId);
        }
    }

    // 应用第59条：了解和使用类库（使用Stream API）
    public Map<Order.OrderStatus, Long> getOrderStatusCounts() {
        return orders.values().stream()
                .collect(Collectors.groupingBy(Order::getStatus, Collectors.counting()));
    }

    // 应用第59条：了解和使用类库（使用Stream API）
    public Optional<Order> findLargestOrder() {
        return orders.values().stream()
                .max((o1, o2) -> o1.getTotalPrice().compareTo(o2.getTotalPrice()));
    }
}
