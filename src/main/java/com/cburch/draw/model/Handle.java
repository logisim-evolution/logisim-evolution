/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.logisim.data.Location;
import lombok.Getter;

public class Handle {
  @Getter private final CanvasObject object;
  @Getter private final int x;
  @Getter private final int y;

  public Handle(CanvasObject object, int x, int y) {
    this.object = object;
    this.x = x;
    this.y = y;
  }

  public Handle(CanvasObject object, Location loc) {
    this(object, loc.getX(), loc.getY());
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof Handle) {
      Handle that = (Handle) other;
      return this.object.equals(that.object) && this.x == that.x && this.y == that.y;
    } else {
      return false;
    }
  }

  public Location getLocation() {
    return Location.create(x, y);
  }

  @Override
  public int hashCode() {
    return (this.object.hashCode() * 31 + x) * 31 + y;
  }

  public boolean isAt(int xq, int yq) {
    return x == xq && y == yq;
  }

  public boolean isAt(Location loc) {
    return x == loc.getX() && y == loc.getY();
  }
}
