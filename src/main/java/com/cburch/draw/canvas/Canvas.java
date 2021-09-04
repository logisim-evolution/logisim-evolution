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
import com.cburch.draw.undo.Action;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.MouseEvent;
import javax.swing.JComponent;
import javax.swing.JPopupMenu;
import lombok.Getter;
import lombok.val;

public class Canvas extends JComponent {
  public static final String TOOL_PROPERTY = "tool";
  public static final String MODEL_PROPERTY = "model";
  private static final long serialVersionUID = 1L;
  private final CanvasListener listener;
  @Getter private CanvasModel model;
  private ActionDispatcher dispatcher;
  @Getter private Selection selection;

  public Canvas() {
    model = null;
    listener = new CanvasListener(this);
    selection = new Selection();

    addMouseListener(listener);
    addMouseMotionListener(listener);
    addKeyListener(listener);
    setPreferredSize(new Dimension(200, 200));
  }

  public void doAction(Action action) {
    dispatcher.doAction(action);
  }

  protected void setSelection(Selection value) {
    selection = value;
    repaint();
  }

  public CanvasTool getTool() {
    return listener.getTool();
  }

  public void setTool(CanvasTool value) {
    val oldValue = listener.getTool();
    if (value != oldValue) {
      listener.setTool(value);
      firePropertyChange(TOOL_PROPERTY, oldValue, value);
    }
  }

  public double getZoomFactor() {
    return 1.0; // subclass will have to override this
  }

  protected void paintBackground(Graphics g) {
    if (AppPreferences.AntiAliassing.getBoolean()) {
      val g2 = (Graphics2D) g;
      g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
      g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
    }
    g.clearRect(0, 0, getWidth(), getHeight());
  }

  @Override
  public void paintComponent(Graphics g) {
    paintBackground(g);
    paintForeground(g);
  }

  protected void paintForeground(Graphics g) {
    val canvasModel = this.model;
    val tool = listener.getTool();
    if (canvasModel != null) {
      val dup = g.create();
      canvasModel.paint(g, selection);
      dup.dispose();
    }
    if (tool != null) {
      val dup = g.create();
      tool.draw(this, dup);
      dup.dispose();
    }
  }

  public void repaintCanvasCoords(int x, int y, int width, int height) {
    repaint(x, y, width, height);
  }

  public void setModel(CanvasModel value, ActionDispatcher dispatcher) {
    val oldValue = model;
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

  public int snapX(int x) {
    return x; // subclass will have to override this
  }

  public int snapY(int y) {
    return y; // subclass will have to override this
  }

  public void toolGestureComplete(CanvasTool tool, CanvasObject created) {
    // nothing to do - subclass may override
  }
}
