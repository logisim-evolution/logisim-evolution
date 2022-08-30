/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.actions.ModelDeleteHandleAction;
import com.cburch.draw.actions.ModelInsertHandleAction;
import com.cburch.draw.actions.ModelReorderAction;
import com.cburch.draw.canvas.Canvas;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.MatchingSet;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

public class AppearanceEditHandler extends EditHandler implements SelectionListener, PropertyChangeListener, CanvasModelListener {
  private final AppearanceCanvas canvas;

  AppearanceEditHandler(AppearanceCanvas canvas) {
    this.canvas = canvas;
    canvas.getSelection().addSelectionListener(this);
    final var model = canvas.getModel();
    if (model != null) model.addCanvasModelListener(this);
    canvas.addPropertyChangeListener(Canvas.MODEL_PROPERTY, this);
  }

  @Override
  public void addControlPoint() {
    final var sel = canvas.getSelection();
    final var handle = sel.getSelectedHandle();
    canvas.doAction(new ModelInsertHandleAction(canvas.getModel(), handle));
  }

  @Override
  public void computeEnabled() {
    final var proj = canvas.getProject();
    final var circ = canvas.getCircuit();
    final var sel = canvas.getSelection();
    final var selEmpty = sel.isEmpty();
    final var canChange = proj.getLogisimFile().contains(circ);
    final var clipExists = !Clipboard.isEmpty();
    var selHasRemovable = false;
    for (final var o : sel.getSelected()) {
      if (!(o instanceof AppearanceElement)) {
        selHasRemovable = true;
        break;
      }
    }
    boolean canRaise;
    boolean canLower;
    if (!selEmpty && canChange) {
      final var zs = ZOrder.getZIndex(sel.getSelected(), canvas.getModel());
      var zMin = Integer.MAX_VALUE;
      var zMax = Integer.MIN_VALUE;
      var count = 0;
      for (final var entry : zs.entrySet()) {
        if (!(entry.getKey() instanceof AppearanceElement)) {
          count++;
          int z = entry.getValue();
          if (z < zMin) zMin = z;
          if (z > zMax) zMax = z;
        }
      }
      final var maxPoss = AppearanceCanvas.getMaxIndex(canvas.getModel());
      if (count > 0 && count <= maxPoss) {
        canRaise = zMin <= maxPoss - count;
        canLower = zMax >= count;
      } else {
        canRaise = false;
        canLower = false;
      }
    } else {
      canRaise = false;
      canLower = false;
    }
    var canAddCtrl = false;
    var canRemCtrl = false;
    final var handle = sel.getSelectedHandle();
    if (handle != null && canChange) {
      final var o = handle.getObject();
      canAddCtrl = o.canInsertHandle(handle.getLocation()) != null;
      canRemCtrl = o.canDeleteHandle(handle.getLocation()) != null;
    }

    setEnabled(LogisimMenuBar.CUT, selHasRemovable && canChange);
    setEnabled(LogisimMenuBar.COPY, !selEmpty);
    setEnabled(LogisimMenuBar.PASTE, canChange && clipExists);
    setEnabled(LogisimMenuBar.DELETE, selHasRemovable && canChange);
    setEnabled(LogisimMenuBar.DUPLICATE, !selEmpty && canChange);
    setEnabled(LogisimMenuBar.SELECT_ALL, true);
    setEnabled(LogisimMenuBar.RAISE, canRaise);
    setEnabled(LogisimMenuBar.LOWER, canLower);
    setEnabled(LogisimMenuBar.RAISE_TOP, canRaise);
    setEnabled(LogisimMenuBar.LOWER_BOTTOM, canLower);
    setEnabled(LogisimMenuBar.ADD_CONTROL, canAddCtrl);
    setEnabled(LogisimMenuBar.REMOVE_CONTROL, canRemCtrl);
  }

  @Override
  public void copy() {
    if (!canvas.getSelection().isEmpty()) {
      canvas.getProject().doAction(ClipboardActions.copy(canvas));
    }
  }

  @Override
  public void cut() {
    if (!canvas.getSelection().isEmpty()) {
      canvas.getProject().doAction(ClipboardActions.cut(canvas));
    }
  }

