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
import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.SECP256K1;
import net.consensys.cava.junit.BouncyCastleExtension;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.util.ArrayList;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Logger;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;

@ExtendWith({VertxExtension.class, BouncyCastleExtension.class})
class VertxRLPxServiceTest {

  @Test
  void invalidPort(@VertxInstance Vertx vertx) {
    assertThrows(
        IllegalArgumentException.class,
        () -> new VertxRLPxService(vertx, Logger.nullLogger(), -1, SECP256K1.KeyPair.random(), new ArrayList<>()));
  }

  @Test
  void startAndStopService(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service =
        new VertxRLPxService(vertx, Logger.nullLogger(), 10000, SECP256K1.KeyPair.random(), new ArrayList<>());

    service.start().join();
    try {
      assertEquals(10000, service.actualPort());
    } finally {
      service.stop();
    }
  }

  @Test
  void startServiceWithPortZero(@VertxInstance Vertx vertx) throws InterruptedException {
    VertxRLPxService service =
        new VertxRLPxService(vertx, Logger.nullLogger(), 0, SECP256K1.KeyPair.random(), new ArrayList<>());

    service.start().join();
    try {
      assertTrue(service.actualPort() != 0);
    } finally {
      service.stop();
    }
  }

  @Test
  void stopServiceWithoutStartingItFirst(@VertxInstance Vertx vertx) {
    VertxRLPxService service =
        new VertxRLPxService(vertx, Logger.nullLogger(), 0, SECP256K1.KeyPair.random(), new ArrayList<>());
    AsyncCompletion completion = service.stop();
    assertTrue(completion.isDone());
  }

  @Test
  void connectToOtherPeer(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();
    VertxRLPxService service = new VertxRLPxService(vertx, Logger.nullLogger(), 0, ourPair, new ArrayList<>());
    service.start().join();

    VertxRLPxService peerService = new VertxRLPxService(vertx, Logger.nullLogger(), 0, peerPair, new ArrayList<>());
    peerService.start().join();

    try {
      service.connectTo(peerPair.publicKey(), new InetSocketAddress(peerService.actualPort()));
    } finally {
      service.stop();
      peerService.stop();
    }
  }

  @Test
  void checkWireConnectionCreated(@VertxInstance Vertx vertx) throws Exception {
    SECP256K1.KeyPair ourPair = SECP256K1.KeyPair.random();
    SECP256K1.KeyPair peerPair = SECP256K1.KeyPair.random();

    LoggerProvider logProvider =
        SimpleLogger.toPrintWriter(new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.err, UTF_8))));

    VertxRLPxService service =
        new VertxRLPxService(vertx, logProvider.getLogger("rlpx"), 0, ourPair, new ArrayList<>());
    service.start().join();

    VertxRLPxService peerService =
        new VertxRLPxService(vertx, logProvider.getLogger("rlpx2"), 0, peerPair, new ArrayList<>());
    peerService.start().join();

    try {
      service.connectTo(peerPair.publicKey(), new InetSocketAddress(peerService.actualPort()));
      Thread.sleep(3000);
      assertEquals(1, service.wireConnections().size());

    } finally {
      service.stop();
      peerService.stop();
    }
  }
}
