package com.production.management.exception;

public class CircularReferenceException extends RuntimeException {

  public CircularReferenceException(String path) {
    super(path);
  }

}
