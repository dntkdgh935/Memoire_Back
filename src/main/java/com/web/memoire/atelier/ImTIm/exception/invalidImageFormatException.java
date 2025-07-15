package com.web.memoire.atelier.ImTIm.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class invalidImageFormatException extends RuntimeException {
    public invalidImageFormatException(String message) {
        super(message);
    }
    public invalidImageFormatException(String message, Throwable cause) {super(message, cause);}
}
