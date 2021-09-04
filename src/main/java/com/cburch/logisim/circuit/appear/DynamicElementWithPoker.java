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
import lombok.val;

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
    val dir = state.getAttributeValue(StdAttr.FACING);
    val loc = state.getInstance().getLocation();
    if (dir == Direction.EAST) {
      val xpos = bounds.getX() - anchorPosition.getX() + loc.getX();
      val ypos = bounds.getY() - anchorPosition.getY() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.WEST) {
      val xpos = anchorPosition.getX() - bounds.getX() - bounds.getWidth() + loc.getX();
      val ypos = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.NORTH) {
      val xpos = bounds.getY() - anchorPosition.getY() + loc.getX();
      val ypos = bounds.getX() - anchorPosition.getX() - bounds.getWidth() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
    }
    val xpos = anchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getX();
    val ypos = bounds.getX() - anchorPosition.getX() + loc.getY();
    return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
  }

  public Boolean mouseInside(InstanceState state, MouseEvent e) {
    val bounds = getScreenBounds(state);
    return bounds.contains(e.getX(), e.getY());
  }

  public abstract void performClickAction(InstanceState state, MouseEvent e);

}
