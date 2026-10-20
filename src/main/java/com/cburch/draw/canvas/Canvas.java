/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.undo.UndoAction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;

public class Canvas extends JComponent {
  public static final String TOOL_PROPERTY = "tool";
  public static final String MODEL_PROPERTY = "model";
  private static final long serialVersionUID = 1L;
  private final CanvasListener listener;
  private CanvasModel model;
  private ActionDispatcher dispatcher;
  private Selection selection;
  private Location tooltipLocation;
  private String tooltipName;

  public Canvas() {
    model = null;
    listener = new CanvasListener(this);
    selection = new Selection();

    addMouseListener(listener);
    addMouseMotionListener(listener);
    addKeyListener(listener);
    setPreferredSize(new Dimension(200, 200));
  }

  public void doAction(UndoAction action) {
    dispatcher.doAction(action);
  }

  public CanvasModel getModel() {
    return model;
  }

  public Selection getSelection() {
    return selection;
  }

  protected void setSelection(Selection value) {
    selection = value;
    repaint();
  }

  public CanvasTool getTool() {
    return listener.getTool();
  }

  public void setTool(CanvasTool value) {
    CanvasTool oldValue = listener.getTool();
    if (value != oldValue) {
      listener.setTool(value);
      firePropertyChange(TOOL_PROPERTY, oldValue, value);
    }
  }

  // FIXME: if this is **must** be overriden (as per code comment), then we need to turn this into
  // abstract class.
  public double getZoomFactor() {
    return 1.0; // subclass will have to override this
  }

  protected void paintBackground(Graphics g) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      final var g2 = (Graphics2D) g;
      g2.setRenderingHint(
          RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.clearRect(0, 0, getWidth(), getHeight());
  }

  @Override
  public void paintComponent(Graphics g) {
    paintBackground(g);
    paintForeground(g);
    paintTooltip(g);
  }

  public void setTooltip(Location loc, String name) {
    tooltipLocation = loc;
    tooltipName = name;
  }

  private void paintTooltip(Graphics g) {
    if (tooltipLocation == null || tooltipName == null) return;
    g.setColor(Color.YELLOW);
    final var x = (int) (tooltipLocation.getX() * getZoomFactor());
    final var y = (int) (tooltipLocation.getY() * getZoomFactor());
    final var width =
        (int) ((tooltipName.length() * DrawAttr.FIXED_FONT_CHAR_WIDTH) * getZoomFactor());
    final var height =
        (int) ((DrawAttr.FIXED_FONT_HEIGHT + DrawAttr.FIXED_FONT_HEIGHT >> 1) * getZoomFactor());
    g.fillRect(x, y, width, height);
    g.setColor(Color.BLUE);
    g.setFont(
        DrawAttr.DEFAULT_FIXED_PICH_FONT.deriveFont(
            (float) (getZoomFactor() * DrawAttr.FIXED_FONT_HEIGHT)));
    GraphicsUtil.drawText(g, tooltipName, x + 2, y, GraphicsUtil.H_LEFT, GraphicsUtil.V_TOP);
  }

  protected void paintForeground(Graphics g) {
    final var canvasModel = this.model;
    final var tool = listener.getTool();
    if (canvasModel != null) {
      final var dup = g.create();
      canvasModel.paint(g, selection);
      dup.dispose();
    }
    if (tool != null) {
      var dup = g.create();
      tool.draw(this, dup);
      dup.dispose();
    }
  }

  public void repaintCanvasCoords(int x, int y, int width, int height) {
    repaint(x, y, width, height);
  }

  public void setModel(CanvasModel value, ActionDispatcher dispatcher) {
    final var oldValue = model;
    if (oldValue != null) {
      if (!oldValue.equals(value)) {
        oldValue.removeCanvasModelListener(listener);
      }
    }
    model = value;
    this.dispatcher = dispatcher;
    if (value != null) {
      value.addCanvasModelListener(listener);
    }

    selection.clearSelected();
    repaint();
    firePropertyChange(MODEL_PROPERTY, oldValue, value);
  }

  protected JPopupMenu showPopupMenu(MouseEvent e, CanvasObject clicked) {
    return null; // subclass will override if it supports popup menus
  }

  // FIXME: if this is **must** be overriden (as per code comment), then we need to turn this into
  // abstract class.
  public int snapX(int x) {
    return x; // subclass will have to override this
  }

  // FIXME: if this is **must** be overriden (as per code comment), then we need to turn this into
  // abstract class.
  public int snapY(int y) {
    return y; // subclass will have to override this
  }

  public void toolGestureComplete(CanvasTool tool, CanvasObject created) {
    // nothing to do - subclass may override
  }
}
