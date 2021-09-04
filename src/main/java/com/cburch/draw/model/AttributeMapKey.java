/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.logisim.data.Attribute;
import lombok.Data;

@Data
public class AttributeMapKey {
  private final Attribute<?> attribute;
  private final CanvasObject object;

  public AttributeMapKey(Attribute<?> attribute, CanvasObject object) {
    this.attribute = attribute;
    this.object = object;
  }
}
