package com.example.userservice.integration;

import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;
import com.redis.testcontainers.RedisContainer;

@ActiveProfiles("test")
public abstract class BaseContainerIntegrationTest {

    public static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:17")
        .withDatabaseName("testdb")
        .withUsername("testuser")
        .withPassword("testpass");

    public static final RedisContainer redis = new RedisContainer(DockerImageName.parse("redis:8.0.2"))
        .withEnv("REDIS_PASSWORD", "testpass");


    static {
        postgres.start();
        redis.start();
    }
    
}
