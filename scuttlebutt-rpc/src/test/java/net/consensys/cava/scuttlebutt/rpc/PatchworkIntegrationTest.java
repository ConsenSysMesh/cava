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

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.bytes.Bytes32;
import net.consensys.cava.concurrent.AsyncResult;
import net.consensys.cava.crypto.sodium.Signature;
import net.consensys.cava.io.Base64;
import net.consensys.cava.junit.VertxExtension;
import net.consensys.cava.junit.VertxInstance;
import net.consensys.cava.scuttlebutt.handshake.vertx.ClientHandler;
import net.consensys.cava.scuttlebutt.handshake.vertx.SecureScuttlebuttVertxClient;

import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
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

  public static class MyClientHandler implements ClientHandler {

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

  @Disabled
  @Test
  void runWithPatchWork(@VertxInstance Vertx vertx) throws Exception {
    String host = "localhost";
    int port = 8008;
    LoggerProvider loggerProvider = SimpleLogger.withLogLevel(Level.DEBUG).toPrintWriter(
        new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out, UTF_8))));
    LoglLogDelegateFactory.setProvider(loggerProvider);

    String networkKeyBase64 = "1KHLiKZvAvjbY1ziZEHMXawbCEIM6qwjCDm3VYRan/s=";
    Signature.KeyPair keyPair = Signature.KeyPair.random();

    String serverPublicKey = "0r9zoCwu/tUH8lpcBSCad4txtmUrphvVG/zif+MBceo="; // TODO use your own identity public key here.
    Signature.PublicKey publicKey = Signature.PublicKey.fromBytes(Base64.decode(serverPublicKey));

    Bytes32 networkKeyBytes32 = Bytes32.wrap(Base64.decode(networkKeyBase64));

    SecureScuttlebuttVertxClient secureScuttlebuttVertxClient =
        new SecureScuttlebuttVertxClient(loggerProvider, vertx, keyPair, networkKeyBytes32);

    AsyncResult<ClientHandler> onConnect =
        secureScuttlebuttVertxClient.connectTo(port, host, publicKey, MyClientHandler::new);

    ClientHandler clientHandler = onConnect.get();
    assertTrue(onConnect.isDone());
    assertFalse(onConnect.isCompletedExceptionally());
    Thread.sleep(1000);
    assertNotNull(clientHandler);
    // An RPC command that just tells us our public key (like ssb-server whoami on the command line.)
    String rpcRequestBody = "{\"name\": [\"whoami\"],\"type\": \"async\",\"args\":[]}";
    Bytes rpcRequest = RPCCodec.encodeRequest(rpcRequestBody, RPCFlag.BodyType.JSON);

    System.out.println("Attempting RPC request...");
    ((MyClientHandler) clientHandler).sendMessage(rpcRequest);

    Thread.sleep(10000);

    secureScuttlebuttVertxClient.stop().join();
  }
}
