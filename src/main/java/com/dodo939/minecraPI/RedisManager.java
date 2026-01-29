package com.dodo939.minecraPI;

import redis.clients.jedis.ConnectionPoolConfig;
import redis.clients.jedis.RedisClient;

import java.time.Duration;

import static com.dodo939.minecraPI.MinecraPI.config;

public class RedisManager {
    private static RedisClient client;

    private RedisManager() {}

    public static void init() {
        if (client != null) {
            client.close();
        }
        ConnectionPoolConfig poolConfig = new ConnectionPoolConfig();
        poolConfig.setMaxTotal(20);
        poolConfig.setMaxIdle(20);
        poolConfig.setMinIdle(5);
        poolConfig.setBlockWhenExhausted(true);
        poolConfig.setMaxWait(Duration.ofSeconds(2));
        poolConfig.setTestWhileIdle(true);
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(30));

        client = RedisClient.builder()
                .fromURI(config.redis_url)
                .poolConfig(poolConfig)
                .build();
    }

    public static RedisClient getClient() {
        if (client == null) {
            throw new IllegalStateException("Redis client not initialized");
        }
        return client;
    }
}
