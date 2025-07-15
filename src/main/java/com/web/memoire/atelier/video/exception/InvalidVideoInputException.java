package com.web.memoire.atelier.video.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class InvalidVideoInputException extends RuntimeException {
    public InvalidVideoInputException(String message) {
        super(message);
    }
    public InvalidVideoInputException(String message, Throwable cause) {
        super(message, cause);
    }
}
