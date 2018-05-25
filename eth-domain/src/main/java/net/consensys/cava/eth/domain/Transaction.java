package net.consensys.cava.eth.domain;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.crypto.SECP256K1.PublicKey;
import net.consensys.cava.crypto.SECP256K1.Signature;
import net.consensys.cava.rlp.InvalidRLPEncodingException;
import net.consensys.cava.rlp.InvalidRLPTypeException;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;
import net.consensys.cava.units.bigints.UInt256;
import net.consensys.cava.units.ethereum.Gas;
import net.consensys.cava.units.ethereum.Wei;

import java.lang.ref.SoftReference;
import java.math.BigInteger;
import java.util.Optional;

import com.google.common.base.Objects;

/**
 * An Ethereum transaction.
 */
public final class Transaction {

  /**
   * Deserialize a transaction from RLP encoded bytes.
   *
   * @param encoded The RLP encoded transaction.
   * @return The deserialized transaction.
   */
  public static Transaction fromBytes(Bytes encoded) {
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
   * @return The deserialized transaction.
   */
  public static Transaction readFrom(RLPReader reader) {
    UInt256 nonce = fromMinimalBytes(reader.readValue(), "nonce");
    Wei gasPrice = Wei.valueOf(fromMinimalBytes(reader.readValue(), "gasPrice"));
    Gas gasLimit = Gas.valueOf(fromMinimalBytes(reader.readValue(), "gasLimit"));
    Optional<Address> addressOptional = Optional.of(reader.readValue()).map(bytes -> {
      if (bytes.isEmpty()) {
        return null;
      } else {
        return Address.fromBytes(bytes);
      }
    });
    Wei value = Wei.valueOf(fromMinimalBytes(reader.readValue(), "wei"));
    Bytes payload = reader.readValue();
    Bytes vbytes = reader.readValue();
    if (vbytes.size() != 1) {
      throw new IllegalArgumentException(
          "The 'v' portion of the signature should be exactly 1 byte, it is " + vbytes.size() + " instead");
    }
    byte v = vbytes.get(0);
    Bytes rbytes = reader.readValue();
    if (rbytes.hasLeadingZeroByte()) {
      throw new IllegalArgumentException("The 'r' portion of the signature contains leading zero-byte values");
    }
    if (rbytes.size() > 32) {
      throw new IllegalArgumentException(
          "The length of the 'r' portion of the signature is " + rbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger r = rbytes.unsignedBigIntegerValue();
    Bytes sbytes = reader.readValue();
    if (sbytes.hasLeadingZeroByte()) {
      throw new IllegalArgumentException("The 's' portion of the signature contains leading zero-byte values");
    }
    if (sbytes.size() > 32) {
      throw new IllegalArgumentException(
          "The length of the 's' portion of the signature is " + sbytes.size() + ", it should be at most 32 bytes");
    }
    BigInteger s = sbytes.unsignedBigIntegerValue();
    if (!reader.isComplete()) {
      throw new InvalidRLPTypeException("Additional bytes present at the end of the encoded transaction list");
    }
    return new Transaction(nonce, gasPrice, gasLimit, addressOptional, value, payload, Signature.create(v, r, s));
  }

  private final UInt256 nonce;
  private final Wei gasPrice;
  private final Gas gasLimit;
  private final Optional<Address> to;
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
      Optional<Address> to,
      Wei value,
      Bytes payload,
      Signature signature) {
    checkNotNull(nonce);
    checkArgument(nonce.compareTo(UInt256.ZERO) >= 0, "Nonce less than zero");
    checkNotNull(gasPrice);
    checkNotNull(to);
    checkNotNull(value);
    checkNotNull(signature);
    checkNotNull(payload);
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
   * @return The target contract address, if any.
   */
  public Optional<Address> to() {
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
      writer.writeValue(nonce().toMinimalBytes());
      writer.writeValue(gasPrice().toMinimalBytes());
      writer.writeValue(gasLimit().toMinimalBytes());
      writer.writeValue(to().map(Address::toBytes).orElse(Bytes.EMPTY));
      writer.writeValue(value().toMinimalBytes());
      writer.writeValue(payload());
    }), signature());
    return Address.fromBytes(Hash.hash(publicKey.encodedBytes()).toBytes().slice(12, 20));
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
        && to.equals(that.to)
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
    writer.writeValue(nonce.toMinimalBytes());
    writer.writeValue(gasPrice.toMinimalBytes());
    writer.writeValue(gasLimit.toMinimalBytes());
    writer.writeValue(to.map(Address::toBytes).orElse(Bytes.EMPTY));
    writer.writeValue(value.toMinimalBytes());
    writer.writeValue(payload);
    writer.writeValue(Bytes.of(signature.v()));
    writer.writeBigInteger(signature.r());
    writer.writeBigInteger(signature.s());
  }

  private static UInt256 fromMinimalBytes(Bytes bytes, String fieldName) {
    if (bytes.hasLeadingZeroByte()) {
      throw new InvalidRLPEncodingException("Unexpected leading zero byte in encoding of " + fieldName);
    }
    return UInt256.fromBytes(bytes);
  }
}
