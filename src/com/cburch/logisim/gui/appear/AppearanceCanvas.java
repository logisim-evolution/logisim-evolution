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

package com.cburch.logisim.gui.appear;

import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JPopupMenu;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelReorderAction;
import com.cburch.draw.canvas.ActionDispatcher;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.CanvasTool;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.undo.Action;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.gui.generic.CanvasPaneContents;
import com.cburch.logisim.gui.generic.GridPainter;
import com.cburch.logisim.proj.Project;

public class AppearanceCanvas extends Canvas implements CanvasPaneContents,
		ActionDispatcher {
	private class Listener implements CanvasModelListener,
			PropertyChangeListener {
		public void modelChanged(CanvasModelEvent event) {
			computeSize(false);
		}

		public void propertyChange(PropertyChangeEvent evt) {
			String prop = evt.getPropertyName();
			if (prop.equals(GridPainter.ZOOM_PROPERTY)) {
				CanvasTool t = getTool();
				if (t != null) {
					t.zoomFactorChanged(AppearanceCanvas.this);
				}
			}
		}
	}

	static int getMaxIndex(CanvasModel model) {
		List<CanvasObject> objects = model.getObjectsFromBottom();
		for (int i = objects.size() - 1; i >= 0; i--) {
			if (!(objects.get(i) instanceof AppearanceElement)) {
				return i;
			}
		}
		return -1;
	}

	private static final long serialVersionUID = 1L;

	private static final int BOUNDS_BUFFER = 70;

	// pixels shown in canvas beyond outermost boundaries
	private static final int THRESH_SIZE_UPDATE = 10;
	// don't bother to update the size if it hasn't changed more than this
	private CanvasTool selectTool;
	private Project proj;
	private CircuitState circuitState;
	private Listener listener;
	private GridPainter grid;
	private CanvasPane canvasPane;
	private Bounds oldPreferredSize;

	private LayoutPopupManager popupManager;

	public AppearanceCanvas(CanvasTool selectTool) {
		this.selectTool = selectTool;
		this.grid = new GridPainter(this);
		this.listener = new Listener();
		this.oldPreferredSize = null;
		setSelection(new AppearanceSelection());
		setTool(selectTool);

		CanvasModel model = super.getModel();
		if (model != null)
			model.addCanvasModelListener(listener);
		grid.addPropertyChangeListener(GridPainter.ZOOM_PROPERTY, listener);
	}

	private void computeSize(boolean immediate) {
		hidePopup();
		Bounds bounds;
		CircuitState circState = circuitState;
		if (circState == null) {
			bounds = Bounds.create(0, 0, 50, 50);
		} else {
			bounds = circState.getCircuit().getAppearance().getAbsoluteBounds();
		}
		int width = bounds.getX() + bounds.getWidth() + BOUNDS_BUFFER;
		int height = bounds.getY() + bounds.getHeight() + BOUNDS_BUFFER;
		Dimension dim;
		if (canvasPane == null) {
			dim = new Dimension(width, height);
		} else {
			dim = canvasPane.supportPreferredSize(width, height);
		}
		if (!immediate) {
			Bounds old = oldPreferredSize;
			if (old != null
					&& Math.abs(old.getWidth() - dim.width) < THRESH_SIZE_UPDATE
					&& Math.abs(old.getHeight() - dim.height) < THRESH_SIZE_UPDATE) {
				return;
			}
		}
		oldPreferredSize = Bounds.create(0, 0, dim.width, dim.height);
		setPreferredSize(dim);
		revalidate();
	}

	@Override
	public void doAction(Action canvasAction) {
		Circuit circuit = circuitState.getCircuit();
		if (!proj.getLogisimFile().contains(circuit)) {
			return;
		}

		if (canvasAction instanceof ModelReorderAction) {
			int max = getMaxIndex(getModel());
			ModelReorderAction reorder = (ModelReorderAction) canvasAction;
			List<ReorderRequest> rs = reorder.getReorderRequests();
			List<ReorderRequest> mod = new ArrayList<ReorderRequest>(rs.size());
			boolean changed = false;
			boolean movedToMax = false;
			for (ReorderRequest r : rs) {
				CanvasObject o = r.getObject();
				if (o instanceof AppearanceElement) {
					changed = true;
				} else {
					if (r.getToIndex() > max) {
						int from = r.getFromIndex();
						changed = true;
						movedToMax = true;
						if (from == max && !movedToMax) {
							; // this change is ineffective - don't add it
						} else {
							mod.add(new ReorderRequest(o, from, max));
						}
					} else {
						if (r.getToIndex() == max)
							movedToMax = true;
						mod.add(r);
					}
				}
			}
			if (changed) {
				if (mod.isEmpty()) {
					return;
				}
				canvasAction = new ModelReorderAction(getModel(), mod);
			}
		}

		if (canvasAction instanceof ModelAddAction) {
			ModelAddAction addAction = (ModelAddAction) canvasAction;
			int cur = addAction.getDestinationIndex();
			int max = getMaxIndex(getModel());
			if (cur > max) {
				canvasAction = new ModelAddAction(getModel(),
						addAction.getObjects(), max + 1);
			}
		}

		proj.doAction(new CanvasActionAdapter(circuit, canvasAction));
	}

	Circuit getCircuit() {
		return circuitState.getCircuit();
	}

	CircuitState getCircuitState() {
		return circuitState;
	}

	GridPainter getGridPainter() {
		return grid;
	}

	public Dimension getPreferredScrollableViewportSize() {
		return getPreferredSize();
	}

	Project getProject() {
		return proj;
	}

	public int getScrollableBlockIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return canvasPane.supportScrollableBlockIncrement(visibleRect,
				orientation, direction);
	}

	public boolean getScrollableTracksViewportHeight() {
		return false;
	}

	public boolean getScrollableTracksViewportWidth() {
		return false;
	}

	public int getScrollableUnitIncrement(Rectangle visibleRect,
			int orientation, int direction) {
		return canvasPane.supportScrollableUnitIncrement(visibleRect,
				orientation, direction);
	}

	@Override
	public double getZoomFactor() {
		return grid.getZoomFactor();
	}

	private void hidePopup() {
		LayoutPopupManager man = popupManager;
		if (man != null) {
			man.hideCurrentPopup();
		}
	}

	@Override
	protected void paintBackground(Graphics g) {
		super.paintBackground(g);
		grid.paintGrid(g);
	}

	@Override
	protected void paintForeground(Graphics g) {
		double zoom = grid.getZoomFactor();
		Graphics gScaled = g.create();
		if (zoom != 1.0 && zoom != 0.0 && gScaled instanceof Graphics2D) {
			((Graphics2D) gScaled).scale(zoom, zoom);
		}
		super.paintForeground(gScaled);
		gScaled.dispose();
	}

	@Override
	protected void processMouseEvent(MouseEvent e) {
		repairEvent(e, grid.getZoomFactor());
		super.processMouseEvent(e);
	}

	@Override
	protected void processMouseMotionEvent(MouseEvent e) {
		repairEvent(e, grid.getZoomFactor());
		super.processMouseMotionEvent(e);
	}

	public void recomputeSize() {
		computeSize(true);
		repaint();
	}

	@Override
	public void repaintCanvasCoords(int x, int y, int width, int height) {
		double zoom = grid.getZoomFactor();
		if (zoom != 1.0) {
			x = (int) (x * zoom - 1);
			y = (int) (y * zoom - 1);
			width = (int) (width * zoom + 4);
			height = (int) (height * zoom + 4);
		}
		super.repaintCanvasCoords(x, y, width, height);
	}

	private void repairEvent(MouseEvent e, double zoom) {
		if (zoom != 1.0) {
			int oldx = e.getX();
			int oldy = e.getY();
			int newx = (int) Math.round(e.getX() / zoom);
			int newy = (int) Math.round(e.getY() / zoom);
			e.translatePoint(newx - oldx, newy - oldy);
		}
	}

	//
	// CanvasPaneContents methods
	//
	public void setCanvasPane(CanvasPane value) {
		canvasPane = value;
		computeSize(true);
		popupManager = new LayoutPopupManager(value, this);
	}

	public void setCircuit(Project proj, CircuitState circuitState) {
		this.proj = proj;
		this.circuitState = circuitState;
		Circuit circuit = circuitState.getCircuit();
		setModel(circuit.getAppearance(), this);
	}

	@Override
	public void setModel(CanvasModel value, ActionDispatcher dispatcher) {
		CanvasModel oldModel = super.getModel();
		if (oldModel != null) {
			oldModel.removeCanvasModelListener(listener);
		}
		super.setModel(value, dispatcher);
		if (value != null) {
			value.addCanvasModelListener(listener);
		}
	}

	@Override
	public void setTool(CanvasTool value) {
		hidePopup();
		super.setTool(value);
	}

	@Override
	public JPopupMenu showPopupMenu(MouseEvent e, CanvasObject clicked) {
		double zoom = grid.getZoomFactor();
		int x = (int) Math.round(e.getX() * zoom);
		int y = (int) Math.round(e.getY() * zoom);
		if (clicked != null && getSelection().isSelected(clicked)) {
			AppearanceEditPopup popup = new AppearanceEditPopup(this);
			popup.show(this, x, y);
			return popup;
		}
		return null;
	}

	@Override
	public int snapX(int x) {
		if (x < 0) {
			return -((-x + 5) / 10 * 10);
		} else {
			return (x + 5) / 10 * 10;
		}
	}

	@Override
	public int snapY(int y) {
		if (y < 0) {
			return -((-y + 5) / 10 * 10);
		} else {
			return (y + 5) / 10 * 10;
		}
	}

	@Override
	public void toolGestureComplete(CanvasTool tool, CanvasObject created) {
		if (tool == getTool() && tool != selectTool) {
			setTool(selectTool);
			if (created != null) {
				getSelection().clearSelected();
				getSelection().setSelected(created, true);
			}
		}
	}
}
