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

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.icons.ZoomIcon;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

public class ZoomControl extends JPanel {
  private static final long serialVersionUID = 1L;
  private final ZoomLabel label;
  private final JSlider slider;
  private final GridIcon grid;
  private final Canvas canvas;
  private final JButton plus;
  private final JButton minus;
  public final AutoZoomButton zoomButton;
  public final ResetZoomButton resetButton;
  private ZoomModel model;
  private SliderModel sliderModel;

  public ZoomControl(ZoomModel model, Canvas canvas) {
    super(new BorderLayout());
    this.model = model;
    this.canvas = canvas;

    label = new ZoomLabel();
    sliderModel = new SliderModel(model);

    plus = new ZoomButton(new ZoomIcon(ZoomIcon.ZOOMIN), false);
    minus = new ZoomButton(new ZoomIcon(ZoomIcon.ZOOMOUT), true);
    slider = new JSlider(sliderModel);

    JPanel zoom = new JPanel(new BorderLayout());
    zoom.add(minus, BorderLayout.WEST);
    zoom.add(label, BorderLayout.CENTER);
    zoom.add(plus, BorderLayout.EAST);
    zoom.add(slider, BorderLayout.SOUTH);

    this.add(zoom, BorderLayout.NORTH);

    grid = new GridIcon();
    this.add(grid, BorderLayout.EAST);
    grid.update();

    zoomButton = new AutoZoomButton(model);
    this.add(zoomButton, BorderLayout.WEST);

    resetButton = new ResetZoomButton(model);
    this.add(resetButton, BorderLayout.CENTER);

    model.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
    model.addPropertyChangeListener(ZoomModel.ZOOM, sliderModel);
    model.addPropertyChangeListener(ZoomModel.ZOOM, label);
  }

  private int nearestZoomOption() {
    double[] choices = model.getZoomOptions();
    double factor = model.getZoomFactor() * 100.0;
    int closest = 0;
    for (int i = 1; i < choices.length; i++) {
      if (Math.abs(choices[i] - factor) < Math.abs(choices[closest] - factor)) closest = i;
    }
    return closest;
  }

  public String zoomString() {
    double factor = model.getZoomFactor();
    return String.format("%.0f%%", factor * 100.0);
  }

  public void zoomIn() {
    double zoom = model.getZoomFactor();
    double[] choices = model.getZoomOptions();
    double factor = zoom * 100.0 * 1.001;
    for (double choice : choices) {
      if (choice > factor) {
        model.setZoomFactor(choice / 100.0);
        return;
      }
    }
  }

  public void zoomOut() {
    double zoom = model.getZoomFactor();
    double[] choices = model.getZoomOptions();
    double factor = zoom * 100.0 * 0.999;
    for (int i = choices.length - 1; i >= 0; i--) {
      if (choices[i] < factor) {
        model.setZoomFactor(choices[i] / 100.0);
        return;
      }
    }
  }

  public void zoomTo(int i) {
    double[] choices = model.getZoomOptions();
    i = Math.max(Math.min(i, choices.length - 1), 0);
    model.setZoomFactor(choices[i] / 100.0);
  }

  public void setAutoZoomButtonEnabled(boolean val) {
    zoomButton.setEnabled(val);
    resetButton.setEnabled(val);
  }

  public void setZoomModel(ZoomModel value) {
    ZoomModel oldModel = model;
    if (oldModel != value) {
      if (oldModel != null) {
        oldModel.removePropertyChangeListener(ZoomModel.SHOW_GRID, grid);
        oldModel.removePropertyChangeListener(ZoomModel.ZOOM, sliderModel);
        oldModel.removePropertyChangeListener(ZoomModel.ZOOM, label);
      }
      model = value;
      if (value == null) {
        slider.setEnabled(false);
        zoomButton.setEnabled(false);
        resetButton.setEnabled(false);
        label.setEnabled(false);
        plus.setEnabled(false);
        minus.setEnabled(false);
      } else {
        slider.setEnabled(true);
        zoomButton.setEnabled(true);
        resetButton.setEnabled(true);
        label.setEnabled(true);
        plus.setEnabled(true);
        minus.setEnabled(true);
        sliderModel = new SliderModel(model);
        slider.setModel(sliderModel);
        grid.update();
        zoomButton.setZoomModel(value);
        resetButton.setZoomModel(value);
        value.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
        value.addPropertyChangeListener(ZoomModel.ZOOM, sliderModel);
        value.addPropertyChangeListener(ZoomModel.ZOOM, label);
        label.setText(zoomString());
      }
    }
  }

  private class GridIcon extends JComponent implements MouseListener, PropertyChangeListener {
    private static final long serialVersionUID = 1L;
    boolean state = true;

    public GridIcon() {
      addMouseListener(this);
      setPreferredSize(
          new Dimension(
              AppPreferences.getScaled(2 * AppPreferences.IconSize),
              AppPreferences.getScaled(AppPreferences.IconSize)));
      setToolTipText("");
      setFocusable(true);
    }

    @Override
    public String getToolTipText(MouseEvent e) {
      return S.get("zoomShowGrid");
    }

    public void mouseClicked(MouseEvent e) {}

    public void mouseEntered(MouseEvent e) {}

    public void mouseExited(MouseEvent e) {}

    public void mousePressed(MouseEvent e) {
      if (model != null) model.setShowGrid(!state);
    }

    public void mouseReleased(MouseEvent e) {}

