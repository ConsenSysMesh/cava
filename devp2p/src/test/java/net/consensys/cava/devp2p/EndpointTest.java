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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class EndpointTest {

  @Test
  void endpointsWithSameHostAndPortsAreEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7654, 8765);
    assertTrue(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentHostsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.2", 7654, 8765);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentUDPPortsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7655, 8765);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void endpointsWithDifferentTCPPortsAreNotEqual() {
    Endpoint endpoint1 = new Endpoint("127.0.0.1", 7654, 8765);
    Endpoint endpoint2 = new Endpoint("127.0.0.1", 7654, 8766);
    assertFalse(endpoint1.equals(endpoint2));
  }

  @Test
  void invalidUDPPortThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Endpoint("127.0.0.1", 76543321, 8765);
    });
  }

  @Test
  void invalidTCPPortThrowsIllegalArgument() {
    assertThrows(IllegalArgumentException.class, () -> {
      new Endpoint("127.0.0.1", 7654, 87654321);
    });
  }
}
