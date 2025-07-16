package com.web.memoire.atelier.text.exception;

/**
 * 이미지 생성 및 저장 과정에서 발생하는 예외를 나타냅니다.
 */
public class ImageGenerationException extends RuntimeException {

    /**
     * 단순 메시지만으로 예외를 생성할 때 사용합니다.
     *
     * @param message 오류 메시지
     */
    public ImageGenerationException(String message) {
        super(message);
    }

    /**
     * 메시지와 원인(cause)을 함께 기록할 때 사용합니다.
     *
     * @param message 오류 메시지
     * @param cause   원인이 되는 예외
     */
    public ImageGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}