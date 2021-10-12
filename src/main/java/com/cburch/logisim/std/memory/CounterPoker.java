/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;

public class CounterPoker extends RegisterPoker {

  @Override
  public void paint(InstancePainter painter) {
    final var bds = painter.getBounds();
    final var dataWidth = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = dataWidth == null ? 8 : dataWidth.getWidth();
    final var len = (width + 3) / 4;

    final var g = painter.getGraphics();
    g.setColor(Color.RED);
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      if (len > 4) {
        g.drawRect(bds.getX(), bds.getY() + 3, bds.getWidth(), 25);
      } else {
        int wid = 7 * len + 2;
        g.drawRect(bds.getX() + (bds.getWidth() - wid) / 2, bds.getY() + 4, wid, 15);
      }
    } else {
      int xcenter = Counter.getSymbolWidth(width) - 25;
      g.drawRect(bds.getX() + xcenter - len * 4, bds.getY() + 22, len * 8, 16);
    }
    g.setColor(Color.BLACK);
  }
}
