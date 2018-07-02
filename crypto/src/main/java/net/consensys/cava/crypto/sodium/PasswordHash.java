/*
 * Copyright 2018, ConsenSys Inc.
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
package net.consensys.cava.crypto.sodium;

import static java.nio.charset.StandardCharsets.UTF_8;

import net.consensys.cava.bytes.Bytes;

import jnr.ffi.Pointer;

// Documentation copied under the ISC License, from
// https://github.com/jedisct1/libsodium-doc/blob/424b7480562c2e063bc8c52c452ef891621c8480/password_hashing/the_argon2i_function.md

/**
 * The Argon2 memory-hard hashing function.
 *
 * <p>
 * Argon2 summarizes the state of the art in the design of memory-hard functions.
 *
 * <p>
 * It aims at the highest memory filling rate and effective use of multiple computing units, while still providing
 * defense against tradeoff attacks.
 *
 * <p>
 * It prevents ASICs from having a significant advantage over software implementations.
 *
 * <h3>Guidelines for choosing the parameters</h3>
 *
 * <p>
 * Start by determining how much memory the function can use. What will be the highest number of threads/processes
 * evaluating the function simultaneously (ideally, no more than 1 per CPU core)? How much physical memory is guaranteed
 * to be available?
 *
 * <p>
 * Set memlimit to the amount of memory you want to reserve for password hashing.
 *
 * <p>
 * Then, set opslimit to 3 and measure the time it takes to hash a password.
 *
 * <p>
 * If this it is way too long for your application, reduce memlimit, but keep opslimit set to 3.
 *
 * <p>
 * If the function is so fast that you can afford it to be more computationally intensive without any usability issues,
 * increase opslimit.
 *
 * <p>
 * For online use (e.g. login in on a website), a 1 second computation is likely to be the acceptable maximum.
 *
 * <p>
 * For interactive use (e.g. a desktop application), a 5 second pause after having entered a password is acceptable if
 * the password doesn't need to be entered more than once per session.
 *
 * <p>
 * For non-interactive use and infrequent use (e.g. restoring an encrypted backup), an even slower computation can be an
 * option.
 *
 * <p>
 * This class depends upon the JNR-FFI library being available on the classpath, along with its dependencies. See
 * https://github.com/jnr/jnr-ffi. JNR-FFI can be included using the gradle dependency 'com.github.jnr:jnr-ffi'.
 */
public final class PasswordHash {

  /**
   * A PasswordHash salt.
   */
  public static final class Salt {
    private final Pointer ptr;

    private Salt(Pointer ptr) {
      this.ptr = ptr;
    }

    @Override
    protected void finalize() {
      Sodium.sodium_free(ptr);
    }

    /**
     * Create a {@link Salt} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Salt fromBytes(Bytes bytes) {
      return fromBytes(bytes.toArrayUnsafe());
    }

    /**
     * Create a {@link Salt} from an array of bytes.
     *
     * <p>
     * The byte array must be of length {@link #length()}.
     *
     * @param bytes The bytes for the seed.
     * @return A seed.
     */
    public static Salt fromBytes(byte[] bytes) {
      if (bytes.length != Sodium.crypto_pwhash_saltbytes()) {
        throw new IllegalArgumentException(
            "key must be " + Sodium.crypto_pwhash_saltbytes() + " bytes, got " + bytes.length);
      }
      return Sodium.dup(bytes, Salt::new);
    }

    /**
     * Obtain the length of the salt in bytes (32).
     *
     * @return The length of the salt in bytes (32).
     */
    public static int length() {
      long saltbytes = Sodium.crypto_pwhash_saltbytes();
      if (saltbytes > Integer.MAX_VALUE) {
        throw new SodiumException("crypto_pwhash_saltbytes: " + saltbytes + " is too large");
      }
      return (int) saltbytes;
    }

    /**
     * Generate a new salt using a random generator.
     *
     * @return A randomly generated salt.
     */
    public static Salt random() {
      return Sodium.randomBytes(length(), Salt::new);
    }

    /**
     * @return The bytes of this salt.
     */
    public Bytes bytes() {
      return Bytes.wrap(bytesArray());
    }

    /**
     * @return The bytes of this salt.
     */
    public byte[] bytesArray() {
      return Sodium.reify(ptr, length());
    }
  }

  /**
   * A PasswordHash algorithm.
   */
  public static final class Algorithm {

    private static Algorithm ARGON2I13;
    private static Algorithm ARGON2ID13;
    private static Algorithm RECOMMENDED;

