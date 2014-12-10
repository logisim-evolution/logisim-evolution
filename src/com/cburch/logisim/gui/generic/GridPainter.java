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

import java.awt.Color;
import java.awt.Component;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.Rectangle;
import java.awt.image.MemoryImageSource;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.Arrays;

public class GridPainter {
	private class Listener implements PropertyChangeListener {
		public void propertyChange(PropertyChangeEvent event) {
			String prop = event.getPropertyName();
			Object val = event.getNewValue();
			if (prop.equals(ZoomModel.ZOOM)) {
				setZoomFactor(((Double) val).doubleValue());
				destination.repaint();
			} else if (prop.equals(ZoomModel.SHOW_GRID)) {
				setShowGrid(((Boolean) val).booleanValue());
				destination.repaint();
			}
		}
	}

	public static final String ZOOM_PROPERTY = "zoom";

	public static final String SHOW_GRID_PROPERTY = "showgrid";
	private static final int GRID_DOT_COLOR = 0xFF777777;

	private static final int GRID_DOT_ZOOMED_COLOR = 0xFFCCCCCC;

	private static final Color GRID_ZOOMED_OUT_COLOR = new Color(210, 210, 210);

	private Component destination;
	private PropertyChangeSupport support;
	private Listener listener;
	private ZoomModel zoomModel;
	private boolean showGrid;
	private int gridSize;
	private double zoomFactor;
	private Image gridImage;
	private int gridImageWidth;

	public GridPainter(Component destination) {
		this.destination = destination;
		support = new PropertyChangeSupport(this);
		showGrid = true;
		gridSize = 10;
		zoomFactor = 1.0;
		updateGridImage(gridSize, zoomFactor);
	}

	public void addPropertyChangeListener(String prop,
			PropertyChangeListener listener) {
		support.addPropertyChangeListener(prop, listener);
	}

	public boolean getShowGrid() {
		return showGrid;
	}

	public double getZoomFactor() {
		return zoomFactor;
	}

	public ZoomModel getZoomModel() {
		return zoomModel;
	}

	public void paintGrid(Graphics g) {
		Rectangle clip = g.getClipBounds();
		Component dest = destination;
		double zoom = zoomFactor;
		int size = gridSize;

		if (!showGrid)
			return;

		Image img = gridImage;
		int w = gridImageWidth;
		if (img == null) {
			paintGridOld(g, size, zoom, clip);
			return;
		}
		int x0 = (clip.x / w) * w; // round down to multiple of w
		int y0 = (clip.y / w) * w;
		for (int x = 0; x < clip.width + w; x += w) {
			for (int y = 0; y < clip.height + w; y += w) {
				g.drawImage(img, x0 + x, y0 + y, dest);
			}
		}
	}

	private void paintGridOld(Graphics g, int size, double f, Rectangle clip) {
		g.setColor(Color.GRAY);
		if (f == 1.0) {
			int start_x = ((clip.x + 9) / size) * size;
			int start_y = ((clip.y + 9) / size) * size;
			for (int x = 0; x < clip.width; x += size) {
				for (int y = 0; y < clip.height; y += size) {
					g.fillRect(start_x + x, start_y + y, 1, 1);
				}
			}
		} else {
			/* Kevin Walsh of Cornell suggested the code below instead. */
			int x0 = size * (int) Math.ceil(clip.x / f / size);
			int x1 = x0 + (int) (clip.width / f);
			int y0 = size * (int) Math.ceil(clip.y / f / size);
			int y1 = y0 + (int) (clip.height / f);
			if (f <= 0.5)
				g.setColor(GRID_ZOOMED_OUT_COLOR);
			for (double x = x0; x < x1; x += size) {
				for (double y = y0; y < y1; y += size) {
					int sx = (int) Math.round(f * x);
					int sy = (int) Math.round(f * y);
					g.fillRect(sx, sy, 1, 1);
				}
			}
			if (f <= 0.5) { // make every 5th pixel darker
				int size5 = 5 * size;
				g.setColor(Color.GRAY);
				x0 = size5 * (int) Math.ceil(clip.x / f / size5);
				y0 = size5 * (int) Math.ceil(clip.y / f / size5);
				for (double x = x0; x < x1; x += size5) {
					for (double y = y0; y < y1; y += size5) {
						int sx = (int) Math.round(f * x);
						int sy = (int) Math.round(f * y);
						g.fillRect(sx, sy, 1, 1);
					}
				}
			}

			/*
			 * Original code by Carl Burch int x0 = 10 * (int) Math.ceil(clip.x
			 * / f / 10); int x1 = x0 + (int)(clip.width / f); int y0 = 10 *
			 * (int) Math.ceil(clip.y / f / 10); int y1 = y0 + (int)
			 * (clip.height / f); int s = f > 0.5 ? 1 : f > 0.25 ? 2 : 3; int i0
			 * = s - ((x0 + 10*s - 1) % (s * 10)) / 10 - 1; int j0 = s - ((y1 +
			 * 10*s - 1) % (s * 10)) / 10 - 1; for (int i = 0; i < s; i++) { for
			 * (int x = x0+i*10; x < x1; x += s*10) { for (int j = 0; j < s;
			 * j++) { g.setColor(i == i0 && j == j0 ? Color.gray :
			 * GRID_ZOOMED_OUT_COLOR); for (int y = y0+j*10; y < y1; y += s*10)
			 * { int sx = (int) Math.round(f * x); int sy = (int) Math.round(f *
			 * y); g.fillRect(sx, sy, 1, 1); } } } }
			 */
		}
	}

	public void removePropertyChangeListener(String prop,
			PropertyChangeListener listener) {
		support.removePropertyChangeListener(prop, listener);
	}

	public void setShowGrid(boolean value) {
		if (showGrid != value) {
			showGrid = value;
			support.firePropertyChange(SHOW_GRID_PROPERTY, !value, value);
		}
	}

	public void setZoomFactor(double value) {
		double oldValue = zoomFactor;
		if (oldValue != value) {
			zoomFactor = value;
			updateGridImage(gridSize, value);
			support.firePropertyChange(ZOOM_PROPERTY, Double.valueOf(oldValue),
					Double.valueOf(value));
		}
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

	//
	// creating the grid image
	//
	private void updateGridImage(int size, double f) {
		double ww = f * size * 5;
		while (2 * ww < 150)
			ww *= 2;
		int w = (int) Math.round(ww);
		int[] pix = new int[w * w];
		Arrays.fill(pix, 0xFFFFFF);

		if (f == 1.0) {
			int lineStep = size * w;
			for (int j = 0; j < pix.length; j += lineStep) {
				for (int i = 0; i < w; i += size) {
					pix[i + j] = GRID_DOT_COLOR;
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

			int dotColor = f <= 0.5 ? GRID_DOT_ZOOMED_COLOR : GRID_DOT_COLOR;
			for (int j = 0; true; j += size) {
				int y = (int) Math.round(f * j);
				if (y + off0 >= w)
					break;

				for (int yo = y + off0; yo < y + off1; yo++) {
					if (yo >= 0 && yo < w) {
						int base = yo * w;
						for (int i = 0; true; i += size) {
							int x = (int) Math.round(f * i);
							if (x + off0 >= w)
								break;
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
					if (y >= w)
						break;
					y *= w;

					for (int i = 0; true; i += size5) {
						int x = (int) Math.round(f * i);
						if (x >= w)
							break;
						pix[y + x] = GRID_DOT_COLOR;
					}
				}
			}
		}
		gridImage = destination.createImage(new MemoryImageSource(w, w, pix, 0,
				w));
		gridImageWidth = w;
	}
}
