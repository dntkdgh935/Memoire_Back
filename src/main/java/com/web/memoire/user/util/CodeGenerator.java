// src/main/java/com/web/memoire/user/util/CodeGenerator.java
package com.web.memoire.user.util;

import java.security.SecureRandom;

public class CodeGenerator {
    private static final String CHARACTERS = "0123456789";
    private static final SecureRandom random = new SecureRandom();

    public static String generateRandomCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(CHARACTERS.charAt(random.nextInt(CHARACTERS.length())));
        }
        return sb.toString();
    }
}