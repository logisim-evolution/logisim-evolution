/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import java.util.AbstractList;

class GateAttributeList extends AbstractList<Attribute<?>> {
  private static final Attribute<?>[] BASE_ATTRIBUTES = {
    StdAttr.FACING,
    StdAttr.WIDTH,
    GateAttributes.ATTR_SIZE,
    GateAttributes.ATTR_INPUTS,
    GateAttributes.ATTR_OUTPUT,
    StdAttr.LABEL,
    StdAttr.LABEL_FONT,
  };

  private final GateAttributes attrs;

  public GateAttributeList(GateAttributes attrs) {
    this.attrs = attrs;
  }

  @Override
  public Attribute<?> get(int index) {
    final var len = BASE_ATTRIBUTES.length;
    if (index < len) {
      return BASE_ATTRIBUTES[index];
    }
    index -= len;
    if (attrs.xorBehave != null) {
      index--;
      if (index < 0) return GateAttributes.ATTR_XOR;
    }
    final var facing = attrs.facing;
    final var inputs = attrs.inputs;
    if (index == 0) {
      if (facing == Direction.EAST || facing == Direction.WEST) {
        return new NegateAttribute(index, Direction.NORTH);
      } else {
        return new NegateAttribute(index, Direction.WEST);
      }
    } else if (index == inputs - 1) {
      if (facing == Direction.EAST || facing == Direction.WEST) {
        return new NegateAttribute(index, Direction.SOUTH);
      } else {
        return new NegateAttribute(index, Direction.EAST);
      }
    } else if (index < inputs) {
      return new NegateAttribute(index, null);
    }
    return null;
  }

  @Override
  public int size() {
    var ret = BASE_ATTRIBUTES.length;
    if (attrs.xorBehave != null) ret++;
    ret += attrs.inputs;
    return ret;
  }
}
