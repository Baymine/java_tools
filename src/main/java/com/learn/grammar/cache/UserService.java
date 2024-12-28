package com.learn.grammar.cache;

import com.github.benmanes.caffeine.cache.Caffeine;
import com.google.common.cache.LoadingCache;

import java.util.concurrent.TimeUnit;

public class UserService {
        private final UserRepository userRepository;
        private final LoadingCache<String, User> userCache;


    public UserService(UserRepository userRepository, LoadingCache<String, User> userCache) {
        this.userRepository = userRepository;
        this.userCache = userCache;
    }
}
