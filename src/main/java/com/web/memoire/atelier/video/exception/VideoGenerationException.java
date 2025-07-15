package com.web.memoire.atelier.video.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class VideoGenerationException extends RuntimeException {
  public VideoGenerationException(String message) {
    super(message);
  }
  public VideoGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
