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
import java.util.Collection;
import java.util.Collections;

public class ModelInsertHandleAction extends ModelAction {
  private final Handle desired;

  public ModelInsertHandleAction(CanvasModel model, Handle desired) {
    super(model);
    this.desired = desired;
  }

  @Override
  void doSub(CanvasModel model) {
    model.insertHandle(desired, null);
  }

  @Override
  public String getName() {
    return S.get("actionInsertHandle");
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.singleton(desired.getObject());
  }

  @Override
  void undoSub(CanvasModel model) {
    model.deleteHandle(desired);
  }
}
