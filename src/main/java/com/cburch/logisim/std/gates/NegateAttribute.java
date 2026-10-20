/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Direction;

public class NegateAttribute extends Attribute<Boolean> {
  private static final Attribute<Boolean> BOOLEAN_ATTR = Attributes.forBoolean("negateDummy");

  final int index;
  private final Direction side;

  public NegateAttribute(int index, Direction side) {
    super("negate" + index, null);
    this.index = index;
    this.side = side;
  }

  @Override
  public boolean equals(Object other) {
    return (other instanceof NegateAttribute o)
           ? this.index == o.index && this.side == o.side
           : false;
  }

  @Override
  public java.awt.Component getCellEditor(Boolean value) {
    return BOOLEAN_ATTR.getCellEditor(null, value);
  }

  @Override
  public String getDisplayName() {
    String ret = S.get("gateNegateAttr", "" + (index + 1));
    if (side != null) {
      ret += " (" + side.toVerticalDisplayString() + ")";
    }
    return ret;
  }

  @Override
  public int hashCode() {
    return index * 31 + (side == null ? 0 : side.hashCode());
  }

  @Override
  public Boolean parse(String value) {
    return BOOLEAN_ATTR.parse(value);
  }

  @Override
  public String toDisplayString(Boolean value) {
    return BOOLEAN_ATTR.toDisplayString(value);
  }
}
