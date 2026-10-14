/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.StdAttr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class LedBarShape extends DotMatrixBaseShape {

  public LedBarShape(int x, int y, DynamicElement.Path p) {
    super(x, y, p);
  }

  @Override
  protected int matrixScaleY() {
    if (path.leaf().getAttributeSet().containsAttribute(LedBar.ATTR_THICKNESS)) {
      final var thickness = path.leafGetAttributeValue(LedBar.ATTR_THICKNESS);
      if (thickness != null) {
        return thickness;
      }
    }
    return super.matrixScaleY();
  }

  @Override
  protected Direction matrixFacing() {
    if (!path.leaf().getAttributeSet().containsAttribute(StdAttr.FACING)) {
      return Direction.EAST;
    }
    final var facing = path.leafGetAttributeValue(StdAttr.FACING);
    return facing == null ? Direction.EAST : facing;
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-ledbar"));
  }

  @Override
  public String getDisplayName() {
    return S.get("ledBarComponent");
  }
}
