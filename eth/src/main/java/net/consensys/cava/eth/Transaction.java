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
package net.consensys.cava.eth;

import static com.google.common.base.Preconditions.checkArgument;
import static java.util.Objects.requireNonNull;
import static net.consensys.cava.crypto.Hash.keccak256;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.crypto.SECP256K1KeyRecoveryException;
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

  // The base of the signature v-value
  private static final int V_BASE = 27;

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
        throw new RLPException("Additional bytes present at the end of the encoded transaction");
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
      throw new RLPException("Value is the wrong size to be an address", e);
    }
    Wei value = Wei.valueOf(reader.readUInt256(false));
    Bytes payload = reader.readValue();
    byte encodedV = reader.readByte();
    Bytes rbytes = reader.readValue();
    if (rbytes.size() > 32) {
      throw new RLPException("r-value of the signature is " + rbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger r = rbytes.toUnsignedBigInteger();
    Bytes sbytes = reader.readValue();
    if (sbytes.size() > 32) {
      throw new RLPException("s-value of the signature is " + sbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger s = sbytes.toUnsignedBigInteger();
    if (!reader.isComplete()) {
      throw new RLPException("Additional bytes present at the end of the encoding");
    }

    byte v = (byte) ((int) encodedV - V_BASE);

    Signature signature;
    try {
      signature = Signature.create(v, r, s);
    } catch (IllegalArgumentException e) {
      throw new RLPException("Invalid signature: " + e.getMessage());
    }
    try {
      return new Transaction(nonce, gasPrice, gasLimit, address, value, payload, signature);
    } catch (IllegalArgumentException e) {
      throw new RLPException(e.getMessage(), e);
    }
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
  private SoftReference<Address> sender;

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
    checkArgument(nonce.compareTo(UInt256.ZERO) >= 0, "nonce must be >= 0");
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
   * @return <tt>true</tt> if the transaction is a contract creation (<tt>to</tt> address is <tt>null</tt>).
   */
  public boolean isContractCreation() {
    return to == null;
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
   * @throws IllegalStateException If the transaction signature is invalid and the sender cannot be determined.
   */
  public Address sender() {
    if (sender != null) {
      Address address = sender.get();
      if (address != null) {
        return address;
      }
    }
    PublicKey publicKey;
    try {
      publicKey = PublicKey.recoverFromSignature(RLP.encodeList(writer -> {
        writer.writeUInt256(nonce);
        writer.writeValue(gasPrice.toMinimalBytes());
        writer.writeValue(gasLimit.toMinimalBytes());
        writer.writeValue((to != null) ? to.toBytes() : Bytes.EMPTY);
        writer.writeValue(value.toMinimalBytes());
        writer.writeValue(payload);
      }), signature);
    } catch (SECP256K1KeyRecoveryException e) {
      throw new IllegalStateException("Invalid transaction signature", e);
    }
    Address address = Address.fromBytes(Bytes.wrap(keccak256(publicKey.bytesArray()), 12, 20));
    sender = new SoftReference<>(address);
    return address;
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
    return String.format(
        "Transaction{nonce=%s, gasPrice=%s, gasLimit=%s, to=%s, value=%s, signature=%s, payload=%s",
        nonce,
        gasPrice,
        gasLimit,
        to,
        value,
        signature,
        payload);
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
    writer.writeByte((byte) ((int) signature.v() + V_BASE));
    writer.writeBigInteger(signature.r());
    writer.writeBigInteger(signature.s());
  }
}
