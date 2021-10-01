/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.actions;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.undo.UndoAction;
import java.util.Collection;
import java.util.Collections;

public abstract class ModelAction implements UndoAction {
  private final CanvasModel model;

  public ModelAction(CanvasModel model) {
    this.model = model;
  }

  static String getShapesName(Collection<CanvasObject> coll) {
    if (coll.size() != 1) {
      return S.get("shapeMultiple");
    } else {
      final var shape = coll.iterator().next();
      return shape.getDisplayName();
    }
  }

  @Override
  public final void doIt() {
    doSub(model);
  }

  abstract void doSub(CanvasModel model);

  public CanvasModel getModel() {
    return model;
  }

  @Override
  public abstract String getName();

  public Collection<CanvasObject> getObjects() {
    return Collections.emptySet();
  }

  @Override
  public final void undo() {
    undoSub(model);
  }

  abstract void undoSub(CanvasModel model);
}
