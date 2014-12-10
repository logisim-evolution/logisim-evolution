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

package com.cburch.draw.tools;

import java.awt.Color;
import java.awt.Cursor;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.swing.Icon;

import com.cburch.draw.actions.ModelMoveHandleAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.actions.ModelTranslateAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.Icons;

public class SelectTool extends AbstractTool {
	private static CanvasObject getObjectAt(CanvasModel model, int x, int y,
			boolean assumeFilled) {
		Location loc = Location.create(x, y);
		for (CanvasObject o : model.getObjectsFromTop()) {
			if (o.contains(loc, assumeFilled))
				return o;
		}
		return null;
	}

	private static final int IDLE = 0;
	private static final int MOVE_ALL = 1;
	private static final int RECT_SELECT = 2;
	private static final int RECT_TOGGLE = 3;

	private static final int MOVE_HANDLE = 4;
	private static final int DRAG_TOLERANCE = 2;

	private static final int HANDLE_SIZE = 8;

	private static final Color RECT_SELECT_BACKGROUND = new Color(0, 0, 0, 32);
	private int curAction;
	private List<CanvasObject> beforePressSelection;
	private Handle beforePressHandle;
	private Location dragStart;
	private Location dragEnd;
	private boolean dragEffective;
	private int lastMouseX;
	private int lastMouseY;

	private HandleGesture curGesture;

	public SelectTool() {
		curAction = IDLE;
		dragStart = Location.create(0, 0);
		dragEnd = dragStart;
		dragEffective = false;
	}

	@Override
	public void cancelMousePress(Canvas canvas) {
		List<CanvasObject> before = beforePressSelection;
		Handle handle = beforePressHandle;
		beforePressSelection = null;
		beforePressHandle = null;
		if (before != null) {
			curAction = IDLE;
			Selection sel = canvas.getSelection();
			sel.clearDrawsSuppressed();
			sel.setMovingShapes(Collections.<CanvasObject> emptySet(), 0, 0);
			sel.clearSelected();
			sel.setSelected(before, true);
			sel.setHandleSelected(handle);
			repaintArea(canvas);
		}
	}

