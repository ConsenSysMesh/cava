package net.consensys.cava.config.schema;

import java.util.List;


/**
 * Result of a validation run against a configuration.
 */
public interface ValidationResults {

  /**
   * Provides an immutable list of all the errors captured.
   * <p>
   * Returns an empty list if no errors have been reported.
   *
   * @return the list of errors captured
   */
  List<String> getErrors();

}
