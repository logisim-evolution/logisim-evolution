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

package com.cburch.logisim.gui.appear;

import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.instance.Instance;
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

class LayoutPopupManager implements SelectionListener, MouseListener, MouseMotionListener {
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

  // adds all the dynamic elements in the current selection to hilight set
  private void addSelectedDynamicElements(HashSet<CanvasObject> hilight) {
    for (CanvasObject o : canvas.getSelection().getSelected()) {
      if (o instanceof DynamicElement) hilight.add(o);
    }
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

  public void mouseClicked(MouseEvent e) {}

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
    if (sincePopup > 50) hideCurrentPopup();
  }

  public void mouseMoved(MouseEvent arg0) {}

  public void mousePressed(MouseEvent e) {
    long sincePopup = System.currentTimeMillis() - curPopupTime;
    if (sincePopup > 50) hideCurrentPopup();
    dragStart = Location.create(e.getX(), e.getY());
  }

  public void mouseReleased(MouseEvent e) {}

  public void selectionChanged(SelectionEvent e) {
    int act = e.getAction();
    if (act == SelectionEvent.ACTION_ADDED) {
      Set<CanvasObject> shapes = shouldShowPopup(e.getAffected());
      if (shapes == null) {
        hideCurrentPopup();
      } else {
        showPopup(shapes);
      }
    }
  }

  private Set<CanvasObject> shouldShowPopup(Collection<CanvasObject> add) {
    boolean found = false;
    for (CanvasObject o : add) {
      if (o instanceof AppearancePort || o instanceof DynamicElement) {
        found = true;
        break;
      }
    }
    if (found) {
      HashSet<CanvasObject> hilight = new HashSet<>();
      Set<AppearancePort> ports = getSelectedPorts();
      if (!ports.isEmpty() && isPortUnselected(ports)) hilight.addAll(ports);
      addSelectedDynamicElements(hilight);
      if (hilight.size() > 0) return hilight;
    }
    return null;
  }

  private void showPopup(Set<CanvasObject> shapes) {
    dragStart = null;
    CircuitState circuitState = canvas.getCircuitState();
    if (circuitState == null) {
      return;
    }
    ArrayList<Instance> pins = new ArrayList<>();
    ArrayList<Instance> elts = new ArrayList<>();
    for (CanvasObject shape : shapes) {
      if (shape instanceof AppearancePort) pins.add(((AppearancePort) shape).getPin());
      else elts.add(((DynamicElement) shape).getFirstInstance().getInstance());
    }
    hideCurrentPopup();
    JViewport owner = canvasPane.getViewport();
    Point ownerLoc = owner.getLocationOnScreen();
    Dimension ownerDim = owner.getSize();
    Dimension layoutDim =
        new Dimension((int) (ownerDim.getWidth() - 10.0), (int) (ownerDim.getHeight() / 2));
    LayoutThumbnail layout = new LayoutThumbnail(layoutDim);
    layout.setCircuit(circuitState, pins, elts);
    int x = ownerLoc.x + Math.max(0, ownerDim.width - layoutDim.width - 5);
    int y = ownerLoc.y + Math.max(0, ownerDim.height - layoutDim.height - 5);
    PopupFactory factory = PopupFactory.getSharedInstance();
    Popup popup = factory.getPopup(canvasPane.getViewport(), layout, x, y);
    popup.show();
    curPopup = popup;
    curPopupTime = System.currentTimeMillis();
  }
}
