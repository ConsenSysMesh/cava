package net.consensys.cava.config.schema;

import net.consensys.cava.config.Configuration;

import java.util.Collection;

/**
 * This validator is invoked once after all configuration entry validation has taken place, and is passed the whole
 * configuration for validation purposes.
 * <p>
 * Consumers should implement this interface to provide specific validation. Note exceptions thrown from inside the
 * validator will interrupt loading the configuration.
 */
public interface ConfigurationValidator {

  /**
   * Validates the complete configuration
   *
   * @param configuration the configuration loaded from disk
   * @return a collection of error messages if any have been provided. A null or empty collection means no errors were
   *         reported.
   */
  Collection<String> validate(Configuration configuration);
}
