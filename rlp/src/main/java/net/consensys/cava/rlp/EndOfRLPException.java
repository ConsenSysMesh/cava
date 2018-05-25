package net.consensys.cava.rlp;

/**
 * Indicates the end of the RLP source has been reached unexpectedly.
 */
public class EndOfRLPException extends RLPException {
  public EndOfRLPException() {
    super("End of RLP source reached");
  }
}
