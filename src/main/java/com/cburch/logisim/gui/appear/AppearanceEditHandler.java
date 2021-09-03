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
import com.cburch.draw.canvas.Selection;
import com.cburch.draw.canvas.SelectionEvent;
import com.cburch.draw.canvas.SelectionListener;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasModelEvent;
import com.cburch.draw.model.CanvasModelListener;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.model.Handle;
import com.cburch.draw.util.MatchingSet;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.menu.EditHandler;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class AppearanceEditHandler extends EditHandler implements SelectionListener, PropertyChangeListener, CanvasModelListener {
  private final AppearanceCanvas canvas;

  AppearanceEditHandler(AppearanceCanvas canvas) {
    this.canvas = canvas;
    canvas.getSelection().addSelectionListener(this);
    CanvasModel model = canvas.getModel();
    if (model != null) model.addCanvasModelListener(this);
    canvas.addPropertyChangeListener(Canvas.MODEL_PROPERTY, this);
  }

  @Override
  public void addControlPoint() {
    Selection sel = canvas.getSelection();
    Handle handle = sel.getSelectedHandle();
    canvas.doAction(new ModelInsertHandleAction(canvas.getModel(), handle));
  }

  @Override
  public void computeEnabled() {
    Project proj = canvas.getProject();
    Circuit circ = canvas.getCircuit();
    Selection sel = canvas.getSelection();
    boolean selEmpty = sel.isEmpty();
    boolean canChange = proj.getLogisimFile().contains(circ);
    final boolean clipExists = !Clipboard.isEmpty();
    boolean selHasRemovable = false;
    for (CanvasObject o : sel.getSelected()) {
      if (!(o instanceof AppearanceElement)) {
        selHasRemovable = true;
        break;
      }
    }
    boolean canRaise;
    boolean canLower;
    if (!selEmpty && canChange) {
      Map<CanvasObject, Integer> zs = ZOrder.getZIndex(sel.getSelected(), canvas.getModel());
      int zmin = Integer.MAX_VALUE;
      int zmax = Integer.MIN_VALUE;
      int count = 0;
      for (Map.Entry<CanvasObject, Integer> entry : zs.entrySet()) {
        if (!(entry.getKey() instanceof AppearanceElement)) {
          count++;
          int z = entry.getValue();
          if (z < zmin) zmin = z;
          if (z > zmax) zmax = z;
        }
      }
      int maxPoss = AppearanceCanvas.getMaxIndex(canvas.getModel());
      if (count > 0 && count <= maxPoss) {
        canRaise = zmin <= maxPoss - count;
        canLower = zmax >= count;
      } else {
        canRaise = false;
        canLower = false;
      }
    } else {
      canRaise = false;
      canLower = false;
    }
    boolean canAddCtrl = false;
    boolean canRemCtrl = false;
    Handle handle = sel.getSelectedHandle();
    if (handle != null && canChange) {
      CanvasObject o = handle.getObject();
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
    Selection sel = canvas.getSelection();
    int n = sel.getSelected().size();
    List<CanvasObject> select = new ArrayList<>(n);
    List<CanvasObject> remove = new ArrayList<>(n);
    Location anchorLocation = null;
    Direction anchorFacing = null;
    for (CanvasObject o : sel.getSelected()) {
      if (o.canRemove()) {
        remove.add(o);
      } else {
        select.add(o);
        if (o instanceof AppearanceAnchor) {
          AppearanceAnchor anchor = (AppearanceAnchor) o;
          anchorLocation = anchor.getLocation();
          anchorFacing = anchor.getFacing();
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
    Selection sel = canvas.getSelection();
    int n = sel.getSelected().size();
    List<CanvasObject> select = new ArrayList<>(n);
    List<CanvasObject> clones = new ArrayList<>(n);
    for (CanvasObject o : sel.getSelected()) {
      if (o.canRemove()) {
        CanvasObject copy = o.clone();
        copy.translate(10, 10);
        clones.add(copy);
        select.add(copy);
      } else {
        select.add(o);
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
    ModelReorderAction act = ModelReorderAction.createLower(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void lowerBottom() {
    ModelReorderAction act = ModelReorderAction.createLowerBottom(canvas.getModel(), canvas.getSelection().getSelected());
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
    ClipboardContents clip = Clipboard.get();
    Collection<CanvasObject> contents = clip.getElements();
    List<CanvasObject> add = new ArrayList<>(contents.size());
    for (CanvasObject o : contents) {
      add.add(o.clone());
    }
    if (add.isEmpty()) return;

    // find how far we have to translate shapes so that at least one of the
    // pasted shapes doesn't match what's already in the model
    Collection<CanvasObject> raw = canvas.getModel().getObjectsFromBottom();
    MatchingSet<CanvasObject> cur = new MatchingSet<>(raw);
    int dx = 0;
    while (true) {
      // if any shapes in "add" aren't in canvas, we are done
      boolean allMatch = true;
      for (CanvasObject o : add) {
        if (!cur.contains(o)) {
          allMatch = false;
          break;
        }
      }
      if (!allMatch) break;

      // otherwise translate everything by 10 pixels and repeat test
      for (CanvasObject o : add) {
        o.translate(10, 10);
      }
      dx += 10;
    }

    Location anchorLocation = clip.getAnchorLocation();
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
    String prop = e.getPropertyName();
    if (prop.equals(Canvas.MODEL_PROPERTY)) {
      CanvasModel oldModel = (CanvasModel) e.getOldValue();
      if (oldModel != null) {
        oldModel.removeCanvasModelListener(this);
      }
      CanvasModel newModel = (CanvasModel) e.getNewValue();
      if (newModel != null) {
        newModel.addCanvasModelListener(this);
      }
    }
  }

  @Override
  public void raise() {
    ModelReorderAction act =
        ModelReorderAction.createRaise(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void raiseTop() {
    ModelReorderAction act =
        ModelReorderAction.createRaiseTop(canvas.getModel(), canvas.getSelection().getSelected());
    if (act != null) {
      canvas.doAction(act);
    }
  }

  @Override
  public void removeControlPoint() {
    Selection sel = canvas.getSelection();
    Handle handle = sel.getSelectedHandle();
    canvas.doAction(new ModelDeleteHandleAction(canvas.getModel(), handle));
  }

  @Override
  public void selectAll() {
    Selection sel = canvas.getSelection();
    sel.setSelected(canvas.getModel().getObjectsFromBottom(), true);
    canvas.repaint();
  }

  @Override
  public void selectionChanged(SelectionEvent e) {
    computeEnabled();
  }
}
