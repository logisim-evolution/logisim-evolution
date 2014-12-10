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

package com.cburch.draw.canvas;

import java.awt.Cursor;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.List;

import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Location;

class CanvasListener implements MouseListener, MouseMotionListener,
		KeyListener, CanvasModelListener {
	private Canvas canvas;
	private CanvasTool tool;

	public CanvasListener(Canvas canvas) {
		this.canvas = canvas;
		tool = null;
	}

	public CanvasTool getTool() {
		return tool;
	}

	private void handlePopupTrigger(MouseEvent e) {
		Location loc = Location.create(e.getX(), e.getY());
		List<CanvasObject> objects = canvas.getModel().getObjectsFromTop();
		CanvasObject clicked = null;
		for (CanvasObject o : objects) {
			if (o.contains(loc, false)) {
				clicked = o;
				break;
			}
		}
		if (clicked == null) {
			for (CanvasObject o : objects) {
				if (o.contains(loc, true)) {
					clicked = o;
					break;
				}
			}
		}
		canvas.showPopupMenu(e, clicked);
	}

	private boolean isButton1(MouseEvent e) {
		return (e.getModifiersEx() & MouseEvent.BUTTON1_DOWN_MASK) != 0;
	}

	public void keyPressed(KeyEvent e) {
		if (tool != null)
			tool.keyPressed(canvas, e);
	}

	public void keyReleased(KeyEvent e) {
		if (tool != null)
			tool.keyReleased(canvas, e);
	}

	public void keyTyped(KeyEvent e) {
		if (tool != null)
			tool.keyTyped(canvas, e);
	}

	public void modelChanged(CanvasModelEvent event) {
		canvas.getSelection().modelChanged(event);
		canvas.repaint();
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		if (isButton1(e)) {
			if (tool != null)
				tool.mouseDragged(canvas, e);
		} else {
			if (tool != null)
				tool.mouseMoved(canvas, e);
		}
	}

	public void mouseEntered(MouseEvent e) {
		if (tool != null)
			tool.mouseEntered(canvas, e);
	}

	public void mouseExited(MouseEvent e) {
		if (tool != null)
			tool.mouseExited(canvas, e);
	}

	public void mouseMoved(MouseEvent e) {
		if (tool != null)
			tool.mouseMoved(canvas, e);
	}

	public void mousePressed(MouseEvent e) {
		canvas.requestFocus();
		if (e.isPopupTrigger()) {
			handlePopupTrigger(e);
		} else if (e.getButton() == 1 && tool != null) {
			tool.mousePressed(canvas, e);
		}
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger()) {
			if (tool != null)
				tool.cancelMousePress(canvas);
			handlePopupTrigger(e);
		} else if (e.getButton() == 1 && tool != null) {
			tool.mouseReleased(canvas, e);
		}
	}

	public void setTool(CanvasTool value) {
		CanvasTool oldValue = tool;
		if (value != oldValue) {
			tool = value;
			if (oldValue != null)
				oldValue.toolDeselected(canvas);
			if (value != null) {
				value.toolSelected(canvas);
				canvas.setCursor(value.getCursor(canvas));
			} else {
				canvas.setCursor(Cursor
						.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
			}
		}
	}
}
