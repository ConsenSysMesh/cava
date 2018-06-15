/*
 * Copyright 2018, ConsenSys Inc.
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

import static net.consensys.cava.config.ConfigurationErrors.noErrors;
import static net.consensys.cava.config.ConfigurationErrors.singleError;

import java.util.List;
import javax.annotation.Nullable;

/**
 * A validator associated with a specific configuration property.
 */
public interface PropertyValidator<T> {

  /**
   * A validator that ensures a property is present.
   *
   * @return A validator that ensures a property is present.
   */
  static PropertyValidator<Object> isPresent() {
    return PropertyValidators.IS_PRESENT;
  }

  /**
   * A validator that ensures a property is within a long integer range.
   *
   * @param from The lower bound of the range (inclusive).
   * @param to The upper bound of the range (exclusive).
   * @return A validator that ensures a property is within an integer range.
   */
  static PropertyValidator<Number> inRange(long from, long to) {
    return (key, position, value) -> {
      if (value == null || value.longValue() < from || value.longValue() >= to) {
        return singleError(position, "Value of property '" + key + "' is outside range [" + from + "," + to + ")");
      }
      return noErrors();
    };
  }

  /**
   * Validate a configuration property.
   *
   * @param key The configuration property key.
   * @param position The position of the property in the input document, if supported. This should be used when
   *        constructing errors.
   * @param value The value associated with the configuration entry.
   * @return A list of errors. If no errors are found, an empty list should be returned.
   */
  List<ConfigurationError> validate(String key, @Nullable DocumentPosition position, @Nullable T value);
}
