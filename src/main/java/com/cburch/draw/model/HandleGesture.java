/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import com.cburch.logisim.util.LineBuffer;
import java.awt.event.InputEvent;
import lombok.Getter;
import lombok.Setter;

public class HandleGesture {
  @Getter private final Handle handle;
  @Getter private final int deltaX;
  @Getter private final int deltaY;
  @Getter private final int modifiersEx;
  @Setter @Getter private Handle resultingHandle;

  public HandleGesture(Handle handle, int deltaX, int deltaY, int modifiersEx) {
    this.handle = handle;
    this.deltaX = deltaX;
    this.deltaY = deltaY;
    this.modifiersEx = modifiersEx;
  }

  public boolean isAltDown() {
    return (modifiersEx & InputEvent.ALT_DOWN_MASK) != 0;
  }

  public boolean isControlDown() {
    return (modifiersEx & InputEvent.CTRL_DOWN_MASK) != 0;
  }

  public boolean isShiftDown() {
    return (modifiersEx & InputEvent.SHIFT_DOWN_MASK) != 0;
  }

  @Override
  public String toString() {
    return "HandleGesture() ["
        + deltaX
        + ", "
        + deltaY
        + " : "
        + handle.getObject()
        + "/"
        + handle.getX()
        + ", "
        + handle.getY()
        + "]";
  }
}
