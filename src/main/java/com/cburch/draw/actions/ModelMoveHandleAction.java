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
import com.cburch.draw.model.Handle;
import com.cburch.draw.model.HandleGesture;
import java.util.Collection;
import java.util.Collections;

public class ModelMoveHandleAction extends ModelAction {
  private final HandleGesture gesture;
  private Handle newHandle;

  public ModelMoveHandleAction(CanvasModel model, HandleGesture gesture) {
    super(model);
    this.gesture = gesture;
  }

  @Override
  void doSub(CanvasModel model) {
    newHandle = model.moveHandle(gesture);
  }

  @Override
  public String getName() {
    return S.get("actionMoveHandle");
  }

  public Handle getNewHandle() {
    return newHandle;
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.singleton(gesture.getHandle().getObject());
  }

  @Override
  void undoSub(CanvasModel model) {
    final var oldHandle = gesture.getHandle();
    final var dx = oldHandle.getX() - newHandle.getX();
    final var dy = oldHandle.getY() - newHandle.getY();
    final var reverse = new HandleGesture(newHandle, dx, dy, 0);
    model.moveHandle(reverse);
  }
}
