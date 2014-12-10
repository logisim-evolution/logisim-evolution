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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractSpinnerModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JSpinner;

public class ZoomControl extends JPanel {
	private class GridIcon extends JComponent implements MouseListener,
			PropertyChangeListener {
		private static final long serialVersionUID = 1L;
		boolean state = true;

		public GridIcon() {
			addMouseListener(this);
			setPreferredSize(new Dimension(15, 15));
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

	private class SpinnerModel extends AbstractSpinnerModel implements
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

	private static final long serialVersionUID = 1L;

	private ZoomModel model;
	private JSpinner spinner;
	private SpinnerModel spinnerModel;
	private GridIcon grid;

	public ZoomControl(ZoomModel model) {
		super(new BorderLayout());
		this.model = model;

		spinnerModel = new SpinnerModel();
		spinner = new JSpinner();
		spinner.setModel(spinnerModel);
		this.add(spinner, BorderLayout.CENTER);

		grid = new GridIcon();
		this.add(grid, BorderLayout.EAST);
		grid.update();

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
			if (value != null) {
				value.addPropertyChangeListener(ZoomModel.SHOW_GRID, grid);
				value.addPropertyChangeListener(ZoomModel.ZOOM, spinnerModel);
			}
		}
	}
}
