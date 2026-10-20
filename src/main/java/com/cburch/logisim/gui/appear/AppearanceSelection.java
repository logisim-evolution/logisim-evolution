/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import java.util.Collection;

public class AppearanceSelection extends Selection {
  @Override
  public void setMovingDelta(int dx, int dy) {
    if (shouldSnap(getSelected())) {
      dx = Math.round(dx / 10.0f) * 10;
      dy = Math.round(dy / 10.0f) * 10;
    }
    super.setMovingDelta(dx, dy);
  }

  @Override
  public void setMovingShapes(Collection<? extends CanvasObject> shapes, int dx, int dy) {
    if (shouldSnap(shapes)) {
      dx = Math.round(dx / 10.0f) * 10;
      dy = Math.round(dy / 10.0f) * 10;
    }
    super.setMovingShapes(shapes, dx, dy);
  }

  private boolean shouldSnap(Collection<? extends CanvasObject> shapes) {
    for (final var obj : shapes) {
      if (obj instanceof AppearanceElement) return true;
    }
    return false;
  }
}
