/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
  @Setter private InstanceComponent instance;
  private InstanceFactory factory;
  private AttributeSet attrs;

  public InstancePainter(ComponentDrawContext context, InstanceComponent instance) {
    this.context = context;
    this.instance = instance;
  }

  //
  // helper methods for drawing common elements in components
  //
  public void drawBounds() {
    context.drawBounds(instance);
  }

  public void drawClock(int i, Direction dir) {
    context.drawClock(instance, i, dir);
  }

  public void drawClockSymbol(int xpos, int ypos) {
    context.drawClockSymbol(instance, xpos, ypos);
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
    context.drawHandles(instance);
  }

  public void drawLabel() {
    if (instance != null) {
      instance.drawLabel(context);
    }
  }

  public void drawPort(int i) {
    context.drawPin(instance, i);
  }

  public void drawPort(int i, String label, Direction dir) {
    context.drawPin(instance, i, label, dir);
  }

  public void drawPorts() {
    context.drawPins(instance);
  }

  public void drawRectangle(Bounds bds, String label) {
    context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), label);
  }

  public void drawRectangle(int x, int y, int width, int height, String label) {
    context.drawRectangle(x, y, width, height, label);
  }

  @Override
  public void fireInvalidated() {
    instance.fireInvalidated();
  }

  @Override
  public AttributeSet getAttributeSet() {
    val c = instance;
    return c == null ? attrs : c.getAttributeSet();
  }

  @Override
  public <E> E getAttributeValue(Attribute<E> attr) {
    val as = (instance == null) ? attrs : instance.getAttributeSet();
    return as.getValue(attr);
  }

  public Bounds getBounds() {
    return (instance == null) ? factory.getOffsetBounds(attrs) : instance.getBounds();
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
    if (circState == null || instance == null)
      throw new UnsupportedOperationException("setData on InstancePainter");
    return (InstanceData) circState.getData(instance);
  }

  public CircuitState getCircuitState() {
    return context.getCircuitState();
  }

  public java.awt.Component getDestination() {
    return context.getDestination();
  }

  @Override
  public InstanceFactory getFactory() {
    return (instance == null) ? factory : (InstanceFactory) instance.getFactory();
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
    return (instance == null) ? null : instance.getInstance();
  }

  public Location getLocation() {
    return (instance == null) ? Location.create(0, 0) : instance.getLocation();
  }

  public Bounds getOffsetBounds() {
    if (instance == null) return factory.getOffsetBounds(attrs);
    val loc = instance.getLocation();
    return instance.getBounds().translate(-loc.getX(), -loc.getY());
  }

  @Override
  public int getPortIndex(Port port) {
    return this.getInstance().getPorts().indexOf(port);
  }

  @Override
  public Value getPortValue(int portIndex) {
    val s = context.getCircuitState();
    if (instance != null && s != null) return s.getValue(instance.getEnd(portIndex).getLocation());
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
    val loc = instance.getEnd(index).getLocation();
    return circ.isConnected(loc, instance);
  }

  public boolean isPrintView() {
    return context.isPrintView();
  }

  @Override
  public void setData(InstanceData value) {
    val circState = context.getCircuitState();
    if (circState == null || instance == null)
      throw new UnsupportedOperationException("setData on InstancePainter");
    circState.setData(instance, value);
  }

  void setFactory(InstanceFactory factory, AttributeSet attrs) {
    this.instance = null;
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
    context.drawRoundBounds(instance, bds, color);
  }

  public void drawRoundBounds(Color color) {
    context.drawRoundBounds(instance, color);
  }
}