    @Override
    protected void paintComponent(Graphics g) {
      if (AppPreferences.AntiAliassing.getBoolean()) {
        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(
            RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
      }
      int width = getWidth();
      int height = getHeight();
      g.setColor(state ? getBackground() : Color.BLACK);
      int three = AppPreferences.getScaled(3);
      int xdim = (width - AppPreferences.getScaled(4)) / three * three + 1;
      int ydim = (height - AppPreferences.getScaled(4)) / three * three + 1;
      int xoff = (width - xdim) / 2;
      int yoff = (height - ydim) / 2;
      for (int x = 0; x < xdim; x += three) {
        for (int y = 0; y < ydim; y += three) {
          g.drawLine(x + xoff, y + yoff, x + xoff, y + yoff);
        }
      }
      g.setColor(Color.BLACK);
      g.drawLine(xoff, yoff, xoff + three, yoff);
      g.drawLine(xoff, yoff, xoff, yoff + three);
      g.drawLine(xoff, yoff + ydim, xoff, yoff + ydim - three);
      g.drawLine(xoff, yoff + ydim, xoff + three, yoff + ydim);
      g.drawLine(xoff + xdim, yoff, xoff + xdim - three, yoff);
      g.drawLine(xoff + xdim, yoff, xoff + xdim, yoff + three);
      g.drawLine(xoff + xdim, yoff + ydim, xoff + xdim - three, yoff + ydim);
      g.drawLine(xoff + xdim, yoff + ydim, xoff + xdim, yoff + ydim - three);
    }

    public void propertyChange(PropertyChangeEvent evt) {
      update();
    }

    private void update() {
      boolean grid = model.getShowGrid();
      if (grid != state) {
        state = grid;
        repaint();
      }
    }
  }

  private class SliderModel extends DefaultBoundedRangeModel implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;

    public SliderModel(ZoomModel model) {
      super(nearestZoomOption(), 0, 0, model.getZoomOptions().length - 1);
    }

    public int getValue() {
      return nearestZoomOption();
    }

    public void setValue(int i) {
      zoomTo(i);
    }

    public void propertyChange(PropertyChangeEvent evt) {
      fireStateChanged();
    }
  }

  private class ZoomButton extends JButton {
    private static final long serialVersionUID = 1L;
    final boolean out;

    public ZoomButton(Icon icon, boolean left) {
      super(icon);
      out = left;
      setOpaque(false);
      setBackground(new java.awt.Color(0, 0, 0, 0));
      setBorderPainted(false);
      if (left) setMargin(new Insets(2, 1, 2, 0));
      else setMargin(new Insets(2, 0, 2, 1));
      addMouseListener(new ZoomMouseListener());
      addActionListener(new ZoomActionListener());
      setFocusable(false);
    }

    protected class ZoomMouseListener extends MouseAdapter {
      public void mouseEntered(MouseEvent ev) {
        setBorderPainted(true);
      }

      public void mouseExited(MouseEvent ev) {
        setBorderPainted(false);
      }
    }

    protected class ZoomActionListener implements ActionListener {
      public void actionPerformed(ActionEvent e) {
        if (out) zoomOut();
        else zoomIn();
      }
    }
  }

  private class ZoomLabel extends JLabel implements PropertyChangeListener {
    private static final long serialVersionUID = 1L;

    public ZoomLabel() {
      super(zoomString(), SwingConstants.CENTER);
    }

    public void propertyChange(PropertyChangeEvent evt) {
      update();
    }

    public void update() {
      setText(zoomString());
    }
  }

  public class AutoZoomButton extends JButton implements ActionListener {
    private static final long serialVersionUID = 1L;
    private ZoomModel zoomModel;

    public AutoZoomButton(ZoomModel model) {
      zoomModel = model;
      super.setText("Auto");
      addActionListener(this);
    }

    public void setZoomModel(ZoomModel model) {
      zoomModel = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (zoomModel != null) {
        Graphics g = getGraphics();
        Bounds bounds;
        if (canvas.getProject().getCurrentCircuit() == null) return;
        if (g != null) bounds = canvas.getProject().getCurrentCircuit().getBounds(getGraphics());
        else bounds = canvas.getProject().getCurrentCircuit().getBounds();
        if (bounds.getHeight() == 0 || bounds.getWidth() == 0) {
          return;
        }

        CanvasPane canvasPane = canvas.getCanvasPane();
        if (canvasPane == null) return;
        // the white space around
        byte padding = 50;
        // set autozoom
        double zoomFactor = zoomModel.getZoomFactor();
        double height = (bounds.getHeight() + 2 * padding) * zoomFactor;
        double width = (bounds.getWidth() + 2 * padding) * zoomFactor;
        double autozoom = zoomFactor;
        autozoom *=
            Math.min(
                canvasPane.getViewport().getSize().getWidth() / width,
                canvasPane.getViewport().getSize().getHeight() / height);
        double max = zoomModel.getZoomOptions()[zoomModel.getZoomOptions().length - 1] / 100.0;
        double min = zoomModel.getZoomOptions()[0] / 100.0;
        if (autozoom > max) autozoom = max;
        if (autozoom < min) autozoom = min;
        if (Math.abs(autozoom - zoomFactor) >= 0.01) {
          zoomModel.setZoomFactorCenter(autozoom);
        }
      }
    }
  }

  public class ResetZoomButton extends JButton implements ActionListener {
    private static final long serialVersionUID = 1L;
    private ZoomModel zoomModel;

    public ResetZoomButton(ZoomModel model) {
      zoomModel = model;
      super.setText("100%");
      addActionListener(this);
    }

    public void setZoomModel(ZoomModel model) {
      zoomModel = model;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (zoomModel != null && canvas.getProject().getCurrentCircuit() != null) {
        zoomModel.setZoomFactor(1.0);
      }
    }
  }
}
