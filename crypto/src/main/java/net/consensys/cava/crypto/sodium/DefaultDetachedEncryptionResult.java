package net.consensys.cava.crypto.sodium;

import net.consensys.cava.bytes.Bytes;

final class DefaultDetachedEncryptionResult implements DetachedEncryptionResult {

  private final byte[] cipherText;
  private final byte[] mac;

  public DefaultDetachedEncryptionResult(byte[] cipherText, byte[] mac) {
    this.cipherText = cipherText;
    this.mac = mac;
  }

  @Override
  public Bytes cipherText() {
    return Bytes.wrap(cipherText);
  }

  @Override
  public byte[] cipherTextArray() {
    return cipherText;
  }

  @Override
  public Bytes mac() {
    return Bytes.wrap(mac);
  }

  @Override
  public byte[] macArray() {
    return mac;
  }
}
