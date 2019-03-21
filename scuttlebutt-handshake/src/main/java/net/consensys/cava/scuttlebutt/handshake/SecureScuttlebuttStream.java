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
import net.consensys.cava.bytes.MutableBytes;
import net.consensys.cava.crypto.sodium.SHA256Hash;
import net.consensys.cava.crypto.sodium.SecretBox;

import java.util.ArrayList;
import java.util.List;

final class SecureScuttlebuttStream implements SecureScuttlebuttStreamClient, SecureScuttlebuttStreamServer {

  private final SecretBox.Key clientToServerKey;
  private final MutableBytes clientToServerNonce;
  private final SecretBox.Key serverToClientKey;
  private final MutableBytes serverToClientNonce;

  SecureScuttlebuttStream(
      SHA256Hash.Hash clientToServerKey,
      Bytes clientToServerNonce,
      SHA256Hash.Hash serverToClientKey,
      Bytes serverToClientNonce) {
    this.clientToServerKey = SecretBox.Key.fromHash(clientToServerKey);
    this.serverToClientKey = SecretBox.Key.fromHash(serverToClientKey);
    this.clientToServerNonce = clientToServerNonce.mutableCopy();
    this.serverToClientNonce = serverToClientNonce.mutableCopy();
  }

  @Override
  public synchronized Bytes sendToServer(Bytes message) {
    return encrypt(message, clientToServerKey, clientToServerNonce);
  }

  @Override
  public synchronized Bytes sendGoodbyeToServer() {
    return sendToServer(Bytes.wrap(new byte[18]));
  }

  @Override
  public synchronized Bytes readFromServer(Bytes message) {
    return decrypt(message, serverToClientKey, serverToClientNonce);
  }

  @Override
  public synchronized Bytes sendToClient(Bytes message) {
    return encrypt(message, serverToClientKey, serverToClientNonce);
  }

  @Override
  public synchronized Bytes sendGoodbyeToClient() {
    return sendToClient(Bytes.wrap(new byte[18]));
  }

  @Override
  public synchronized Bytes readFromClient(Bytes message) {
    return decrypt(message, clientToServerKey, clientToServerNonce);
  }

  private Bytes decrypt(Bytes message, SecretBox.Key key, MutableBytes nonce) {
    int index = 0;
    List<Bytes> decryptedMessages = new ArrayList<>();
    while (index < message.size()) {
      Bytes decryptedMessage = decryptMessage(message.slice(index), key, nonce);
      decryptedMessages.add(decryptedMessage);
      index += decryptedMessage.size() + 34;
    }
    return Bytes.concatenate(decryptedMessages.toArray(new Bytes[decryptedMessages.size()]));
  }

  private Bytes decryptMessage(Bytes message, SecretBox.Key key, MutableBytes nonce) {
    SecretBox.Nonce headerNonce = SecretBox.Nonce.fromBytes(nonce);
    SecretBox.Nonce bodyNonce = SecretBox.Nonce.fromBytes(nonce.increment());
    nonce.increment();
    Bytes decryptedHeader = SecretBox.decrypt(message.slice(0, 34), key, headerNonce);

    if (decryptedHeader == null) {
      throw new StreamException("Failed to decrypt message header");
    }

    int bodySize = ((decryptedHeader.get(0)) << 8) + (decryptedHeader.get(1));
    Bytes body = message.slice(34, bodySize);
    Bytes decryptedBody = SecretBox.decrypt(Bytes.concatenate(decryptedHeader.slice(2), body), key, bodyNonce);
    if (decryptedBody == null) {
      throw new StreamException("Failed to decrypt message");
    }
    return decryptedBody;
  }

  private Bytes encrypt(Bytes message, SecretBox.Key clientToServerKey, MutableBytes clientToServerNonce) {
    int messages = (int) Math.ceil((double) message.size() / 4096d);
    Bytes[] encryptedMessages = new Bytes[messages];
    for (int i = 0; i < messages; i++) {
      Bytes encryptedMessage = encryptMessage(
          message.slice(i * 4096, Math.min((i + 1) * 4096, message.size() - i * 4096)),
          clientToServerKey,
          clientToServerNonce);
      encryptedMessages[i] = encryptedMessage;
    }
    return Bytes.concatenate(encryptedMessages);
  }

  private Bytes encryptMessage(Bytes message, SecretBox.Key key, MutableBytes nonce) {
    SecretBox.Nonce headerNonce = SecretBox.Nonce.fromBytes(nonce);
    SecretBox.Nonce bodyNonce = SecretBox.Nonce.fromBytes(nonce.increment());
    nonce.increment();
    Bytes encryptedBody = SecretBox.encrypt(message, key, bodyNonce);
    int bodySize = encryptedBody.size() - 16;
    Bytes encodedBodySize = Bytes.ofUnsignedInt(bodySize).slice(2);
    Bytes header = SecretBox.encrypt(Bytes.concatenate(encodedBodySize, encryptedBody.slice(0, 16)), key, headerNonce);

    return Bytes.concatenate(header, encryptedBody.slice(16));
  }
}
