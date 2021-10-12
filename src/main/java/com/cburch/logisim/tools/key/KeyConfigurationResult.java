/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.key;

import com.cburch.logisim.data.Attribute;
import java.util.HashMap;
import java.util.Map;

public class KeyConfigurationResult {
  private final KeyConfigurationEvent event;
  private final Map<Attribute<?>, Object> attrValueMap;

  public KeyConfigurationResult(KeyConfigurationEvent event, Attribute<?> attr, Object value) {
    this.event = event;
    final var singleMap = new HashMap<Attribute<?>, Object>(1);
    singleMap.put(attr, value);
    this.attrValueMap = singleMap;
  }

  public KeyConfigurationResult(KeyConfigurationEvent event, Map<Attribute<?>, Object> values) {
    this.event = event;
    this.attrValueMap = values;
  }

  public Map<Attribute<?>, Object> getAttributeValues() {
    return attrValueMap;
  }

  public KeyConfigurationEvent getEvent() {
    return event;
  }
}
