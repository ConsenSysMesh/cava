package net.consensys.cava.rlp;

/**
 * Indicates that an unexpected type was encountered when decoding RLP.
 */
public class InvalidRLPTypeException extends RLPException {
  public InvalidRLPTypeException(String message) {
    super(message);
  }
}
