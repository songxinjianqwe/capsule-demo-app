package com.capsule.demo.app.repository;

import com.capsule.demo.app.model.User;
import org.springframework.data.repository.CrudRepository;

public interface UserRepository extends CrudRepository<User, String> {
}
