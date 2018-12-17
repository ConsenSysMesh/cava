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
package net.consensys.cava.rlpx.wire;

import net.consensys.cava.concurrent.AsyncCompletion;

/**
 * Handler managing messages and new connections of peers related for a given subprotocol.
 */
public interface SubProtocolHandler {

  /**
   * Handle an incoming wire protocol message
   * 
   * @param message the message to be handled
   */
  void handle(WireSubProtocolMessage message);

  /**
   * Handle a new peer connection
   * 
   * @param conn the new peer connection
   */
  void newPeerConnection(WireConnection conn);

  /**
   * Stops a subprotocol operations.
   *
   * @return a handle to track when the subprotocol operations have stopped
   */
  AsyncCompletion stop();
}
