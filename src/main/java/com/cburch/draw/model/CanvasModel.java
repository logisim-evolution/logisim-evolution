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

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Bounds;
import java.awt.Graphics;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public interface CanvasModel {
  // listener methods
  void addCanvasModelListener(CanvasModelListener l);

  // methods that alter the model
  void addObjects(int index, Collection<? extends CanvasObject> shapes);

  void addObjects(Map<? extends CanvasObject, Integer> shapes);

  Handle deleteHandle(Handle handle);

  List<CanvasObject> getObjectsFromBottom();

  List<CanvasObject> getObjectsFromTop();

  Collection<CanvasObject> getObjectsIn(Bounds bds);

  Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape);

  void insertHandle(Handle desired, Handle previous);

  Handle moveHandle(HandleGesture gesture);

  // methods that don't change any data in the model
  void paint(Graphics g, Selection selection);

  void removeCanvasModelListener(CanvasModelListener l);

  void removeObjects(Collection<? extends CanvasObject> shapes);

  void reorderObjects(List<ReorderRequest> requests);

  void setAttributeValues(Map<AttributeMapKey, Object> values);

  void setText(Text text, String value);

  void translateObjects(Collection<? extends CanvasObject> shapes, int dx, int dy);
}
