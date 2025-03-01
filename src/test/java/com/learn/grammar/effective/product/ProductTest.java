package com.learn.grammar.effective.product;

import org.junit.jupiter.api.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.*;

import java.math.BigDecimal;

class ProductTest {

    @Test
    void testProductBuilder() {
        Product product = new Product.Builder("P001", "Laptop", new BigDecimal("999.99"))
                .description("High-performance laptop")
                .build();

        assertEquals("P001", product.getId());
        assertEquals("Laptop", product.getName());
        assertEquals(new BigDecimal("999.99"), product.getPrice());
        assertEquals("High-performance laptop", product.getDescription());
    }

    @Test
    void testProductBuilderWithoutOptionalFields() {
        Product product = new Product.Builder("P002", "Smartphone", new BigDecimal("499.99"))
                .build();

        assertEquals("P002", product.getId());
        assertEquals("Smartphone", product.getName());
        assertEquals(new BigDecimal("499.99"), product.getPrice());
        assertEquals("", product.getDescription());
    }

    @Test
    void testProductToString() {
        Product product = new Product.Builder("P003", "Tablet", new BigDecimal("299.99"))
                .description("10-inch tablet")
                .build();

        String expectedString = "Product{id='P003', name='Tablet', price=299.99, description='10-inch tablet'}";
        assertEquals(expectedString, product.toString());
    }
}
