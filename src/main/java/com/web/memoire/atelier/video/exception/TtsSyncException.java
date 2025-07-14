package com.web.memoire.atelier.video.exception;

public class TtsSyncException extends RuntimeException {
  public TtsSyncException(String message) {
    super(message);
  }
  public TtsSyncException(String message, Throwable cause) {
    super(message, cause);
  }
}