	@Override
	public void draw(Canvas canvas, Graphics g) {
		Selection selection = canvas.getSelection();
		int action = curAction;

		Location start = dragStart;
		Location end = dragEnd;
		HandleGesture gesture = null;
		boolean drawHandles;
		switch (action) {
		case MOVE_ALL:
			drawHandles = !dragEffective;
			break;
		case MOVE_HANDLE:
			drawHandles = !dragEffective;
			if (dragEffective)
				gesture = curGesture;
			break;
		default:
			drawHandles = true;
		}

		CanvasObject moveHandleObj = null;
		if (gesture != null)
			moveHandleObj = gesture.getHandle().getObject();
		if (drawHandles) {
			// unscale the coordinate system so that the stroke width isn't
			// scaled
			double zoom = 1.0;
			Graphics gCopy = g.create();
			if (gCopy instanceof Graphics2D) {
				zoom = canvas.getZoomFactor();
				if (zoom != 1.0) {
					((Graphics2D) gCopy).scale(1.0 / zoom, 1.0 / zoom);
				}
			}
			GraphicsUtil.switchToWidth(gCopy, 1);

			int size = (int) Math.ceil(HANDLE_SIZE * Math.sqrt(zoom));
			int offs = size / 2;
			for (CanvasObject obj : selection.getSelected()) {
				List<Handle> handles;
				if (action == MOVE_HANDLE && obj == moveHandleObj) {
					handles = obj.getHandles(gesture);
				} else {
					handles = obj.getHandles(null);
				}
				for (Handle han : handles) {
					int x = han.getX();
					int y = han.getY();
					if (action == MOVE_ALL && dragEffective) {
						Location delta = selection.getMovingDelta();
						x += delta.getX();
						y += delta.getY();
					}
					x = (int) Math.round(zoom * x);
					y = (int) Math.round(zoom * y);
					gCopy.clearRect(x - offs, y - offs, size, size);
					gCopy.drawRect(x - offs, y - offs, size, size);
				}
			}
			Handle selHandle = selection.getSelectedHandle();
			if (selHandle != null) {
				int x = selHandle.getX();
				int y = selHandle.getY();
				if (action == MOVE_ALL && dragEffective) {
					Location delta = selection.getMovingDelta();
					x += delta.getX();
					y += delta.getY();
				}
				x = (int) Math.round(zoom * x);
				y = (int) Math.round(zoom * y);
				int[] xs = { x - offs, x, x + offs, x };
				int[] ys = { y, y - offs, y, y + offs };
				gCopy.setColor(Color.WHITE);
				gCopy.fillPolygon(xs, ys, 4);
				gCopy.setColor(Color.BLACK);
				gCopy.drawPolygon(xs, ys, 4);
			}
		}

		switch (action) {
		case RECT_SELECT:
		case RECT_TOGGLE:
			if (dragEffective) {
				// find rectangle currently to show
				int x0 = start.getX();
				int y0 = start.getY();
				int x1 = end.getX();
				int y1 = end.getY();
				if (x1 < x0) {
					int t = x0;
					x0 = x1;
					x1 = t;
				}
				if (y1 < y0) {
					int t = y0;
					y0 = y1;
					y1 = t;
				}

				// make the region that's not being selected darker
				int w = canvas.getWidth();
				int h = canvas.getHeight();
				g.setColor(RECT_SELECT_BACKGROUND);
				g.fillRect(0, 0, w, y0);
				g.fillRect(0, y0, x0, y1 - y0);
				g.fillRect(x1, y0, w - x1, y1 - y0);
				g.fillRect(0, y1, w, h - y1);

				// now draw the rectangle
				g.setColor(Color.GRAY);
				g.drawRect(x0, y0, x1 - x0, y1 - y0);
			}
			break;
		default:
			break;
		}
	}

	@Override
	public List<Attribute<?>> getAttributes() {
		return Collections.emptyList();
	}

