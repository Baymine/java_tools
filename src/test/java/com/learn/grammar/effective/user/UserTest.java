package com.learn.grammar.effective.user;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void testUserBuilder() {
        User user = new User.Builder("johndoe", "john@example.com", "password123")
                .age(30)
                .address("123 Main St")
                .build();

        assertEquals("johndoe", user.getUsername());
        assertEquals("john@example.com", user.getEmail());
        assertEquals("password123", user.getPassword());
        assertEquals(30, user.getAge());
        assertEquals("123 Main St", user.getAddress());
    }

    @Test
    void testUserBuilderWithoutOptionalFields() {
        User user = new User.Builder("janedoe", "jane@example.com", "password456")
                .build();

        assertEquals("janedoe", user.getUsername());
        assertEquals("jane@example.com", user.getEmail());
        assertEquals("password456", user.getPassword());
        assertEquals(0, user.getAge());
        assertEquals("", user.getAddress());
    }

    @Test
    void testUserToString() {
        User user = new User.Builder("testuser", "test@example.com", "testpass")
                .age(25)
                .address("456 Elm St")
                .build();

        String expectedString = "User{username='testuser', email='test@example.com', age=25, address='456 Elm St'}";
        assertEquals(expectedString, user.toString());
    }
}
