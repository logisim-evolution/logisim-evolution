/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.WidthIncompatibilityData;
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Collections;
import java.util.Set;

class CanvasPainter implements PropertyChangeListener {
  private static final Set<Component> NO_COMPONENTS = Collections.emptySet();

  private final Canvas canvas;
  private final GridPainter grid;
  private Component haloedComponent = null;
  private Circuit haloedCircuit = null;
  private WireSet highlightedWires = WireSet.EMPTY;

  CanvasPainter(Canvas canvas) {
    this.canvas = canvas;
    this.grid = new GridPainter(canvas);

    AppPreferences.PRINTER_VIEW.addPropertyChangeListener(this);
    AppPreferences.ATTRIBUTE_HALO.addPropertyChangeListener(this);
    AppPreferences.CANVAS_BG_COLOR.addPropertyChangeListener(this);
    AppPreferences.GRID_BG_COLOR.addPropertyChangeListener(this);
    AppPreferences.GRID_DOT_COLOR.addPropertyChangeListener(this);
    AppPreferences.GRID_ZOOMED_DOT_COLOR.addPropertyChangeListener(this);
  }

  private void drawWidthIncompatibilityData(Graphics base, Graphics g, Project proj) {
    Set<WidthIncompatibilityData> exceptions;
    exceptions = proj.getCurrentCircuit().getWidthIncompatibilityData();
    if (exceptions == null || exceptions.size() == 0) return;

    FontMetrics fm = base.getFontMetrics(g.getFont());
    for (WidthIncompatibilityData ex : exceptions) {
      BitWidth common = ex.getCommonBitWidth();
      for (int i = 0; i < ex.size(); i++) {
        Location p = ex.getPoint(i);
        BitWidth w = ex.getBitWidth(i);

        // ensure it hasn't already been drawn
        boolean drawn = false;
        for (int j = 0; j < i; j++) {
          if (ex.getPoint(j).equals(p)) {
            drawn = true;
            break;
          }
        }
        if (drawn) continue;

        // compute the caption combining all similar points
        String caption = "" + w.getWidth();
        for (int j = i + 1; j < ex.size(); j++) {
          if (ex.getPoint(j).equals(p)) {
            caption += "/" + ex.getBitWidth(j);
            break;
          }
        }
        GraphicsUtil.switchToWidth(g, 2);
        if (common != null && !w.equals(common)) {
          g.setColor(Value.widthErrorHighlightColor);
          g.drawOval(p.getX() - 5, p.getY() - 5, 10, 10);
        }
        g.setColor(Value.widthErrorColor);
        g.drawOval(p.getX() - 4, p.getY() - 4, 8, 8);
        GraphicsUtil.switchToWidth(g, 3);
        GraphicsUtil.outlineText(
            g,
            caption,
            p.getX() + 4,
            p.getY() + 1 + fm.getAscent(),
            Value.widthErrorCaptionColor,
            common != null && !w.equals(common)
                ? Value.widthErrorHighlightColor
                : Value.widthErrorCaptionBgcolor);
      }
    }
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 1);
  }

  private void drawWithUserState(Graphics base, Graphics g, Project proj) {
    final var circ = proj.getCurrentCircuit();
    final var sel = proj.getSelection();
    Set<Component> hidden;
    var dragTool = canvas.getDragTool();
    if (dragTool == null) {
      hidden = NO_COMPONENTS;
    } else {
      hidden = dragTool.getHiddenComponents(canvas);
      if (hidden == null) hidden = NO_COMPONENTS;
    }

    // draw halo around component whose attributes we are viewing
    boolean showHalo = AppPreferences.ATTRIBUTE_HALO.getBoolean();
    if (showHalo
        && haloedComponent != null
        && haloedCircuit == circ
        && !hidden.contains(haloedComponent)) {
      GraphicsUtil.switchToWidth(g, 3);
      g.setColor(Canvas.HALO_COLOR);
      Bounds bds = haloedComponent.getBounds(g).expand(5);
      int w = bds.getWidth();
      int h = bds.getHeight();
      double a = Canvas.SQRT_2 * w;
      double b = Canvas.SQRT_2 * h;
      g.drawOval(
          (int) Math.round(bds.getX() + w / 2.0 - a / 2.0),
          (int) Math.round(bds.getY() + h / 2.0 - b / 2.0),
          (int) Math.round(a),
          (int) Math.round(b));
      GraphicsUtil.switchToWidth(g, 1);
      g.setColor(Color.BLACK);
    }

    // draw circuit and selection
    CircuitState circState = proj.getCircuitState();
    boolean printerView = AppPreferences.PRINTER_VIEW.getBoolean();
    ComponentDrawContext context =
        new ComponentDrawContext(canvas, circ, circState, base, g, printerView);
    context.setHighlightedWires(highlightedWires);
    circ.draw(context, hidden);
    sel.draw(context, hidden);

    // draw tool
    Tool tool = dragTool != null ? dragTool : proj.getTool();
    if (tool != null && !canvas.isPopupMenuUp()) {
      var gfxCopy = g.create();
      context.setGraphics(gfxCopy);
      tool.draw(canvas, context);
      gfxCopy.dispose();
    }
  }

  private void exposeHaloedComponent(Graphics g) {
    Component c = haloedComponent;
    if (c == null) return;
    Bounds bds = c.getBounds(g).expand(7);
    int w = bds.getWidth();
    int h = bds.getHeight();
    double a = Canvas.SQRT_2 * w;
    double b = Canvas.SQRT_2 * h;
    canvas.repaint(
        (int) Math.round(bds.getX() + w / 2.0 - a / 2.0),
        (int) Math.round(bds.getY() + h / 2.0 - b / 2.0),
        (int) Math.round(a),
        (int) Math.round(b));
  }

  //
  // accessor methods
  //
  GridPainter getGridPainter() {
    return grid;
  }

  Component getHaloedComponent() {
    return haloedComponent;
  }

  //
  // painting methods
  //
  void paintContents(Graphics g, Project proj) {
    var clip = g.getClipBounds();
    var size = canvas.getSize();
    final double zoomFactor = canvas.getZoomFactor();
    if (canvas.ifPaintDirtyReset() || clip == null) {
      clip = new Rectangle(0, 0, size.width, size.height);
    }

    grid.paintGrid(g);
    g.setColor(Color.black);

    var gfxScaled = g.create();
    if (zoomFactor != 1.0 && gfxScaled instanceof Graphics2D g2d) {
      g2d.scale(zoomFactor, zoomFactor);
    }
    drawWithUserState(g, gfxScaled, proj);
    drawWidthIncompatibilityData(g, gfxScaled, proj);
    var circ = proj.getCurrentCircuit();

    var circState = proj.getCircuitState();
    var ptContext = new ComponentDrawContext(canvas, circ, circState, g, gfxScaled);
    ptContext.setHighlightedWires(highlightedWires);
    gfxScaled.setColor(Color.RED);
    circState.drawOscillatingPoints(ptContext);
    gfxScaled.setColor(Color.BLUE);
    proj.getSimulator().drawStepPoints(ptContext);
    gfxScaled.setColor(Color.MAGENTA); // fixme
    proj.getSimulator().drawPendingInputs(ptContext);
    gfxScaled.dispose();
  }

  @java.lang.Override
  public void propertyChange(PropertyChangeEvent event) {
    if (AppPreferences.GRID_BG_COLOR.isSource(event)
        || AppPreferences.GRID_DOT_COLOR.isSource(event)
        || AppPreferences.GRID_ZOOMED_DOT_COLOR.isSource(event)) {
      canvas.repaint();
    }
  }

  void setHaloedComponent(Circuit circ, Component comp) {
    if (comp == haloedComponent) return;
    Graphics g = canvas.getGraphics();
    exposeHaloedComponent(g);
    haloedCircuit = circ;
    haloedComponent = comp;
    exposeHaloedComponent(g);
  }

  //
  // mutator methods
  //
  void setHighlightedWires(WireSet value) {
    highlightedWires = value == null ? WireSet.EMPTY : value;
  }
}
