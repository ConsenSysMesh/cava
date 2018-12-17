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

import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.rlpx.RLPxMessage;

import java.util.LinkedHashMap;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Test;
import org.logl.LoggerProvider;

class PingPongTest {

  @Test
  void pingPongRoundtrip() {
    AtomicReference<RLPxMessage> capturedPing = new AtomicReference<>();
    WireConnection conn =
        new WireConnection("abc", LoggerProvider.nullProvider().getLogger("rlpx"), capturedPing::set, () -> {
        }, new LinkedHashMap<>());

    AsyncCompletion completion = conn.sendPing();
    assertFalse(completion.isDone());
    assertNotNull(capturedPing.get());

    conn.messageReceived(new RLPxMessage(3, Bytes.EMPTY));
    assertTrue(completion.isDone());
  }

  @Test
  void pongPingRoundtrip() {
    AtomicReference<RLPxMessage> capturedPong = new AtomicReference<>();
    WireConnection conn =
        new WireConnection("abc", LoggerProvider.nullProvider().getLogger("rlpx"), capturedPong::set, () -> {
        }, new LinkedHashMap<>());

    conn.messageReceived(new RLPxMessage(2, Bytes.EMPTY));
    assertNotNull(capturedPong.get());
    assertEquals(3, capturedPong.get().messageId());
  }
}
