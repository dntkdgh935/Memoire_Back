package com.web.memoire.atelier.ImTIm.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class ImageGenerationException extends RuntimeException {
    public ImageGenerationException(String message) {
        super(message);
    }
    public ImageGenerationException(String message, Throwable cause) {super (message, cause);}
}
