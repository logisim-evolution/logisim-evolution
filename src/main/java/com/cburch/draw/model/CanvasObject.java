/*
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

package com.cburch.draw.model;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import java.awt.Graphics;
import java.util.List;

public interface CanvasObject {
  Handle canDeleteHandle(Location desired);

  Handle canInsertHandle(Location desired);

  boolean canMoveHandle(Handle handle);

  boolean canRemove();

  CanvasObject clone();

  boolean contains(Location loc, boolean assumeFilled);

  Handle deleteHandle(Handle handle);

  AttributeSet getAttributeSet();

  Bounds getBounds();

  String getDisplayName();

  String getDisplayNameAndLabel();

  List<Handle> getHandles(HandleGesture gesture);

  <V> V getValue(Attribute<V> attr);

  void insertHandle(Handle desired, Handle previous);

  boolean matches(CanvasObject other);

  int matchesHashCode();

  Handle moveHandle(HandleGesture gesture);

  boolean overlaps(CanvasObject other);

  void paint(Graphics g, HandleGesture gesture);

  <V> void setValue(Attribute<V> attr, V value);

  void translate(int dx, int dy);
}
