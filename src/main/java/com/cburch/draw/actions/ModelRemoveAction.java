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
import com.cburch.draw.util.ZOrder;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;

public class ModelRemoveAction extends ModelAction {
  private final Map<CanvasObject, Integer> removed;

  public ModelRemoveAction(CanvasModel model, CanvasObject removed) {
    this(model, Collections.singleton(removed));
  }

  public ModelRemoveAction(CanvasModel model, Collection<CanvasObject> removed) {
    super(model);
    this.removed = ZOrder.getZIndex(removed, model);
  }

  @Override
  void doSub(CanvasModel model) {
    model.removeObjects(removed.keySet());
  }

  @Override
  public String getName() {
    return S.get("actionRemove", getShapesName(removed.keySet()));
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.unmodifiableSet(removed.keySet());
  }

  @Override
  void undoSub(CanvasModel model) {
    model.addObjects(removed);
  }
}
