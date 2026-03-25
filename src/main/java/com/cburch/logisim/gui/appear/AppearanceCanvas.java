/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelReorderAction;
import com.cburch.draw.canvas.ActionDispatcher;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.undo.UndoAction;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CanvasPaneContents;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.proj.Project;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JPopupMenu;

public class AppearanceCanvas extends Canvas implements CanvasPaneContents, ActionDispatcher {
  private static final long serialVersionUID = 1L;
  private static final int BOUNDS_BUFFER = 70;
  // pixels shown in canvas beyond outermost boundaries
  private static final int THRESH_SIZE_UPDATE = 10;
  // don't bother to update the size if it hasn't changed more than this
  private final CanvasTool selectTool;
  private final Listener listener;
  private final GridPainter grid;
  private Project proj;
  private CircuitState circuitState;
  private CanvasPane canvasPane;
  private Bounds oldPreferredSize;
  private LayoutPopupManager popupManager;

  public AppearanceCanvas(CanvasTool selectTool) {
    this.selectTool = selectTool;
    this.grid = new GridPainter(this);
    this.listener = new Listener();
    this.oldPreferredSize = null;
    setSelection(new AppearanceSelection());
    setTool(selectTool);

    CanvasModel model = super.getModel();
    if (model != null) model.addCanvasModelListener(listener);
    grid.addPropertyChangeListener(GridPainter.ZOOM_PROPERTY, listener);
  }

  static int getMaxIndex(CanvasModel model) {
    final var objects = model.getObjectsFromBottom();
    for (var i = objects.size() - 1; i >= 0; i--) {
      if (!(objects.get(i) instanceof AppearanceElement)) {
        return i;
      }
    }
    return -1;
  }

  @Override
  public void center() {
    // do nothing
  }

  private void computeSize(boolean immediate) {
    hidePopup();

    final var circState = circuitState;
    Bounds bounds =
        (circState == null)
            ? Bounds.create(0, 0, 50, 50)
            : circState.getCircuit().getAppearance().getAbsoluteBounds();
    final var width = bounds.getX() + bounds.getWidth() + BOUNDS_BUFFER;
    final var height = bounds.getY() + bounds.getHeight() + BOUNDS_BUFFER;
    Dimension dim =
        (canvasPane == null)
            ? new Dimension(width, height)
            : canvasPane.supportPreferredSize(width, height);
    if (!immediate) {
      final var old = oldPreferredSize;
      if (old != null
          && Math.abs(old.getWidth() - dim.width) < THRESH_SIZE_UPDATE
          && Math.abs(old.getHeight() - dim.height) < THRESH_SIZE_UPDATE) {
        return;
      }
    }
    oldPreferredSize = Bounds.create(0, 0, dim.width, dim.height);
    setPreferredSize(dim);
    revalidate();
  }

  @Override
  public void doAction(UndoAction canvasAction) {
    final var circuit = circuitState.getCircuit();
    if (!proj.getLogisimFile().contains(circuit)) {
      return;
    }

    if (canvasAction instanceof ModelReorderAction reorder) {
      final var max = getMaxIndex(getModel());
      final var requests = reorder.getReorderRequests();
      final var mod = new ArrayList<ReorderRequest>(requests.size());
      var changed = false;
      var movedToMax = false;
      for (final var singleRequest : requests) {
        final var obj = singleRequest.getObject();
        if (obj instanceof AppearanceElement) {
          changed = true;
        } else {
          if (singleRequest.getToIndex() > max) {
            final var from = singleRequest.getFromIndex();
            changed = true;
            movedToMax = true;
            if (from == max && !movedToMax) {
              // this change is ineffective - don't add it
            } else {
              mod.add(new ReorderRequest(obj, from, max));
            }
          } else {
            if (singleRequest.getToIndex() == max) movedToMax = true;
            mod.add(singleRequest);
          }
        }
      }
      if (changed) {
        if (mod.isEmpty()) return;
        canvasAction = new ModelReorderAction(getModel(), mod);
      }
    }

    if (canvasAction instanceof ModelAddAction addAction) {
      final var cur = addAction.getDestinationIndex();
      final var max = getMaxIndex(getModel());
      if (cur > max) {
        canvasAction = new ModelAddAction(getModel(), addAction.getObjects(), max + 1);
      }
    }

    proj.doAction(new CanvasActionAdapter(circuit, canvasAction));
  }

  Circuit getCircuit() {
    return circuitState.getCircuit();
  }

  CircuitState getCircuitState() {
    return circuitState;
  }

  GridPainter getGridPainter() {
    return grid;
  }

  @Override
  public Dimension getPreferredScrollableViewportSize() {
    return getPreferredSize();
  }

