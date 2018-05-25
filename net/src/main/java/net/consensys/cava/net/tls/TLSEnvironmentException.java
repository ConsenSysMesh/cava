package net.consensys.cava.net.tls;

final class TLSEnvironmentException extends RuntimeException {

  TLSEnvironmentException(String message, Throwable t) {
    super(message, t);
  }
}
