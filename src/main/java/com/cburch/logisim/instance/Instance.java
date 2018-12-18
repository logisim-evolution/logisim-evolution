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

package com.cburch.logisim.instance;

import java.awt.Font;
import java.util.List;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;

public class Instance {
	public static Component getComponentFor(Instance instance) {
		return instance.comp;
	}

	public static Instance getInstanceFor(Component comp) {
		if (comp instanceof InstanceComponent) {
			return ((InstanceComponent) comp).getInstance();
		} else {
			return null;
		}
	}

	private InstanceComponent comp;

	Instance(InstanceComponent comp) {
		this.comp = comp;
	}

	public void addAttributeListener() {
		comp.addAttributeListener(this);
	}

	public void fireInvalidated() {
		comp.fireInvalidated();
	}

	public AttributeSet getAttributeSet() {
		return comp.getAttributeSet();
	}

	public <E> E getAttributeValue(Attribute<E> attr) {
		return comp.getAttributeSet().getValue(attr);
	}

	public Bounds getBounds() {
		return comp.getBounds();
	}

	public InstanceComponent getComponent() {
		return comp;
	}

	public InstanceData getData(CircuitState state) {
		return (InstanceData) state.getData(comp);
	}

	public InstanceFactory getFactory() {
		return (InstanceFactory) comp.getFactory();
	}

	public Location getLocation() {
		return comp.getLocation();
	}

	public Location getPortLocation(int index) {
		return comp.getEnd(index).getLocation();
	}

	public List<Port> getPorts() {
		return comp.getPorts();
	}

	public void recomputeBounds() {
		comp.recomputeBounds();
	}

	public void setAttributeReadOnly(Attribute<?> attr, boolean value) {
		comp.getAttributeSet().setReadOnly(attr, value);
	}

	public void setData(CircuitState state, InstanceData data) {
		state.setData(comp, data);
	}

	public void setPorts(Port[] ports) {
		comp.setPorts(ports);
	}

	public void setTextField(Attribute<String> labelAttr,
			Attribute<Font> fontAttr, int x, int y, int halign, int valign) {
		comp.setTextField(labelAttr, fontAttr, x, y, halign, valign);
	}
}
