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
package net.consensys.cava.devp2p;

import static com.google.common.base.Preconditions.checkArgument;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.rlp.RLP;
import net.consensys.cava.rlp.RLPReader;
import net.consensys.cava.rlp.RLPWriter;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Objects;

import com.google.common.net.InetAddresses;

/**
 * An Ethereum node endpoint.
 */
public final class Endpoint {

  public static final int DEFAULT_PORT = 30303;

  private final String host;
  private final int udpPort;
  private final int tcpPort;

  public Endpoint(String host, int udpPort, int tcpPort) {
    checkArgument(host != null && InetAddresses.isInetAddress(host), "host requires a valid IP address");
    checkArgument(udpPort > 0 && udpPort < 65536, "UDP port requires a value between 1 and 65535");
    checkArgument(tcpPort > 0 && tcpPort < 65536, "TCP port requires a value between 1 and 65535");

    this.host = host;
    this.udpPort = udpPort;
    this.tcpPort = tcpPort;
  }

  public String host() {
    return host;
  }

  public int udpPort() {
    return udpPort;
  }

  public int tcpPort() {
    return tcpPort;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj == null) {
      return false;
    }
    if (obj == this) {
      return true;
    }
    if (!(obj instanceof Endpoint)) {
      return false;
    }
    Endpoint other = (Endpoint) obj;
    return host.equals(other.host) && (this.udpPort == other.udpPort) && (this.tcpPort == other.tcpPort);
  }

  @Override
  public int hashCode() {
    return Objects.hash(host, udpPort, tcpPort);
  }

  @Override
  public String toString() {
    return "Endpoint{" + "host='" + host + '\'' + ", udpPort=" + udpPort + ", tcpPort=" + tcpPort + '}';
  }

  /**
   * Write this endpoint as an RLP list item.
   * 
   * @return the result of the RLP encoding as {@link Bytes}
   */
  public Bytes toBytes() {
    return RLP.encodeList(this::write);

  }

  /**
   * Write this endpoint as separate fields.
   *
   * @param writer The RLP writer.
   */
  void write(RLPWriter writer) {
    writer.writeValue(Bytes.of(InetAddresses.forString(host).getAddress()));
    writer.writeInt(udpPort);
    writer.writeInt(tcpPort);
  }

  /**
   * Decodes the RLP stream as a standalone Endpoint instance, which is not part of a Peer.
   *
   * @param reader The RLP input stream from which to read.
   * @return The decoded endpoint.
   */
  public static Endpoint readFrom(RLPReader reader) {
    return reader.readList(Endpoint::read);
  }

  /**
   * Create an Endpoint by reading fields from the RLP input stream.
   *
   * @param reader The RLP input stream from which to read.
   * @return The decoded endpoint.
   */
  public static Endpoint read(RLPReader reader) {
    InetAddress addr;
    try {
      addr = InetAddress.getByAddress(reader.readValue().toArrayUnsafe());
    } catch (UnknownHostException e) {
      throw new IllegalArgumentException(e);
    }
    int udpPort = reader.readInt();

    // Some mainnet packets have been shown to either not have the TCP port field at all,
    // or to have an RLP NULL value for it. Assume the same as the UDP port if it's missing.
    int tcpPort = udpPort;
    if (!reader.isComplete()) {
      tcpPort = reader.readInt();
    }

    return new Endpoint(addr.getHostAddress(), udpPort, tcpPort);
  }
}
