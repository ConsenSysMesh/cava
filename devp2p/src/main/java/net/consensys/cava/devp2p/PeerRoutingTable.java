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

import net.consensys.cava.bytes.Bytes;

import java.util.stream.Stream;

/**
 * Represents the organization of peers as part of a routing table.
 *
 */
public interface PeerRoutingTable {

  /**
   * Adds a new peer to the routing table.
   *
   * <p>
   * This method may return false if the routing table doesn't allow for this new peer.
   *
   * @param peer the new peer
   * @return true if the insertion was successful.
   */
  boolean add(Peer peer);

  /**
   * Whether the routing table contains the peer.
   * 
   * @param peer the peer to investigate
   * @return true if the peer is in the routing table, false otherwise.
   */
  boolean contains(Peer peer);

  /**
   * Returns a series of peers, nearest to the target expressed.
   * 
   * @param target a set of bytes representing a target that the routing table can choose to compute a distance with
   *        other peers in the routing table.
   * @return the series of peers nearest to target.
   */
  Stream<Peer> nearest(Bytes target);

  /**
   * Clears the routing table of all participants.
   */
  void clear();
}
