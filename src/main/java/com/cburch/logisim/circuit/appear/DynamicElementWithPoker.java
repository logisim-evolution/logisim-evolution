/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.circuit.appear;

import java.awt.event.MouseEvent;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public abstract class DynamicElementWithPoker extends DynamicElement {
  
  private boolean isPressed;
  private Location AnchorPosition;

  public DynamicElementWithPoker(Path p, Bounds b) {
    super(p, b);
    isPressed = false;
  }
  
  public void mousePressed(InstanceState state, MouseEvent e) { isPressed = true; }
  
  public void mouseReleased(InstanceState state, MouseEvent e) {
    if (isPressed)
      performClickAction(state,e);
    isPressed=false;
  }
  
  public void setAnchor(Location loc) { AnchorPosition = loc; };
  
  public Bounds getScreenBounds(InstanceState state) {
    Direction dir = state.getAttributeValue(StdAttr.FACING);
    Location loc = state.getInstance().getLocation();
    if (dir == Direction.EAST) {
      int xpos = bounds.getX()-AnchorPosition.getX()+loc.getX();
      int ypos = bounds.getY()-AnchorPosition.getY()+loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.WEST) {
      int xpos = AnchorPosition.getX() - bounds.getX() - bounds.getWidth() + loc.getX();
      int ypos = AnchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getWidth(), bounds.getHeight());
    }
    if (dir == Direction.NORTH) {
      int xpos = bounds.getY() - AnchorPosition.getY() + loc.getX();
      int ypos = bounds.getX() - AnchorPosition.getX() - bounds.getWidth() + loc.getY();
      return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
    }
    int xpos = AnchorPosition.getY() - bounds.getY() - bounds.getHeight() + loc.getX();
    int ypos = bounds.getX() - AnchorPosition.getX() + loc.getY();
    return Bounds.create(xpos, ypos, bounds.getHeight(), bounds.getWidth());
  }
  
  public Boolean mouseInside(InstanceState state , MouseEvent e) {
    Bounds b = getScreenBounds(state);
    return b.contains(e.getX(),e.getY());
  }
  
  public abstract void performClickAction(InstanceState state, MouseEvent e);

}
