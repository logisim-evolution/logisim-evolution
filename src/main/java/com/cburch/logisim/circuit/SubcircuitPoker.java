/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.circuit.appear.DynamicElementWithPoker;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import java.awt.Color;
import java.awt.event.MouseEvent;

public class SubcircuitPoker extends InstancePoker {

  private static final Color MAGNIFYING_INTERIOR = new Color(200, 200, 255, 64);
  private static final Color MAGNIFYING_INTERIOR_DOWN = new Color(128, 128, 255, 192);

  private boolean mouseDown;

  @Override
  public Bounds getBounds(InstancePainter painter) {
    final var bds = painter.getInstance().getBounds();
    int cx = bds.getX() + bds.getWidth() / 2;
    int cy = bds.getY() + bds.getHeight() / 2;
    return Bounds.create(cx - 5, cy - 5, 15, 15);
  }

  private boolean isWithin(InstanceState state, MouseEvent e) {
    final var bds = state.getInstance().getBounds();
    int cx = bds.getX() + bds.getWidth() / 2;
    int cy = bds.getY() + bds.getHeight() / 2;
    int dx = e.getX() - cx;
    int dy = e.getY() - cy;
    return dx * dx + dy * dy <= 60;
  }

  @Override
  public void mousePressed(InstanceState state, MouseEvent e) {
    for (final var c :
        ((SubcircuitFactory) state.getInstance().getFactory())
            .getSubcircuit()
            .getAppearance()
            .getObjectsFromTop()) {
      if (c instanceof DynamicElementWithPoker dynEl)
        dynEl.mousePressed(state, e);
    }
    if (isWithin(state, e)) {
      mouseDown = true;
      state.getInstance().fireInvalidated();
    }
  }

  @Override
  public void mouseReleased(InstanceState state, MouseEvent e) {
    for (final var c :
        ((SubcircuitFactory) state.getInstance().getFactory())
            .getSubcircuit()
            .getAppearance()
            .getObjectsFromTop()) {
      if (c instanceof DynamicElementWithPoker dynEl)
        dynEl.mouseReleased(state, e);
    }
    if (mouseDown) {
      mouseDown = false;
      Object sub = state.getData();
      if (e.getClickCount() == 2 && isWithin(state, e) && sub instanceof CircuitState) {
        state.getProject().setCircuitState((CircuitState) sub);
      } else {
        state.getInstance().fireInvalidated();
      }
    }
  }

  @Override
  public void paint(InstancePainter painter) {
    if (painter.getDestination() instanceof Canvas && painter.getData() instanceof CircuitState) {
      final var bds = painter.getInstance().getBounds();
      final var cx = bds.getX() + bds.getWidth() / 2;
      final var cy = bds.getY() + bds.getHeight() / 2;

      final var tx = cx + 7;
      final var ty = cy + 7;
      int[] xp = {tx - 2, cx + 13, cx + 15, tx + 2};
      int[] yp = {ty + 2, cy + 15, cy + 13, ty - 2};
      final var g = painter.getGraphics();
      if (mouseDown) {
        g.setColor(MAGNIFYING_INTERIOR_DOWN);
      } else {
        g.setColor(MAGNIFYING_INTERIOR);
      }
      g.fillOval(cx - 9, cy - 9, 18, 18);
      g.setColor(Color.GRAY);
      g.drawOval(cx - 9, cy - 9, 18, 18);
      g.fillPolygon(xp, yp, xp.length);
    }
  }
}
