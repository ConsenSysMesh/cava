package net.consensys.cava.config.schema;

import java.util.Collection;

/**
 * Validator associated with a specific configuration entry.
 * <p>
 * This validator is fed to the schema builder which is associated with a configuration.
 * <p>
 * Consumers should implement this interface to provide specific validation. Note exceptions thrown from inside the
 * validator will interrupt loading the configuration.
 */
public interface Validator {

  /**
   * Validates a configuration entry.
   *
   * @param key the configuration entry name
   * @param value the value associated with the configuration entry
   * @return a collection of error messages if any have been provided. A null or empty collection means no errors were
   *         reported.
   */
  Collection<String> validate(String key, Object value);
}
