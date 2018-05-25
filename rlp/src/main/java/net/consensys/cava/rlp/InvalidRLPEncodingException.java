package net.consensys.cava.rlp;

/**
 * Indicates that invalid RLP encoding was encountered.
 */
public class InvalidRLPEncodingException extends RLPException {
  public InvalidRLPEncodingException(String message) {
    super(message);
  }
}
