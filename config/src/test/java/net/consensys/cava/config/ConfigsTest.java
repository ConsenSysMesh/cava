package net.consensys.cava.config;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import net.consensys.cava.config.schema.Schema;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;

import org.junit.jupiter.api.Test;

class ConfigsTest {

  @Test
  void loadSimpleConfigFile() throws Exception {
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "foo=\"12\"\nbar=\"13\"\nfoobar = 156.34".getBytes(UTF_8));
    Configuration config = Configuration.load(configFile);
    assertEquals("12", config.get("foo").get());
    assertEquals("13", config.get("bar").get());
    assertEquals((Double) 156.34, config.getDouble("foobar").get());
  }

  @Test
  void loadMissingFile() {
    assertThrows(IllegalArgumentException.class, () -> {
      Configuration.load(Paths.get("FileThatDoesntExist"));
    });
  }

  @Test
  void invalidTOMLFile() throws Exception {
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "foo=\"12\"\nbar=\"13\"\nfoobar = \"156.34".getBytes(UTF_8));
    Configuration config = Configuration.load(configFile);
    assertEquals("\"156.34", config.get("foobar").get());
    assertFalse(config.getDouble("foobar").isPresent());
  }

  @Test
  void testKeyPresent() throws Exception {
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "foo=\"12\"\nbar=\"13\"\nfoobar = 156.34".getBytes(UTF_8));
    Configuration config = Configuration.load(configFile);
    assertEquals(new HashSet<String>(Arrays.asList("foo", "bar", "foobar")), config.keys());
    assertTrue(config.key("foo"));
    assertTrue(config.key("bar"));
    assertTrue(config.key("foobar"));
    assertFalse(config.key("example"));
  }

  @Test
  void schemaBuilderIsImmutable() throws Exception {
    Schema builder = Schema.schema();
    Schema builder2 = builder.add("somekey", "somevalue", null, null);
    assertNotSame(builder, builder2);
  }

  @Test
  void getDefaultValue() throws Exception {
    Schema builder = Schema.schema().add("foo", "bar", null, null);
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "foobar = noes".getBytes(UTF_8));
    Configuration config = Configuration.load(configFile, builder, null);
    assertEquals("noes", config.get("foobar").get());
    assertEquals("bar", config.get("foo").get());
  }

  @Test
  void keysContainSchemaKeys() throws Exception {
    Schema builder = Schema.schema().add("foo", "bar", null, null);
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "foobar = noes".getBytes(UTF_8));
    Configuration config = Configuration.load(configFile, builder, null);
    assertEquals(new HashSet<>(Arrays.asList("foo", "foobar")), config.keys());
  }

  @Test
  void buildSchemaAndDumpToToml() throws Exception {
    Schema builder = Schema
        .schema()
        .add("somekey", "somevalue", "Got milk", null)
        .add("foo", false, "Toggle switch", null)
        .add("bar", 1.0, "Value of currency", null)
        .add("Here now", 123L, "One two three", null)
        .add("No defaults", (String) null, null, null);
    Path configFile = Files.createTempFile("config", ".toml");
    Configuration config = Configuration.load(configFile, builder, null);
    LocalDateTime now = LocalDateTime.now();
    String toml = ((Konfig) config).toString(() -> now);
    assertEquals(
        "#####\n"
            + "## Generated on "
            + now
            + "\n"
            + "## ---------------------------------\n"
            + "\n"
            + "## bar\n"
            + "## Default: bar\n"
            + "bar = 1.0\n"
            + "\n"
            + "## foo\n"
            + "## Default: foo\n"
            + "foo = false\n"
            + "\n"
            + "## Here now\n"
            + "## Default: Here now\n"
            + "\"Here now\" = 123\n"
            + "\n"
            + "## somekey\n"
            + "## Default: somekey\n"
            + "somekey = \"somevalue\"\n"
            + "\n",
        toml);
  }

  @Test
  void validateConfiguration() throws Exception {
    Schema builder = Schema.schema().validate((config) -> {
      if (config.getLong("expenses").get().compareTo(config.getLong("revenue").get()) == 1) {
        return Collections.singleton("Expenses cannot be larger than revenue");
      }
      return null;
    });
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, "expenses = 2000\nrevenue = 1500".getBytes(UTF_8));
    try {
      Configuration.load(configFile, builder, visitor -> {
        throw new RuntimeException(visitor.getErrors().iterator().next());
      });
      fail("should not reach there");
    } catch (RuntimeException e) {
      assertEquals("Expenses cannot be larger than revenue", e.getMessage());
    }
  }

  @Test
  void validateConfigurationEntry() throws Exception {
    Schema builder = Schema.schema().add("foo", "foobar", null, (key, value) -> {
      if ("bar".equals(value)) {
        return Collections.singleton("No bar allowed");
      }
      return null;
    });
    Path configFile = Files.createTempFile("config", ".toml");
    Files.write(configFile, ("foo = \"bar\"").getBytes(UTF_8));
    try {
      Configuration.load(configFile, builder, visitor -> {
        throw new RuntimeException(visitor.getErrors().iterator().next());
      });
      fail("should not reach there");
    } catch (RuntimeException e) {
      assertEquals("No bar allowed", e.getMessage());
    }
  }

  @Test
  void saveToFile() throws Exception {
    Path file = Files.createTempFile("config", "toml");
    Path oldConfigFile = Files.createTempFile("config", ".toml");
    Files.write(oldConfigFile, "foobar = noes".getBytes(UTF_8));
    Configuration config = Configuration.load(oldConfigFile, null, null);
    LocalDateTime dt = LocalDateTime.now();
    ((Konfig) config).save(file, () -> dt);
    assertEquals(
        "#####\n"
            + "## Generated on "
            + dt.toString()
            + "\n"
            + "## ---------------------------------\n"
            + "\n"
            + "foobar = \"noes\"\n"
            + "\n",
        new String(Files.readAllBytes(file), UTF_8));
  }

  @Test
  void saveCreatesDirectories() throws Exception {
    Path newConfigDir = Files.createTempDirectory("newConfig");
    Path toBeCreated = newConfigDir.resolve("foo/bar/config.toml");
    Path oldConfigFile = Files.createTempFile("config", ".toml");
    Files.write(oldConfigFile, "foobar = noes".getBytes(UTF_8));
    Configuration config = Configuration.load(oldConfigFile, null, null);
    config.save(toBeCreated);
    assertTrue(Files.exists(toBeCreated));
  }

  @Test
  void throwsWhenItCannotWriteFile() throws Exception {
    Path shouldHaveBeenAFile = Files.createTempDirectory("newConfig");
    Path oldConfigFile = Files.createTempFile("config", ".toml");
    Files.write(oldConfigFile, "foobar = noes".getBytes(UTF_8));
    Configuration config = Configuration.load(oldConfigFile, null, null);

    assertThrows(IOException.class, () -> {
      config.save(shouldHaveBeenAFile);
    });
  }
}
