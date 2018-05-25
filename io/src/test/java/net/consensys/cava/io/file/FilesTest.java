package net.consensys.cava.io.file;

import static net.consensys.cava.io.file.Files.deleteRecursively;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

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
}
