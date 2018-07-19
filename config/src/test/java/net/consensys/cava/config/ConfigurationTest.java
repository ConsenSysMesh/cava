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
package net.consensys.cava.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class ConfigurationTest {

  @Tag("slow")
  @Test
  void reloadConfig(@TempDirectory Path tempDir) throws Exception {
    Files.write(tempDir.resolve("config.toml"), "key=\"foo\"".getBytes(UTF_8));
    AtomicReference<Configuration> newConfigReference = new AtomicReference<>();
    Configuration config =
        Configuration.fromToml(tempDir.resolve("config.toml"), (newConfig) -> newConfigReference.set(newConfig));
    assertEquals("foo", config.get("key"));
    Thread.sleep(500);
    Files.write(tempDir.resolve("config.toml"), "key=\"bar\"".getBytes(UTF_8));
    for (int i = 0; i < 30; i++) {
      // by default, the polling mechanism of WatchService polls every 10s.
      Thread.sleep(1000);
      if (newConfigReference.get() != null) {
        break;
      }
    }
    assertNotNull(newConfigReference.get());
    assertEquals("bar", newConfigReference.get().get("key"));

  }

}
