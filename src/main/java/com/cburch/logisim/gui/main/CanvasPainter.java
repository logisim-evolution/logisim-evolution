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
import com.cburch.logisim.circuit.WireSet;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.CollectionUtil;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
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
    final var exceptions = proj.getCurrentCircuit().getWidthIncompatibilityData();
    if (CollectionUtil.isNullOrEmpty(exceptions)) return;

    final var fm = base.getFontMetrics(g.getFont());
    for (final var ex : exceptions) {
      final var common = ex.getCommonBitWidth();
      for (int i = 0; i < ex.size(); i++) {
        final var p = ex.getPoint(i);
        final var w = ex.getBitWidth(i);

        // ensure it hasn't already been drawn
        var drawn = false;
        for (var j = 0; j < i; j++) {
          if (ex.getPoint(j).equals(p)) {
            drawn = true;
            break;
          }
        }
        if (drawn) continue;

        // compute the caption combining all similar points
        var caption = "" + w.getWidth();
        for (var j = i + 1; j < ex.size(); j++) {
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
    var dragTool = canvas.getDragTool();
    Set<Component> hidden;
    if (dragTool == null) {
      hidden = NO_COMPONENTS;
    } else {
      hidden = dragTool.getHiddenComponents(canvas);
      if (hidden == null) hidden = NO_COMPONENTS;
    }

    // draw halo around component whose attributes we are viewing
    final var showHalo = AppPreferences.ATTRIBUTE_HALO.getBoolean();
    if (showHalo
        && haloedComponent != null
        && haloedCircuit == circ
        && !hidden.contains(haloedComponent)) {
      GraphicsUtil.switchToWidth(g, 3);
      g.setColor(Canvas.HALO_COLOR);
      final var bds = haloedComponent.getBounds(g).expand(5);
      final var width = bds.getWidth();
      final var height = bds.getHeight();
      final var a = Canvas.SQRT_2 * width;
      final var b = Canvas.SQRT_2 * height;
      g.drawOval(
          (int) Math.round(bds.getX() + width / 2.0 - a / 2.0),
          (int) Math.round(bds.getY() + height / 2.0 - b / 2.0),
          (int) Math.round(a),
          (int) Math.round(b));
      GraphicsUtil.switchToWidth(g, 1);
      g.setColor(Color.BLACK);
    }

    // draw circuit and selection
    final var circState = proj.getCircuitState();
    final var printerView = AppPreferences.PRINTER_VIEW.getBoolean();
    final var context = new ComponentDrawContext(canvas, circ, circState, base, g, printerView);
    context.setHighlightedWires(highlightedWires);
    circ.draw(context, hidden);
    sel.draw(context, hidden);

    // draw tool
    final var tool = dragTool != null ? dragTool : proj.getTool();
    if (tool != null && !canvas.isPopupMenuUp()) {
      final var gfxCopy = g.create();
      context.setGraphics(gfxCopy);
      tool.draw(canvas, context);
      gfxCopy.dispose();
    }
  }

  private void exposeHaloedComponent(Graphics gfx) {
    final var comp = haloedComponent;
    if (comp == null) return;
    final var bds = comp.getBounds(gfx).expand(7);
    final var width = bds.getWidth();
    final var height = bds.getHeight();
    final var a = Canvas.SQRT_2 * width;
    final var b = Canvas.SQRT_2 * height;
    canvas.repaint(
        (int) Math.round(bds.getX() + width / 2.0 - a / 2.0),
        (int) Math.round(bds.getY() + height / 2.0 - b / 2.0),
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

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (AppPreferences.GRID_BG_COLOR.isSource(event)
        || AppPreferences.GRID_DOT_COLOR.isSource(event)
        || AppPreferences.GRID_ZOOMED_DOT_COLOR.isSource(event)) {
      canvas.repaint();
    }
  }

  void setHaloedComponent(Circuit circ, Component comp) {
    if (comp == haloedComponent) return;
    final var g = canvas.getGraphics();
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
