/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;

public abstract class AbstractComponent implements Component {
  protected AbstractComponent() {}

  @Override
  public boolean contains(Location pt) {
    final var bds = getBounds();
    if (bds == null) return false;
    return bds.contains(pt, 1);
  }

  @Override
  public boolean contains(Location pt, Graphics g) {
    final var bds = getBounds(g);
    if (bds == null) return false;
    return bds.contains(pt, 1);
  }

  @Override
  public boolean endsAt(Location pt) {
    for (final var data : getEnds()) {
      if (data.getLocation().equals(pt)) return true;
    }
    return false;
  }

  @Override
  public Bounds getBounds(Graphics g) {
    return getBounds();
  }

  @Override
  public EndData getEnd(int index) {
    return getEnds().get(index);
  }
}