    static {
      ARGON2I13 = new Algorithm("argon2i13", Sodium.crypto_pwhash_alg_argon2i13(), 3);
      ARGON2ID13 = (Sodium.supportsVersion(Sodium.VERSION_10_0_13))
          ? new Algorithm("argon2id13", Sodium.crypto_pwhash_alg_argon2id13(), 1)
          : null;
      if (Sodium.crypto_pwhash_alg_default() == ARGON2I13.id) {
        RECOMMENDED = ARGON2I13;
      } else if (ARGON2ID13 != null && Sodium.crypto_pwhash_alg_default() == ARGON2ID13.id) {
        RECOMMENDED = ARGON2ID13;
      } else {
        throw new IllegalStateException("Unknown value from crypto_pwhash_alg_default");
      }
    }

    private final String name;
    private final int id;
    private final long minOps;

    private Algorithm(String name, int id, long minOps) {
      this.name = name;
      this.id = id;
      this.minOps = minOps;
    }

    /**
     * @return The currently recommended algorithm.
     */
    public static Algorithm recommended() {
      return RECOMMENDED;
    }

    /**
     * @return Version 1.3 of the Argon2i algorithm.
     */
    public static Algorithm argon2i13() {
      return ARGON2I13;
    }

    /**
     * @return Version 1.3 of the Argon2id algorithm.
     */
    public static Algorithm argon2id13() {
      if (ARGON2ID13 == null) {
        throw new UnsupportedOperationException("Sodium argon2id13 algorithm is not available");
      }
      return ARGON2ID13;
    }