  Project getProject() {
    return proj;
  }

  @Override
  public int getScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    return canvasPane.supportScrollableBlockIncrement(visibleRect, orientation, direction);
  }

  @Override
  public boolean getScrollableTracksViewportHeight() {
    return false;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return false;
  }

  @Override
  public int getScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return canvasPane.supportScrollableUnitIncrement(visibleRect, orientation, direction);
  }

  @Override
  public double getZoomFactor() {
    return grid.getZoomFactor();
  }

  private void hidePopup() {
    final var man = popupManager;
    if (man != null) {
      man.hideCurrentPopup();
    }
  }

  @Override
  protected void paintBackground(Graphics g) {
    super.paintBackground(g);
    grid.paintGrid(g);
  }

  @Override
  protected void paintForeground(Graphics g) {
    final var zoom = grid.getZoomFactor();
    final var gfxScaled = g.create();
    if (zoom != 1.0 && zoom != 0.0 && gfxScaled instanceof Graphics2D g2d) {
      g2d.scale(zoom, zoom);
    }
    super.paintForeground(gfxScaled);
    gfxScaled.dispose();
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    repairEvent(e, grid.getZoomFactor());
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    repairEvent(e, grid.getZoomFactor());
    super.processMouseMotionEvent(e);
  }

  @Override
  public void recomputeSize() {
    computeSize(true);
    repaint();
  }

  @Override
  public void repaintCanvasCoords(int x, int y, int width, int height) {
    final var zoom = grid.getZoomFactor();
    if (zoom != 1.0) {
      x = (int) (x * zoom - 1);
      y = (int) (y * zoom - 1);
      width = (int) (width * zoom + 4);
      height = (int) (height * zoom + 4);
    }
    super.repaintCanvasCoords(x, y, width, height);
  }

  private void repairEvent(MouseEvent e, double zoom) {
    if (zoom != 1.0) {
      final var oldx = e.getX();
      final var oldy = e.getY();
      final var newx = (int) Math.round(e.getX() / zoom);
      final var newy = (int) Math.round(e.getY() / zoom);
      e.translatePoint(newx - oldx, newy - oldy);
    }
  }

  //
  // CanvasPaneContents methods
  //
  @Override
  public void setCanvasPane(CanvasPane value) {
    canvasPane = value;
    computeSize(true);
    popupManager = new LayoutPopupManager(value, this);
  }

  public void setCircuit(Project proj, CircuitState circuitState) {
    this.proj = proj;
    this.circuitState = circuitState;
    final var circuit = circuitState.getCircuit();
    setModel(circuit.getAppearance().getCustomAppearanceDrawing(), this);
  }

  @Override
  public void setModel(CanvasModel value, ActionDispatcher dispatcher) {
    final var oldModel = super.getModel();
    if (oldModel != null) {
      oldModel.removeCanvasModelListener(listener);
    }
    super.setModel(value, dispatcher);
    if (value != null) {
      value.addCanvasModelListener(listener);
    }
  }

  @Override
  public void setTool(CanvasTool value) {
    hidePopup();
    super.setTool(value);
  }

  @Override
  public JPopupMenu showPopupMenu(MouseEvent e, CanvasObject clicked) {
    double zoom = grid.getZoomFactor();
    int x = (int) Math.round(e.getX() * zoom);
    int y = (int) Math.round(e.getY() * zoom);
    if (clicked != null && getSelection().isSelected(clicked)) {
      AppearanceEditPopup popup = new AppearanceEditPopup(this);
      popup.show(this, x, y);
      return popup;
    }
    return null;
  }

  @Override
  public int snapX(int x) {
    return (x < 0)
       ? -((-x + 5) / 10 * 10)
       : (x + 5) / 10 * 10;
  }

  @Override
  public int snapY(int y) {
    return (y < 0)
       ? -((-y + 5) / 10 * 10)
       : (y + 5) / 10 * 10;
  }

  @Override
  public void toolGestureComplete(CanvasTool tool, CanvasObject created) {
    if (tool == getTool() && tool != selectTool) {
      setTool(selectTool);
      if (created != null) {
        getSelection().clearSelected();
        getSelection().setSelected(created, true);
      }
    }
  }

  private class Listener implements CanvasModelListener, PropertyChangeListener {
    @Override
    public void modelChanged(CanvasModelEvent event) {
      computeSize(false);
    }

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      final var prop = evt.getPropertyName();
      if (prop.equals(GridPainter.ZOOM_PROPERTY)) {
        final var t = getTool();
        if (t != null) {
          t.zoomFactorChanged(AppearanceCanvas.this);
        }
      }
    }
  }
}
