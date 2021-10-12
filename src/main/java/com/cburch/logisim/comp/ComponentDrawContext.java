/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.comp;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class ComponentDrawContext {
  private static final int PIN_OFFS = 2;
  private static final int PIN_RAD = 4;

  private final java.awt.Component dest;
  private final Circuit circuit;
  private final CircuitState circuitState;
  private final Graphics base;
  private Graphics g;
  private boolean showState;
  private boolean showColor;
  private final boolean printView;
  private WireSet highlightedWires;
  private final InstancePainter instancePainter;

  public ComponentDrawContext(
      java.awt.Component dest,
      Circuit circuit,
      CircuitState circuitState,
      Graphics base,
      Graphics g) {
    this(dest, circuit, circuitState, base, g, false);
  }

  public ComponentDrawContext(
      java.awt.Component dest,
      Circuit circuit,
      CircuitState circuitState,
      Graphics base,
      Graphics g,
      boolean printView) {
    this.dest = dest;
    this.circuit = circuit;
    this.circuitState = circuitState;
    this.base = base;
    this.g = g;
    this.showState = true;
    this.showColor = true;
    this.printView = printView;
    this.highlightedWires = WireSet.EMPTY;
    this.instancePainter = new InstancePainter(this, null);
  }

  //
  // helper methods
  //
  public void drawBounds(Component comp) {
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    final var bds = comp.getBounds();
    g.drawRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
  }

  public void drawClock(Component comp, int i, Direction dir) {
    final var curColor = g.getColor();
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);

    final var e = comp.getEnd(i);
    final var pt = e.getLocation();
    final var x = pt.getX();
    final var y = pt.getY();
    final var CLK_SZ = 4;
    final var CLK_SZD = CLK_SZ - 1;
    if (dir == Direction.NORTH) {
      g.drawLine(x - CLK_SZD, y - 1, x, y - CLK_SZ);
      g.drawLine(x + CLK_SZD, y - 1, x, y - CLK_SZ);
    } else if (dir == Direction.SOUTH) {
      g.drawLine(x - CLK_SZD, y + 1, x, y + CLK_SZ);
      g.drawLine(x + CLK_SZD, y + 1, x, y + CLK_SZ);
    } else if (dir == Direction.EAST) {
      g.drawLine(x + 1, y - CLK_SZD, x + CLK_SZ, y);
      g.drawLine(x + 1, y + CLK_SZD, x + CLK_SZ, y);
    } else if (dir == Direction.WEST) {
      g.drawLine(x - 1, y - CLK_SZD, x - CLK_SZ, y);
      g.drawLine(x - 1, y + CLK_SZD, x - CLK_SZ, y);
    }

    g.setColor(curColor);
    GraphicsUtil.switchToWidth(g, 1);
  }

  public void drawClockSymbol(Component comp, int xpos, int ypos) {
    GraphicsUtil.switchToWidth(g, 2);
    int[] xcoords = {xpos + 1, xpos + 8, xpos + 1};
    int[] ycoords = {ypos - 4, ypos, ypos + 4};
    g.drawPolyline(xcoords, ycoords, 3);
    GraphicsUtil.switchToWidth(g, 1);
  }

  public void drawDongle(int x, int y) {
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(x - 4, y - 4, 9, 9);
  }

  public void drawHandle(int x, int y) {
    g.setColor(Color.white);
    g.fillRect(x - 3, y - 3, 7, 7);
    g.setColor(Color.black);
    g.drawRect(x - 3, y - 3, 7, 7);
  }

  public void drawHandle(Location loc) {
    drawHandle(loc.getX(), loc.getY());
  }

  public void drawHandles(Component comp) {
    final var b = comp.getBounds(g);
    final var left = b.getX();
    final var right = left + b.getWidth();
    final var top = b.getY();
    final var bot = top + b.getHeight();
    drawHandle(right, top);
    drawHandle(left, bot);
    drawHandle(right, bot);
    drawHandle(left, top);
  }


  protected void drawPinMarker(int x, int y) {
    String appearance = AppPreferences.PinAppearance.get();
    int radius = 4;
    int offset = 2;
    switch (appearance) {
      case AppPreferences.PIN_APPEAR_DOT_MEDIUM:
        radius = 6;
        offset = 3;
        break;
      case AppPreferences.PIN_APPEAR_DOT_BIG:
        radius = 8;
        offset = 4;
        break;
      case AppPreferences.PIN_APPEAR_DOT_BIGGER:
        radius = 10;
        offset = 5;
        break;
    }
    g.fillOval(x - offset, y - offset, radius, radius);
  }

  public void drawPin(Component comp, int i) {
    final var e = comp.getEnd(i);
    final var pt = e.getLocation();
    final var curColor = g.getColor();
    g.setColor(getShowState()
            ? getCircuitState().getValue(pt).getColor()
            : Color.BLACK);
    drawPinMarker(pt.getX(), pt.getY());
    g.setColor(curColor);
  }

  public void drawPin(Component comp, int i, String label, Direction dir) {
    final var curColor = g.getColor();
    if (i < 0 || i >= comp.getEnds().size()) return;
    final var e = comp.getEnd(i);
    final var pt = e.getLocation();
    int x = pt.getX();
    int y = pt.getY();
    if (getShowState()) {
      g.setColor(getCircuitState().getValue(pt).getColor());
    } else {
      g.setColor(Color.BLACK);
    }
    drawPinMarker(x, y);
    g.setColor(curColor);
    if (dir == Direction.EAST) {
      GraphicsUtil.drawText(g, label, x + 3, y, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    } else if (dir == Direction.WEST) {
      GraphicsUtil.drawText(g, label, x - 3, y, GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);
    } else if (dir == Direction.SOUTH) {
      GraphicsUtil.drawText(g, label, x, y - 3, GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
    } else if (dir == Direction.NORTH) {
      GraphicsUtil.drawText(g, label, x, y + 3, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    }
  }

  public void drawPins(Component comp) {
    final var curColor = g.getColor();
    for (final var e : comp.getEnds()) {
      final var pt = e.getLocation();
      if (getShowState()) {
        g.setColor(getCircuitState().getValue(pt).getColor());
      } else {
        g.setColor(Color.BLACK);
      }
      drawPinMarker(pt.getX(), pt.getY());
    }
    g.setColor(curColor);
  }

  public void drawRectangle(Component comp) {
    drawRectangle(comp, "");
  }

  public void drawRectangle(Component comp, String label) {
    final var bds = comp.getBounds(g);
    drawRectangle(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), label);
  }

  public void drawRectangle(ComponentFactory source, int x, int y, AttributeSet attrs, String label) {
    final var bds = source.getOffsetBounds(attrs);
    drawRectangle(source, x + bds.getX(), y + bds.getY(), bds.getWidth(), bds.getHeight(), label);
  }

  public void drawRectangle(ComponentFactory source, int x, int y, int width, int height, String label) {
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x + 1, y + 1, width - 1, height - 1);
    if (label != null && !label.equals("")) {
      final var fm = base.getFontMetrics(g.getFont());
      final var lwid = fm.stringWidth(label);
      if (height > 20) { // centered at top edge
        g.drawString(label, x + (width - lwid) / 2, y + 2 + fm.getAscent());
      } else { // centered overall
        g.drawString(label, x + (width - lwid) / 2, y + (height + fm.getAscent()) / 2 - 1);
      }
    }
  }

  public void drawRectangle(int x, int y, int width, int height, String label) {
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x, y, width, height);
    if (label != null && !label.equals("")) {
      final var fm = base.getFontMetrics(g.getFont());
      final var lwid = fm.stringWidth(label);
      if (height > 20) { // centered at top edge
        g.drawString(label, x + (width - lwid) / 2, y + 2 + fm.getAscent());
      } else { // centered overall
        g.drawString(label, x + (width - lwid) / 2, y + (height + fm.getAscent()) / 2 - 1);
      }
    }
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public CircuitState getCircuitState() {
    return circuitState;
  }

  public java.awt.Component getDestination() {
    return dest;
  }

  public Object getGateShape() {
    return AppPreferences.GATE_SHAPE.get();
  }

  public Graphics getGraphics() {
    return g;
  }

  public WireSet getHighlightedWires() {
    return highlightedWires;
  }

  public InstancePainter getInstancePainter() {
    return instancePainter;
  }

  public boolean getShowState() {
    return !printView && showState;
  }

  public boolean isPrintView() {
    return printView;
  }

  public void setGraphics(Graphics g) {
    this.g = g;
  }

  public void setHighlightedWires(WireSet value) {
    this.highlightedWires = value == null ? WireSet.EMPTY : value;
  }

  public void setShowColor(boolean value) {
    showColor = value;
  }

  public void setShowState(boolean value) {
    showState = value;
  }

  public boolean shouldDrawColor() {
    return !printView && showColor;
  }

  public void drawRoundBounds(Component comp, Bounds bds, Color color) {
    GraphicsUtil.switchToWidth(g, 2);
    if (color != null && !color.equals(Color.WHITE)) {
      g.setColor(color);
      g.fillRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
    }
    g.setColor(Color.BLACK);
    g.drawRoundRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight(), 10, 10);
    GraphicsUtil.switchToWidth(g, 1);
  }

  public void drawRoundBounds(Component comp, Color color) {
    drawRoundBounds(comp, comp.getBounds(), color);
  }
}
