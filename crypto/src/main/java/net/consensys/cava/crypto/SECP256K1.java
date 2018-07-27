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
package net.consensys.cava.crypto;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.crypto.Hash.keccak256;
import static net.consensys.cava.io.file.Files.atomicReplace;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.units.bigints.UInt256;

import java.io.IOException;
import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.InvalidAlgorithmParameterException;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.spec.ECGenParameterSpec;
import java.util.Arrays;
import java.util.List;

import com.google.common.base.Objects;
import org.bouncycastle.asn1.sec.SECNamedCurves;
import org.bouncycastle.asn1.x9.X9ECParameters;
import org.bouncycastle.asn1.x9.X9IntegerConverter;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.params.ECDomainParameters;
import org.bouncycastle.crypto.params.ECPrivateKeyParameters;
import org.bouncycastle.crypto.params.ECPublicKeyParameters;
import org.bouncycastle.crypto.signers.ECDSASigner;
import org.bouncycastle.crypto.signers.HMacDSAKCalculator;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPrivateKey;
import org.bouncycastle.jcajce.provider.asymmetric.ec.BCECPublicKey;
import org.bouncycastle.math.ec.ECAlgorithms;
import org.bouncycastle.math.ec.ECPoint;
import org.bouncycastle.math.ec.FixedPointCombMultiplier;
import org.bouncycastle.math.ec.custom.sec.SecP256K1Curve;

/*
 * Adapted from the BitcoinJ ECKey (Apache 2 License) implementation:
 * https://github.com/bitcoinj/bitcoinj/blob/master/core/src/main/java/org/bitcoinj/core/ECKey.java
 *
 *
 * Adapted from the web3j (Apache 2 License) implementations:
 * https://github.com/web3j/web3j/crypto/src/main/java/org/web3j/crypto/*.java
 */

/**
 * An Elliptic Curve Digital Signature using parameters as used by Bitcoin, and defined in Standards for Efficient
 * Cryptography (SEC) (Certicom Research, http://www.secg.org/sec2-v2.pdf).
 *
 * <p>
 * This class depends upon the BouncyCastle library being available and added as a {@link java.security.Provider}. See
 * https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation.
 *
 * <p>
 * BouncyCastle can be included using the gradle dependency 'org.bouncycastle:bcprov-jdk15on'.
 */
public final class SECP256K1 {
  private SECP256K1() {}

  private static final String ALGORITHM = "ECDSA";
  private static final String CURVE_NAME = "secp256k1";
  private static final String PROVIDER = "BC";

  // Lazily initialize parameters by using java initialization on demand
  static final class Parameters {
    static final ECDomainParameters CURVE;
    static final BigInteger HALF_CURVE_ORDER;
    static final KeyPairGenerator KEY_PAIR_GENERATOR;

    static {
      try {
        Class.forName("org.bouncycastle.asn1.sec.SECNamedCurves");
      } catch (ClassNotFoundException e) {
        throw new IllegalStateException(
            "BouncyCastle is not available on the classpath, see https://www.bouncycastle.org/latest_releases.html");
      }
      X9ECParameters params = SECNamedCurves.getByName(CURVE_NAME);
      CURVE = new ECDomainParameters(params.getCurve(), params.getG(), params.getN(), params.getH());
      HALF_CURVE_ORDER = CURVE.getN().shiftRight(1);
      try {
        KEY_PAIR_GENERATOR = KeyPairGenerator.getInstance(ALGORITHM, PROVIDER);
      } catch (NoSuchProviderException e) {
        throw new IllegalStateException(
            "BouncyCastleProvider is not available, see https://www.bouncycastle.org/wiki/display/JA1/Provider+Installation",
            e);
      } catch (NoSuchAlgorithmException e) {
        throw new IllegalStateException("Algorithm should be available but was not", e);
      }
      ECGenParameterSpec ecGenParameterSpec = new ECGenParameterSpec(CURVE_NAME);
      try {
        KEY_PAIR_GENERATOR.initialize(ecGenParameterSpec, new SecureRandom());
      } catch (InvalidAlgorithmParameterException e) {
        throw new IllegalStateException("Algorithm parameter should be available but was not", e);
      }
    }
  }

