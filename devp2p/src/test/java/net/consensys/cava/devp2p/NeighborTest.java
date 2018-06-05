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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import net.consensys.cava.crypto.SECP256K1.KeyPair;
import net.consensys.cava.devp2p.NeighborsPayload.Neighbor;
import net.consensys.cava.junit.BouncyCastleExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class NeighborTest {

  @Test
  void shouldNotAllowNullNodeId() {
    Exception e =
        assertThrows(IllegalArgumentException.class, () -> new Neighbor(null, new Endpoint("10.0.0.2", 1234, 1234)));
    assertEquals("nodeId cannot be null", e.getMessage());
  }

  @Test
  void shouldNotAllowNullEndpoint() {
    Exception e =
        assertThrows(IllegalArgumentException.class, () -> new Neighbor(KeyPair.random().publicKey().bytes(), null));
    assertEquals("endpoint cannot be null", e.getMessage());
  }

}
