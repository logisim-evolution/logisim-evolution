/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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
    Direction dir = state.getAttributeValue(StdAttr.FACING);
    Location loc = state.getInstance().getLocation();
    if (dir == Direction.EAST) {
      int xpos = bounds.getX() - anchorPosition.getX() + loc.getX();
      int ypos = bounds.getY() - anchorPosition.getY() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.WEST) {
      int xpos = anchorPosition.getX() - bounds.getX() - bounds.getWidth() + loc.getX();
      int ypos = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.NORTH) {
      int xpos = bounds.getY() - anchorPosition.getY() + loc.getX();
      int ypos = bounds.getX() - anchorPosition.getX() - bounds.getWidth() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
    }
    int xpos = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getX();
    int ypos = bounds.getX() - anchorPosition.getX() + loc.getY();
    return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
  }

  public Boolean mouseInside(InstanceState state, MouseEvent e) {
    Bounds b = getScreenBounds(state);
    return b.contains(e.getX(), e.getY());
  }

  public abstract void performClickAction(InstanceState state, MouseEvent e);

}
