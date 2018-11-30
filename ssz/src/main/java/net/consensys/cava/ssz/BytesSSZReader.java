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
package net.consensys.cava.ssz;

import net.consensys.cava.bytes.Bytes;


final class BytesSSZReader implements SSZReader {

  private final Bytes content;
  private int index = 0;

  BytesSSZReader(Bytes content) {
    this.content = content;
  }

  @Override
  public Bytes readValue(int length) {
    if (content.size() - index - length < 0) {
      throw new EndOfSSZException();
    }

    Bytes bytes = content.slice(index, length);

    index += length;
    return bytes;
  }

  @Override
  public boolean isComplete() {
    return (content.size() - index) == 0;
  }
}
