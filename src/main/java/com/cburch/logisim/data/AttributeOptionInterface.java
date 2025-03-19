/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

/**
 * A general interface that defines the getter methods of the data
 * represented by a {@link AttributeOption} object.
 */
public interface AttributeOptionInterface {

  /**
   * @return The value associated with this attribute option.
   */
  Object getValue();

  /**
   * @return The string description of this attribute option.
   */
  String toDisplayString();

  /**
   * @return The name of this attribute option.
   */
  String toString();
}
