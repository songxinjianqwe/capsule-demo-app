package com.capsule.demo.app.service;

import com.capsule.demo.app.model.User;

import java.util.List;

public interface UserService {
    List<User> queryAll();
    User queryEntity(String id);
    User createUser(User user);
}
