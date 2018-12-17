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
package net.consensys.cava.rlpx.vertx;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.rlpx.RLPxService;
import net.consensys.cava.rlpx.wire.*;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxAcceptanceTest {

  private static class MyMessage implements WireSubProtocolMessage {

    private final SubProtocolIdentifier identifier;
    private final String connectionId;

    public MyMessage(SubProtocolIdentifier identifier, String connectionId) {
      this.identifier = identifier;
      this.connectionId = connectionId;
    }

    @Override
    public SubProtocolIdentifier subProtocolIdentifier() {
      return identifier;
    }

    @Override
    public String connectionId() {
      return connectionId;
    }

    @Override
    public Bytes toBytes() {
      return Bytes.fromHexString("deadbeef");
    }

    @Override
    public int messageType() {
      return 0;
    }
  }

  private static class MyCustomSubProtocolHandler implements SubProtocolHandler {

    public final List<WireSubProtocolMessage> messages = new ArrayList<>();

    private final RLPxService rlpxService;
    private final SubProtocolIdentifier identifier;
    private final int i;

    public MyCustomSubProtocolHandler(RLPxService rlpxService, SubProtocolIdentifier identifier, int i) {
      this.rlpxService = rlpxService;
      this.identifier = identifier;
      this.i = i;
    }

    @Override
    public void handle(WireSubProtocolMessage message) {
      messages.add(message);
    }

    @Override
    public void newPeerConnection(WireConnection conn) {
      rlpxService.send(new MyMessage(identifier, conn.id()));
    }

    @Override
    public AsyncCompletion stop() {
      return AsyncCompletion.completed();
    }
  }

  private static class MyCustomSubProtocol implements SubProtocol {

    private final int i;

    public MyCustomSubProtocol(int i) {
      this.i = i;
    }

    public MyCustomSubProtocolHandler handler;

    @Override
    public SubProtocolIdentifier id() {
      return SubProtocolIdentifier.of("cus", "1");
    }

    @Override
    public boolean supports(SubProtocolIdentifier subProtocolIdentifier) {
      return "cus".equals(subProtocolIdentifier.name()) && "1".equals(subProtocolIdentifier.version());
    }

    @Override
    public int versionRange(String version) {
      return 1;
    }

    @Override
    public SubProtocolHandler createHandler(RLPxService service) {
      handler = new MyCustomSubProtocolHandler(service, id(), i);
      return handler;
    }
  }

  @Test
  void testTwoServicesSendingMessagesOfCustomSubProtocolToEachOther(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair kp = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair secondKp = SECP256K1.KeyPair.random();
    MyCustomSubProtocol sp = new MyCustomSubProtocol(1);
    MyCustomSubProtocol secondSp = new MyCustomSubProtocol(2);
    LoggerProvider logProvider =
        SimpleLogger.toPrintWriter(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8))));
    VertxRLPxService service =
        new VertxRLPxService(vertx, logProvider.getLogger("rlpx"), 0, kp, Collections.singletonList(sp));
    VertxRLPxService secondService =
        new VertxRLPxService(vertx, logProvider.getLogger("rlpx2"), 0, secondKp, Collections.singletonList(secondSp));
    service.start().join();
    secondService.start().join();

    try {
      service.connectTo(secondKp.publicKey(), new InetSocketAddress("localhost", secondService.actualPort()));

      Thread.sleep(3000);
      assertEquals(1, service.wireConnections().size());
      assertEquals(1, secondService.wireConnections().size());

      assertEquals(1, sp.handler.messages.size());
      assertEquals(1, secondSp.handler.messages.size());

      AsyncCompletion completion = service.wireConnections().iterator().next().sendPing();
      completion.join();
      assertTrue(completion.isDone());
    } finally {
      AsyncCompletion.allOf(service.stop(), secondService.stop());
    }
  }
}
