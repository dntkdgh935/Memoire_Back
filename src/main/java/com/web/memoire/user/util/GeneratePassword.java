package com.web.memoire.user.util;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

public class GeneratePassword {
    /**
     * 지정된 길이 범위 내에서 랜덤한 비밀번호를 생성합니다.
     * 영문 대소문자, 숫자, 특수문자를 포함합니다.
     *
     * @param minLength 최소 길이
     * @param maxLength 최대 길이
     * @return 생성된 랜덤 비밀번호
     */
    public static String generateRandomPassword(int minLength, int maxLength) {
        String upperCaseLetters = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        String lowerCaseLetters = "abcdefghijklmnopqrstuvwxyz";
        String digits = "0123456789";
        String specialChars = "!@#$%^&*()-_=+[]{}|;:,.<>?"; // 일반적으로 사용되는 특수문자
        String allChars = upperCaseLetters + lowerCaseLetters + digits + specialChars;

        SecureRandom random = new SecureRandom();
        // minLength와 maxLength 사이의 랜덤 길이 선택
        int length = random.nextInt(maxLength - minLength + 1) + minLength;

        List<Character> passwordChars = IntStream.range(0, length)
                .mapToObj(i -> allChars.charAt(random.nextInt(allChars.length())))
                .collect(Collectors.toList());

        // 생성된 비밀번호가 최소한 각 문자 유형(대문자, 소문자, 숫자, 특수문자)을 하나씩 포함하도록 강제할 수 있습니다.
        // 현재는 단순히 모든 문자 유형에서 랜덤하게 선택하여 생성합니다.
        // 더 강력한 복잡성 요구사항이 있다면 이 부분을 수정해야 합니다.

        Collections.shuffle(passwordChars, random); // 문자 순서를 섞어 예측 불가능하게 만듦

        return passwordChars.stream()
                .map(String::valueOf)
                .collect(Collectors.joining());
    }
}