  // Decompress a compressed public key (x co-ord and low-bit of y-coord).
  private static ECPoint decompressKey(BigInteger xBN, boolean yBit) {
    X9IntegerConverter x9 = new X9IntegerConverter();
    byte[] compEnc = x9.integerToBytes(xBN, 1 + x9.getByteLength(Parameters.CURVE.getCurve()));
    compEnc[0] = (byte) (yBit ? 0x03 : 0x02);
    return Parameters.CURVE.getCurve().decodePoint(compEnc);
  }

  /**
   * Given the components of a signature and a selector value, recover and return the public key that generated the
   * signature according to the algorithm in SEC1v2 section 4.1.6.
   *
   * <p>
   * The recId is an index from 0 to 3 which indicates which of the 4 possible keys is the correct one. Because the key
   * recovery operation yields multiple potential keys, the correct key must either be stored alongside the signature,
   * or you must be willing to try each recId in turn until you find one that outputs the key you are expecting.
   *
   * <p>
   * If this method returns null it means recovery was not possible and recId should be iterated.
   *
   * <p>
   * Given the above two points, a correct usage of this method is inside a for loop from 0 to 3, and if the output is
   * null OR a key that is not the one you expect, you try again with the next recId.
   *
   * @param recId Which possible key to recover.
   * @param r The R component of the signature.
   * @param s The S component of the signature.
   * @param message Hash of the data that was signed.
   * @throws IllegalArgumentException if no key can be recovered from the components
   * @return A ECKey containing only the public part.
   */
  private static BigInteger recoverFromSignature(int recId, BigInteger r, BigInteger s, Bytes32 message) {
    assert (recId >= 0);
    assert (r.signum() >= 0);
    assert (s.signum() >= 0);
    assert (message != null);

    // 1.0 For j from 0 to h (h == recId here and the loop is outside this function)
    // 1.1 Let x = r + jn
    BigInteger n = Parameters.CURVE.getN(); // Curve order.
    BigInteger i = BigInteger.valueOf((long) recId / 2);
    BigInteger x = r.add(i.multiply(n));
    // 1.2. Convert the integer x to an octet string X of length mlen using the conversion
    // routine specified in Section 2.3.7, where mlen = ⌈(log2 p)/8⌉ or mlen = ⌈m/8⌉.
    // 1.3. Convert the octet string (16 set binary digits)||X to an elliptic curve point R
    // using the conversion routine specified in Section 2.3.4. If this conversion
    // routine outputs "invalid", then do another iteration of Step 1.
    //
    // More concisely, what these points mean is to use X as a compressed public key.
    BigInteger prime = SecP256K1Curve.q;
    if (x.compareTo(prime) >= 0) {
      // Cannot have point co-ordinates larger than this as everything takes place modulo Q.
      throw new IllegalArgumentException("x is larger than curve q");
    }
    // Compressed keys require you to know an extra bit of data about the y-coord as there are
    // two possibilities. So it's encoded in the recId.
    ECPoint R = decompressKey(x, (recId & 1) == 1);
    // 1.4. If nR != point at infinity, then do another iteration of Step 1 (callers
    // responsibility).
    if (!R.multiply(n).isInfinity()) {
      throw new IllegalArgumentException("R times n does not point at infinity");
    }
    // 1.5. Compute e from M using Steps 2 and 3 of ECDSA signature verification.
    BigInteger e = message.unsignedBigIntegerValue();
    // 1.6. For k from 1 to 2 do the following. (loop is outside this function via
    // iterating recId)
    // 1.6.1. Compute a candidate public key as:
    // Q = mi(r) * (sR - eG)
    //
    // Where mi(x) is the modular multiplicative inverse. We transform this into the following:
    // Q = (mi(r) * s ** R) + (mi(r) * -e ** G)
    // Where -e is the modular additive inverse of e, that is z such that z + e = 0 (mod n).
    // In the above equation ** is point multiplication and + is point addition (the EC group
    // operator).
    //
    // We can find the additive inverse by subtracting e from zero then taking the mod. For
    // example the additive inverse of 3 modulo 11 is 8 because 3 + 8 mod 11 = 0, and
    // -3 mod 11 = 8.
    BigInteger eInv = BigInteger.ZERO.subtract(e).mod(n);
    BigInteger rInv = r.modInverse(n);
    BigInteger srInv = rInv.multiply(s).mod(n);
    BigInteger eInvrInv = rInv.multiply(eInv).mod(n);
    ECPoint q = ECAlgorithms.sumOfTwoMultiplies(Parameters.CURVE.getG(), eInvrInv, R, srInv);

    byte[] qBytes = q.getEncoded(false);
    // We remove the prefix
    return new BigInteger(1, Arrays.copyOfRange(qBytes, 1, qBytes.length));
  }

