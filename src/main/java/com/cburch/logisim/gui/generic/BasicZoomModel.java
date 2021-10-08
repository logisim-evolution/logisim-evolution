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

  public BasicZoomModel(PrefMonitor<Boolean> gridPref, PrefMonitor<Double> zoomPref, List<Double> zoomOpts, CanvasPane pane) {
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
      if (canvas == null) setZoomFactor(value);
      // Attempt to maintain mouse position during zoom, using
      // [m]ax, [v]alue, [e]xtent, and [r]elative position within it,
      // to calculate target [n]ew[m]ax, [p]ercent and [n]ew[v]alue.
      final var mx = canvas.getHorizontalScrollBar().getMaximum();
      final var vx = canvas.getHorizontalScrollBar().getValue();
      final var ex = canvas.getHorizontalScrollBar().getVisibleAmount();
      final var rx = e.getX() - vx;
      final var my = canvas.getVerticalScrollBar().getMaximum();
      final var vy = canvas.getVerticalScrollBar().getValue();
      final var ey = canvas.getVerticalScrollBar().getVisibleAmount();
      final var ry = e.getY() - vy;
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
      final var nmx = mx * value / oldValue;
      final var px = (vx / mx) + (ex / mx - ex / nmx) * (rx / ex);
      final var nvx = (int) (nmx * px);
      final var nmy = my * value / oldValue;
      final var py = (vy / my) + (ey / my - ey / nmy) * (ry / ey);
      final var nvy = (int) (nmy * py);
      canvas.getHorizontalScrollBar().setValue(nvx);
      canvas.getVerticalScrollBar().setValue(nvy);
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
