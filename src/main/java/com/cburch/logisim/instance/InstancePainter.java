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

package com.cburch.logisim.instance;

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
import java.awt.Color;
import java.awt.Graphics;
import lombok.Setter;
import lombok.val;

public class InstancePainter implements InstanceState {
  private final ComponentDrawContext context;
  @Setter private InstanceComponent instanceComponent;
  private InstanceFactory factory;
  private AttributeSet attrs;

  public InstancePainter(ComponentDrawContext context, InstanceComponent instance) {
    this.context = context;
    this.instanceComponent = instance;
  }

  //
  // helper methods for drawing common elements in components
  //
  public void drawBounds() {
    context.drawBounds(instanceComponent);
  }

  public void drawClock(int i, Direction dir) {
    context.drawClock(instanceComponent, i, dir);
  }

  public void drawClockSymbol(int xpos, int ypos) {
    context.drawClockSymbol(instanceComponent, xpos, ypos);
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
    context.drawHandles(instanceComponent);
  }

  public void drawLabel() {
    if (instanceComponent != null) {
      instanceComponent.drawLabel(context);
    }
  }

  public void drawPort(int i) {
    context.drawPin(instanceComponent, i);
  }

  public void drawPort(int i, String label, Direction dir) {
    context.drawPin(instanceComponent, i, label, dir);
  }

  public void drawPorts() {
    context.drawPins(instanceComponent);
  }

  public void drawRectangle(Bounds bds, String label) {
    context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), label);
  }

  public void drawRectangle(int x, int y, int width, int height, String label) {
    context.drawRectangle(x, y, width, height, label);
  }

  @Override
  public void fireInvalidated() {
    instanceComponent.fireInvalidated();
  }

  @Override
  public AttributeSet getAttributeSet() {
    val c = instanceComponent;
    return c == null ? attrs : c.getAttributeSet();
  }

  @Override
  public <E> E getAttributeValue(Attribute<E> attr) {
    val as = (instanceComponent == null) ? attrs : instanceComponent.getAttributeSet();
    return as.getValue(attr);
  }

  public Bounds getBounds() {
    return (instanceComponent == null) ? factory.getOffsetBounds(attrs) : instanceComponent.getBounds();
  }

  public Circuit getCircuit() {
    return context.getCircuit();
  }

  /**
   * This medthod returns the instance data information.
   *
   * @pre it assumes that your circuit was instantiate before.
   */
  @Override
  public InstanceData getData() {
    val circState = context.getCircuitState();
    if (circState == null || instanceComponent == null)
      throw new UnsupportedOperationException("setData on InstancePainter");
    return (InstanceData) circState.getData(instanceComponent);
  }

  public CircuitState getCircuitState() {
    return context.getCircuitState();
  }

  public java.awt.Component getDestination() {
    return context.getDestination();
  }

  @Override
  public InstanceFactory getFactory() {
    return (instanceComponent == null) ? factory : (InstanceFactory) instanceComponent.getFactory();
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
  @Override
  public Instance getInstance() {
    return (instanceComponent == null) ? null : instanceComponent.getInstance();
  }

  public Location getLocation() {
    return (instanceComponent == null) ? Location.create(0, 0) : instanceComponent.getLocation();
  }

  public Bounds getOffsetBounds() {
    if (instanceComponent == null) return factory.getOffsetBounds(attrs);
    val loc = instanceComponent.getLocation();
    return instanceComponent.getBounds().translate(-loc.getX(), -loc.getY());
  }

  @Override
  public int getPortIndex(Port port) {
    return this.getInstance().getPorts().indexOf(port);
  }

  @Override
  public Value getPortValue(int portIndex) {
    val s = context.getCircuitState();
    if (instanceComponent != null && s != null) return s.getValue(instanceComponent.getEnd(portIndex).getLocation());
    return Value.UNKNOWN;
  }

  //
  // methods related to the circuit state
  //
  @Override
  public Project getProject() {
    return context.getCircuitState().getProject();
  }

  public boolean getShowState() {
    return context.getShowState();
  }

  @Override
  public int getTickCount() {
    return context.getCircuitState().getPropagator().getTickCount();
  }

  @Override
  public boolean isCircuitRoot() {
    return !context.getCircuitState().isSubstate();
  }

  @Override
  public boolean isPortConnected(int index) {
    val circ = context.getCircuit();
    val loc = instanceComponent.getEnd(index).getLocation();
    return circ.isConnected(loc, instanceComponent);
  }

  public boolean isPrintView() {
    return context.isPrintView();
  }

  @Override
  public void setData(InstanceData value) {
    val circState = context.getCircuitState();
    if (circState == null || instanceComponent == null)
      throw new UnsupportedOperationException("setData on InstancePainter");
    circState.setData(instanceComponent, value);
  }

  void setFactory(InstanceFactory factory, AttributeSet attrs) {
    this.instanceComponent = null;
    this.factory = factory;
    this.attrs = attrs;
  }

  @Override
  public void setPort(int portIndex, Value value, int delay) {
    throw new UnsupportedOperationException("setValue() on InstancePainter");
  }

  public boolean shouldDrawColor() {
    return context.shouldDrawColor();
  }

  public void drawRoundBounds(Bounds bds, Color color) {
    context.drawRoundBounds(instanceComponent, bds, color);
  }

  public void drawRoundBounds(Color color) {
    context.drawRoundBounds(instanceComponent, color);
  }
}
