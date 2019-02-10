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
package net.consensys.cava.scuttlebutt.handshake;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.crypto.Hash;
import net.consensys.cava.crypto.sodium.Box;
import net.consensys.cava.crypto.sodium.DiffieHelman;
import net.consensys.cava.crypto.sodium.MessageAuthenticationCodes;
import net.consensys.cava.crypto.sodium.SecretBox;
import net.consensys.cava.crypto.sodium.Signature;

/**
 * Class responsible for performing a Secure Scuttlebutt handshake with a remote peer, as defined in the
 * <a href="https://ssbc.github.io/scuttlebutt-protocol-guide/">Secure Scuttlebutt protocol guide</a>
 * <p>
 * Please note that only handshakes over the Ed25519 curve are supported.
 * <p>
 * This class manages the state of one handshake. It should not be reused across handshakes.
 *
 * If the handshake fails, a HandshakeException will be thrown.
 */
public final class SecureScuttlebuttHandshakeServer {

  private final Signature.KeyPair longTermKeyPair;
  private final Box.KeyPair ephemeralKeyPair;
  private final Bytes32 networkIdentifier;
  private Box.PublicKey clientEphemeralPublicKey;
  private Bytes sharedSecret;
  private Bytes sharedSecret2;
  private Signature.PublicKey clientLongTermPublicKey;
  private Bytes sharedSecret3;
  private Bytes detachedSignature;


  /**
   * Creates a new handshake server able to reply to the request of one client
   *
   * @param ourKeyPair the server long term key pair
   * @param networkIdentifier the network identifier
   * @return the handshake server
   */
  public static SecureScuttlebuttHandshakeServer create(Signature.KeyPair ourKeyPair, Bytes32 networkIdentifier) {
    return new SecureScuttlebuttHandshakeServer(ourKeyPair, networkIdentifier);
  }

  private SecureScuttlebuttHandshakeServer(Signature.KeyPair longTermKeyPair, Bytes32 networkIdentifier) {
    this.longTermKeyPair = longTermKeyPair;
    this.ephemeralKeyPair = Box.KeyPair.random();
    this.networkIdentifier = networkIdentifier;
  }