  /**
   * Generates an ECDSA signature.
   *
   * @param data The data to sign.
   * @param keyPair The keypair to sign using.
   * @return The signature.
   */
  public static Signature sign(Bytes data, KeyPair keyPair) {
    ECDSASigner signer = new ECDSASigner(new HMacDSAKCalculator(new SHA256Digest()));

    ECPrivateKeyParameters privKey =
        new ECPrivateKeyParameters(keyPair.getPrivateKey().encodedBytes().unsignedBigIntegerValue(), Parameters.CURVE);
    signer.init(true, privKey);

    Bytes32 dataHash = keccak256(data);
    BigInteger[] components = signer.generateSignature(dataHash.toArrayUnsafe());
    BigInteger r = components[0];
    BigInteger s = components[1];

    // Automatically adjust the S component to be less than or equal to half the curve
    // order, if necessary. This is required because for every signature (r,s) the signature
    // (r, -s (mod N)) is a valid signature of the same message. However, we dislike the
    // ability to modify the bits of a Bitcoin transaction after it's been signed, as that
    // violates various assumed invariants. Thus in future only one of those forms will be
    // considered legal and the other will be banned.
    if (s.compareTo(Parameters.HALF_CURVE_ORDER) > 0) {
      // The order of the curve is the number of valid points that exist on that curve.
      // If S is in the upper half of the number of valid points, then bring it back to
      // the lower half. Otherwise, imagine that
      // N = 10
      // s = 8, so (-8 % 10 == 2) thus both (r, 8) and (r, 2) are valid solutions.
      // 10 - 8 == 2, giving us always the latter solution, which is canonical.
      s = Parameters.CURVE.getN().subtract(s);
    }

    // Now we have to work backwards to figure out the recId needed to recover the signature.
    int recId = -1;
    BigInteger publicKeyBI = keyPair.getPublicKey().encodedBytes().unsignedBigIntegerValue();
    for (int i = 0; i < 4; i++) {
      BigInteger k = recoverFromSignature(i, r, s, dataHash);
      if (k.equals(publicKeyBI)) {
        recId = i;
        break;
      }
    }
    if (recId == -1) {
      throw new Error("Unexpected error - could not construct a recoverable key.");
    }

    byte v = (byte) (recId + 27);

    return new Signature(r, s, v);
  }

  /**
   * Verifies the given ECDSA signature against the message bytes using the public key bytes.
   *
   * <p>
   * When using native ECDSA verification, data must be 32 bytes, and no element may be larger than 520 bytes.
   *
   * @param data Hash of the data to verify.
   * @param signature ASN.1 encoded signature.
   * @param pub The public key bytes to use.
   * @return True if the verification is successful.
   */
  public static boolean verify(Bytes data, Signature signature, PublicKey pub) {
    ECDSASigner signer = new ECDSASigner();
    Bytes toDecode = Bytes.wrap(Bytes.of((byte) 4), pub.encodedBytes());
    ECPublicKeyParameters params =
        new ECPublicKeyParameters(Parameters.CURVE.getCurve().decodePoint(toDecode.toArray()), Parameters.CURVE);
    signer.init(false, params);
    try {
      Bytes32 dataHash = keccak256(data);
      return signer.verifySignature(dataHash.toArrayUnsafe(), signature.r, signature.s);
    } catch (NullPointerException e) {
      // Bouncy Castle contains a bug that can cause NPEs given specially crafted signatures. Those
      // signatures
      // are inherently invalid/attack sigs so we just fail them here rather than crash the thread.
      return false;
    }
  }

