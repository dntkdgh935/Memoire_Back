package com.web.memoire.atelier.text.exception;

/**
 * 텍스트 생성 중 발생하는 예외를 처리하는 커스텀 예외 클래스
 */
public class TextGenerationException extends RuntimeException {

    // 기본 메시지만 받는 생성자
    public TextGenerationException(String message) {
        super(message);
    }

    // 메시지와 원인 예외(Throwable)를 함께 받는 생성자
    public TextGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}