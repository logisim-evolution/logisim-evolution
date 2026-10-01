/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.prefs.PrefMonitor;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.List;
import javax.swing.SwingUtilities;

public class BasicZoomModel implements ZoomModel {
  private final List<Double> zoomOptions;

  private final PropertyChangeSupport support;
  private final CanvasPane canvas;
  private double zoomFactor;
  private boolean showGrid;

  public BasicZoomModel(
      PrefMonitor<Boolean> gridPref,
      PrefMonitor<Double> zoomPref,
      List<Double> zoomOpts,
      CanvasPane pane) {
    zoomOptions = zoomOpts;
    support = new PropertyChangeSupport(this);
    zoomFactor = 1.0;
    showGrid = true;
    canvas = pane;

    setZoomFactor(zoomPref.get());
    setShowGrid(gridPref.getBoolean());
  }

  @Override
  public void addPropertyChangeListener(String prop, PropertyChangeListener l) {
    support.addPropertyChangeListener(prop, l);
  }

  @Override
  public boolean getShowGrid() {
    return showGrid;
  }

  @Override
  public void setShowGrid(boolean value) {
    if (value != showGrid) {
      showGrid = value;
      support.firePropertyChange(ZoomModel.SHOW_GRID, !value, value);
    }
  }

  @Override
  public double getZoomFactor() {
    return zoomFactor;
  }

  @Override
  public List<Double> getZoomOptions() {
    return zoomOptions;
  }

  @Override
  public void setZoomFactor(double value) {
    final var oldValue = zoomFactor;
    if (value != oldValue) {
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
    }
  }

  @Override
  public void setZoomFactor(double value, MouseEvent e) {
    final var oldValue = zoomFactor;
    if (value != oldValue) {
      if (canvas == null) {
        setZoomFactor(value);
        return;
      }
      final var viewport = canvas.getViewport();
      // Capture the cursor position before zoom listeners resize and scroll the canvas.
      final var cursor = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), viewport);
      final var position = viewport.getViewPosition();
      final var ratio = value / oldValue;
      final var nextX = (int) Math.round((position.x + cursor.x) * ratio - cursor.x);
      final var nextY = (int) Math.round((position.y + cursor.y) * ratio - cursor.y);

      setZoomFactor(value);
      // Update the scroll ranges before applying the position anchored at the cursor.
      viewport.doLayout();
      canvas.getHorizontalScrollBar().setValue(nextX);
      canvas.getVerticalScrollBar().setValue(nextY);
    }
  }

  @Override
  public void removePropertyChangeListener(String prop, PropertyChangeListener l) {
    support.removePropertyChangeListener(prop, l);
  }

  @Override
  public void setZoomFactorCenter(double value) {
    final var oldValue = zoomFactor;
    if (value != oldValue) {
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
      SwingUtilities.invokeLater(
          () -> support.firePropertyChange(ZoomModel.CENTER, oldValue, value));
    }
  }
}
