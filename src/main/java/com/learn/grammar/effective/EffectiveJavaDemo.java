package com.learn.grammar.effective;

import com.learn.grammar.effective.user.User;
import com.learn.grammar.effective.user.UserManager;
import com.learn.grammar.effective.product.Product;
import com.learn.grammar.effective.product.ProductManager;
import com.learn.grammar.effective.order.Order;
import com.learn.grammar.effective.order.OrderManager;
import com.learn.grammar.effective.cart.ShoppingCart;
import com.learn.grammar.effective.cart.SimpleShoppingCart;

import java.math.BigDecimal;

public class EffectiveJavaDemo {
    public static void main(String[] args) {
        demonstrateUserManagement();
        demonstrateProductManagement();
        demonstrateShoppingCart();
        demonstrateOrderManagement();
    }

    // 应用第57条：将局部变量的作用域最小化
    private static void demonstrateUserManagement() {
        System.out.println("User Management Demonstration:");
        UserManager userManager = UserManager.getInstance();
        User user = userManager.createUser("john_doe", "jo***@example.com", "password123");
        System.out.println("Created user: " + user);
        System.out.println();
    }

    private static void demonstrateProductManagement() {
        System.out.println("Product Management Demonstration:");
        ProductManager productManager = ProductManager.getInstance();
        Product product1 = productManager.createProduct("P001", "Laptop", new BigDecimal("999.99"));
        Product product2 = productManager.createProduct("P002", "Smartphone", new BigDecimal("499.99"));
        System.out.println("Created products: " + product1 + ", " + product2);
        System.out.println();
    }

    private static void demonstrateShoppingCart() {
        System.out.println("Shopping Cart Demonstration:");
        ProductManager productManager = ProductManager.getInstance();
        Product product1 = productManager.getProduct("P001");
        Product product2 = productManager.getProduct("P002");
        
        ShoppingCart cart = new SimpleShoppingCart();
        cart.addProduct(product1, 1);
        cart.addProduct(product2, 2);
        System.out.println("Shopping cart total: $" + cart.getTotalPrice());
        System.out.println();
    }

    private static void demonstrateOrderManagement() {
        System.out.println("Order Management Demonstration:");
        UserManager userManager = UserManager.getInstance();
        User user = userManager.getUser("john_doe");
        
        OrderManager orderManager = OrderManager.getInstance();
        Order order = orderManager.createOrder(user);
        
        ProductManager productManager = ProductManager.getInstance();
        Product product1 = productManager.getProduct("P001");
        Product product2 = productManager.getProduct("P002");
        
        orderManager.addProductToOrder(order.getId(), product1, 1);
        orderManager.addProductToOrder(order.getId(), product2, 2);
        
        System.out.println("Created order: " + order);
        System.out.println("Order total: $" + order.getTotalPrice());

        orderManager.updateOrderStatus(order.getId(), Order.OrderStatus.PROCESSING);
        System.out.println("Updated order status: " + orderManager.getOrder(order.getId()).get().getStatus());
    }
}
