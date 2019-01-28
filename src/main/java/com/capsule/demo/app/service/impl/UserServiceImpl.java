package com.capsule.demo.app.service.impl;

import com.capsule.demo.app.model.User;
import com.capsule.demo.app.repository.UserRepository;
import com.capsule.demo.app.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.*;

@Service
public class UserServiceImpl implements UserService {
    @Autowired
    private UserRepository userRepository;

    @Override
    public List<User> queryAll() {
        Iterable<User> iterable = userRepository.findAll();
        List<User> users = new ArrayList<>();
        for(Iterator<User> it = iterable.iterator(); it.hasNext(); ){
            users.add(it.next());
        }
        return users;
    }

    @Cacheable(value = "users", key="#id")
    @Override
    public User queryEntity(String id) {
        Optional<User> user = userRepository.findById(id);
        return user.orElse(null);
    }

    @CachePut(value = "users", key = "#user.id")
    @Transactional
    @Override
    public User createUser(User user) {
        Date now = new Date();
        user.setUtcCreate(now);
        user.setUtcModified(now);
        return userRepository.save(user);
    }
}
