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
package net.consensys.cava.io.file;

import static java.nio.charset.StandardCharsets.UTF_8;
import static net.consensys.cava.io.file.Files.copyResource;
import static net.consensys.cava.io.file.Files.deleteRecursively;
import static net.consensys.cava.io.file.Files.listenToFileChanges;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import net.consensys.cava.junit.TempDirectory;
import net.consensys.cava.junit.TempDirectoryExtension;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.sun.nio.file.SensitivityWatchEventModifier;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(TempDirectoryExtension.class)
class FilesTest {

  @Test
  void deleteRecursivelyShouldDeleteEverything() throws Exception {
    Path directory = Files.createTempDirectory(this.getClass().getSimpleName());
    Path testData = directory.resolve("test_data");
    Files.createFile(testData);

    Path testDir = directory.resolve("test_dir");
    Files.createDirectory(testDir);
    Path testData2 = testDir.resolve("test_data");
    Files.createFile(testData2);

    assertTrue(Files.exists(directory));
    assertTrue(Files.exists(testData));
    assertTrue(Files.exists(testDir));
    assertTrue(Files.exists(testData2));

    deleteRecursively(directory);

    assertFalse(Files.exists(directory));
    assertFalse(Files.exists(testData));
    assertFalse(Files.exists(testDir));
    assertFalse(Files.exists(testData2));
  }

  @Test
  void canCopyResources(@TempDirectory Path tempDir) throws Exception {
    Path file = copyResource("net/consensys/cava/io/file/test.txt", tempDir.resolve("test.txt"));
    assertTrue(Files.exists(file));
    assertEquals(81, Files.size(file));
  }

  @Tag("slow")
  @Test
  void fileChangesListen(@TempDirectory Path tempDir) throws Exception {
    CompletableFuture<Boolean> called = new CompletableFuture<>();
    Files.createDirectories(tempDir.resolve("changed"));
    Path file = tempDir.resolve("changed/fileToListen.txt");
    Files.write(file, "foo".getBytes(UTF_8));
    listenToFileChanges(file, (path) -> {
      try {
        String content = new String(Files.readAllBytes(path), UTF_8);
        assertEquals("bar", content);
        called.complete(true);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, SensitivityWatchEventModifier.HIGH);
    boolean found = false;
    for (int i = 0; i < 30; i++) {
      try {
        Files.write(file, "bar".getBytes(UTF_8));
        assertTrue(called.get(1, TimeUnit.SECONDS));
        found = true;
        break;
      } catch (TimeoutException ignored) {
      }
    }
    assertTrue(found);
  }

  @Tag("slow")
  @Test
  void fileCreatedAfterwards(@TempDirectory Path tempDir) throws Exception {
    CompletableFuture<Boolean> called = new CompletableFuture<>();
    Files.createDirectories(tempDir.resolve("created"));
    Path file = tempDir.resolve("created/fileToListen.txt");
    listenToFileChanges(file, (path) -> {
      try {
        String content = new String(Files.readAllBytes(path), UTF_8);
        assertEquals("bar", content);
        called.complete(true);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, SensitivityWatchEventModifier.HIGH);
    boolean found = false;
    for (int i = 0; i < 30; i++) {
      try {
        Files.write(file, "bar".getBytes(UTF_8));
        assertTrue(called.get(1, TimeUnit.SECONDS));
        found = true;
        break;
      } catch (TimeoutException ignored) {
      }
    }
    assertTrue(found);
  }

  @Tag("slow")
  @Test
  void fileRecreated(@TempDirectory Path tempDir) throws Exception {
    CompletableFuture<Boolean> called = new CompletableFuture<>();
    Files.createDirectories(tempDir.resolve("recreated"));
    Path file = tempDir.resolve("recreated/fileToListen.txt");
    Files.write(file, "foo".getBytes(UTF_8));
    listenToFileChanges(file, (path) -> {
      try {
        String content = new String(Files.readAllBytes(path), UTF_8);
        assertEquals("bar", content);
        called.complete(true);
      } catch (IOException e) {
        throw new UncheckedIOException(e);
      }
    }, SensitivityWatchEventModifier.HIGH);
    boolean found = false;
    for (int i = 0; i < 30; i++) {
      try {
        Files.delete(file);
        Files.write(file, "bar".getBytes(UTF_8));
        assertTrue(called.get(1, TimeUnit.SECONDS));
        found = true;
        break;
      } catch (TimeoutException ignored) {
      }
    }
    assertTrue(found);
  }
}