  /**
   * A SECP256K1 private key.
   */
  public static class PrivateKey implements java.security.PrivateKey {

    private final Bytes32 encoded;

    private static Bytes32 toBytes32(byte[] backing) {
      if (backing.length == Bytes32.SIZE) {
        return Bytes32.wrap(backing);
      } else if (backing.length > Bytes32.SIZE) {
        return Bytes32.wrap(backing, backing.length - Bytes32.SIZE);
      } else {
        return Bytes32.leftPad(Bytes.wrap(backing));
      }
    }

    /**
     * Create the private key from a {@link BigInteger}.
     *
     * @param key The integer describing the key.
     * @return The private key.
     */
    public static PrivateKey create(BigInteger key) {
      checkNotNull(key);
      return create(toBytes32(key.toByteArray()));
    }

    /**
     * Create the private key from encoded bytes.
     *
     * @param encoded The encoded key bytes.
     * @return The private key.
     */
    public static PrivateKey create(Bytes32 encoded) {
      return new PrivateKey(encoded);
    }

    /**
     * Load a private key from a file.
     *
     * @param file The file to read the key from.
     * @return The private key.
     * @throws IOException On a filesystem error.
     * @throws InvalidSEC256K1PrivateKeyStoreException If the file does not contain a valid key.
     */
    public static PrivateKey load(Path file) throws IOException, InvalidSEC256K1PrivateKeyStoreException {
      try {
        List<String> info = Files.readAllLines(file);
        if (info.size() != 1) {
          throw new InvalidSEC256K1PrivateKeyStoreException();
        }
        return SECP256K1.PrivateKey.create(Bytes32.fromHexString((info.get(0))));
      } catch (IllegalArgumentException ex) {
        throw new InvalidSEC256K1PrivateKeyStoreException();
      }
    }

    private PrivateKey(Bytes32 encoded) {
      checkNotNull(encoded);
      this.encoded = encoded;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof PrivateKey)) {
        return false;
      }

