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
import lombok.Data;
import lombok.Getter;
import lombok.val;

@Data
public class KeyConfigurationResult {
  private final KeyConfigurationEvent event;
  private final Map<Attribute<?>, Object> attributeValues;

  public KeyConfigurationResult(KeyConfigurationEvent event, Attribute<?> attributeValues, Object value) {
    this.event = event;
    val singleMap = new HashMap<Attribute<?>, Object>(1);
    singleMap.put(attributeValues, value);
    this.attributeValues = singleMap;
  }

  public KeyConfigurationResult(KeyConfigurationEvent event, Map<Attribute<?>, Object> values) {
    this.event = event;
    this.attributeValues = values;
  }
}
