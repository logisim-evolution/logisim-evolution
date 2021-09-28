/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.AbstractCanvasObject;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public abstract class AppearanceElement extends AbstractCanvasObject {
  private Location location;

  public AppearanceElement(Location location) {
    this.location = location;
  }

  @Override
  public boolean canRemove() {
    return false;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return Collections.emptyList();
  }

  protected Bounds getBounds(int radius) {
    return Bounds.create(location.getX() - radius, location.getY() - radius, 2 * radius, 2 * radius);
  }

  public Location getLocation() {
    return location;
  }

  @Override
  public Location getRandomPoint(Bounds bds, Random rand) {
    // this is only used to determine what lies on top of what but the elements will always be on
    // top anyway
    return null;
  }

  @Override
  public <V> V getValue(Attribute<V> attr) {
    return null;
  }

  protected boolean isInCircle(Location loc, int radius) {
    final var dx = loc.getX() - location.getX();
    final var dy = loc.getY() - location.getY();
    return dx * dx + dy * dy < radius * radius;
  }

  @Override
  public boolean matches(CanvasObject other) {
    if (other instanceof AppearanceElement that) {
      return this.location.equals(that.location);
    }
    return false;
  }

  @Override
  public int matchesHashCode() {
    return location.hashCode();
  }

  @Override
  public void translate(int dx, int dy) {
    location = location.translate(dx, dy);
  }

  @Override
  protected void updateValue(Attribute<?> attr, Object value) {
    // nothing to do
  }
}
