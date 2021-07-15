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

import com.cburch.contracts.BaseComponentListenerContract;
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
  private static final long serialVersionUID = 1L;
  private final CanvasPaneContents contents;
  private final Listener listener;
  private final ZoomListener zoomListener;
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
    final var size = new Dimension();
    getViewport().getSize(size);
    return size;
  }

  public double getZoomFactor() {
    ZoomModel model = zoomModel;
    return model == null ? 1.0 : model.getZoomFactor();
  }

  public void setZoomModel(ZoomModel model) {
    final var oldModel = zoomModel;
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
    final var zoom = getZoomFactor();
    if (zoom != 1.0) {
      width = (int) Math.ceil(width * zoom);
      height = (int) Math.ceil(height * zoom);
    }
    final var minSize = getViewportSize();
    if (minSize.width > width) width = minSize.width;
    if (minSize.height > height) height = minSize.height;
    return new Dimension(width, height);
  }

  public int supportScrollableBlockIncrement(Rectangle visibleRect, int orientation, int direction) {
    final var unit = supportScrollableUnitIncrement(visibleRect, orientation, direction);
    return (direction == SwingConstants.VERTICAL)
        ? visibleRect.height / unit * unit
        : visibleRect.width / unit * unit;
  }

  public int supportScrollableUnitIncrement(Rectangle visibleRect, int orientation, int direction) {
    return (int) Math.round(10 * getZoomFactor());
  }

  private class Listener implements BaseComponentListenerContract, PropertyChangeListener {

    @Override
    public void componentHidden(ComponentEvent e) {
      // do nothing
    }

    @Override
    public void componentMoved(ComponentEvent e) {
      // do nothing
    }

    //
    // ComponentListener methods
    //
    @Override
    public void componentResized(ComponentEvent e) {
      contents.recomputeSize();
    }

    @Override
    public void propertyChange(PropertyChangeEvent e) {
      final var prop = e.getPropertyName();
      if (prop.equals(ZoomModel.ZOOM)) {
        final var oldZoom = (Double) e.getOldValue();
        var r = getViewport().getViewRect();
        final var cx = (r.x + r.width / 2) / oldZoom;
        final var cy = (r.y + r.height / 2) / oldZoom;

        final var newZoom = (Double) e.getNewValue();
        r = getViewport().getViewRect();
        final var hv = (int) (cx * newZoom) - r.width / 2;
        final var vv = (int) (cy * newZoom) - r.height / 2;
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
        var zoom = zoomModel.getZoomFactor();
        final var opts = zoomModel.getZoomOptions();
        if (mwe.getWheelRotation() < 0) { // ZOOM IN
          zoom += 0.1;
          final var max = opts.get(opts.size() - 1) / 100.0;
          zoomModel.setZoomFactor(Math.min(zoom, max), mwe);
        } else { // ZOOM OUT
          zoom -= 0.1;
          final var min = opts.get(0) / 100.0;
          zoomModel.setZoomFactor(Math.max(zoom, min), mwe);
        }
      } else if (mwe.isShiftDown()) {
        getHorizontalScrollBar().setValue(scrollValue(getHorizontalScrollBar(), mwe.getWheelRotation()));
      } else {
        getVerticalScrollBar().setValue(scrollValue(getVerticalScrollBar(), mwe.getWheelRotation()));
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
}
