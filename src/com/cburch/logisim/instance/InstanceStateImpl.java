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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public class InstanceStateImpl implements InstanceState {
	private CircuitState circuitState;
	private Component component;

	public InstanceStateImpl(CircuitState circuitState, Component component) {
		this.circuitState = circuitState;
		this.component = component;

		if (component instanceof InstanceComponent) {
			((InstanceComponent) component).setInstanceStateImpl(this);
		}
	}

	public void fireInvalidated() {
		if (component instanceof InstanceComponent) {
			((InstanceComponent) component).fireInvalidated();
		}
	}

	public AttributeSet getAttributeSet() {
		return component.getAttributeSet();
	}

	public <E> E getAttributeValue(Attribute<E> attr) {
		return component.getAttributeSet().getValue(attr);
	}

	public CircuitState getCircuitState() {
		return circuitState;
	}

	public InstanceData getData() {
		InstanceData ret = (InstanceData) circuitState.getData(component);
		return (ret);
	}

	public InstanceFactory getFactory() {
		if (component instanceof InstanceComponent) {
			InstanceComponent comp = (InstanceComponent) component;
			return (InstanceFactory) comp.getFactory();
		} else {
			return null;
		}
	}

	public Instance getInstance() {
		if (component instanceof InstanceComponent) {
			return ((InstanceComponent) component).getInstance();
		} else {
			return null;
		}
	}

	public int getPortIndex(Port port) {
		return this.getInstance().getPorts().indexOf(port);
	}

	public Value getPortValue(int portIndex) {
		EndData data = component.getEnd(portIndex);
		return circuitState.getValue(data.getLocation());
	}

	public Project getProject() {
		return circuitState.getProject();
	}

	public long getTickCount() {
		return circuitState.getPropagator().getTickCount();
	}

	public boolean isCircuitRoot() {
		return !circuitState.isSubstate();
	}

	public boolean isPortConnected(int index) {
		Circuit circ = circuitState.getCircuit();
		Location loc = component.getEnd(index).getLocation();
		return circ.isConnected(loc, component);
	}

	public void repurpose(CircuitState circuitState, Component component) {
		this.circuitState = circuitState;
		this.component = component;
	}

	public void setData(InstanceData value) {
		circuitState.setData(component, value);
	}

	public void setPort(int portIndex, Value value, int delay) {
		EndData end = component.getEnd(portIndex);
		circuitState.setValue(end.getLocation(), value, component, delay);
	}
}
