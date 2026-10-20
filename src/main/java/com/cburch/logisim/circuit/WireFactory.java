/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.comp.AbstractComponentFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import java.awt.Color;

class WireFactory extends AbstractComponentFactory {
  public static final WireFactory instance = new WireFactory();

  private WireFactory() {}

  @Override
  public AttributeSet createAttributeSet() {
    return Wire.create(Location.create(0, 0, true), Location.create(100, 0, true));
  }

  @Override
  public Component createComponent(Location loc, AttributeSet attrs) {
    final var dir = attrs.getValue(Wire.DIR_ATTR);
    final var len = attrs.getValue(Wire.LEN_ATTR);
    return (dir == Wire.VALUE_HORZ)
        ? Wire.create(loc, loc.translate(len, 0))
        : Wire.create(loc, loc.translate(0, len));
  }

  //
  // user interface methods
  //
  @Override
  public void drawGhost(
      ComponentDrawContext context, Color color, int x, int y, AttributeSet attrs) {
    final var g = context.getGraphics();
    final var dir = attrs.getValue(Wire.DIR_ATTR);
    final var len = attrs.getValue(Wire.LEN_ATTR);

    g.setColor(color);
    GraphicsUtil.switchToWidth(g, 3);
    if (dir == Wire.VALUE_HORZ) {
      g.drawLine(x, y, x + len, y);
    } else {
      g.drawLine(x, y, x, y + len);
    }
  }

  @Override
  public StringGetter getDisplayGetter() {
    return S.getter("wireComponent");
  }

  @Override
  public String getName() {
    return "Wire";
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var dir = attrs.getValue(Wire.DIR_ATTR);
    final var len = attrs.getValue(Wire.LEN_ATTR);

    return (dir == Wire.VALUE_HORZ) ? Bounds.create(0, -2, len, 5) : Bounds.create(-2, 0, 5, len);
  }

  @Override
  public boolean isHDLSupportedComponent(AttributeSet attrs) {
    return true;
  }
}
