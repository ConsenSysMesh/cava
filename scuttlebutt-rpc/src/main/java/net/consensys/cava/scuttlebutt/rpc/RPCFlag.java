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
package net.consensys.cava.scuttlebutt.rpc;

/**
 * Defines constants for dealing with SecureScuttlebutt RPC flags.
 */
public interface RPCFlag {

  /**
   * The value of the flag
   * 
   * @return the value of the flag to set on a byte.
   */
  int value();

  /**
   * Applies the flag to the byte
   * 
   * @param flagsByte the byte to apply the bit to
   * @return the modified byte
   */
  default byte apply(byte flagsByte) {
    return (byte) (flagsByte | value());
  }

  /**
   * Checks if the flag bit is applied to this byte
   * 
   * @param flagsByte the flag byte
   * @return true if the flag is set
   */
  default boolean isApplied(byte flagsByte) {
    return (flagsByte & value()) == value();
  }

  /**
   * Flag to set a stream message.
   */
  enum Stream implements RPCFlag {
    STREAM(1 << 4);

    private final int value;

    Stream(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }

  /**
   * Flag to set an end or error message.
   */
  enum EndOrError implements RPCFlag {
    END(1 << 5);

    private final int value;

    EndOrError(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }
  }

  /**
   * Flag to set a RPC body type.
   */
  enum BodyType implements RPCFlag {
    BINARY(0), UTF_8_STRING(1 << 7), JSON(1 << 6 | 1 << 7);

    private final int value;

    BodyType(int value) {
      this.value = value;
    }

    @Override
    public int value() {
      return value;
    }

    @Override
    public boolean isApplied(byte flagByte) {
      boolean matchesUTF8 = (UTF_8_STRING.value() & flagByte) == UTF_8_STRING.value();
      boolean matchesJSON = (JSON.value() & flagByte) == JSON.value();
      if (matchesUTF8 && matchesJSON) {
        return this == JSON;
      } else if (matchesUTF8) {
        return this == UTF_8_STRING;
      } else {
        return this == BINARY;
      }
    }

    /**
     * Extract the body type from a flag byte
     * 
     * @param flagByte the flag byte encoding the body type
     * @return the body type, either JSON, UTF_8_STRING or BINARY
     */
    public static BodyType extractBodyType(byte flagByte) {
      boolean matchesUTF8 = (UTF_8_STRING.value() & flagByte) == UTF_8_STRING.value();
      boolean matchesJSON = (JSON.value() & flagByte) == JSON.value();
      if (matchesUTF8 && matchesJSON) {
        return JSON;
      } else if (matchesUTF8) {
        return UTF_8_STRING;
      } else {
        return BINARY;
      }
    }
  }
}
