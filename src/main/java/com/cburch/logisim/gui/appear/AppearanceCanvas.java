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
import com.cburch.logisim.gui.generic.ZoomModel;
import com.cburch.logisim.proj.Project;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;

public class AppearanceCanvas extends Canvas implements CanvasPaneContents, ActionDispatcher {
  private static final long serialVersionUID = 1L;
  /** Safety clamp so the camera origin cannot overflow its {@code int} representation. */
  private static final int VIEW_ORIGIN_LIMIT = 1_000_000_000;
  private static final double ZOOM_WHEEL_STEP = 1.1;
  private static final double MIN_ZOOM = 0.01;
  private static final double MAX_ZOOM = 100.0;
  private final CanvasTool selectTool;
  private final Listener listener;
  private final GridPainter grid;
  private Project proj;
  private CircuitState circuitState;
  private CanvasPane canvasPane;
  private LayoutPopupManager popupManager;
  // Infinite-canvas camera: component-pixel position where model coordinate (0,0) is drawn.
  private int viewX = 0;
  private int viewY = 0;
  private int panStartScreenX;
  private int panStartScreenY;
  private int panStartViewX;
  private int panStartViewY;
  private boolean middleButtonPanning;
  private Cursor cursorBeforeMiddleButtonPanning;
  // Screen point (component pixels) that must stay fixed while the next zoom change is applied.
  private Point zoomAnchorScreen = null;

  public AppearanceCanvas(CanvasTool selectTool) {
    this.selectTool = selectTool;
    this.grid = new GridPainter(this);
    this.listener = new Listener();
    setSelection(new AppearanceSelection());
    setTool(selectTool);
    addMouseWheelListener(this::handleWheel);

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
    final var circState = circuitState;
    final var bounds =
        (circState == null)
            ? Bounds.create(0, 0, 50, 50)
            : circState.getCircuit().getAppearance().getAbsoluteBounds();
    if (canvasPane == null || bounds.getWidth() == 0 || bounds.getHeight() == 0) {
      setViewOrigin(0, 0);
      return;
    }
    final var viewportSize = canvasPane.getViewportSize();
    final var zoom = getZoomFactor();
    final var centerX = bounds.getX() + bounds.getWidth() / 2.0;
    final var centerY = bounds.getY() + bounds.getHeight() / 2.0;
    setViewOrigin(
        (int) Math.round(viewportSize.width / 2.0 - centerX * zoom),
        (int) Math.round(viewportSize.height / 2.0 - centerY * zoom));
  }

  /** Moves the camera origin to the given component-pixel position. */
  private void setViewOrigin(final Point origin) {
    if (origin != null) setViewOrigin(origin.x, origin.y);
  }

  /** Moves the camera origin to the given component-pixel position. */
  private void setViewOrigin(final int x, final int y) {
    final var nx = Math.max(-VIEW_ORIGIN_LIMIT, Math.min(VIEW_ORIGIN_LIMIT, x));
    final var ny = Math.max(-VIEW_ORIGIN_LIMIT, Math.min(VIEW_ORIGIN_LIMIT, y));
    if (nx == viewX && ny == viewY) return;
    viewX = nx;
    viewY = ny;
    repaint();
  }

  /** Current camera origin (component pixels). */
  Point getViewOrigin() {
    return new Point(viewX, viewY);
  }

  /** Pans the view by the given component-pixel delta (positive moves the content right/down). */
  private void panBy(final int dx, final int dy) {
    setViewOrigin(viewX + dx, viewY + dy);
  }

  /** Handles mouse-wheel zoom (Ctrl) and panning (plain / Shift). */
  private void handleWheel(final MouseWheelEvent e) {
    if (e.isControlDown()) {
      final var zoom = getZoomFactor();
      repairEvent(e, zoom);
      doZoom(e.getPoint(), e.getWheelRotation() < 0);
      e.consume();
    } else {
      final int amount = e.getWheelRotation() * 2 * (int) Math.round(10 * getZoomFactor());
      if (e.isShiftDown()) {
        panBy(-amount, 0);
      } else {
        panBy(0, -amount);
      }
      e.consume();
    }
  }

  private void doZoom(final Point mouseLocation, final boolean zoomIn) {
    final var model = grid.getZoomModel();
    if (model == null || canvasPane == null) return;
    final var oldZoom = getZoomFactor();
    final var newZoom =
        Math.max(
            MIN_ZOOM,
            Math.min(MAX_ZOOM, zoomIn ? oldZoom * ZOOM_WHEEL_STEP : oldZoom / ZOOM_WHEEL_STEP));
    if (newZoom == oldZoom) return;
    if (mouseLocation != null) {
      zoomAnchorScreen =
          new Point(
              (int) Math.round(viewX + mouseLocation.x * oldZoom),
              (int) Math.round(viewY + mouseLocation.y * oldZoom));
    } else {
      zoomAnchorScreen = null;
    }
    model.setZoomFactor(newZoom);
  }

