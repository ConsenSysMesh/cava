/*
 * Copyright 2018, ConsenSys Inc.
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
package net.consensys.cava.crypto;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.*;

import net.consensys.cava.bytes.Bytes;
import net.consensys.cava.junit.BouncyCastleExtension;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(BouncyCastleExtension.class)
class HashTest {

  @Test
  void testSha256Hash() {
    String horseSha256 = "fd62862b6dc213bee77c2badd6311528253c6cb3107e03c16051aa15584eca1c";
    String cowSha256 = "beb134754910a4b4790c69ab17d3975221f4c534b70c8d6e82b30c165e8c0c09";

    Bytes resultHorse = Hash.sha256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha256), resultHorse);

    byte[] resultHorse2 = Hash.sha256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha256).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha256), resultCow);

    byte[] resultCow2 = Hash.sha256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha256).toArray(), resultCow2);
  }

  @Test
  void testKeccak256Hash() {
    String horseKeccak256 = "c87f65ff3f271bf5dc8643484f66b200109caffe4bf98c4cb393dc35740b28c0";
    String cowKeccak256 = "c85ef7d79691fe79573b1a7064c19c1a9819ebdbd1faaab1a8ec92344438aaf4";

    Bytes resultHorse = Hash.keccak256(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseKeccak256), resultHorse);

    byte[] resultHorse2 = Hash.keccak256("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseKeccak256).toArray(), resultHorse2);

    Bytes resultCow = Hash.keccak256(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowKeccak256), resultCow);

    byte[] resultCow2 = Hash.keccak256("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowKeccak256).toArray(), resultCow2);
  }

  @Test
  void testSha3Hash() {
    String horseSha3 = "d8137088d21c7c0d69107cd51d1c32440a57aa5c59f73ed7310522ea491000ac";
    String cowSha3 = "fba26f1556b8c7b473d01e3eae218318f752e808407794fc0b6490988a33a82d";

    Bytes resultHorse = Hash.sha3(Bytes.wrap("horse".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(horseSha3), resultHorse);

    byte[] resultHorse2 = Hash.sha3("horse".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(horseSha3).toArray(), resultHorse2);

    Bytes resultCow = Hash.sha3(Bytes.wrap("cow".getBytes(UTF_8)));
    assertEquals(Bytes.fromHexString(cowSha3), resultCow);

    byte[] resultCow2 = Hash.sha3("cow".getBytes(UTF_8));
    assertArrayEquals(Bytes.fromHexString(cowSha3).toArray(), resultCow2);
  }
}
