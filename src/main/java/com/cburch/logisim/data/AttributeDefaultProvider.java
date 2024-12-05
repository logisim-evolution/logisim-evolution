/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.data;

import com.cburch.logisim.LogisimVersion;

/**
 * An AttributeDefaultProvider is an object with the capability of
 * providing default values for a given attribute.
 */
public interface AttributeDefaultProvider {

  /**
   * Finds the default value for the provided attribute, within the provided logisim evolution
   * version.
   *
   * @param attr The attribute find the default value for.
   * @param ver The logisim evolution version of the attribute to lookup.
   * @return The default value of the given attribute, or null if no such attribute is found.
   */
  Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver);

  /**
   * Determines whether all the attributes of an attribute set have their attributes set
   * to their default values.
   *
   * @param attrs The attribute set to lookup
   * @param ver The logisim evolution version to lookup
   * @return true iff all the keys of the given set have their default values
   *         with regard to the provided logisim evolution version.
   */
  boolean isAllDefaultValues(AttributeSet attrs, LogisimVersion ver);
}
