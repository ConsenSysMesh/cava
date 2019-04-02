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
package net.consensys.cava.plumtree;

import net.consensys.cava.bytes.Bytes;

/**
 * Validator for a message and a peer.
 *
 * This validator is called prior to gossiping the message from that peer to other peers.
 */
public interface MessageValidator {

  /**
   * Validates that the message from the peer is valid.
   *
   * @param message the payload sent over the network
   * @param peer the peer that sent the message
   * @return true if the message is valid
   */
  boolean validate(Bytes message, Peer peer);
}
