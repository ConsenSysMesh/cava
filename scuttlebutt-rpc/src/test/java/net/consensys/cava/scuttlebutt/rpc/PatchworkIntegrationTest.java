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
package net.consensys.cava.scuttlebutt.rpc;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.crypto.sodium.Signature;
import net.consensys.cava.io.Base64;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.scuttlebutt.handshake.vertx.ClientHandler;
import net.consensys.cava.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;

import java.io.PrintWriter;
import java.util.function.Consumer;

import io.vertx.core.Vertx;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.logl.Level;
import org.logl.LoggerProvider;
import org.logl.logl.SimpleLogger;
import org.logl.vertx.LoglLogDelegateFactory;

/**
 * Test used with a local installation of Patchwork on the developer machine.
 *
 * Usable as a demo or to check manually connections.
 */
@ExtendWith(VertxExtension.class)
class PatchworkIntegrationTest {

  public class MyClientHandler implements ClientHandler {

    private final Consumer<Bytes> sender;
    private final Runnable terminationFn;

    public MyClientHandler(Consumer<Bytes> sender, Runnable terminationFn) {
      this.sender = sender;
      this.terminationFn = terminationFn;
    }

    @Override
    public void receivedMessage(Bytes message) {

      System.out.println("We received a message?");

    }

    @Override
    public void streamClosed() {

      System.out.println("Stream closed?");

    }

    void sendMessage(Bytes bytes) {
      System.out.println("Sending message?");
      sender.accept(bytes);
    }

    void closeStream() {
      terminationFn.run();
    }
  }

  private MyClientHandler clientHandler;

  @Disabled
  @Test
  void runWithPatchWork(@VertxInstance Vertx vertx) throws Exception {
    String host = "localhost";
    int port = 8008;
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(new PrintWriter(System.out));
    LoglLogDelegateFactory.setProvider(loggerProvider);

    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";
    Signature.KeyPair keyPair = Signature.KeyPair.random();

    String serverPublicKey = ""; // TODO use your own identity public key here.
    Signature.PublicKey publicKey = Signature.PublicKey.fromBytes(Base64.decode(serverPublicKey));

    Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(loggerProvider, vertx, keyPair, networkKeyBytes32);

    AsyncCompletion onConnect = secureScuttlebuttVertxClient.connectTo(port, host, publicKey, (senderFn, stopFn) -> {

      // Tell the client handler how to send bytes to the server
      clientHandler = new MyClientHandler(senderFn, stopFn);

      // We hand over a ClientHandler so we get called back when new bytes arrive
      return clientHandler;
    });

    onConnect.join();
    assertTrue(onConnect.isDone());
    assertFalse(onConnect.isCompletedExceptionally());
    Thread.sleep(1000);
    assertNotNull(clientHandler);
    // An RPC command that just tells us our public key (like ssb-server whoami on the command line.)
    String rpcRequestBody = "{\n" + "  \"name\": [\"whoami\"],\n" + "  \"type\": \"sync\" " + "}";
    Bytes rpcRequest = RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON);

    System.out.println("Attempting RPC request...");
    clientHandler.sendMessage(rpcRequest);

    Thread.sleep(10000);

    secureScuttlebuttVertxClient.stop().join();
  }
}
