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

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;

/**
 * A peer routing table that can limit its maximum number of peers.
 */
public final class SimplePeerRoutingTable implements PeerRoutingTable {

  private final Optional<Integer> maxEntries;
  private Set<Peer> peers = new HashSet<>();

  /**
   * Create a routing table with unlimited peers.
   */
  public SimplePeerRoutingTable() {
    this.maxEntries = Optional.empty();
  }

  /**
   * Create a routing table with a maximum number of peers.
   * 
   * @param maxEntries the maximum number of peers allowed.
   */
  SimplePeerRoutingTable(int maxEntries) {
    this.maxEntries = Optional.of(maxEntries);
  }

  @Override
  public boolean add(Peer peer) {
    checkArgument(peer != null);
    checkArgument(peer.endpoint().isPresent());
    if (maxEntries.orElse(Integer.MAX_VALUE) <= peers.size()) {
      return false;
    }
    return peers.add(peer);
  }

  @Override
  public boolean contains(Peer peer) {
    return peers.contains(peer);
  }

  @Override
  public Stream<Peer> nearest(Bytes target) {
    return peers.stream();
  }

  @Override
  public void clear() {
    peers.clear();
  }
}
