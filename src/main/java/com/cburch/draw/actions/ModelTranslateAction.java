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
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

public class ModelTranslateAction extends ModelAction {
  private final Set<CanvasObject> moved;
  private final int dx;
  private final int dy;

  public ModelTranslateAction(CanvasModel model, Collection<CanvasObject> moved, int dx, int dy) {
    super(model);
    this.moved = new HashSet<>(moved);
    this.dx = dx;
    this.dy = dy;
  }

  @Override
  void doSub(CanvasModel model) {
    model.translateObjects(moved, dx, dy);
  }

  @Override
  public String getName() {
    return S.get("actionTranslate", getShapesName(moved));
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.unmodifiableSet(moved);
  }

  @Override
  void undoSub(CanvasModel model) {
    model.translateObjects(moved, -dx, -dy);
  }
}
