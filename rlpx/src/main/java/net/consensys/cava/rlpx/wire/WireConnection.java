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

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.concurrent.AsyncCompletion;
import net.consensys.cava.concurrent.CompletableAsyncCompletion;
import net.consensys.cava.rlpx.RLPxMessage;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.google.common.collect.BoundType;
import com.google.common.collect.Range;
import com.google.common.collect.RangeMap;
import com.google.common.collect.TreeRangeMap;
import org.logl.Logger;

/**
 * A stateful connection between two peers under the Devp2p wire protocol.
 */
public final class WireConnection {


  private CompletableAsyncCompletion awaitingPong;

  private static class WireSubprotocolMessageImpl implements WireSubProtocolMessage {

    private final SubProtocolIdentifier subProtocolIdentifier;
    private final Bytes data;
    private final int messageType;
    private final String connectionId;

    WireSubprotocolMessageImpl(
        SubProtocolIdentifier subProtocolIdentifier,
        Bytes data,
        int messageType,
        String connectionId) {
      this.subProtocolIdentifier = subProtocolIdentifier;
      this.data = data;
      this.messageType = messageType;
      this.connectionId = connectionId;
    }

    @Override
    public Bytes toBytes() {
      return data;
    }

    @Override
    public int messageType() {
      return messageType;
    }

    @Override
    public SubProtocolIdentifier subProtocolIdentifier() {
      return subProtocolIdentifier;
    }

    @Override
    public String connectionId() {
      return connectionId;
    }
  }

  private final Logger logger;
  private final String id;
  private final Consumer<RLPxMessage> writer;
  private final Runnable disconnectHandler;
  private final LinkedHashMap<SubProtocol, SubProtocolHandler> subprotocols;

  private HelloMessage myHelloMessage;
  private HelloMessage peerHelloMessage;
  private RangeMap<Integer, SubProtocol> subprotocolRangeMap = TreeRangeMap.create();

  /**
   * Default constructor.
   *
   * @param id the id of the connection
   * @param writer the message writer
   * @param disconnectHandler the handler to run upon receiving a disconnect message
   * @param subprotocols the subprotocols supported by this connection
   */
  public WireConnection(
      String id,
      Logger logger,
      Consumer<RLPxMessage> writer,
      Runnable disconnectHandler,
      LinkedHashMap<SubProtocol, SubProtocolHandler> subprotocols) {
    this.id = id;
    this.logger = logger;
    this.writer = writer;
    this.disconnectHandler = disconnectHandler;
    this.subprotocols = subprotocols;
  }

  public void messageReceived(RLPxMessage message) {
    if (message.messageId() == 0) {
      HelloMessage helloMessage = HelloMessage.read(message.content());
      peerHelloMessage = helloMessage;
      initSupportedRange(peerHelloMessage.capabilities());
      if (myHelloMessage == null) {
        sendHello();
      }
      for (SubProtocol subProtocol : subprotocolRangeMap.asMapOfRanges().values()) {
        subprotocols.get(subProtocol).newPeerConnection(this);
      }
    } else if (message.messageId() == 1) {
      DisconnectMessage.read(message.content());
      disconnectHandler.run();
    } else if (message.messageId() == 2) {
      sendPong();
    } else if (message.messageId() == 3) {
      if (awaitingPong != null) {
        awaitingPong.complete();
      }
    } else {
      Map.Entry<Range<Integer>, SubProtocol> subProtocolEntry = subprotocolRangeMap.getEntry(message.messageId());
      if (subProtocolEntry == null) {
        throw new UnsupportedOperationException("unimplemented");
      } else {
        int offset = subProtocolEntry.getKey().lowerEndpoint();
        WireSubprotocolMessageImpl wireProtocolMessage = new WireSubprotocolMessageImpl(
            subProtocolEntry.getValue().id(),
            message.content(),
            message.messageId() - offset,
            id());
        subprotocols.get(subProtocolEntry.getValue()).handle(wireProtocolMessage);
      }
    }
  }

  private void initSupportedRange(List<Capability> capabilities) {
    int startRange = 17;
    for (Capability cap : capabilities) {
      for (SubProtocol sp : subprotocols.keySet()) {
        if (sp.supports(SubProtocolIdentifier.of(cap.name(), cap.version()))) {
          int numberOfMessageTypes = sp.versionRange(cap.version());
          subprotocolRangeMap
              .put(Range.range(startRange, BoundType.CLOSED, startRange + numberOfMessageTypes, BoundType.CLOSED), sp);
          startRange += numberOfMessageTypes + 1;
          break;
        }
      }
    }
  }

  /**
   * Sends a message to the peer explaining that we are about to disconnect.
   *
   * @param reason the reason for disconnection
   */
  public void disconnect(int reason) {
    writer.accept(new RLPxMessage(1, new DisconnectMessage(reason).toBytes()));
  }

  /**
   * Sends a ping message
   */
  public AsyncCompletion sendPing() {
    writer.accept(new RLPxMessage(2, Bytes.EMPTY));
    this.awaitingPong = AsyncCompletion.incomplete();
    return awaitingPong;
  }

  private void sendPong() {
    writer.accept(new RLPxMessage(3, Bytes.EMPTY));
  }

  private void sendHello() {
    myHelloMessage = new HelloMessage(//TODO fix those parameters!
        Bytes.of(1),
        0,
        "abc",
        1,
        subprotocols.keySet().stream().map(sp -> new Capability(sp.id().name(), sp.id().version())).collect(
            Collectors.toList()));
    writer.accept(new RLPxMessage(0, myHelloMessage.toBytes()));
  }

  public String id() {
    return id;
  }

  public void sendMessage(WireSubProtocolMessage message) {
    Integer offset = null;
    for (Map.Entry<Range<Integer>, SubProtocol> entry : subprotocolRangeMap.asMapOfRanges().entrySet()) {
      if (entry.getValue().supports(message.subProtocolIdentifier())) {
        offset = entry.getKey().lowerEndpoint();
        break;
      }
    }
    if (offset == null) {
      throw new UnsupportedOperationException(); // no subprotocol mapped to this connection. Exit.
    }
    writer.accept(new RLPxMessage(message.messageType() + offset, message.toBytes()));
  }

  public void handleConnectionStart() {
    sendHello();
  }
}
