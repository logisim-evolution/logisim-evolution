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

public class InstancePainter implements InstanceState {
  private final ComponentDrawContext context;
  private InstanceComponent comp;
  private InstanceFactory factory;
  private AttributeSet attrs;

  public InstancePainter(ComponentDrawContext context, InstanceComponent instance) {
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
    context.drawRectangle(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), label);
  }

  public void drawRectangle(int x, int y, int width, int height, String label) {
    context.drawRectangle(x, y, width, height, label);
  }

  @Override
  public void fireInvalidated() {
    comp.fireInvalidated();
  }

  @Override
  public AttributeSet getAttributeSet() {
    return comp == null ? attrs : comp.getAttributeSet();
  }

  @Override
  public <E> E getAttributeValue(Attribute<E> attr) {
    final var as = (comp == null) ? attrs : comp.getAttributeSet();
    return as.getValue(attr);
  }

  public Bounds getBounds() {
    return comp == null ? factory.getOffsetBounds(attrs) : comp.getBounds();
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
    final var circState = context.getCircuitState();
    if (circState == null || comp == null) {
      throw new UnsupportedOperationException("setData on InstancePainter");
    }
    return (InstanceData) circState.getData(comp);
  }

  public CircuitState getCircuitState() {
    return context.getCircuitState();
  }

  public java.awt.Component getDestination() {
    return context.getDestination();
  }

  @Override
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
  @Override
  public Instance getInstance() {
    return comp == null ? null : comp.getInstance();
  }

  public Location getLocation() {
    return comp == null ? Location.create(0, 0, false) : comp.getLocation();
  }

  public Bounds getOffsetBounds() {
    if (comp == null) return factory.getOffsetBounds(attrs);

    final var loc = comp.getLocation();
    return comp.getBounds().translate(-loc.getX(), -loc.getY());
  }

  @Override
  public int getPortIndex(Port port) {
    return this.getInstance().getPorts().indexOf(port);
  }

  @Override
  public Value getPortValue(int portIndex) {
    final var s = context.getCircuitState();
    return (comp != null && s != null)
        ? s.getValue(comp.getEnd(portIndex).getLocation())
        : Value.UNKNOWN;
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
    final var loc = comp.getEnd(index).getLocation();
    return context.getCircuit().isConnected(loc, comp);
  }

  public boolean isPrintView() {
    return context.isPrintView();
  }

  @Override
  public void setData(InstanceData value) {
    final var circState = context.getCircuitState();
    if (circState == null || comp == null) {
      throw new UnsupportedOperationException("setData on InstancePainter");
    }
    circState.setData(comp, value);
  }

  public CircuitState createCircuitSubstateFor(Circuit circ) {
    CircuitState circState = context.getCircuitState();
    if (circState == null || comp == null) {
      throw new UnsupportedOperationException("createCircuitSubstateFor on InstancePainter");
    }
    return circState.createCircuitSubstateFor(comp, circ);
  }


  void setFactory(InstanceFactory factory, AttributeSet attrs) {
    this.comp = null;
    this.factory = factory;
    this.attrs = attrs;
  }

  void setInstance(InstanceComponent value) {
    this.comp = value;
  }

  @Override
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
