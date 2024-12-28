package com.learn.grammar.cache;

import java.util.TreeMap;

public class UserRepository {
    public User getUserById(String userId) {
        try {
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return new User(userId, "Jone Doe", "John.doe@example.com");
    }
}
