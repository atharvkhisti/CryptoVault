package com.cryptovault.auth;

import com.cryptovault.auth.repository.UserRepository;
import com.cryptovault.auth.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import java.util.List;

@SpringBootTest
class AuthServiceApplicationTests {

    @Autowired
    private UserRepository userRepository;

    @Test
    void contextLoads() {
        List<User> users = userRepository.findAll();
        System.out.println("DATABASE_USERS_START");
        for (User u : users) {
            System.out.println("USER: " + u.getEmail() + " | ROLE: " + u.getRole() + " | ID: " + u.getId());
        }
        System.out.println("DATABASE_USERS_END");
    }
}
