package com.learn.grammar.effective.user;

import java.util.HashMap;
import java.util.Map;

// 应用第1条：用静态工厂方法代替构造器
// 应用第3条：用私有构造器或者枚举类型强化Singleton属性
public class UserManager {
    private static final UserManager INSTANCE = new UserManager();
    private final Map<String, User> users = new HashMap<>();

    private UserManager() {} // 私有构造器

    public static UserManager getInstance() {
        return INSTANCE;
    }

    // 静态工厂方法
    public static User createUser(String username, String email, String password) {
        String maskedEmail = maskEmail(email);
        return new User.Builder(username, maskedEmail, password).build();
    }

    public void registerUser(User user) {
        users.put(user.getUsername(), user);
    }

    public User getUser(String username) {
        return users.get(username);
    }

    public boolean authenticateUser(String username, String password) {
        User user = users.get(username);
        return user != null && user.getPassword().equals(password);
    }

    private static String maskEmail(String email) {
        int atIndex = email.indexOf('@');
        if (atIndex > 1) {
            return email.substring(0, 2) + "***" + email.substring(atIndex);
        }
        return email;
    }
}