  /**
   * Creates a hello message to be sent to the other party, comprised of our ephemeral public key and an authenticator
   * against our network identifier.
   * 
   * @return the message
   */
  public Bytes createHello() {
    Bytes hmac =
        MessageAuthenticationCodes.HMACSHA512256.authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier);
    return Bytes.concatenate(hmac, ephemeralKeyPair.publicKey().bytes());
  }

  /**
   * Validates the initial message's MAC with our network identifier, and returns the peer ephemeral public key.
   *
   * @param message initial handshake message
   */
  public void readHello(Bytes message) {
    if (message.size() != 64) {
      throw new HandshakeException("Invalid handshake message length: " + message.size());
    }
    Bytes hmac = message.slice(0, 32);
    Bytes key = message.slice(32, 32);
    if (!MessageAuthenticationCodes.HMACSHA512256.verify(hmac, key, networkIdentifier)) {
      throw new HandshakeException("MAC does not match our network identifier");
    }
    this.clientEphemeralPublicKey = Box.PublicKey.fromBytes(key);
    computeSharedSecrets();
  }

  void computeSharedSecrets() {
    sharedSecret = DiffieHelman.scalarMultiply(ephemeralKeyPair.secretKey(), clientEphemeralPublicKey);
    sharedSecret2 = DiffieHelman
        .scalarMultiply(Box.SecretKey.forSignatureSecretKey(longTermKeyPair.secretKey()), clientEphemeralPublicKey);
  }

  Bytes sharedSecret() {
    return sharedSecret;
  }

  Bytes sharedSecret2() {
    return sharedSecret2;
  }

  Bytes sharedSecret3() {
    return sharedSecret3;
  }

  Signature.PublicKey clientLongTermPublicKey() {
    return clientLongTermPublicKey;
  }

  /**
   * Reads the message containing the identity of the client, verifying it matches our shared secrets.
   *
   * @param message the message containing the identity of the client
   */
  public void readIdentityMessage(Bytes message) {
    Bytes plaintext = SecretBox.decrypt(
        message,
        SecretBox.Key.fromBytes(Hash.sha2_256(Bytes.concatenate(networkIdentifier, sharedSecret, sharedSecret2))),
        SecretBox.Nonce.fromBytes(new byte[24]));

    if (plaintext == null) {
      throw new HandshakeException("Could not decrypt the plaintext with our shared secrets");
    }

    if (plaintext.size() != 96) {
      throw new HandshakeException("Identity message should be 96 bytes long, was " + plaintext.size());
    }

    detachedSignature = plaintext.slice(0, 64);
    clientLongTermPublicKey = Signature.PublicKey.fromBytes(plaintext.slice(64, 32));

    boolean verified = clientLongTermPublicKey.verify(
        Bytes.concatenate(networkIdentifier, longTermKeyPair.publicKey().bytes(), Hash.sha2_256(sharedSecret)),
        detachedSignature);
    if (!verified) {
      throw new HandshakeException("Identity message signature does not match");
    }
    sharedSecret3 = DiffieHelman
        .scalarMultiply(ephemeralKeyPair.secretKey(), Box.PublicKey.forSignaturePublicKey(clientLongTermPublicKey));
  }

  /**
   * Produces a message to accept the handshake with the client
   *
   * @return a message to accept the handshake
   */
  public Bytes createAcceptMessage() {
    Bytes signature = Signature.signDetached(
        Bytes.concatenate(
            networkIdentifier,
            detachedSignature,
            clientLongTermPublicKey.bytes(),
            Hash.sha2_256(sharedSecret)),
        longTermKeyPair.secretKey());

    return SecretBox.encrypt(
        signature,
        SecretBox.Key
            .fromBytes(Hash.sha2_256(Bytes.concatenate(networkIdentifier, sharedSecret, sharedSecret2, sharedSecret3))),
        SecretBox.Nonce.fromBytes(new byte[24]));
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to send messages to the server
   * going forward.
   *
   * @return a new secret box key for use with encrypting messages to the server.
   */
  Bytes clientToServerSecretBoxKey() {
    return Hash.sha2_256(
        Bytes.concatenate(
            Hash.sha2_256(
                Hash.sha2_256(Bytes.concatenate(networkIdentifier, sharedSecret, sharedSecret2, sharedSecret3))),
            longTermKeyPair.publicKey().bytes()));
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to send messages to the
   * server going forward.
   *
   * @return a clientToServerNonce for use with encrypting messages to the server.
   */
  Bytes clientToServerNonce() {
    return MessageAuthenticationCodes.HMACSHA512256
        .authenticate(ephemeralKeyPair.publicKey().bytes(), networkIdentifier)
        .slice(0, 24);
  }

  /**
   * If the handshake completed successfully, this provides the secret box key to use to receive messages from the
   * server going forward.
   *
   * @return a new secret box key for use with decrypting messages from the server.
   */
  Bytes serverToClientSecretBoxKey() {
    return Hash.sha2_256(
        Bytes.concatenate(
            Hash.sha2_256(
                Hash.sha2_256(Bytes.concatenate(networkIdentifier, sharedSecret, sharedSecret2, sharedSecret3))),
            clientLongTermPublicKey.bytes()));
  }

  /**
   * If the handshake completed successfully, this provides the clientToServerNonce to use to receive messages from the
   * server going forward.
   *
   * @return a clientToServerNonce for use with decrypting messages from the server.
   */
  Bytes serverToClientNonce() {
    return MessageAuthenticationCodes.HMACSHA512256
        .authenticate(clientLongTermPublicKey.bytes(), networkIdentifier)
        .slice(0, 24);
  }

  /**
   * Creates a stream to allow communication with the other peer after the handshake has completed
   * 
   * @return a new stream for encrypted communications with the peer
   */
  public SecureScuttlebuttStreamServer createStream() {
    return new SecureScuttlebuttStream(
        clientToServerSecretBoxKey(),
        clientToServerNonce(),
        serverToClientSecretBoxKey(),
        serverToClientNonce());
  }
}