    public String name() {
      return name;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Algorithm)) {
        return false;
      }
      Algorithm other = (Algorithm) obj;
      return this.id == other.id;
    }

    @Override
    public int hashCode() {
      return Integer.hashCode(id);
    }

    @Override
    public String toString() {
      return name;
    }
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hash(String password, int length, Salt salt) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hash(Bytes password, int length, Salt salt) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static byte[] hash(byte[] password, int length, Salt salt) {
    return hash(password, length, salt, moderateOpsLimit(), moderateMemLimit(), Algorithm.recommended());
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hash(String password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hash(Bytes password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for most use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static byte[] hash(byte[] password, int length, Salt salt, Algorithm algorithm) {
    return hash(password, length, salt, moderateOpsLimit(), moderateMemLimit(), algorithm);
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hashInteractive(String password, int length, Salt salt) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, Algorithm.recommended()));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hashInteractive(Bytes password, int length, Salt salt) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, Algorithm.recommended()));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static byte[] hashInteractive(byte[] password, int length, Salt salt) {
    return hash(password, length, salt, interactiveOpsLimit(), interactiveMemLimit(), Algorithm.recommended());
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hashInteractive(String password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hashInteractive(Bytes password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for interactive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static byte[] hashInteractive(byte[] password, int length, Salt salt, Algorithm algorithm) {
    return hash(password, length, salt, interactiveOpsLimit(), interactiveMemLimit(), algorithm);
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hashSensitive(String password, int length, Salt salt) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, Algorithm.recommended()));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static Bytes hashSensitive(Bytes password, int length, Salt salt) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, Algorithm.recommended()));
  }

  /**
   * Compute a key from a password, using the currently recommended algorithm and limits on operations and memory that
   * are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @return The derived key.
   */
  public static byte[] hashSensitive(byte[] password, int length, Salt salt) {
    return hash(password, length, salt, sensitiveOpsLimit(), sensitiveMemLimit(), Algorithm.recommended());
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hashSensitive(String password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hashSensitive(Bytes password, int length, Salt salt, Algorithm algorithm) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, algorithm));
  }

  /**
   * Compute a key from a password, using limits on operations and memory that are suitable for sensitive use-cases.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static byte[] hashSensitive(byte[] password, int length, Salt salt, Algorithm algorithm) {
    return hash(password, length, salt, sensitiveOpsLimit(), sensitiveMemLimit(), algorithm);
  }

  /**
   * Compute a key from a password.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hash(String password, int length, Salt salt, long opsLimit, long memLimit, Algorithm algorithm) {
    return Bytes.wrap(hash(password.getBytes(UTF_8), length, salt, opsLimit, memLimit, algorithm));
  }

  /**
   * Compute a key from a password.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static Bytes hash(Bytes password, int length, Salt salt, long opsLimit, long memLimit, Algorithm algorithm) {
    return Bytes.wrap(hash(password.toArrayUnsafe(), length, salt, opsLimit, memLimit, algorithm));
  }

  /**
   * Compute a key from a password.
   *
   * @param password The password to hash.
   * @param length The key length to generate.
   * @param salt A salt.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @param algorithm The algorithm to use.
   * @return The derived key.
   */
  public static byte[] hash(byte[] password, int length, Salt salt, long opsLimit, long memLimit, Algorithm algorithm) {
    assertHashLength(length);
    assertOpsLimit(opsLimit);
    assertMemLimit(memLimit);
    if (opsLimit < algorithm.minOps) {
      throw new IllegalArgumentException("opsLimit " + opsLimit + " too low for specified algorithm");
    }
    byte[] out = new byte[length];

    int rc = Sodium.crypto_pwhash(out, length, password, password.length, salt.ptr, opsLimit, memLimit, algorithm.id);
    if (rc != 0) {
      throw new SodiumException("crypto_pwhash: failed with result " + rc);
    }
    return out;
  }

  /**
   * @return The minimum hash length (16).
   */
  public static int minHashLength() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return 16;
    }
    long len = Sodium.crypto_pwhash_bytes_min();
    if (len > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_pwhash_bytes_min: " + len + " is too large");
    }
    return (int) len;
  }

  /**
   * @return The maximum hash length.
   */
  public static int maxHashLength() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return Integer.MAX_VALUE;
    }
    long len = Sodium.crypto_pwhash_bytes_max();
    if (len > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) len;
  }

  private static void assertHashLength(int length) {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      if (length < 16) {
        throw new IllegalArgumentException("length out of range");
      }
      return;
    }
    if (length < Sodium.crypto_pwhash_bytes_min() || length > Sodium.crypto_pwhash_bytes_max()) {
      throw new IllegalArgumentException("length out of range");
    }
  }

  /**
   * Compute a hash from a password, using limits on operations and memory that are suitable for most use-cases.
   *
   * <p>
   * Equivalent to {@code hash(password, moderateOpsLimit(), moderateMemLimit())}.
   *
   * @param password The password to hash.
   * @return The hash string.
   */
  public static String hash(String password) {
    return hash(password, moderateOpsLimit(), moderateMemLimit());
  }

  /**
   * Compute a hash from a password, using limits on operations and memory that are suitable for interactive use-cases.
   *
   * <p>
   * Equivalent to {@code hash(password, sensitiveOpsLimit(), sensitiveMemLimit())}.
   *
   * @param password The password to hash.
   * @return The hash string.
   */
  public static String hashInteractive(String password) {
    return hash(password, interactiveOpsLimit(), interactiveMemLimit());
  }

  /**
   * Compute a hash from a password, using limits on operations and memory that are suitable for sensitive use-cases.
   *
   * <p>
   * Equivalent to {@code hash(password, sensitiveOpsLimit(), sensitiveMemLimit())}.
   *
   * @param password The password to hash.
   * @return The hash string.
   */
  public static String hashSensitive(String password) {
    return hash(password, sensitiveOpsLimit(), sensitiveMemLimit());
  }

  /**
   * Compute a hash from a password.
   *
   * @param password The password to hash.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @return The hash string.
   */
  public static String hash(String password, long opsLimit, long memLimit) {
    assertOpsLimit(opsLimit);
    assertMemLimit(memLimit);

    byte[] out = new byte[hashStringLength()];

    byte[] pwbytes = password.getBytes(UTF_8);
    int rc = Sodium.crypto_pwhash_str(out, pwbytes, pwbytes.length, opsLimit, memLimit);
    if (rc != 0) {
      throw new SodiumException("crypto_pwhash_str: failed with result " + rc);
    }

    int i = 0;
    while (i < out.length && out[i] != 0) {
      ++i;
    }
    return new String(out, 0, i, UTF_8);
  }

  /**
   * A hash verification result.
   */
  public enum VerificationResult {
    /** The hash verification failed. */
    FAILED,
    /** The hash verification passed. */
    PASSED,
    /**
     * The hash verification passed, but the hash is out-of-date and should be regenerated.
     *
     * <p>
     * Note: this is only supported by the sodium native library version &gt;= 10.0.14.
     */
    NEEDS_REHASH;

    /**
     * @return <tt>true</tt> if the verification passed.
     */
    public boolean passed() {
      return this != FAILED;
    }

    /**
     * @return <tt>true</tt> if the hash should be regenerated.
     */
    public boolean needsRehash() {
      return this == NEEDS_REHASH;
    }
  }

  /**
   * Verify a password against a hash using limits on operations and memory that are suitable for most use-cases.
   *
   * <p>
   * Equivalent to {@code verify(hash, password, moderateOpsLimit(), moderateMemLimit())}.
   *
   * @param hash The hash.
   * @param password The password to verify.
   * @return The result of verification.
   */
  public static VerificationResult verify(String hash, String password) {
    return verify(hash, password, moderateOpsLimit(), moderateMemLimit());
  }

  /**
   * Verify a password against a hash using limits on operations and memory that are suitable for interactive use-cases.
   *
   * <p>
   * Equivalent to {@code verify(hash, password, interactiveOpsLimit(), interactiveMemLimit())}.
   *
   * @param hash The hash.
   * @param password The password to verify.
   * @return The result of verification.
   */
  public static VerificationResult verifyInteractive(String hash, String password) {
    return verify(hash, password, interactiveOpsLimit(), interactiveMemLimit());
  }

  /**
   * Verify a password against a hash using limits on operations and memory that are suitable for sensitive use-cases.
   *
   * <p>
   * Equivalent to {@code verify(hash, password, sensitiveOpsLimit(), sensitiveMemLimit())}.
   *
   * @param hash The hash.
   * @param password The password to verify.
   * @return The result of verification.
   */
  public static VerificationResult verifySensitive(String hash, String password) {
    return verify(hash, password, sensitiveOpsLimit(), sensitiveMemLimit());
  }

  /**
   * Verify a password against a hash.
   *
   * @param hash The hash.
   * @param password The password to verify.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @return The result of verification.
   */
  public static VerificationResult verify(String hash, String password, long opsLimit, long memLimit) {
    assertOpsLimit(opsLimit);
    assertMemLimit(memLimit);

    byte[] hashBytes = hash.getBytes(UTF_8);

    int strbytes = hashStringLength();
    if (hashBytes.length >= strbytes) {
      throw new IllegalArgumentException("hash is too long");
    }

    Pointer str = Sodium.malloc(strbytes);
    try {
      str.put(0, hashBytes, 0, hashBytes.length);
      str.putByte(hashBytes.length, (byte) 0);

      byte[] pwbytes = password.getBytes(UTF_8);
      int rc = Sodium.crypto_pwhash_str_verify(str, pwbytes, pwbytes.length);
      if (rc != 0) {
        return VerificationResult.FAILED;
      }

      if (!Sodium.supportsVersion(Sodium.VERSION_10_0_14)) {
        return VerificationResult.PASSED;
      }

      rc = Sodium.crypto_pwhash_str_needs_rehash(str, opsLimit, memLimit);
      if (rc < 0) {
        throw new SodiumException("crypto_pwhash_str_needs_rehash: failed with result " + rc);
      }
      return (rc == 0) ? VerificationResult.PASSED : VerificationResult.NEEDS_REHASH;
    } finally {
      Sodium.sodium_free(str);
    }
  }

  /**
   * Verify a password against a hash.
   *
   * @param hash The hash.
   * @param password The password to verify.
   * @return <tt>true</tt> if the password matches the hash.
   */
  public static boolean verifyOnly(String hash, String password) {
    byte[] hashBytes = hash.getBytes(UTF_8);

    int strbytes = hashStringLength();
    if (hashBytes.length >= strbytes) {
      throw new IllegalArgumentException("hash is too long");
    }

    Pointer str = Sodium.malloc(strbytes);
    try {
      str.put(0, hashBytes, 0, hashBytes.length);
      str.putByte(hashBytes.length, (byte) 0);

      byte[] pwbytes = password.getBytes(UTF_8);
      int rc = Sodium.crypto_pwhash_str_verify(str, pwbytes, pwbytes.length);
      return (rc == 0);
    } finally {
      Sodium.sodium_free(str);
    }
  }

  /**
   * Check if a hash needs to be regenerated using limits on operations and memory that are suitable for most use-cases.
   *
   * <p>
   * Equivalent to {@code needsRehash(hash, moderateOpsLimit(), moderateMemLimit())}.
   *
   * @param hash The hash.
   * @return <tt>true</tt> if the hash should be regenerated.
   */
  public static boolean needsRehash(String hash) {
    return needsRehash(hash, moderateOpsLimit(), moderateMemLimit());
  }

  /**
   * Check if a hash needs to be regenerated using limits on operations and memory that are suitable for interactive
   * use-cases.
   *
   * <p>
   * Equivalent to {@code needsRehash(hash, interactiveOpsLimit(), interactiveMemLimit())}.
   *
   * @param hash The hash.
   * @return <tt>true</tt> if the hash should be regenerated.
   */
  public static boolean needsRehashInteractive(String hash) {
    return needsRehash(hash, interactiveOpsLimit(), interactiveMemLimit());
  }

  /**
   * Check if a hash needs to be regenerated using limits on operations and memory that are suitable for sensitive
   * use-cases.
   *
   * <p>
   * Equivalent to {@code needsRehash(hash, sensitiveOpsLimit(), sensitiveMemLimit())}.
   *
   * @param hash The hash.
   * @return <tt>true</tt> if the hash should be regenerated.
   */
  public static boolean needsRehashSensitive(String hash) {
    return needsRehash(hash, sensitiveOpsLimit(), sensitiveMemLimit());
  }

  /**
   * Check if a hash needs to be regenerated.
   *
   * <p>
   * Check if a hash matches the parameters opslimit and memlimit, and the current default algorithm.
   *
   * @param hash The hash.
   * @param opsLimit The operations limit, which must be in the range {@link #minOpsLimit()} to {@link #maxOpsLimit()}.
   * @param memLimit The memory limit, which must be in the range {@link #minMemLimit()} to {@link #maxMemLimit()}.
   * @return <tt>true</tt> if the hash should be regenerated.
   */
  public static boolean needsRehash(String hash, long opsLimit, long memLimit) {
    assertOpsLimit(opsLimit);
    assertMemLimit(memLimit);

    byte[] hashBytes = hash.getBytes(UTF_8);

    int strbytes = hashStringLength();
    if (hashBytes.length >= strbytes) {
      throw new IllegalArgumentException("hash is too long");
    }

    Pointer str = Sodium.malloc(strbytes);
    try {
      str.put(0, hashBytes, 0, hashBytes.length);
      str.putByte(hashBytes.length, (byte) 0);

      int rc = Sodium.crypto_pwhash_str_needs_rehash(str, opsLimit, memLimit);
      if (rc < 0) {
        throw new SodiumException("crypto_pwhash_str_needs_rehash: failed with result " + rc);
      }
      return (rc == 0);
    } finally {
      Sodium.sodium_free(str);
    }
  }

  private static int hashStringLength() {
    long strbytes = Sodium.crypto_pwhash_strbytes();
    if (strbytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("crypto_pwhash_strbytes: " + strbytes + " is too large");
    }
    return (int) strbytes;
  }

  /**
   * @return The minimum operations limit (1).
   */
  public static long minOpsLimit() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return 3;
    }
    return Sodium.crypto_pwhash_opslimit_min();
  }

  /**
   * @return An operations limit suitable for interactive use-cases (2).
   */
  public static long interactiveOpsLimit() {
    return Sodium.crypto_pwhash_opslimit_interactive();
  }

  /**
   * @return An operations limit suitable for most use-cases (3).
   */
  public static long moderateOpsLimit() {
    return Sodium.crypto_pwhash_opslimit_moderate();
  }

  /**
   * @return An operations limit for sensitive use-cases (4).
   */
  public static long sensitiveOpsLimit() {
    return Sodium.crypto_pwhash_opslimit_sensitive();
  }

  /**
   * @return The maximum operations limit (4294967295).
   */
  public static long maxOpsLimit() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return 4294967295L;
    }
    return Sodium.crypto_pwhash_opslimit_max();
  }

  private static void assertOpsLimit(long opsLimit) {
    if (opsLimit < minOpsLimit() || opsLimit > maxOpsLimit()) {
      throw new IllegalArgumentException("opsLimit out of range");
    }
  }

  /**
   * @return The minimum memory limit (8192).
   */
  public static long minMemLimit() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return 8192;
    }
    return Sodium.crypto_pwhash_memlimit_min();
  }

  /**
   * @return A memory limit suitable for interactive use-cases (67108864).
   */
  public static long interactiveMemLimit() {
    return Sodium.crypto_pwhash_memlimit_interactive();
  }

  /**
   * @return A memory limit suitable for most use-cases (268435456).
   */
  public static long moderateMemLimit() {
    return Sodium.crypto_pwhash_memlimit_moderate();
  }

  /**
   * @return A memory limit suitable for sensitive use-cases (1073741824).
   */
  public static long sensitiveMemLimit() {
    return Sodium.crypto_pwhash_memlimit_sensitive();
  }

  /**
   * @return The maximum memory limit (4398046510080).
   */
  public static long maxMemLimit() {
    // When support for 10.0.11 is dropped, remove this
    if (!Sodium.supportsVersion(Sodium.VERSION_10_0_12)) {
      return 4398046510080L;
    }
    return Sodium.crypto_pwhash_memlimit_max();
  }

  private static void assertMemLimit(long memLimit) {
    if (memLimit < minMemLimit() || memLimit > maxMemLimit()) {
      throw new IllegalArgumentException("memLimit out of range");
    }
  }
}
