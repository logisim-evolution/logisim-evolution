/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Map;

class SelectionAction extends Action {
  private final StringGetter displayName;
  private final AppearanceCanvas canvas;
  private final CanvasModel canvasModel;
  private final Map<CanvasObject, Integer> toRemove;
  private final Collection<CanvasObject> toAdd;
  private final Collection<CanvasObject> oldSelection;
  private final Collection<CanvasObject> newSelection;
  private final Location anchorNewLocation;
  private final Direction anchorNewFacing;
  private Location anchorOldLocation;
  private Direction anchorOldFacing;

  public SelectionAction(
      AppearanceCanvas canvas,
      StringGetter displayName,
      Collection<CanvasObject> toRemove,
      Collection<CanvasObject> toAdd,
      Collection<CanvasObject> newSelection,
      Location anchorLocation,
      Direction anchorFacing) {
    this.canvas = canvas;
    this.canvasModel = canvas.getModel();
    this.displayName = displayName;
    this.toRemove = toRemove == null ? null : ZOrder.getZIndex(toRemove, canvasModel);
    this.toAdd = toAdd;
    this.oldSelection = new ArrayList<>(canvas.getSelection().getSelected());
    this.newSelection = newSelection;
    this.anchorNewLocation = anchorLocation;
    this.anchorNewFacing = anchorFacing;
  }

  @Override
  public void doIt(Project proj) {
    final var sel = canvas.getSelection();
    sel.clearSelected();
    if (toRemove != null) canvasModel.removeObjects(toRemove.keySet());
    int dest = AppearanceCanvas.getMaxIndex(canvasModel) + 1;
    if (toAdd != null) canvasModel.addObjects(dest, toAdd);

    final var anchor = findAnchor(canvasModel);
    if (anchor != null && anchorNewLocation != null) {
      anchorOldLocation = anchor.getLocation();
      anchor.translate(
          anchorNewLocation.getX() - anchorOldLocation.getX(),
          anchorNewLocation.getY() - anchorOldLocation.getY());
    }
    if (anchor != null && anchorNewFacing != null) {
      anchorOldFacing = anchor.getFacingDirection();
      anchor.setValue(AppearanceAnchor.FACING, anchorNewFacing);
    }
    sel.setSelected(newSelection, true);
    canvas.repaint();
  }

  private AppearanceAnchor findAnchor(CanvasModel canvasModel) {
    for (final Object obj : canvasModel.getObjectsFromTop()) {
      if (obj instanceof AppearanceAnchor anchor) return anchor;
    }
    return null;
  }

  @Override
  public String getName() {
    return displayName.toString();
  }

  @Override
  public void undo(Project proj) {
    final var anchor = findAnchor(canvasModel);
    if (anchor != null && anchorOldLocation != null) {
      anchor.translate(
          anchorOldLocation.getX() - anchorNewLocation.getX(),
          anchorOldLocation.getY() - anchorNewLocation.getY());
    }
    if (anchor != null && anchorOldFacing != null) {
      anchor.setValue(AppearanceAnchor.FACING, anchorOldFacing);
    }
    final var sel = canvas.getSelection();
    sel.clearSelected();
    if (toAdd != null) canvasModel.removeObjects(toAdd);
    if (toRemove != null) canvasModel.addObjects(toRemove);
    sel.setSelected(oldSelection, true);
    canvas.repaint();
  }
}
