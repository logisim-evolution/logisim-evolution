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

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

public class GridPainter implements PropertyChangeListener {
  public static final String ZOOM_PROPERTY = "zoom";
  public static final String SHOW_GRID_PROPERTY = "showgrid";
  private final Component destination;
  private final PropertyChangeSupport support;
  private final int gridSize = 10;
  private Listener listener;
  private ZoomModel zoomModel;
  private boolean showGrid = true;
  private double zoomFactor = 1.0;
  private Image gridImage;
  private int gridImageWidth;

  public GridPainter(Component destination) {
    this.destination = destination;
    support = new PropertyChangeSupport(this);
    createGridImage(gridSize, zoomFactor);

    AppPreferences.GRID_BG_COLOR.addPropertyChangeListener(this);
    AppPreferences.GRID_DOT_COLOR.addPropertyChangeListener(this);
    AppPreferences.GRID_ZOOMED_DOT_COLOR.addPropertyChangeListener(this);
  }

  public void addPropertyChangeListener(String prop, PropertyChangeListener listener) {
    support.addPropertyChangeListener(prop, listener);
  }

  public boolean getShowGrid() {
    return showGrid;
  }

  public void setShowGrid(boolean value) {
    if (showGrid != value) {
      showGrid = value;
      support.firePropertyChange(SHOW_GRID_PROPERTY, !value, value);
    }
  }

  public double getZoomFactor() {
    return zoomFactor;
  }

  public void setZoomFactor(double value) {
    double oldValue = zoomFactor;
    if (oldValue != value) {
      zoomFactor = value;
      createGridImage(gridSize, value);
      support.firePropertyChange(ZOOM_PROPERTY, oldValue, value);
    }
  }

  public ZoomModel getZoomModel() {
    return zoomModel;
  }

  public void setZoomModel(ZoomModel model) {
    ZoomModel old = zoomModel;
    if (model != old) {
      if (listener == null) {
        listener = new Listener();
      }
      if (old != null) {
        old.removePropertyChangeListener(ZoomModel.ZOOM, listener);
        old.removePropertyChangeListener(ZoomModel.SHOW_GRID, listener);
      }
      zoomModel = model;
      if (model != null) {
        model.addPropertyChangeListener(ZoomModel.ZOOM, listener);
        model.addPropertyChangeListener(ZoomModel.SHOW_GRID, listener);
      }
      setShowGrid(model.getShowGrid());
      setZoomFactor(model.getZoomFactor());
      destination.repaint();
    }
  }

  public void paintGrid(Graphics g) {
    if (!showGrid) return;

    Rectangle clip = g.getClipBounds();
    int x0 = (clip.x / gridImageWidth) * gridImageWidth; // round down to multiple of w
    int y0 = (clip.y / gridImageWidth) * gridImageWidth;
    for (int x = 0; x < clip.width + gridImageWidth; x += gridImageWidth) {
      for (int y = 0; y < clip.height + gridImageWidth; y += gridImageWidth) {
        g.drawImage(gridImage, x0 + x, y0 + y, destination);
      }
    }
  }

  public void removePropertyChangeListener(String prop, PropertyChangeListener listener) {
    support.removePropertyChangeListener(prop, listener);
  }

  private void createGridImage(int size, double f) {
    double ww = f * size * 5;
    while (2 * ww < 150) ww *= 2;
    int w = (int) Math.round(ww);
    int[] pix = new int[w * w];
    Arrays.fill(pix, AppPreferences.GRID_BG_COLOR.get());

    if (f == 1.0) {
      int lineStep = size * w;
      for (int j = 0; j < pix.length; j += lineStep) {
        for (int i = 0; i < w; i += size) {
          pix[i + j] = AppPreferences.GRID_DOT_COLOR.get();
        }
      }
    } else {
      int off0 = 0;
      int off1 = 1;
      if (f >= 2.0) { // we'll draw several pixels for each grid point
        int num = (int) (f + 0.001);
        off0 = -(num / 2);
        off1 = off0 + num;
      }

      int dotColor =
          f <= 0.5
              ? AppPreferences.GRID_ZOOMED_DOT_COLOR.get()
              : AppPreferences.GRID_DOT_COLOR.get();
      for (int j = 0; true; j += size) {
        int y = (int) Math.round(f * j);
        if (y + off0 >= w) break;

        for (int yo = y + off0; yo < y + off1; yo++) {
          if (yo >= 0 && yo < w) {
            int base = yo * w;
            for (int i = 0; true; i += size) {
              int x = (int) Math.round(f * i);
              if (x + off0 >= w) break;
              for (int xo = x + off0; xo < x + off1; xo++) {
                if (xo >= 0 && xo < w) {
                  pix[base + xo] = dotColor;
                }
              }
            }
          }
        }
      }
      if (f <= 0.5) { // repaint over every 5th pixel so it is darker
        int size5 = size * 5;
        for (int j = 0; true; j += size5) {
          int y = (int) Math.round(f * j);
          if (y >= w) break;
          y *= w;

          for (int i = 0; true; i += size5) {
            int x = (int) Math.round(f * i);
            if (x >= w) break;
            pix[y + x] = AppPreferences.GRID_DOT_COLOR.get();
          }
        }
      }
    }
    gridImage = destination.createImage(new MemoryImageSource(w, w, pix, 0, w));
    gridImageWidth = w;
  }

  @Override
  public void propertyChange(PropertyChangeEvent event) {
    if (AppPreferences.GRID_BG_COLOR.isSource(event)
        || AppPreferences.GRID_DOT_COLOR.isSource(event)
        || AppPreferences.GRID_ZOOMED_DOT_COLOR.isSource(event)) {
      createGridImage(gridSize, zoomFactor);
    }
  }

  private class Listener implements PropertyChangeListener {
    public void propertyChange(PropertyChangeEvent event) {
      String prop = event.getPropertyName();
      Object val = event.getNewValue();
      if (prop.equals(ZoomModel.ZOOM)) {
        setZoomFactor((Double) val);
        destination.repaint();
      } else if (prop.equals(ZoomModel.SHOW_GRID)) {
        setShowGrid((Boolean) val);
        destination.repaint();
      }
    }
  }



}
