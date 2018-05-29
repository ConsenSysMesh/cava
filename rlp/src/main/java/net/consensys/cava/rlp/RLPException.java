package net.consensys.cava.rlp;

/**
 * Base type for all RLP encoding and decoding exceptions.
 */
public class RLPException extends RuntimeException {
  public RLPException(String message) {
    super(message);
  }
}
