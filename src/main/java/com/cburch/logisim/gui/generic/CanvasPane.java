/**
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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;

public class CanvasPane extends JScrollPane {
  private class Listener implements ComponentListener, PropertyChangeListener {

    public void componentHidden(ComponentEvent e) {}

    public void componentMoved(ComponentEvent e) {}

    //
    // ComponentListener methods
    //
    public void componentResized(ComponentEvent e) {
      contents.recomputeSize();
    }

    public void componentShown(ComponentEvent e) {}

    public void propertyChange(PropertyChangeEvent e) {
      String prop = e.getPropertyName();
      if (prop.equals(ZoomModel.ZOOM)) {
        double oldZoom = ((Double) e.getOldValue()).doubleValue();
        Rectangle r = getViewport().getViewRect();
        double cx = (r.x + r.width / 2) / oldZoom;
        double cy = (r.y + r.height / 2) / oldZoom;

        double newZoom = ((Double) e.getNewValue()).doubleValue();
        r = getViewport().getViewRect();
        int hv = (int) (cx * newZoom) - r.width / 2;
        int vv = (int) (cy * newZoom) - r.height / 2;
        getHorizontalScrollBar().setValue(hv);
        getVerticalScrollBar().setValue(vv);
        contents.recomputeSize();
      } else if (prop.equals(ZoomModel.CENTER)) {
        contents.center();
      }
    }
  }

  private class ZoomListener implements MouseWheelListener {
    @Override
    public void mouseWheelMoved(MouseWheelEvent mwe) {
      if (mwe.isControlDown()) {
        double zoom = zoomModel.getZoomFactor();
        double opts[] = zoomModel.getZoomOptions();
        if (mwe.getWheelRotation() < 0) { // ZOOM IN
          zoom += 0.1;
          double max = opts[opts.length - 1] / 100.0;
          zoomModel.setZoomFactor(zoom >= max ? max : zoom, mwe);
        } else { // ZOOM OUT
          zoom -= 0.1;
          double min = opts[0] / 100.0;
          zoomModel.setZoomFactor(zoom <= min ? min : zoom, mwe);
        }
      } else if (mwe.isShiftDown()) {
        getHorizontalScrollBar()
            .setValue(scrollValue(getHorizontalScrollBar(), mwe.getWheelRotation()));
      } else {
        getVerticalScrollBar()
            .setValue(scrollValue(getVerticalScrollBar(), mwe.getWheelRotation()));
      }
    }

    private int scrollValue(JScrollBar bar, int val) {
      if (val > 0) {
        if (bar.getValue() < bar.getMaximum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      } else {
        if (bar.getValue() > bar.getMinimum() + val * 2 * bar.getBlockIncrement()) {
          return bar.getValue() + val * 2 * bar.getBlockIncrement();
        }
      }
      return 0;
    }
  }

  private static final long serialVersionUID = 1L;

  private CanvasPaneContents contents;
  private Listener listener;
  private ZoomListener zoomListener;
  private ZoomModel zoomModel;

  public CanvasPane(CanvasPaneContents contents) {
    super((Component) contents);
    this.contents = contents;
    this.listener = new Listener();
    this.zoomListener = new ZoomListener();
    this.zoomModel = null;
    addComponentListener(listener);
    setWheelScrollingEnabled(false);
    addMouseWheelListener(zoomListener);
    contents.setCanvasPane(this);
  }

  public Dimension getViewportSize() {
    Dimension size = new Dimension();
    getViewport().getSize(size);
    return size;
  }

  public double getZoomFactor() {
    ZoomModel model = zoomModel;
    return model == null ? 1.0 : model.getZoomFactor();
  }

  public void setZoomModel(ZoomModel model) {
    ZoomModel oldModel = zoomModel;
    if (oldModel != null) {
      oldModel.removePropertyChangeListener(ZoomModel.ZOOM, listener);
      oldModel.removePropertyChangeListener(ZoomModel.CENTER, listener);
    }
    zoomModel = model;
    if (model != null) {
      model.addPropertyChangeListener(ZoomModel.ZOOM, listener);
      model.addPropertyChangeListener(ZoomModel.CENTER, listener);
    }
  }

  public Dimension supportPreferredSize(int width, int height) {
    double zoom = getZoomFactor();
    if (zoom != 1.0) {
      width = (int) Math.ceil(width * zoom);
      height = (int) Math.ceil(height * zoom);
    }
    Dimension minSize = getViewportSize();
    if (minSize.width > width) width = minSize.width;
    if (minSize.height > height) height = minSize.height;
    return new Dimension(width, height);
  }

  public int supportScrollableBlockIncrement(
      Rectangle visibleRect, int orientation, int direction) {
    int unit = supportScrollableUnitIncrement(visibleRect, orientation, direction);
    if (direction == SwingConstants.VERTICAL) {
      return visibleRect.height / unit * unit;
    } else {
      return visibleRect.width / unit * unit;
    }
  }

  public int supportScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    double zoom = getZoomFactor();
    return (int) Math.round(10 * zoom);
  }
}
