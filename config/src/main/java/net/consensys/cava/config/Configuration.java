package net.consensys.cava.config;

import net.consensys.cava.config.schema.Schema;
import net.consensys.cava.config.schema.ValidationResults;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;

import org.jetbrains.annotations.NotNull;

/**
 * Instances of this class represent configuration data loaded from a configuration file in TOML format and a schema
 * providing default values.
 */
public interface Configuration {

  /**
   * Loads a configuration from a given file.
   *
   * @param path the path to a valid, existing TOML file
   * @return a Configuration object loaded from the TOML file
   * @throws IllegalArgumentException if the file could not be found
   * @throws RuntimeException if the file could not be read
   */
  static Configuration load(Path path) {
    return load(path, null, null);
  }

  /**
   * Loads a configuration from a given file, associated with a schema and a validator.
   *
   * @param path the path to a valid, existing TOML file
   * @param builder the schema builder used to represent the schema of the configuration.
   * @param validationHandler the handler called to validate the configuration
   * @return a Configuration object loaded from the TOML file
   * @throws IllegalArgumentException if the file could not be found
   * @throws RuntimeException if the file could not be read
   */
  static Configuration load(Path path, Schema builder, Consumer<ValidationResults> validationHandler) {
    try (FileInputStream fis = new FileInputStream(path.toFile())) {
      return new Konfig(fis, builder, validationHandler);
    } catch (FileNotFoundException e) {
      throw new IllegalArgumentException(path.toAbsolutePath().toString() + " was not found");
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * Save a configuration to a file. This method will attempt to create parent directories if needed.
   *
   * @param path the path to which the configuration should be saved
   * @throws IOException if the file cannot be written or the parent directories cannot be created
   */
  void save(Path path) throws IOException;

  /**
   * Provides the keys of the entries present in this configuration
   *
   * @return the keys of the entries of the configuration
   */
  Set<String> keys();

  /**
   * Checks if a given key is present in the configuration entries
   *
   * @param key the key associated with a configuration entry
   * @return true if the entry is present
   */
  boolean key(@NotNull String key);

  /**
   * Optionally provides the value of a given key as a String, or the default value if specified if the value is not
   * present, or none if neither are present
   *
   * @param key the configuration entry name
   * @return the value associated with the key as a String, if present
   */
  Optional<String> get(@NotNull String key);

  /**
   * Optionally provides the value of a given key as a Double if it can be formatted as such, or the default value if
   * specified if the value is not present, or none if neither are present
   *
   * @param key the configuration entry name
   * @return the value associated with the key as a Double, if present
   */
  Optional<Double> getDouble(@NotNull String key);

  /**
   * Optionally provides the value of a given key as a Boolean if it can be formatted as such, or the default value if
   * specified if the value is not present, or none if neither are present
   *
   * @param key the configuration entry name
   * @return the value associated with the key as a Boolean, if present
   */
  Optional<Boolean> getBoolean(@NotNull String key);

  /**
   * Optionally provides the value of a given key as a Long if it can be formatted as such, or the default value if
   * specified if the value is not present, or none if neither are present
   *
   * @param key the configuration entry name
   * @return the value associated with the key as a Long, if present
   */
  Optional<Long> getLong(@NotNull String key);

  /**
   * Optionally provides the value of a given key as a native array if it can be formatted as such, or the default value
   * if specified if the value is not present, or none if neither are present
   *
   * @param key the configuration entry name
   * @return the value associated with the key as a native array, if present
   */
  Optional<Object[]> getArray(@NotNull String key);


}