  @Override
  public void delete() {
    final var sel = canvas.getSelection();
    final var n = sel.getSelected().size();
    final var select = new ArrayList<CanvasObject>(n);
    final var remove = new ArrayList<CanvasObject>(n);
    Location anchorLocation = null;
    Direction anchorFacing = null;
    for (final var obj : sel.getSelected()) {
      if (obj.canRemove()) {
        remove.add(obj);
      } else {
        select.add(obj);
        if (obj instanceof AppearanceAnchor anchor) {
          anchorLocation = anchor.getLocation();
          anchorFacing = anchor.getFacingDirection();
        }
      }
    }

    if (!remove.isEmpty()) {
      canvas
          .getProject()
          .doAction(
              new SelectionAction(
                  canvas,
                  S.getter("deleteSelectionAction"),
                  remove,
                  null,
                  select,
                  anchorLocation,
                  anchorFacing));
    }
  }

  @Override
  public void duplicate() {
    final var sel = canvas.getSelection();
    final var n = sel.getSelected().size();
    final var select = new ArrayList<CanvasObject>(n);
    final var clones = new ArrayList<CanvasObject>(n);
    for (final var obj : sel.getSelected()) {
      if (obj.canRemove()) {
        final var copy = obj.clone();
        copy.translate(10, 10);
        clones.add(copy);
        select.add(copy);
      } else {
        select.add(obj);
      }
    }

    if (!clones.isEmpty()) {
      canvas
          .getProject()
          .doAction(
              new SelectionAction(
                  canvas, S.getter("duplicateSelectionAction"), null, clones, select, null, null));
    }
  }

  @Override
  public void lower() {
    final var act = ModelReorderAction.createLower(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void lowerBottom() {
    final var act = ModelReorderAction.createLowerBottom(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void modelChanged(CanvasModelEvent event) {
    computeEnabled();
  }

  @Override
  public void paste() {
    final var clip = Clipboard.get();
    final var contents = clip.getElements();
    final var add = new ArrayList<CanvasObject>(contents.size());
    for (final var obj : contents) {
      add.add(obj.clone());
    }
    if (add.isEmpty()) return;

    // find how far we have to translate shapes so that at least one of the
    // pasted shapes doesn't match what's already in the model
    final var raw = canvas.getModel().getObjectsFromBottom();
    final var cur = new MatchingSet<>(raw);
    int dx = 0;
    while (true) {
      // if any shapes in "add" aren't in canvas, we are done
      boolean allMatch = true;
      for (final var obj : add) {
        if (!cur.contains(obj)) {
          allMatch = false;
          break;
        }
      }
      if (!allMatch) break;

      // otherwise translate everything by 10 pixels and repeat test
      for (final var obj : add) {
        obj.translate(10, 10);
      }
      dx += 10;
    }

    var anchorLocation = clip.getAnchorLocation();
    if (anchorLocation != null && dx != 0) {
      anchorLocation = anchorLocation.translate(dx, dx);
    }

    canvas
        .getProject()
        .doAction(
            new SelectionAction(
                canvas,
                S.getter("pasteClipboardAction"),
                null,
                add,
                add,
                anchorLocation,
                clip.getAnchorFacing()));
  }

  @Override
  public void propertyChange(PropertyChangeEvent e) {
    final var prop = e.getPropertyName();
    if (prop.equals(Canvas.MODEL_PROPERTY)) {
      final var oldModel = (CanvasModel) e.getOldValue();
      if (oldModel != null) {
        oldModel.removeCanvasModelListener(this);
      }
      final var newModel = (CanvasModel) e.getNewValue();
      if (newModel != null) {
        newModel.addCanvasModelListener(this);
      }
    }
  }

  @Override
  public void raise() {
    final var act = ModelReorderAction.createRaise(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void raiseTop() {
    final var act = ModelReorderAction.createRaiseTop(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void removeControlPoint() {
    final var sel = canvas.getSelection();
    final var handle = sel.getSelectedHandle();
    canvas.doAction(new ModelDeleteHandleAction(canvas.getModel(), handle));
  }

  @Override
  public void selectAll() {
    final var sel = canvas.getSelection();
    sel.setSelected(canvas.getModel().getObjectsFromBottom(), true);
    canvas.repaint();
  }

  @Override
  public void selectionChanged(SelectionEvent e) {
    computeEnabled();
  }
}
