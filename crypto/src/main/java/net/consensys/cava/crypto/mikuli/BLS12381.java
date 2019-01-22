/*
 * Copyright 2019 ConsenSys AG.
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
package net.consensys.cava.crypto.mikuli;

import net.consensys.cava.bytes.Bytes;

import java.util.List;

import org.apache.milagro.amcl.BLS381.BIG;
import org.apache.milagro.amcl.BLS381.ECP;
import org.apache.milagro.amcl.BLS381.ECP2;
import org.apache.milagro.amcl.BLS381.MPIN;

/*
 * Adapted from the ConsenSys/mikuli (Apache 2 License) implementation:
 * https://github.com/ConsenSys/mikuli/blob/master/src/main/java/net/consensys/mikuli/crypto/*.java
 */

/**
 * This Boneh-Lynn-Shacham (BLS) signature implementation is constructed from a pairing friendly elliptic curve, the
 * BLS12-381 curve. It uses parameters as defined in https://z.cash/blog/new-snark-curve and the points in groups G1 and
 * G2 are defined https://github.com/zkcrypto/pairing/blob/master/src/bls12_381/README.md
 * 
 * <p>
 * This class depends upon the Apache Milagro library being available. See https://milagro.apache.org.
 *
 * <p>
 * Apache Milagro can be included using the gradle dependency 'org.miracl.milagro.amcl:milagro-crypto-java'.
 */
public final class BLS12381 {

  private BLS12381() {}

  /**
   * Generates a SignatureAndPublicKey.
   *
   * @param keyPair The public and private key pair, not null
   * @param message The message to sign, not null
   * @return The SignatureAndPublicKey, not null
   */
  public static SignatureAndPublicKey sign(KeyPair keyPair, byte[] message) {
    G2Point hashInGroup2 = hashFunction(message);
    /*
     * The signature is hash point in G2 multiplied by the private key.
     */
    G2Point sig = keyPair.privateKey().sign(hashInGroup2);
    return new SignatureAndPublicKey(new Signature(sig), keyPair.publicKey());
  }

  /**
   * Generates a SignatureAndPublicKey.
   *
   * @param keyPair The public and private key pair, not null
   * @param message The message to sign, not null
   * @return The SignatureAndPublicKey, not null
   */
  public static SignatureAndPublicKey sign(KeyPair keyPair, Bytes message) {
    return sign(keyPair, message.toArray());
  }

  /**
   * Verifies the given BLS signature against the message bytes using the public key.
   * 
   * @param publicKey The public key, not null
   * @param signature The signature, not null
   * @param message The message data to verify, not null
   * 
   * @return True if the verification is successful.
   */
  public static boolean verify(PublicKey publicKey, Signature signature, byte[] message) {
    G1Point g1Generator = KeyPair.g1Generator;

    G2Point hashInGroup2 = hashFunction(message);
    GTPoint e1 = AtePairing.pair(publicKey.g1Point(), hashInGroup2);
    GTPoint e2 = AtePairing.pair(g1Generator, signature.g2Point());

    return e1.equals(e2);
  }

  /**
   * Verifies the given BLS signature against the message bytes using the public key.
   * 
   * @param publicKey The public key, not null
   * @param signature The signature, not null
   * @param message The message data to verify, not null
   * 
   * @return True if the verification is successful.
   */
  public static boolean verify(PublicKey publicKey, Signature signature, Bytes message) {
    return verify(publicKey, signature, message.toArray());
  }

  /**
   * Verifies the given BLS signature against the message bytes using the public key.
   * 
   * @param sigAndPubKey The signature and public key, not null
   * @param message The message data to verify, not null
   * 
   * @return True if the verification is successful, not null
   */
  public static boolean verify(SignatureAndPublicKey sigAndPubKey, byte[] message) {
    return verify(sigAndPubKey.publicKey(), sigAndPubKey.signature(), message);
  }

  /**
   * Verifies the given BLS signature against the message bytes using the public key.
   * 
   * @param sigAndPubKey The public key, not null
   * @param message The message data to verify, not null
   * 
   * @return True if the verification is successful.
   */
  public static boolean verify(SignatureAndPublicKey sigAndPubKey, Bytes message) {
    return verify(sigAndPubKey.publicKey(), sigAndPubKey.signature(), message);
  }

  /**
   * Aggregates list of Signature and PublicKey pairs
   * 
   * @param sigAndPubKeyList The list of Signatures and corresponding Public keys to aggregate, not null
   * @return SignatureAndPublicKey, not null
   * @throws IllegalArgumentException if parameter list is empty
   */
  public static SignatureAndPublicKey aggregate(List<SignatureAndPublicKey> sigAndPubKeyList) {
    listNotEmpty(sigAndPubKeyList);
    return sigAndPubKeyList.stream().reduce((a, b) -> a.combine(b)).get();
  }

  /**
   * Aggregates list of PublicKey pairs
   * 
   * @param publicKeyList The list of public keys to aggregate, not null
   * @return PublicKey The public key, not null
   * @throws IllegalArgumentException if parameter list is empty
   */
  public static PublicKey aggregatePublicKey(List<PublicKey> publicKeyList) {
    listNotEmpty(publicKeyList);
    return publicKeyList.stream().reduce((a, b) -> a.combine(b)).get();
  }

  /**
   * Aggregates list of Signature pairs
   * 
   * @param signatureList The list of signatures to aggregate, not null
   * @throws IllegalArgumentException if parameter list is empty
   * @return Signature, not null
   */
  public static Signature aggregateSignatures(List<Signature> signatureList) {
    listNotEmpty(signatureList);
    return signatureList.stream().reduce((a, b) -> a.combine(b)).get();
  }

  private static void listNotEmpty(List<?> list) {
    if (list.isEmpty()) {
      throw new IllegalArgumentException("Parameter list is empty");
    }
  }

  private static G2Point hashFunction(byte[] message) {
    byte[] hashByte = MPIN.HASH_ID(ECP.SHA256, message, BIG.MODBYTES);
    return new G2Point(ECP2.mapit(hashByte));
  }
}
