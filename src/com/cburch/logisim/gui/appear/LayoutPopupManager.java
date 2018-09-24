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
import java.awt.Point;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.swing.JViewport;
import javax.swing.Popup;
import javax.swing.PopupFactory;

import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.instance.Instance;

class LayoutPopupManager implements SelectionListener, MouseListener,
		MouseMotionListener {
	private CanvasPane canvasPane;
	private AppearanceCanvas canvas;
	private Popup curPopup;
	private long curPopupTime;
	private Location dragStart;

	public LayoutPopupManager(CanvasPane canvasPane, AppearanceCanvas canvas) {
		this.canvasPane = canvasPane;
		this.canvas = canvas;
		this.curPopup = null;
		this.dragStart = null;

		canvas.getSelection().addSelectionListener(this);
		canvas.addMouseListener(this);
		canvas.addMouseMotionListener(this);
	}

	// returns all the ports in the current selection
	private Set<AppearancePort> getSelectedPorts() {
		HashSet<AppearancePort> ports = new HashSet<AppearancePort>();
		for (CanvasObject o : canvas.getSelection().getSelected()) {
			if (o instanceof AppearancePort) {
				ports.add((AppearancePort) o);
			}
		}
		return ports;
	}

	public void hideCurrentPopup() {
		Popup cur = curPopup;
		if (cur != null) {
			curPopup = null;
			dragStart = null;
			cur.hide();
		}
	}

	// returns true if the canvas contains any port not in the given set
	private boolean isPortUnselected(Set<AppearancePort> selected) {
		for (CanvasObject o : canvas.getModel().getObjectsFromBottom()) {
			if (o instanceof AppearancePort) {
				if (!selected.contains(o)) {
					return true;
				}
			}
		}
		return false;
	}

	public void mouseClicked(MouseEvent e) {
	}

	public void mouseDragged(MouseEvent e) {
		Location start = dragStart;
		if (start != null && start.manhattanDistanceTo(e.getX(), e.getY()) > 4) {
			hideCurrentPopup();
		}
	}

	public void mouseEntered(MouseEvent e) {
		hideCurrentPopup();
	}

	public void mouseExited(MouseEvent e) {
		long sincePopup = System.currentTimeMillis() - curPopupTime;
		if (sincePopup > 50)
			hideCurrentPopup();
	}

	public void mouseMoved(MouseEvent arg0) {
	}

	public void mousePressed(MouseEvent e) {
		long sincePopup = System.currentTimeMillis() - curPopupTime;
		if (sincePopup > 50)
			hideCurrentPopup();
		dragStart = Location.create(e.getX(), e.getY());
	}

	public void mouseReleased(MouseEvent e) {
	}

	public void selectionChanged(SelectionEvent e) {
		int act = e.getAction();
		if (act == SelectionEvent.ACTION_ADDED) {
			Set<AppearancePort> ports = shouldShowPopup(e.getAffected());
			if (ports == null) {
				hideCurrentPopup();
			} else {
				showPopup(ports);
			}
		}
	}

	private Set<AppearancePort> shouldShowPopup(Collection<CanvasObject> add) {
		boolean found = false;
		for (CanvasObject o : add) {
			if (o instanceof AppearancePort) {
				found = true;
				break;
			}
		}
		if (found) {
			Set<AppearancePort> ports = getSelectedPorts();
			if (!ports.isEmpty() && isPortUnselected(ports)) {
				return ports;
			}
		}
		return null;
	}

	private void showPopup(Set<AppearancePort> portObjects) {
		dragStart = null;
		CircuitState circuitState = canvas.getCircuitState();
		if (circuitState == null)
			return;
		ArrayList<Instance> ports = new ArrayList<Instance>(portObjects.size());
		for (AppearancePort portObject : portObjects) {
			ports.add(portObject.getPin());
		}

		hideCurrentPopup();
		LayoutThumbnail layout = new LayoutThumbnail();
		layout.setCircuit(circuitState, ports);
		JViewport owner = canvasPane.getViewport();
		Point ownerLoc = owner.getLocationOnScreen();
		Dimension ownerDim = owner.getSize();
		Dimension layoutDim = layout.getPreferredSize();
		int x = ownerLoc.x + Math.max(0, ownerDim.width - layoutDim.width - 5);
		int y = ownerLoc.y
				+ Math.max(0, ownerDim.height - layoutDim.height - 5);
		PopupFactory factory = PopupFactory.getSharedInstance();
		Popup popup = factory.getPopup(canvasPane.getViewport(), layout, x, y);
		popup.show();
		curPopup = popup;
		curPopupTime = System.currentTimeMillis();
	}

}
