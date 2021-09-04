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
import lombok.Getter;
import lombok.val;

public class ModelMoveHandleAction extends ModelAction {
  private final HandleGesture gesture;
  @Getter private Handle newHandle;

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

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.singleton(gesture.getHandle().getObject());
  }

  @Override
  void undoSub(CanvasModel model) {
    val oldHandle = gesture.getHandle();
    val dx = oldHandle.getX() - newHandle.getX();
    val dy = oldHandle.getY() - newHandle.getY();
    val reverse = new HandleGesture(newHandle, dx, dy, 0);
    model.moveHandle(reverse);
  }
}
