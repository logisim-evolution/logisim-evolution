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

import java.awt.Color;
import java.awt.Graphics;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public class InstancePainter implements InstanceState {
	private ComponentDrawContext context;
	private InstanceComponent comp;
	private InstanceFactory factory;
	private AttributeSet attrs;

	public InstancePainter(ComponentDrawContext context,
			InstanceComponent instance) {
		this.context = context;
		this.comp = instance;
	}

	//
	// helper methods for drawing common elements in components
	//
	public void drawBounds() {
		context.drawBounds(comp);
	}

	public void drawClock(int i, Direction dir) {
		context.drawClock(comp, i, dir);
	}

	public void drawClockSymbol(int xpos, int ypos) {
		context.drawClockSymbol(comp, xpos, ypos);
	}

	public void drawDongle(int x, int y) {
		context.drawDongle(x, y);
	}

	public void drawHandle(int x, int y) {
		context.drawHandle(x, y);
	}

	public void drawHandle(Location loc) {
		context.drawHandle(loc);
	}

	public void drawHandles() {
		context.drawHandles(comp);
	}

	public void drawLabel() {
		if (comp != null) {
			comp.drawLabel(context);
		}
	}

	public void drawPort(int i) {
		context.drawPin(comp, i);
	}

	public void drawPort(int i, String label, Direction dir) {
		context.drawPin(comp, i, label, dir);
	}

	public void drawPorts() {
		context.drawPins(comp);
	}

	public void drawRectangle(Bounds bds, String label) {
		context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(),
				bds.getHeight(), label);
	}

	public void drawRectangle(int x, int y, int width, int height, String label) {
		context.drawRectangle(x, y, width, height, label);
	}

	public void fireInvalidated() {
		comp.fireInvalidated();
	}

	public AttributeSet getAttributeSet() {
		InstanceComponent c = comp;
		return c == null ? attrs : c.getAttributeSet();
	}

	public <E> E getAttributeValue(Attribute<E> attr) {
		InstanceComponent c = comp;
		AttributeSet as = c == null ? attrs : c.getAttributeSet();
		return as.getValue(attr);
	}

	public Bounds getBounds() {
		InstanceComponent c = comp;
		return c == null ? factory.getOffsetBounds(attrs) : c.getBounds();
	}

	public Circuit getCircuit() {
		return context.getCircuit();
	}

	/**
	 *  @brief this function will return the instance data information
	 *  @pre	it assumes that your circuit was instantiate before.
	 */
	public InstanceData getData() {
		CircuitState circState = context.getCircuitState();
		if (circState == null || comp == null) {
			throw new UnsupportedOperationException(
					"setData on InstancePainter");
		} else {
			
			return (InstanceData) circState.getData(comp);
		}
	}

	public java.awt.Component getDestination() {
		return context.getDestination();
	}

	public InstanceFactory getFactory() {
		return comp == null ? factory : (InstanceFactory) comp.getFactory();
	}

	public Object getGateShape() {
		return context.getGateShape();
	}

	public Graphics getGraphics() {
		return context.getGraphics();
	}

	//
	// methods related to the context of the canvas
	//
	public WireSet getHighlightedWires() {
		return context.getHighlightedWires();
	}

	//
	// methods related to the instance
	//
	public Instance getInstance() {
		InstanceComponent c = comp;
		return c == null ? null : c.getInstance();
	}

	public Location getLocation() {
		InstanceComponent c = comp;
		return c == null ? Location.create(0, 0) : c.getLocation();
	}

	public Bounds getOffsetBounds() {
		InstanceComponent c = comp;
		if (c == null) {
			return factory.getOffsetBounds(attrs);
		} else {
			Location loc = c.getLocation();
			return c.getBounds().translate(-loc.getX(), -loc.getY());
		}
	}

	public int getPortIndex(Port port) {
		return this.getInstance().getPorts().indexOf(port);
	}

	public Value getPortValue(int portIndex) {
		InstanceComponent c = comp;
		CircuitState s = context.getCircuitState();
		if (c != null && s != null) {
			return s.getValue(c.getEnd(portIndex).getLocation());
		} else {
			return Value.UNKNOWN;
		}
	}

	//
	// methods related to the circuit state
	//
	public Project getProject() {
		return context.getCircuitState().getProject();
	}

	public boolean getShowState() {
		return context.getShowState();
	}

	public long getTickCount() {
		return context.getCircuitState().getPropagator().getTickCount();
	}

	public boolean isCircuitRoot() {
		return !context.getCircuitState().isSubstate();
	}

	public boolean isPortConnected(int index) {
		Circuit circ = context.getCircuit();
		Location loc = comp.getEnd(index).getLocation();
		return circ.isConnected(loc, comp);
	}

	public boolean isPrintView() {
		return context.isPrintView();
	}

	public void setData(InstanceData value) {
		CircuitState circState = context.getCircuitState();
		if (circState == null || comp == null) {
			throw new UnsupportedOperationException(
					"setData on InstancePainter");
		} else {
			circState.setData(comp, value);
		}
	}

	void setFactory(InstanceFactory factory, AttributeSet attrs) {
		this.comp = null;
		this.factory = factory;
		this.attrs = attrs;
	}

	void setInstance(InstanceComponent value) {
		this.comp = value;
	}

	public void setPort(int portIndex, Value value, int delay) {
		throw new UnsupportedOperationException("setValue on InstancePainter");
	}

	public boolean shouldDrawColor() {
		return context.shouldDrawColor();
	}

	public void drawRoundBounds(Bounds bds, Color color) {
		context.drawRoundBounds(comp, bds, color);
	}

	public void drawRoundBounds(Color color) {
		context.drawRoundBounds(comp, color);
	}

}