  @Override
  public void zoomFactorChanged(final ZoomModel model, final double oldZoom, final double newZoom) {
    if (canvasPane == null || oldZoom <= 0.0 || newZoom <= 0.0) return;
    final var viewportSize = canvasPane.getViewportSize();
    final var anchor = zoomAnchorScreen;
    final var screenX = (anchor != null) ? anchor.x : viewportSize.width / 2;
    final var screenY = (anchor != null) ? anchor.y : viewportSize.height / 2;
    final var circuitX = (screenX - viewX) / oldZoom;
    final var circuitY = (screenY - viewY) / oldZoom;
    zoomAnchorScreen = null;
    setViewOrigin(
        (int) Math.round(screenX - circuitX * newZoom),
        (int) Math.round(screenY - circuitY * newZoom));
  }

  private void computeSize(boolean immediate) {
    // The infinite canvas fills the viewport and pans itself, so a repaint is enough.
    repaint();
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
    // The infinite canvas fills the viewport; panning is done through the camera, not scroll bars.
    return true;
  }

  @Override
  public boolean getScrollableTracksViewportWidth() {
    return true;
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
    grid.paintGrid(g, viewX, viewY);
  }

  @Override
  protected void paintForeground(Graphics g) {
    final var zoom = grid.getZoomFactor();
    final var gfxScaled = g.create();
    if (gfxScaled instanceof Graphics2D g2d) {
      g2d.translate(viewX, viewY);
      if (zoom != 1.0 && zoom != 0.0) {
        g2d.scale(zoom, zoom);
      }
    }
    super.paintForeground(gfxScaled);
    gfxScaled.dispose();
  }

  @Override
  protected void processMouseEvent(MouseEvent e) {
    if (handleMiddleButtonPanEvent(e)) return;
    repairEvent(e, grid.getZoomFactor());
    super.processMouseEvent(e);
  }

  @Override
  protected void processMouseMotionEvent(MouseEvent e) {
    if (handleMiddleButtonPanMotion(e)) return;
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
    // Coordinates arrive in model space; translate them through the camera and zoom.
    final var zoom = grid.getZoomFactor();
    final var newX = (int) Math.floor(viewX + x * zoom);
    final var newY = (int) Math.floor(viewY + y * zoom);
    width = (int) Math.ceil(viewX + (x + width) * zoom) - newX;
    height = (int) Math.ceil(viewY + (y + height) * zoom) - newY;
    super.repaintCanvasCoords(newX, newY, width, height);
  }

  private void repairEvent(MouseEvent e, double zoom) {
    // Translate a component-pixel event into model coordinates.
    final var oldx = e.getX();
    final var oldy = e.getY();
    final var newx = (int) Math.round((oldx - viewX) / zoom);
    final var newy = (int) Math.round((oldy - viewY) / zoom);
    e.translatePoint(newx - oldx, newy - oldy);
  }

  private boolean handleMiddleButtonPanEvent(MouseEvent e) {
    if (e.getButton() == MouseEvent.BUTTON2 && e.getID() == MouseEvent.MOUSE_PRESSED) {
      if (canvasPane != null) {
        middleButtonPanning = true;
        panStartScreenX = e.getXOnScreen();
        panStartScreenY = e.getYOnScreen();
        panStartViewX = viewX;
        panStartViewY = viewY;
        cursorBeforeMiddleButtonPanning = getCursor();
        setCursor(Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
      }
      e.consume();
      return true;
    }
    if (e.getButton() == MouseEvent.BUTTON2 && e.getID() == MouseEvent.MOUSE_RELEASED) {
      middleButtonPanning = false;
      if (cursorBeforeMiddleButtonPanning != null) {
        setCursor(cursorBeforeMiddleButtonPanning);
        cursorBeforeMiddleButtonPanning = null;
      }
      e.consume();
      return true;
    }
    return false;
  }

  private boolean handleMiddleButtonPanMotion(MouseEvent e) {
    if (!middleButtonPanning) {
      return false;
    }
    setViewOrigin(
        panStartViewX + (e.getXOnScreen() - panStartScreenX),
        panStartViewY + (e.getYOnScreen() - panStartScreenY));
    e.consume();
    return true;
  }

  //
  // CanvasPaneContents methods
  //
  @Override
  public void setCanvasPane(CanvasPane value) {
    canvasPane = value;
    // The infinite canvas fills the viewport and pans itself, so scroll bars are never needed.
    canvasPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
    canvasPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_NEVER);
    computeSize(true);
    popupManager = new LayoutPopupManager(value, this);
  }

  public void setCircuit(Project proj, CircuitState circuitState) {
    this.proj = proj;
    this.circuitState = circuitState;
    final var circuit = circuitState.getCircuit();
    setModel(circuit.getAppearance().getCustomAppearanceDrawing(), this);
    SwingUtilities.invokeLater(this::center);
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
    int x = (int) Math.round(viewX + e.getX() * zoom);
    int y = (int) Math.round(viewY + e.getY() * zoom);
    if (clicked != null && getSelection().isSelected(clicked)) {
      AppearanceEditPopup popup = new AppearanceEditPopup(this);
      popup.show(this, x, y);
      return popup;
    }
    return null;
  }

  @Override
  protected int getViewOriginX() {
    return viewX;
  }

  @Override
  protected int getViewOriginY() {
    return viewY;
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
