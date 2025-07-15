package com.web.memoire.atelier.video.exception;

import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus
public class TtsSyncException extends RuntimeException {
  public TtsSyncException(String message) {
    super(message);
  }
  public TtsSyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
