/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by Logisim-evolution developers
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
import java.util.ArrayList;
import javax.swing.SwingUtilities;

public class BasicZoomModel implements ZoomModel {
  private final ArrayList<Double> zoomOptions;

  private final PropertyChangeSupport support;
  private final CanvasPane canvas;
  private double zoomFactor;
  private boolean showGrid;

  public BasicZoomModel(
      PrefMonitor<Boolean> gridPref,
      PrefMonitor<Double> zoomPref,
      ArrayList<Double> zoomOpts,
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
  public ArrayList<Double> getZoomOptions() {
    return zoomOptions;
  }

  @Override
  public void setZoomFactor(double value) {
    double oldValue = zoomFactor;
    if (value != oldValue) {
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
    }
  }

  @Override
  public void setZoomFactor(double value, MouseEvent e) {
    double oldValue = zoomFactor;
    if (value != oldValue) {
      if (canvas == null) setZoomFactor(value);
      // Attempt to maintain mouse position during zoom, using
      // [m]ax, [v]alue, [e]xtent, and [r]elative position within it,
      // to calculate target [n]ew[m]ax, [p]ercent and [n]ew[v]alue.
      double mx = canvas.getHorizontalScrollBar().getMaximum();
      int vx = canvas.getHorizontalScrollBar().getValue();
      double ex = canvas.getHorizontalScrollBar().getVisibleAmount();
      int rx = e.getX() - vx;
      double my = canvas.getVerticalScrollBar().getMaximum();
      int vy = canvas.getVerticalScrollBar().getValue();
      double ey = canvas.getVerticalScrollBar().getVisibleAmount();
      int ry = e.getY() - vy;
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
      double nmx = mx * value / oldValue;
      double px = (vx / mx) + (ex / mx - ex / nmx) * (rx / ex);
      int nvx = (int) (nmx * px);
      double nmy = my * value / oldValue;
      double py = (vy / my) + (ey / my - ey / nmy) * (ry / ey);
      int nvy = (int) (nmy * py);
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
    double oldValue = zoomFactor;
    if (value != oldValue) {
      zoomFactor = value;
      support.firePropertyChange(ZoomModel.ZOOM, oldValue, value);
      SwingUtilities.invokeLater(
          () -> support.firePropertyChange(ZoomModel.CENTER, oldValue, value));
    }
  }
}
