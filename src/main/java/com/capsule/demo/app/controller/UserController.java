package com.capsule.demo.app.controller;

import com.capsule.demo.app.model.User;
import com.capsule.demo.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class UserController {
    @Autowired
    private UserService userService;

    @GetMapping("/users")
    public List<User> queryAll() {
        return userService.queryAll();
    }

    @GetMapping("/users/{userId}")
    public User queryEntity(@PathVariable("userId") String userId) {
        return userService.queryEntity(userId);
    }

    @PostMapping("/users")
    public void createUser(@RequestBody User user) {
        userService.createUser(user);
    }
}
