/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import com.cburch.draw.model.CanvasObject;
import java.util.Collection;
import java.util.EventObject;

public class SelectionEvent extends EventObject {
  public static final int ACTION_ADDED = 0;
  public static final int ACTION_REMOVED = 1;
  public static final int ACTION_HANDLE = 2;
  private static final long serialVersionUID = 1L;
  private final int action;
  private final Collection<CanvasObject> affected;

  public SelectionEvent(Selection source, int action, Collection<CanvasObject> affected) {
    super(source);
    this.action = action;
    this.affected = affected;
  }

  public int getAction() {
    return action;
  }

  public Collection<CanvasObject> getAffected() {
    return affected;
  }

  public Selection getSelection() {
    return (Selection) getSource();
  }
}
