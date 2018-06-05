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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

final class NeighborsPayload extends Payload {
  static NeighborsPayload decode(Bytes payloadBytes) {
    return RLP.decodeList(payloadBytes, reader -> {

      List<Neighbor> neighbors = reader.readList((neighborsReader) -> {
        List<Neighbor> list = new ArrayList<>();
        while (!neighborsReader.isComplete()) {
          Endpoint endpoint = neighborsReader.readList(Endpoint::read);
          Bytes nodeId = neighborsReader.readValue();
          list.add(new Neighbor(nodeId, endpoint));
        }
        return list;
      });

      long expiration = reader.readLong();
      return new NeighborsPayload(neighbors, expiration);
    });
  }

  private final List<Neighbor> neighbors;

  NeighborsPayload(List<Neighbor> neighbors, long expiration) {
    super(expiration);
    this.neighbors = neighbors;
  }

  public List<Neighbor> neighbors() {
    return neighbors;
  }

  @Override
  public Bytes createPayloadBytes() {
    return RLP.encodeList(rlpWriter -> {
      rlpWriter.writeList(neighborWriter -> neighbors.forEach(neighbor -> {
        neighborWriter.writeList(neighbor.endpoint()::write);
        neighborWriter.writeValue(neighbor.nodeId());
      }));
      rlpWriter.writeLong(expiration());
    });
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof NeighborsPayload)) {
      return false;
    }
    NeighborsPayload that = (NeighborsPayload) o;
    return expiration() == that.expiration() && Objects.equals(neighbors, that.neighbors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(neighbors, expiration());
  }

  static final class Neighbor {

    private final Bytes nodeId;
    private final Endpoint endpoint;

    Neighbor(Bytes nodeId, Endpoint endpoint) {
      checkArgument(nodeId != null, "nodeId cannot be null");
      checkArgument(endpoint != null, "endpoint cannot be null");
      this.nodeId = nodeId;
      this.endpoint = endpoint;
    }

    Bytes nodeId() {
      return nodeId;
    }

    Endpoint endpoint() {
      return endpoint;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj == null) {
        return false;
      }
      if (obj == this) {
        return true;
      }
      if (!(obj instanceof Neighbor)) {
        return false;
      }
      Neighbor other = (Neighbor) obj;
      return nodeId.equals(other.nodeId) && endpoint.equals(other.endpoint);
    }

    @Override
    public int hashCode() {
      return Objects.hash(nodeId, endpoint);
    }

    @Override
    public String toString() {
      return "Neighbor{" + "nodeId=" + nodeId + ", endpoint=" + endpoint + '}';
    }
  }
}
