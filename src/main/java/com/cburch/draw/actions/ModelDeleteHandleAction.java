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

public class ModelDeleteHandleAction extends ModelAction {
  private final Handle handle;
  private Handle previous;

  public ModelDeleteHandleAction(CanvasModel model, Handle handle) {
    super(model);
    this.handle = handle;
  }

  @Override
  void doSub(CanvasModel model) {
    previous = model.deleteHandle(handle);
  }

  @Override
  public String getName() {
    return S.get("actionDeleteHandle");
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.singleton(handle.getObject());
  }

  @Override
  void undoSub(CanvasModel model) {
    model.insertHandle(handle, previous);
  }
}
