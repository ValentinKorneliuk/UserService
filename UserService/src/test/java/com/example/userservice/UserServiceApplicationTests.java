package com.example.userservice;

import static org.junit.Assert.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@ActiveProfiles("test")
@SpringBootTest
class UserServiceApplicationTests {

    @Test
    void contextLoads() {
        assertTrue(true);
    }

}
