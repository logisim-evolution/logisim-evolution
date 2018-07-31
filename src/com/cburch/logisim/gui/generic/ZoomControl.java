/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.gui.generic;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;

import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.prefs.AppPreferences;

public class ZoomControl extends JPanel {
	private class GridIcon extends JComponent implements MouseListener,
			PropertyChangeListener {
		private static final long serialVersionUID = 1L;
		boolean state = true;

		public GridIcon() {
			addMouseListener(this);
			setPreferredSize(new Dimension(AppPreferences.getScaled(AppPreferences.IconSize), AppPreferences.getScaled(AppPreferences.IconSize)));
			setToolTipText("");
			setFocusable(true);
		}

		@Override
		public String getToolTipText(MouseEvent e) {
			return Strings.get("zoomShowGrid");
		}

		public void mouseClicked(MouseEvent e) {
		}

		public void mouseEntered(MouseEvent e) {
		}

		public void mouseExited(MouseEvent e) {
		}

		public void mousePressed(MouseEvent e) {
			model.setShowGrid(!state);
		}

		public void mouseReleased(MouseEvent e) {
		}

		@Override
		protected void paintComponent(Graphics g) {
			if (AppPreferences.AntiAliassing.getBoolean()) {
				Graphics2D g2 = (Graphics2D) g;
				g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
				g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
			}
			int width = getWidth();
			int height = getHeight();
			g.setColor(state ? Color.black : getBackground().darker());
			int dim = (Math.min(width, height) - 4) / 3 * 3 + 1;
			int xoff = (width - dim) / 2;
			int yoff = (height - dim) / 2;
			for (int x = 0; x < dim; x += 3) {
				for (int y = 0; y < dim; y += 3) {
					g.drawLine(x + xoff, y + yoff, x + xoff, y + yoff);
				}
			}
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

	public class SpinnerModel extends AbstractSpinnerModel implements
			PropertyChangeListener {
		private static final long serialVersionUID = 1L;

		public Object getNextValue() {
			double zoom = model.getZoomFactor();
			double[] choices = model.getZoomOptions();
			double factor = zoom * 100.0 * 1.001;
			for (int i = 0; i < choices.length; i++) {
				if (choices[i] > factor)
					return toString(choices[i]);
			}
			return null;
		}

		public Object getPreviousValue() {
			double zoom = model.getZoomFactor();
			double[] choices = model.getZoomOptions();
			double factor = zoom * 100.0 * 0.999;
			for (int i = choices.length - 1; i >= 0; i--) {
				if (choices[i] < factor)
					return toString(choices[i]);
			}
			return null;
		}

		public Object getValue() {
			double zoom = model.getZoomFactor();
			return toString(zoom * 100.0);
		}

		public void propertyChange(PropertyChangeEvent evt) {
			fireStateChanged();
		}

		public void setValue(Object value) {
			if (value instanceof String) {
				String s = (String) value;
				if (s.endsWith("%"))
					s = s.substring(0, s.length() - 1);
				s = s.trim();
				try {
					double zoom = Double.parseDouble(s) / 100.0;
					model.setZoomFactor(zoom);
				} catch (NumberFormatException e) {
				}
			}
		}

		private String toString(double factor) {
			if (factor > 10) {
				return (int) (factor + 0.5) + "%";
			} else if (factor > 0.1) {
				return (int) (factor * 100 + 0.5) / 100.0 + "%";
			} else {
				return factor + "%";
			}
		}
	}
	
	public class AutoZoomButton extends JButton implements ActionListener {
        private ZoomModel MyZoom;
        private Canvas MyCanvas;
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public AutoZoomButton(ZoomModel model, Canvas canvas) {
			MyZoom = model;
			MyCanvas = canvas;
			super.setText("Auto");
			addActionListener(this);
		}
		
		public void SetZoomModel(ZoomModel model) {
			MyZoom = model;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (MyZoom != null) {
				Graphics g = getGraphics();
				Bounds bounds;
				if (g != null)
					bounds = MyCanvas.getProject().getCurrentCircuit().getBounds(getGraphics());
				else
					bounds = MyCanvas.getProject().getCurrentCircuit().getBounds();
				if (bounds.getHeight() == 0 || bounds.getWidth() == 0) {
					return;
				}
				CanvasPane canvasPane = MyCanvas.getCanvasPane();
				if (canvasPane == null)
					return;
				// the white space around
				byte padding = 50;
				// set autozoom
				double ZoomFactor = MyZoom.getZoomFactor();
				double height = (bounds.getHeight() + 2 * padding) * ZoomFactor;
				double width = (bounds.getWidth() + 2 * padding) * ZoomFactor;
				double autozoom = ZoomFactor;
				if (canvasPane.getViewport().getSize().getWidth() / width < canvasPane.getViewport().getSize().getHeight()
						/ height) {
					autozoom *= canvasPane.getViewport().getSize().getWidth() / width;
				} else
					autozoom *= canvasPane.getViewport().getSize().getHeight() / height;
				if (Math.abs(autozoom - ZoomFactor) >= 0.01)
					MyZoom.setZoomFactor(autozoom);
			}
		}
		
	}

	public class ResetZoomButton extends JButton implements ActionListener {
        private ZoomModel MyZoom;
		
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public ResetZoomButton(ZoomModel model) {
			MyZoom = model;
			super.setText("100%");
			addActionListener(this);
		}
		
		public void SetZoomModel(ZoomModel model) {
			MyZoom = model;
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (MyZoom != null) {
				MyZoom.setZoomFactor(1.0);
			}
		}
		
	}

	private static final long serialVersionUID = 1L;

	private ZoomModel model;
	private JSpinner spinner;
	public SpinnerModel spinnerModel;
	public AutoZoomButton ZoomButton;
	public ResetZoomButton ResetButton;
	private GridIcon grid;

	public ZoomControl(ZoomModel model, Canvas canvas) {
		super(new BorderLayout());
		this.model = model;

		spinnerModel = new SpinnerModel();
		spinner = new JSpinner();
		spinner.setModel(spinnerModel);
		this.add(spinner, BorderLayout.SOUTH);

		grid = new GridIcon();
		this.add(grid, BorderLayout.EAST);
		grid.update();
		
		ZoomButton = new AutoZoomButton(model,canvas); 
		this.add(ZoomButton,BorderLayout.WEST);
		
		ResetButton = new ResetZoomButton(model);
		this.add(ResetButton,BorderLayout.CENTER);

		model.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
		model.addPropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
	}

	public void setZoomModel(ZoomModel value) {
		ZoomModel oldModel = model;
		if (oldModel != value) {
			if (oldModel != null) {
				oldModel.removePropertyChangeListener(ZoomModel.SHOW_GRID, grid);
				oldModel.removePropertyChangeListener(ZoomModel.ZOOM,
						spinnerModel);
			}
			model = value;
			spinnerModel = new SpinnerModel();
			spinner.setModel(spinnerModel);
			grid.update();
			ZoomButton.SetZoomModel(value);
			ResetButton.SetZoomModel(value);
			if (value != null) {
				value.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
				value.addPropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
			}
		}
	}
}
