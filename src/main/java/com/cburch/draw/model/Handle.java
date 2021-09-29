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

public class Handle {
  private final CanvasObject object;
  private final int x;
  private final int y;

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
    return (other instanceof Handle that)
           ? this.object.equals(that.object) && this.x == that.x && this.y == that.y
           : false;
  }

  public Location getLocation() {
    return Location.create(x, y);
  }

  public CanvasObject getObject() {
    return object;
  }

  public int getX() {
    return x;
  }

  public int getY() {
    return y;
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
