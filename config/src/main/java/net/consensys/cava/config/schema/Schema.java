package net.consensys.cava.config.schema;

import net.consensys.cava.config.KonfigSchema;

import org.jetbrains.annotations.NotNull;

/**
 * This interface allows customers to determine a schema to associate with a configuration to validate the entries read
 * from configuration files, and provide default values if no value is present in the configuration file.
 */
public interface Schema {

  /**
   * Creates a new configuration schema.
   *
   * @return a new empty Schema instance
   */
  static Schema schema() {
    return KonfigSchema.Companion.create();
  }

  /**
   * Adds a configuration entry key to the schema, associated with metadata useful for validation and documentation.
   *
   * @param key the configuration entry name
   * @param defaultValue a default value for the entry. May be null.
   * @param description the description associated with this configuration entry. May be null.
   * @param validator the validator associated with the configuration entry. May be null.
   * @return a new schema builder associated with this new entry
   */
  Schema add(@NotNull String key, String defaultValue, String description, Validator validator);

  /**
   * Adds a configuration entry key to the schema, associated with metadata useful for validation and documentation.
   *
   * @param key the configuration entry name
   * @param defaultValue a default value for the entry. May be null.
   * @param description the description associated with this configuration entry. May be null.
   * @param validator the validator associated with the configuration entry. May be null.
   * @return a new schema builder associated with this new entry
   */
  Schema add(@NotNull String key, Boolean defaultValue, String description, Validator validator);

  /**
   * Adds a configuration entry key to the schema, associated with metadata useful for validation and documentation.
   *
   * @param key the configuration entry name
   * @param defaultValue a default value for the entry. May be null.
   * @param description the description associated with this configuration entry. May be null.
   * @param validator the validator associated with the configuration entry. May be null.
   * @return a new schema builder associated with this new entry
   */
  Schema add(@NotNull String key, Double defaultValue, String description, Validator validator);

  /**
   * Adds a configuration entry key to the schema, associated with metadata useful for validation and documentation.
   *
   * @param key the configuration entry name
   * @param defaultValue a default value for the entry. May be null.
   * @param description the description associated with this configuration entry. May be null.
   * @param validator the validator associated with the configuration entry. May be null.
   * @return a new schema builder associated with this new entry
   */
  Schema add(@NotNull String key, Long defaultValue, String description, Validator validator);

  /**
   * Adds a validator to the schema.
   *
   * @param configurationValidator the complete configuration validator
   * @return a new schema builder associated with this validator
   */
  Schema validate(@NotNull ConfigurationValidator configurationValidator);
}