	@Override
	public Cursor getCursor(Canvas canvas) {
		return Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR);
	}

	private int getHandleSize(Canvas canvas) {
		double zoom = canvas.getZoomFactor();
		return (int) Math.ceil(HANDLE_SIZE / Math.sqrt(zoom));
	}

	@Override
	public Icon getIcon() {
		return Icons.getIcon("select.gif");
	}

	@Override
	public void keyPressed(Canvas canvas, KeyEvent e) {
		int code = e.getKeyCode();
		if ((code == KeyEvent.VK_SHIFT || code == KeyEvent.VK_CONTROL || code == KeyEvent.VK_ALT)
				&& curAction != IDLE) {
			setMouse(canvas, lastMouseX, lastMouseY, e.getModifiersEx());
		}
	}

	@Override
	public void keyReleased(Canvas canvas, KeyEvent e) {
		keyPressed(canvas, e);
	}

	@Override
	public void keyTyped(Canvas canvas, KeyEvent e) {
		char ch = e.getKeyChar();
		Selection selected = canvas.getSelection();
		if ((ch == '\u0008' || ch == '\u007F') && !selected.isEmpty()) {
			ArrayList<CanvasObject> toRemove = new ArrayList<CanvasObject>();
			for (CanvasObject shape : selected.getSelected()) {
				if (shape.canRemove()) {
					toRemove.add(shape);
				}
			}
			if (!toRemove.isEmpty()) {
				e.consume();
				CanvasModel model = canvas.getModel();
				canvas.doAction(new ModelRemoveAction(model, toRemove));
				selected.clearSelected();
				repaintArea(canvas);
			}
		} else if (ch == '\u001b' && !selected.isEmpty()) {
			selected.clearSelected();
			repaintArea(canvas);
		}
	}

	@Override
	public void mouseDragged(Canvas canvas, MouseEvent e) {
		setMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());
	}

	@Override
	public void mousePressed(Canvas canvas, MouseEvent e) {
		beforePressSelection = new ArrayList<CanvasObject>(canvas
				.getSelection().getSelected());
		beforePressHandle = canvas.getSelection().getSelectedHandle();
		int mx = e.getX();
		int my = e.getY();
		boolean shift = (e.getModifiersEx() & MouseEvent.SHIFT_DOWN_MASK) != 0;
		dragStart = Location.create(mx, my);
		dragEffective = false;
		dragEnd = dragStart;
		lastMouseX = mx;
		lastMouseY = my;
		Selection selection = canvas.getSelection();
		selection.setHandleSelected(null);

		// see whether user is pressing within an existing handle
		int halfSize = getHandleSize(canvas) / 2;
		CanvasObject clicked = null;
		for (CanvasObject shape : selection.getSelected()) {
			List<Handle> handles = shape.getHandles(null);
			for (Handle han : handles) {
				int dx = han.getX() - mx;
				int dy = han.getY() - my;
				if (dx >= -halfSize && dx <= halfSize && dy >= -halfSize
						&& dy <= halfSize) {
					if (shape.canMoveHandle(han)) {
						curAction = MOVE_HANDLE;
						curGesture = new HandleGesture(han, 0, 0,
								e.getModifiersEx());
						repaintArea(canvas);
						return;
					} else if (clicked == null) {
						clicked = shape;
					}
				}
			}
		}

		// see whether the user is clicking within a shape
		if (clicked == null) {
			clicked = getObjectAt(canvas.getModel(), e.getX(), e.getY(), false);
		}
		if (clicked != null) {
			if (shift && selection.isSelected(clicked)) {
				selection.setSelected(clicked, false);
				curAction = IDLE;
			} else {
				if (!shift && !selection.isSelected(clicked)) {
					selection.clearSelected();
				}
				selection.setSelected(clicked, true);
				selection.setMovingShapes(selection.getSelected(), 0, 0);
				curAction = MOVE_ALL;
			}
			repaintArea(canvas);
			return;
		}

		clicked = getObjectAt(canvas.getModel(), e.getX(), e.getY(), true);
		if (clicked != null && selection.isSelected(clicked)) {
			if (shift) {
				selection.setSelected(clicked, false);
				curAction = IDLE;
			} else {
				selection.setMovingShapes(selection.getSelected(), 0, 0);
				curAction = MOVE_ALL;
			}
			repaintArea(canvas);
			return;
		}

		if (shift) {
			curAction = RECT_TOGGLE;
		} else {
			selection.clearSelected();
			curAction = RECT_SELECT;
		}
		repaintArea(canvas);
	}

	@Override
	public void mouseReleased(Canvas canvas, MouseEvent e) {
		beforePressSelection = null;
		beforePressHandle = null;
		setMouse(canvas, e.getX(), e.getY(), e.getModifiersEx());

		CanvasModel model = canvas.getModel();
		Selection selection = canvas.getSelection();
		Set<CanvasObject> selected = selection.getSelected();
		int action = curAction;
		curAction = IDLE;

		if (!dragEffective) {
			Location loc = dragEnd;
			CanvasObject o = getObjectAt(model, loc.getX(), loc.getY(), false);
			if (o != null) {
				Handle han = o.canDeleteHandle(loc);
				if (han != null) {
					selection.setHandleSelected(han);
				} else {
					han = o.canInsertHandle(loc);
					if (han != null) {
						selection.setHandleSelected(han);
					}
				}
			}
		}

		Location start = dragStart;
		int x1 = e.getX();
		int y1 = e.getY();
		switch (action) {
		case MOVE_ALL:
			Location moveDelta = selection.getMovingDelta();
			if (dragEffective && !moveDelta.equals(Location.create(0, 0))) {
				canvas.doAction(new ModelTranslateAction(model, selected,
						moveDelta.getX(), moveDelta.getY()));
			}
			break;
		case MOVE_HANDLE:
			HandleGesture gesture = curGesture;
			curGesture = null;
			if (dragEffective && gesture != null) {
				ModelMoveHandleAction act;
				act = new ModelMoveHandleAction(model, gesture);
				canvas.doAction(act);
				Handle result = act.getNewHandle();
				if (result != null) {
					Handle h = result.getObject().canDeleteHandle(
							result.getLocation());
					selection.setHandleSelected(h);
				}
			}
			break;
		case RECT_SELECT:
			if (dragEffective) {
				Bounds bds = Bounds.create(start).add(x1, y1);
				selection
						.setSelected(canvas.getModel().getObjectsIn(bds), true);
			} else {
				CanvasObject clicked;
				clicked = getObjectAt(model, start.getX(), start.getY(), true);
				if (clicked != null) {
					selection.clearSelected();
					selection.setSelected(clicked, true);
				}
			}
			break;
		case RECT_TOGGLE:
			if (dragEffective) {
				Bounds bds = Bounds.create(start).add(x1, y1);
				selection.toggleSelected(canvas.getModel().getObjectsIn(bds));
			} else {
				CanvasObject clicked;
				clicked = getObjectAt(model, start.getX(), start.getY(), true);
				selection.setSelected(clicked, !selected.contains(clicked));
			}
			break;
		default:
			break;
		}
		selection.clearDrawsSuppressed();
		repaintArea(canvas);
	}

	private void repaintArea(Canvas canvas) {
		canvas.repaint();
	}

	private void setMouse(Canvas canvas, int mx, int my, int mods) {
		lastMouseX = mx;
		lastMouseY = my;
		boolean shift = (mods & MouseEvent.SHIFT_DOWN_MASK) != 0;
		boolean ctrl = (mods & InputEvent.CTRL_DOWN_MASK) != 0;
		Location newEnd = Location.create(mx, my);
		dragEnd = newEnd;

		Location start = dragStart;
		int dx = newEnd.getX() - start.getX();
		int dy = newEnd.getY() - start.getY();
		if (!dragEffective) {
			if (Math.abs(dx) + Math.abs(dy) > DRAG_TOLERANCE) {
				dragEffective = true;
			} else {
				return;
			}
		}

		switch (curAction) {
		case MOVE_HANDLE:
			HandleGesture gesture = curGesture;
			if (ctrl) {
				Handle h = gesture.getHandle();
				dx = canvas.snapX(h.getX() + dx) - h.getX();
				dy = canvas.snapY(h.getY() + dy) - h.getY();
			}
			curGesture = new HandleGesture(gesture.getHandle(), dx, dy, mods);
			canvas.getSelection().setHandleGesture(curGesture);
			break;
		case MOVE_ALL:
			if (ctrl) {
				int minX = Integer.MAX_VALUE;
				int minY = Integer.MAX_VALUE;
				for (CanvasObject o : canvas.getSelection().getSelected()) {
					for (Handle handle : o.getHandles(null)) {
						int x = handle.getX();
						int y = handle.getY();
						if (x < minX)
							minX = x;
						if (y < minY)
							minY = y;
					}
				}
				dx = canvas.snapX(minX + dx) - minX;
				dy = canvas.snapY(minY + dy) - minY;
			}
			if (shift) {
				if (Math.abs(dx) > Math.abs(dy)) {
					dy = 0;
				} else {
					dx = 0;
				}
			}
			canvas.getSelection().setMovingDelta(dx, dy);
			break;
		default:
			break;
		}
		repaintArea(canvas);
	}

	@Override
	public void toolDeselected(Canvas canvas) {
		curAction = IDLE;
		canvas.getSelection().clearSelected();
		repaintArea(canvas);
	}

	@Override
	public void toolSelected(Canvas canvas) {
		curAction = IDLE;
		canvas.getSelection().clearSelected();
		repaintArea(canvas);
	}
}
