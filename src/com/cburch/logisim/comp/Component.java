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

package com.cburch.logisim.comp;

import java.awt.Graphics;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;

public interface Component {
	// listener methods
	public void addComponentListener(ComponentListener l);

	public boolean contains(Location pt);

	public boolean contains(Location pt, Graphics g);

	public void draw(ComponentDrawContext context);

	public boolean endsAt(Location pt);

	// user interface methods
	public void expose(ComponentDrawContext context);

	public AttributeSet getAttributeSet();

	public Bounds getBounds();

	public Bounds getBounds(Graphics g);

	public EndData getEnd(int index);

	// propagation methods
	public List<EndData> getEnds(); // list of EndDatas
	// basic information methods

	public ComponentFactory getFactory();

	/**
	 * Retrieves information about a special-purpose feature for this component.
	 * This technique allows future Logisim versions to add new features for
	 * components without requiring changes to existing components. It also
	 * removes the necessity for the Component API to directly declare methods
	 * for each individual feature. In most cases, the <code>key</code> is a
	 * <code>Class</code> object corresponding to an interface, and the method
	 * should return an implementation of that interface if it supports the
	 * feature.
	 * 
	 * As of this writing, possible values for <code>key</code> include:
	 * <code>Pokable.class</code>, <code>CustomHandles.class</code>,
	 * <code>WireRepair.class</code>, <code>TextEditable.class</code>,
	 * <code>MenuExtender.class</code>, <code>ToolTipMaker.class</code>,
	 * <code>ExpressionComputer.class</code>, and <code>Loggable.class</code>.
	 * 
	 * @param key
	 *            an object representing a feature.
	 * @return an object representing information about how the component
	 *         supports the feature, or <code>null</code> if it does not support
	 *         the feature.
	 */
	public Object getFeature(Object key);

	// location/extent methods
	public Location getLocation();

	public void propagate(CircuitState state);

	public void removeComponentListener(ComponentListener l);
}
