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
package net.consensys.cava.scuttlebutt.handshake.vertx;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.crypto.sodium.Signature;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;

import java.net.InetSocketAddress;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(VertxExtension.class)
class VertxIntegrationTest {

  private static class MyClientHandler implements ClientHandler {


    private final Consumer<Bytes> sender;
    private final Runnable terminationFn;

    public MyClientHandler(Consumer<Bytes> sender, Runnable terminationFn) {
      this.sender = sender;
      this.terminationFn = terminationFn;
    }

    @Override
    public void receivedMessage(Bytes message) {

    }

    @Override
    public void streamClosed() {

    }

    void sendMessage(Bytes bytes) {
      sender.accept(bytes);
    }

    void closeStream() {
      terminationFn.run();
    }
  }

  private static class MyServerHandler implements ServerHandler {

    boolean closed = false;
    Bytes received = null;

    @Override
    public void receivedMessage(Bytes message) {
      received = message;
    }

    @Override
    public void streamClosed() {
      closed = true;
    }
  }

  @Test
  void connectToServer(@VertxInstance Vertx vertx) throws Exception {
    Signature.KeyPair serverKeyPair = Signature.KeyPair.random();
    Bytes32 networkIdentifier = Bytes32.random();
    AtomicReference<MyServerHandler> serverHandlerRef = new AtomicReference<>();
    SecureScuttlebuttVertxServer server = new SecureScuttlebuttVertxServer(
        vertx,
        new InetSocketAddress("0.0.0.0", 20000),
        serverKeyPair,
        networkIdentifier,
        (streamServer, fn) -> {
          serverHandlerRef.set(new MyServerHandler());
          return serverHandlerRef.get();
        });

    server.start().join();

    SecureScuttlebuttVertxClient client =
        new SecureScuttlebuttVertxClient(vertx, Signature.KeyPair.random(), networkIdentifier);
    AtomicReference<MyClientHandler> handlerRef = new AtomicReference<>();
    client.connectTo(20000, "0.0.0.0", serverKeyPair.publicKey(), (sendFn, fn) -> {
      handlerRef.set(new MyClientHandler(sendFn, fn));
      return handlerRef.get();
    }).join();


    Thread.sleep(1000);
    MyClientHandler handler = handlerRef.get();
    assertNotNull(handler);
    handler.sendMessage(Bytes.fromHexString("deadbeef"));
    Thread.sleep(1000);
    MyServerHandler serverHandler = serverHandlerRef.get();
    assertEquals(Bytes.fromHexString("deadbeef"), serverHandler.received);

    handler.closeStream();
    Thread.sleep(1000);
    assertTrue(serverHandler.closed);

    client.stop();
    server.stop();

  }

}
