/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.contracts.BaseMouseMotionListenerContract;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.appear.AppearancePort;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.CanvasPane;
import com.cburch.logisim.instance.Instance;
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Popup;
import javax.swing.PopupFactory;

class LayoutPopupManager implements SelectionListener, BaseMouseListenerContract, BaseMouseMotionListenerContract {
  private final CanvasPane canvasPane;
  private final AppearanceCanvas canvas;
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
    final var ports = new HashSet<AppearancePort>();
    for (final var o : canvas.getSelection().getSelected()) {
      if (o instanceof AppearancePort appPort) ports.add(appPort);
    }
    return ports;
  }

  // adds all the dynamic elements in the current selection to hilight set
  private void addSelectedDynamicElements(HashSet<CanvasObject> hilight) {
    for (final var obj : canvas.getSelection().getSelected()) {
      if (obj instanceof DynamicElement) hilight.add(obj);
    }
  }

  public void hideCurrentPopup() {
    final var cur = curPopup;
    if (cur != null) {
      curPopup = null;
      dragStart = null;
      cur.hide();
    }
  }

  // returns true if the canvas contains any port not in the given set
  private boolean isPortUnselected(Set<AppearancePort> selected) {
    for (final var obj : canvas.getModel().getObjectsFromBottom()) {
      if (obj instanceof AppearancePort) {
        if (!selected.contains(obj)) return true;
      }
    }
    return false;
  }

  @Override
  public void mouseDragged(MouseEvent e) {
    final var start = dragStart;
    if (start != null && start.manhattanDistanceTo(e.getX(), e.getY()) > 4) {
      hideCurrentPopup();
    }
  }

  @Override
  public void mouseEntered(MouseEvent e) {
    hideCurrentPopup();
  }

  @Override
  public void mouseExited(MouseEvent e) {
    final var sincePopup = System.currentTimeMillis() - curPopupTime;
    if (sincePopup > 50) hideCurrentPopup();
  }

  @Override
  public void mouseClicked(MouseEvent mouseEvent) {
    // do nothing
  }

  @Override
  public void mousePressed(MouseEvent e) {
    final var sincePopup = System.currentTimeMillis() - curPopupTime;
    if (sincePopup > 50) hideCurrentPopup();
    dragStart = Location.create(e.getX(), e.getY(), false);
  }

  @Override
  public void selectionChanged(SelectionEvent e) {
    final var act = e.getAction();
    if (act == SelectionEvent.ACTION_ADDED) {
      final var shapes = shouldShowPopup(e.getAffected());
      if (shapes == null) {
        hideCurrentPopup();
      } else {
        showPopup(shapes);
      }
    }
  }

  private Set<CanvasObject> shouldShowPopup(Collection<CanvasObject> add) {
    var found = false;
    for (final var obj : add) {
      if (obj instanceof AppearancePort || obj instanceof DynamicElement) {
        found = true;
        break;
      }
    }
    if (found) {
      final var hilight = new HashSet<CanvasObject>();
      final var ports = getSelectedPorts();
      if (!ports.isEmpty() && isPortUnselected(ports)) hilight.addAll(ports);
      addSelectedDynamicElements(hilight);
      if (hilight.size() > 0) return hilight;
    }
    return null;
  }

  private void showPopup(Set<CanvasObject> shapes) {
    dragStart = null;
    final var circuitState = canvas.getCircuitState();
    if (circuitState == null) return;
    final var pins = new ArrayList<Instance>();
    final var elts = new ArrayList<Instance>();
    for (final var shape : shapes) {
      if (shape instanceof AppearancePort appPort) pins.add(appPort.getPin());
      else elts.add(((DynamicElement) shape).getFirstInstance().getInstance());
    }
    hideCurrentPopup();
    final var owner = canvasPane.getViewport();
    final var ownerLoc = owner.getLocationOnScreen();
    final var ownerDim = owner.getSize();
    final var layoutDim = new Dimension((int) (ownerDim.getWidth() - 10.0), (int) (ownerDim.getHeight() / 2));
    final var layout = new LayoutThumbnail(layoutDim);
    layout.setCircuit(circuitState, pins, elts);
    final var x = ownerLoc.x + Math.max(0, ownerDim.width - layoutDim.width - 5);
    final var y = ownerLoc.y + Math.max(0, ownerDim.height - layoutDim.height - 5);
    final var factory = PopupFactory.getSharedInstance();
    final var popup = factory.getPopup(canvasPane.getViewport(), layout, x, y);
    popup.show();
    curPopup = popup;
    curPopupTime = System.currentTimeMillis();
  }
}