      PrivateKey that = (PrivateKey) other;
      return this.encoded.equals(that.encoded);
    }

    @Override
    public byte[] getEncoded() {
      return encoded.toArrayUnsafe();
    }

    /**
     * @return The encoded bytes of the key.
     */
    public Bytes32 encodedBytes() {
      return encoded;
    }

    @Override
    public String getAlgorithm() {
      return ALGORITHM;
    }

    @Override
    public String getFormat() {
      return null;
    }

    @Override
    public int hashCode() {
      return encoded.hashCode();
    }

    /**
     * Write the private key to a file.
     *
     * @param file The file to write to.
     * @throws IOException On a filesystem error.
     */
    public void store(Path file) throws IOException {
      atomicReplace(file, encoded.toString().getBytes(UTF_8));
    }

    @Override
    public String toString() {
      return encoded.toString();
    }
  }

  /**
   * A SECP256K1 public key.
   */
  public static class PublicKey implements java.security.PublicKey {

    private static final int BYTE_LENGTH = 64;

    private final Bytes encoded;

    /**
     * Create the public key from a private key.
     *
     * @param privateKey The private key.
     * @return The associated public key.
     */
    public static PublicKey create(PrivateKey privateKey) {
      BigInteger privKey = privateKey.encodedBytes().unsignedBigIntegerValue();

      /*
       * TODO: FixedPointCombMultiplier currently doesn't support scalars longer than the group
       * order, but that could change in future versions.
       */
      if (privKey.bitLength() > Parameters.CURVE.getN().bitLength()) {
        privKey = privKey.mod(Parameters.CURVE.getN());
      }

      ECPoint point = new FixedPointCombMultiplier().multiply(Parameters.CURVE.getG(), privKey);
      return PublicKey.create(Bytes.wrap(Arrays.copyOfRange(point.getEncoded(false), 1, 65)));
    }

    private static Bytes toBytes64(byte[] backing) {
      if (backing.length == BYTE_LENGTH) {
        return Bytes.wrap(backing);
      } else if (backing.length > BYTE_LENGTH) {
        return Bytes.wrap(backing, backing.length - BYTE_LENGTH, BYTE_LENGTH);
      } else {
        MutableBytes res = MutableBytes.create(BYTE_LENGTH);
        Bytes.wrap(backing).copyTo(res, BYTE_LENGTH - backing.length);
        return res;
      }
    }

    /**
     * Create the public key from a private key.
     *
     * @param privateKey The private key.
     * @return The associated public key.
     */
    public static PublicKey create(BigInteger privateKey) {
      checkNotNull(privateKey);
      return create(toBytes64(privateKey.toByteArray()));
    }

    /**
     * Create the public key from encoded bytes.
     *
     * @param encoded The encoded key bytes.
     * @return The public key.
     */
    public static PublicKey create(Bytes encoded) {
      return new PublicKey(encoded);
    }

    /**
     * Create a public key using a digital signature.
     *
     * @param data The signed data.
     * @param signature The digital signature.
     * @throws SECP256K1KeyRecoveryException If no signature can be recovered from the data.
     * @return The associated public key.
     */
    public static PublicKey recoverFromSignature(Bytes data, Signature signature) {
      Bytes32 dataHash = keccak256(data);
      int v = signature.v();
      v = v == 27 || v == 28 ? v - 27 : v;
      BigInteger publicKeyBI;
      try {
        publicKeyBI = SECP256K1.recoverFromSignature(v, signature.r(), signature.s(), dataHash);
      } catch (IllegalArgumentException e) {
        throw new SECP256K1KeyRecoveryException("Public key cannot be recovered: " + e.getMessage(), e);
      }
      return create(publicKeyBI);
    }

    private PublicKey(Bytes encoded) {
      checkNotNull(encoded);
      checkArgument(
          encoded.size() == BYTE_LENGTH,
          "Encoding must be %s bytes long, got %s",
          BYTE_LENGTH,
          encoded.size());
      this.encoded = encoded;
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof PublicKey)) {
        return false;
      }

      PublicKey that = (PublicKey) other;
      return this.encoded.equals(that.encoded);
    }

    @Override
    public byte[] getEncoded() {
      return encoded.toArrayUnsafe();
    }

    /**
     * @return The encoded bytes of the key.
     */
    public Bytes encodedBytes() {
      return encoded;
    }

    @Override
    public String getAlgorithm() {
      return ALGORITHM;
    }

    @Override
    public String getFormat() {
      return null;
    }

    @Override
    public int hashCode() {
      return encoded.hashCode();
    }

    @Override
    public String toString() {
      return encoded.toString();
    }
  }

  /**
   * A SECP256K1 key pair.
   */
  public static class KeyPair {

    private final PrivateKey privateKey;
    private final PublicKey publicKey;

    /**
     * Create a keypair from a private and public key.
     *
     * @param privateKey The private key.
     * @param publicKey The public key.
     * @return The key pair.
     */
    public static KeyPair create(PrivateKey privateKey, PublicKey publicKey) {
      return new KeyPair(privateKey, publicKey);
    }

    /**
     * Create a keypair using only a private key.
     *
     * @param privateKey The private key.
     * @return The key pair.
     */
    public static KeyPair create(PrivateKey privateKey) {
      return new KeyPair(privateKey, PublicKey.create(privateKey));
    }

    /**
     * Generate a new keypair.
     *
     * Entropy for the generation is drawn from {@link SecureRandom}.
     *
     * @return A new keypair.
     */
    public static KeyPair random() {
      java.security.KeyPair rawKeyPair = Parameters.KEY_PAIR_GENERATOR.generateKeyPair();
      BCECPrivateKey privateKey = (BCECPrivateKey) rawKeyPair.getPrivate();
      BCECPublicKey publicKey = (BCECPublicKey) rawKeyPair.getPublic();

      BigInteger privateKeyValue = privateKey.getD();

      // Ethereum does not use encoded public keys like bitcoin - see
      // https://en.bitcoin.it/wiki/Elliptic_Curve_Digital_Signature_Algorithm for details
      // Additionally, as the first bit is a constant prefix (0x04) we ignore this value
      byte[] publicKeyBytes = publicKey.getQ().getEncoded(false);
      BigInteger publicKeyValue = new BigInteger(1, Arrays.copyOfRange(publicKeyBytes, 1, publicKeyBytes.length));

      return new KeyPair(PrivateKey.create(privateKeyValue), PublicKey.create(publicKeyValue));
    }

    /**
     * Load a key pair from a path.
     *
     * @param file The file containing an encoded private key.
     * @return The key pair.
     * @throws IOException On a filesystem error.
     * @throws InvalidSEC256K1PrivateKeyStoreException If the file does not contain a valid key.
     */
    public static KeyPair load(Path file) throws IOException, InvalidSEC256K1PrivateKeyStoreException {
      return create(PrivateKey.load(file));
    }

    private KeyPair(PrivateKey privateKey, PublicKey publicKey) {
      checkNotNull(privateKey);
      checkNotNull(publicKey);
      this.privateKey = privateKey;
      this.publicKey = publicKey;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(privateKey, publicKey);
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof KeyPair)) {
        return false;
      }

      KeyPair that = (KeyPair) other;
      return this.privateKey.equals(that.privateKey) && this.publicKey.equals(that.publicKey);
    }

    /**
     * @return The private key.
     */
    public PrivateKey getPrivateKey() {
      return privateKey;
    }

    /**
     * @return The public key.
     */
    public PublicKey getPublicKey() {
      return publicKey;
    }

    /**
     * Write the key pair to a file.
     *
     * @param file The file to write to.
     * @throws IOException On a filesystem error.
     */
    public void store(Path file) throws IOException {
      privateKey.store(file);
    }
  }

  /**
   * A SECP256K1 digital signature.
   */
  public static class Signature {
    private byte v;
    private final BigInteger r;
    private final BigInteger s;

    /**
     * Create a signature from encoded bytes.
     *
     * @param encoded The encoded bytes.
     * @return The signature.
     */
    public static Signature create(Bytes encoded) {
      checkNotNull(encoded);
      checkArgument(encoded.size() == 65, "encoded must be 65 bytes, but got %s instead", encoded.size());
      BigInteger r = encoded.slice(0, 32).unsignedBigIntegerValue();
      BigInteger s = encoded.slice(32, 32).unsignedBigIntegerValue();
      return new Signature(r, s, encoded.get(64));
    }

    public static Signature create(byte v, BigInteger r, BigInteger s) {
      return new Signature(r, s, v);
    }

    Signature(BigInteger r, BigInteger s, byte v) {
      checkNotNull(r);
      checkNotNull(s);
      checkInBounds("r", r);
      checkInBounds("s", s);
      this.r = r;
      this.s = s;
      this.v = v;
    }

    private static void checkInBounds(String name, BigInteger value) {
      if (value.compareTo(BigInteger.ONE) < 0) {
        throw new IllegalArgumentException(String.format("Invalid '%s' value, should be >= 1 but got %s", name, value));
      }

      if (value.compareTo(Parameters.CURVE.getN()) >= 0) {
        throw new IllegalArgumentException(
            String.format("Invalid '%s' value, should be < %s but got %s", name, Parameters.CURVE.getN(), value));
      }
    }

    @Override
    public boolean equals(Object other) {
      if (!(other instanceof Signature)) {
        return false;
      }

      Signature that = (Signature) other;
      return this.r.equals(that.r) && this.s.equals(that.s) && this.v == that.v;
    }

    /**
     * @return The encoded bytes of the signature.
     */
    public Bytes encodedBytes() {
      MutableBytes encoded = MutableBytes.create(65);
      UInt256.valueOf(r).toBytes().copyTo(encoded, 0);
      UInt256.valueOf(s).toBytes().copyTo(encoded, 32);
      encoded.set(64, v == 27 || v == 28 ? (byte) (v - 27) : v);
      return encoded;
    }

    @Override
    public int hashCode() {
      return Objects.hashCode(r, s, v);
    }

    @Override
    public String toString() {
      return "Signature{" + "v=" + v + ", r=" + r + ", s=" + s + '}';
    }

    /**
     * @return The v-value of the signature.
     */
    public byte v() {
      return v;
    }

    /**
     * @return The r-value of the signature.
     */
    public BigInteger r() {
      return r;
    }

    /**
     * @return The s-value of the signature.
     */
    public BigInteger s() {
      return s;
    }
  }
}
