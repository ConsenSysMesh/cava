/*
 * Copyright 2018 ConsenSys AG.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations under the License.
 */
package net.consensys.cava.eth.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.rlp.InvalidRLPTypeException;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPException;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.lang.ref.SoftReference;
import java.math.BigInteger;
import javax.annotation.Nullable;

import com.google.common.base.Objects;

/**
 * An Ethereum transaction.
 */
public final class Transaction {

  /**
   * Deserialize a transaction from RLP encoded bytes.
   *
   * @param encoded The RLP encoded transaction.
   * @return The de-serialized transaction.
   * @throws RLPException If there is an error decoding the transaction.
   */
  public static Transaction fromBytes(Bytes encoded) {
    requireNonNull(encoded);
    return RLP.decode(encoded, (reader) -> {
      Transaction tx = reader.readList(Transaction::readFrom);
      if (!reader.isComplete()) {
        throw new InvalidRLPTypeException("Additional bytes present at the end of the encoded transaction");
      }
      return tx;
    });
  }

  /**
   * Deserialize a transaction from an RLP input.
   *
   * @param reader The RLP reader.
   * @return The de-serialized transaction.
   * @throws RLPException If there is an error decoding the transaction.
   */
  public static Transaction readFrom(RLPReader reader) {
    UInt256 nonce = reader.readUInt256(false);
    Wei gasPrice = Wei.valueOf(reader.readUInt256(false));
    Gas gasLimit = Gas.valueOf(reader.readLong(false));
    Bytes addressBytes = reader.readValue();
    Address address;
    try {
      address = addressBytes.isEmpty() ? null : Address.fromBytes(addressBytes);
    } catch (IllegalArgumentException e) {
      throw new InvalidRLPTypeException("Value is the wrong size to be an address");
    }
    Wei value = Wei.valueOf(reader.readUInt256(false));
    Bytes payload = reader.readValue();
    Bytes vbytes = reader.readValue();
    if (vbytes.size() != 1) {
      throw new InvalidRLPTypeException(
          "The 'v' portion of the signature should be exactly 1 byte, it is " + vbytes.size() + " instead");
    }
    byte v = vbytes.get(0);
    Bytes rbytes = reader.readValue();
    if (rbytes.size() > 32) {
      throw new InvalidRLPTypeException(
          "The length of the 'r' portion of the signature is " + rbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger r = rbytes.toUnsignedBigInteger();
    Bytes sbytes = reader.readValue();
    if (sbytes.size() > 32) {
      throw new InvalidRLPTypeException(
          "The length of the 's' portion of the signature is " + sbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger s = sbytes.toUnsignedBigInteger();
    if (!reader.isComplete()) {
      throw new InvalidRLPTypeException("Additional bytes present at the end of the RLP transaction encoding");
    }
    return new Transaction(nonce, gasPrice, gasLimit, address, value, payload, Signature.create(v, r, s));
  }

  private final UInt256 nonce;
  private final Wei gasPrice;
  private final Gas gasLimit;
  @Nullable
  private final Address to;
  private final Wei value;
  private final Signature signature;
  private final Bytes payload;
  private SoftReference<Hash> hash;

  /**
   * Create a transaction.
   *
   * @param nonce The transaction nonce.
   * @param gasPrice The transaction gas price.
   * @param gasLimit The transaction gas limit.
   * @param to The target contract address, if any.
   * @param value The amount of Eth to transfer.
   * @param payload The transaction payload.
   * @param signature The transaction signature.
   */
  public Transaction(
      UInt256 nonce,
      Wei gasPrice,
      Gas gasLimit,
      @Nullable Address to,
      Wei value,
      Bytes payload,
      Signature signature) {
    requireNonNull(nonce);
    checkArgument(nonce.compareTo(UInt256.ZERO) >= 0, "Nonce less than zero");
    requireNonNull(gasPrice);
    requireNonNull(value);
    requireNonNull(signature);
    requireNonNull(payload);
    this.nonce = nonce;
    this.gasPrice = gasPrice;
    this.gasLimit = gasLimit;
    this.to = to;
    this.value = value;
    this.signature = signature;
    this.payload = payload;
  }

  /**
   * @return The transaction nonce.
   */
  public UInt256 nonce() {
    return nonce;
  }

  /**
   * @return The transaction gas price.
   */
  public Wei gasPrice() {
    return gasPrice;
  }

  /**
   * @return The transaction gas limit.
   */
  public Gas gasLimit() {
    return gasLimit;
  }

  /**
   * @return The target contract address, or null if not present.
   */
  @Nullable
  public Address to() {
    return to;
  }

  /**
   * @return The amount of Eth to transfer.
   */
  public Wei value() {
    return value;
  }

  /**
   * @return The transaction signature.
   */
  public Signature signature() {
    return signature;
  }

  /**
   * @return The transaction payload.
   */
  public Bytes payload() {
    return payload;
  }

  /**
   * Calculate and return the hash for this transaction.
   *
   * <p>
   * Note: the hash is calculated lazily and stored (as a {@link SoftReference} for future access.
   *
   * @return The hash.
   */
  public Hash hash() {
    if (hash != null) {
      Hash hashed = hash.get();
      if (hashed != null) {
        return hashed;
      }
    }
    Bytes rlp = toBytes();
    Hash hashed = Hash.hash(rlp);
    hash = new SoftReference<>(hashed);
    return hashed;
  }

  /**
   * @return The sender of the transaction.
   */
  public Address sender() {
    PublicKey publicKey = PublicKey.recoverFromSignature(RLP.encodeList(writer -> {
      writer.writeUInt256(nonce);
      writer.writeValue(gasPrice.toMinimalBytes());
      writer.writeValue(gasLimit.toMinimalBytes());
      writer.writeValue((to != null) ? to.toBytes() : Bytes.EMPTY);
      writer.writeValue(value.toMinimalBytes());
      writer.writeValue(payload);
    }), signature);
    return Address.fromBytes(Hash.hash(publicKey.bytes()).toBytes().slice(12, 20));
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof Transaction)) {
      return false;
    }
    Transaction that = (Transaction) obj;
    return nonce.equals(that.nonce)
        && gasPrice.equals(that.gasPrice)
        && gasLimit.equals(that.gasLimit)
        && Objects.equal(to, that.to)
        && value.equals(that.value)
        && signature.equals(that.signature)
        && payload.equals(that.payload);
  }

  @Override
  public int hashCode() {
    return Objects.hashCode(nonce, gasPrice, gasLimit, to, value, signature, payload);
  }

  @Override
  public String toString() {
    return "Transaction{"
        + "nonce="
        + nonce
        + ", gasPrice="
        + gasPrice
        + ", gasLimit="
        + gasLimit
        + ", to="
        + to
        + ", value="
        + value
        + ", signature="
        + signature
        + ", payload="
        + payload
        + '}';
  }

  /**
   * @return The RLP serialized form of this transaction.
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::writeTo);
  }

  /**
   * Write this transaction to an RLP output.
   *
   * @param writer The RLP writer.
   */
  public void writeTo(RLPWriter writer) {
    writer.writeUInt256(nonce);
    writer.writeUInt256(gasPrice.toUInt256());
    writer.writeLong(gasLimit.toLong());
    writer.writeValue((to != null) ? to.toBytes() : Bytes.EMPTY);
    writer.writeUInt256(value.toUInt256());
    writer.writeValue(payload);
    writer.writeValue(Bytes.of(signature.v()));
    writer.writeBigInteger(signature.r());
    writer.writeBigInteger(signature.s());
  }
}
