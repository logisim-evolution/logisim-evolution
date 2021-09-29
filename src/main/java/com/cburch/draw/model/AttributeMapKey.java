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
import java.util.Objects;

public class AttributeMapKey {
  private final Attribute<?> attr;
  private final CanvasObject object;

  public AttributeMapKey(Attribute<?> attr, CanvasObject object) {
    this.attr = attr;
    this.object = object;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof AttributeMapKey o)
           ? (Objects.equals(attr, o.attr)) && (Objects.equals(object, o.object))
           : false;
  }

  public Attribute<?> getAttribute() {
    return attr;
  }

  public CanvasObject getObject() {
    return object;
  }

  @Override
  public int hashCode() {
    int a = attr == null ? 0 : attr.hashCode();
    int b = object == null ? 0 : object.hashCode();
    return a ^ b;
  }
}
