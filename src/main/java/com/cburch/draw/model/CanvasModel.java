/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.draw.model;

import java.awt.Graphics;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import com.cburch.draw.canvas.Selection;
import com.cburch.draw.shapes.Text;
import com.cburch.logisim.data.Bounds;

public interface CanvasModel {
	// listener methods
	public void addCanvasModelListener(CanvasModelListener l);

	// methods that alter the model
	public void addObjects(int index, Collection<? extends CanvasObject> shapes);

	public void addObjects(Map<? extends CanvasObject, Integer> shapes);

	public Handle deleteHandle(Handle handle);

	public List<CanvasObject> getObjectsFromBottom();

	public List<CanvasObject> getObjectsFromTop();

	public Collection<CanvasObject> getObjectsIn(Bounds bds);

	public Collection<CanvasObject> getObjectsOverlapping(CanvasObject shape);

	public void insertHandle(Handle desired, Handle previous);

	public Handle moveHandle(HandleGesture gesture);

	// methods that don't change any data in the model
	public void paint(Graphics g, Selection selection);

	public void removeCanvasModelListener(CanvasModelListener l);

	public void removeObjects(Collection<? extends CanvasObject> shapes);

	public void reorderObjects(List<ReorderRequest> requests);

	public void setAttributeValues(Map<AttributeMapKey, Object> values);

	public void setText(Text text, String value);

	public void translateObjects(Collection<? extends CanvasObject> shapes,
			int dx, int dy);
}
