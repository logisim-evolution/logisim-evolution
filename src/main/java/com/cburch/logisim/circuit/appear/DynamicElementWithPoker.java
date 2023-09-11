/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.event.MouseEvent;

public abstract class DynamicElementWithPoker extends DynamicElement {

  private boolean isPressed;
  private Location anchorPosition;

  public DynamicElementWithPoker(Path p, Bounds b) {
    super(p, b);
    isPressed = false;
  }

  public void mousePressed(InstanceState state, MouseEvent e) {
    isPressed = true;
  }

  public void mouseReleased(InstanceState state, MouseEvent e) {
    if (isPressed) performClickAction(state, e);
    isPressed = false;
  }

  public void setAnchor(Location loc) {
    anchorPosition = loc;
  }

  public Bounds getScreenBounds(InstanceState state) {
    final var dir = state.getAttributeValue(StdAttr.FACING);
    final var loc = state.getInstance().getLocation();
    if (dir == Direction.EAST) {
      final var posX = bounds.getX() - anchorPosition.getX() + loc.getX();
      final var posY = bounds.getY() - anchorPosition.getY() + loc.getY();
      return Bounds.create(posX, posY, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.WEST) {
      final var posX = anchorPosition.getX() - bounds.getX() - bounds.getWidth() + loc.getX();
      final var posY = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getY();
      return Bounds.create(posX, posY, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.NORTH) {
      final var posX = bounds.getY() - anchorPosition.getY() + loc.getX();
      final var posY = bounds.getX() - anchorPosition.getX() - bounds.getWidth() + loc.getY();
      return Bounds.create(posX, posY, bounds.getHeight(), bounds.getWidth());
    }
    final var posX = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getX();
    final var posY = bounds.getX() - anchorPosition.getX() + loc.getY();
    return Bounds.create(posX, posY, bounds.getHeight(), bounds.getWidth());
  }

  public Boolean mouseInside(InstanceState state, MouseEvent e) {
    final var b = getScreenBounds(state);
    return b.contains(e.getX(), e.getY());
  }

  public abstract void performClickAction(InstanceState state, MouseEvent e);
}
