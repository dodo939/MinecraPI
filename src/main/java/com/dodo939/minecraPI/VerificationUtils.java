package com.dodo939.minecraPI;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

public class VerificationUtils {
    private static final Map<String, VerificationData> verificationDatas = new HashMap<>();
    private static final String CHARACTERS = "0123456789";
    private static final Random RANDOM = new Random(System.currentTimeMillis());

    public static String setVerificationCode(UUID uuid) {
        String code = generateVerificationCode();
        long expiryTime = System.currentTimeMillis() + (60 * 1000);
        verificationDatas.put(code, new VerificationData(uuid, expiryTime));
        return code;
    }

    public static UUID getUUIDByCode(String code) {
        VerificationData data = verificationDatas.get(code);
        if (data == null) return null;

        if (System.currentTimeMillis() > data.expiryTime) {
            verificationDatas.remove(code);
            return null;
        }

        return data.uuid;
    }

    public static void removeVerificationCode(String code) {
        verificationDatas.remove(code);
    }

    private static String generateVerificationCode() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            int index = RANDOM.nextInt(CHARACTERS.length());
            sb.append(CHARACTERS.charAt(index));
        }
        String code = sb.toString();
        if (verificationDatas.get(code) != null) {
            return generateVerificationCode();
        }
        return code;
    }

    public static void cleanCodes() {
        long currentTime = System.currentTimeMillis();
        verificationDatas.entrySet().removeIf(entry -> currentTime > entry.getValue().expiryTime);
    }

    private static class VerificationData {
        UUID uuid;
        long expiryTime;

        VerificationData(UUID uuid, long expiryTime) {
            this.uuid = uuid;
            this.expiryTime = expiryTime;
        }
    }
}
