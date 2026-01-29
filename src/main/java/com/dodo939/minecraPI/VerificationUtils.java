package com.dodo939.minecraPI;

import redis.clients.jedis.RedisClient;

import java.util.Random;

public class VerificationUtils {
    private static final String CHARACTERS = "0123456789";
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static String setVerificationCode(String uuid) {
        String code = generateVerificationCode();
        RedisClient client = RedisManager.getClient();
        client.setex("minecrapi:code:" + code, 60, uuid);
        return code;
    }

    public static String getUUIDByCode(String code) {
        RedisClient client = RedisManager.getClient();
        return client.get("minecrapi:code:" + code);
    }

    public static void removeVerificationCode(String code) {
        RedisClient client = RedisManager.getClient();
        client.del("minecrapi:code:" + code);
    }

    private static String generateVerificationCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String code = sb.toString();
        RedisClient client = RedisManager.getClient();
        if (client.exists("minecrapi:code:" + code)) {
            return generateVerificationCode();
        }
        return code;
    }

    public static void cleanCodes(String uuid) {
        RedisClient client = RedisManager.getClient();
        client.keys("minecrapi:code:*").forEach(key -> {
            if (client.get(key).equals(uuid)) {
                client.del(key);
            }
        });
    }
}
