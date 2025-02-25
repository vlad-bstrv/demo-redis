package com.example.demoredis.service;

import com.example.demoredis.entity.User;
import com.example.demoredis.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User save(User user) {
        return userRepository.save(user);
    }

    @Cacheable(value = "user", key = "#id")
    public User getById(Integer id) {
        return userRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    @CacheEvict(value = "user", key = "#user.id")
    public User update(User user) {
        return userRepository.save(user);
    }

    @CacheEvict(value = "user", key = "#id")
    public void delete(Integer id) {
        userRepository.deleteById(id);
    }
}
