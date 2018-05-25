package net.consensys.cava.crypto.sodium;

/**
 * An exception that is thrown when an error occurs using the native sodium library.
 */
public final class SodiumException extends RuntimeException {

  /**
   * @param message The exception message.
   */
  public SodiumException(String message) {
    super(message);
  }
}
