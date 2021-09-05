/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
