package com.web.memoire.atelier.video.exception;

public class InvalidVideoInputException extends RuntimeException {
    public InvalidVideoInputException(String message) {
        super(message);
    }
    public InvalidVideoInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
