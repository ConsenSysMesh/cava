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
package net.consensys.cava.rlpx;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.rlpx.wire.WireSubProtocolMessage;

import java.net.InetSocketAddress;

/**
 * Service allowing connections to remote peers over RLPx connections.
 */
public interface RLPxService {

  /**
   * Connects to a remote peer
   *
   * @param peerPublicKey the peer public key
   * @param peerAddress the peer host and port
   */
  void connectTo(SECP256K1.PublicKey peerPublicKey, InetSocketAddress peerAddress);


  /**
   * Starts the service.
   * 
   * @return a future handler tracking starting the service.
   */
  AsyncCompletion start();

  /**
   * Stops the service.
   * 
   * @return a future handler tracking stopping the service.
   */
  AsyncCompletion stop();

  /**
   * Sends a wire message to a peer
   * 
   * @param message the message, addressed to a connection.
   */
  void send(WireSubProtocolMessage message);

  /**
   * Sends a wire message to all connected peers
   * 
   * @param message the message to broadcast
   */
  void broadcast(WireSubProtocolMessage message);
}
