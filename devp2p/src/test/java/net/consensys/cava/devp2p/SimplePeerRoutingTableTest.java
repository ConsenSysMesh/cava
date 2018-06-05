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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class SimplePeerRoutingTableTest {

  @Test
  void moreThanMaxAllowedPeers() {
    SimplePeerRoutingTable routingTable = new SimplePeerRoutingTable(3);
    PeerRepository repo = new PeerRepository();
    Peer peer1 = repo.get("enode://abcd@10.0.0.1:1345");
    assertTrue(routingTable.add(peer1));
    Peer peer2 = repo.get("enode://abcdef@10.0.0.1:6666");
    assertTrue(routingTable.add(peer2));
    Peer peer3 = repo.get("enode://abcdea@10.0.0.1:12312");
    assertTrue(routingTable.add(peer3));
    Peer peer4 = repo.get("enode://abcdee@10.0.0.1:12315");
    assertFalse(routingTable.add(peer4));
  }

  @Test
  void clearPeers() {
    SimplePeerRoutingTable routingTable = new SimplePeerRoutingTable(2);
    PeerRepository repo = new PeerRepository();
    Peer peer1 = repo.get("enode://abcd@10.0.0.1:1345");
    routingTable.add(peer1);
    Peer peer2 = repo.get("enode://abcdef@10.0.0.1:6666");
    routingTable.add(peer2);
    routingTable.clear();
    Peer peer3 = repo.get("enode://abcdea@10.0.0.1:12312");
    assertTrue(routingTable.add(peer3));
  }
}
