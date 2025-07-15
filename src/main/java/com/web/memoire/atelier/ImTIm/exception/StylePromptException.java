package com.web.memoire.atelier.ImTIm.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class StylePromptException extends RuntimeException {
    public StylePromptException(String message) {super(message);}
    public StylePromptException(String message, Throwable cause) {super(message, cause);}
}
