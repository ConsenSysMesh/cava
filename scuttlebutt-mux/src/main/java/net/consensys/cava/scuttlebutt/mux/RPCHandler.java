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
package net.consensys.cava.scuttlebutt.mux;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.scuttlebutt.handshake.vertx.ClientHandler;
import net.consensys.cava.scuttlebutt.rpc.RPCCodec;
import net.consensys.cava.scuttlebutt.rpc.RPCFlag;
import net.consensys.cava.scuttlebutt.rpc.RPCMessage;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public class RPCHandler implements Multiplexer, ClientHandler {

  private final Consumer<Bytes> messageSender;
  private Map<Integer, CompletableFuture<RPCMessage>> awaitingAsyncResponse = new HashMap<>();

  private Map<Integer, ScuttlebuttStreamHandler> streams = new HashMap<>();

  /**
   *
   * @param messageSender sends the request to the server
   */
  public RPCHandler(Consumer<Bytes> messageSender, Runnable terminationFn) {
    this.messageSender = messageSender;
  }


  @Override
  public CompletableFuture<RPCMessage> makeAsyncRequest(RPCMessage request) {
    CompletableFuture<RPCMessage> future = new CompletableFuture<>();

    byte rpcFlags = request.rpcFlags();
    boolean isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags);

    if (isStream) {
      future.completeExceptionally(new Exception("Expected async type request, not stream"));
    } else {
      int requestNumber = request.requestNumber();
      awaitingAsyncResponse.put(requestNumber, future);

      Bytes bytes = RPCCodec.encodeRequest(request.body(), requestNumber, rpcFlags);

      messageSender.accept(bytes);
    }

    return future;
  }

  @Override
  public void openStream(RPCMessage request, ScuttlebuttStreamHandler responseSink) {
    CompletableFuture<RPCMessage> future = new CompletableFuture<>();

    byte rpcFlags = request.rpcFlags();
    boolean isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags);

    if (!isStream) {
      future.completeExceptionally(new Exception("Expected stream type request, not sync type request"));
    } else {
      int requestNumber = request.requestNumber();
      streams.put(requestNumber, responseSink);
    }

  }

  @Override
  public void receivedMessage(Bytes message) {

    RPCMessage response = new RPCMessage(message);

    int requestNumber = response.requestNumber() * -1;
    byte rpcFlags = response.rpcFlags();

    boolean isStream = RPCFlag.Stream.STREAM.isApplied(rpcFlags);

    if (isStream) {
      ScuttlebuttStreamHandler scuttlebuttStreamHandler = streams.get(requestNumber);

      if (scuttlebuttStreamHandler != null) {
        scuttlebuttStreamHandler.onMessage(response);

        boolean lastMessageOrError = response.lastMessageOrError();

        if (lastMessageOrError) {
          streams.remove(requestNumber);
          scuttlebuttStreamHandler.onStreamEnd();
        }
      } else {
        System.out.println("couldn't find stream handler for RPC response with request number " + requestNumber);
      }

    } else {

      CompletableFuture<RPCMessage> rpcMessageFuture = awaitingAsyncResponse.get(requestNumber);

      if (rpcMessageFuture != null) {

        rpcMessageFuture.complete(response);

        awaitingAsyncResponse.remove(requestNumber);

      } else {
        System.out.println("couldn't find future handler for RPC response with request number " + requestNumber);
      }
    }


  }

  @Override
  public void streamClosed() {
    System.out.println("Stream closed!");
  }
}
