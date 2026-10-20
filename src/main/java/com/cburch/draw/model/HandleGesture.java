/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import java.awt.event.InputEvent;

public class HandleGesture {
  private final Handle handle;
  private final int dx;
  private final int dy;
  private final int modifiersEx;
  private Handle resultingHandle;

  public HandleGesture(Handle handle, int dx, int dy, int modifiersEx) {
    this.handle = handle;
    this.dx = dx;
    this.dy = dy;
    this.modifiersEx = modifiersEx;
  }

  public int getDeltaX() {
    return dx;
  }

  public int getDeltaY() {
    return dy;
  }

  public Handle getHandle() {
    return handle;
  }

  public int getModifiersEx() {
    return modifiersEx;
  }

  public Handle getResultingHandle() {
    return resultingHandle;
  }

  public void setResultingHandle(Handle value) {
    resultingHandle = value;
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
    return ("HandleGesture() ["
        + dx
        + ", "
        + dy
        + " : "
        + handle.getObject()
        + "/"
        + handle.getX()
        + ", "
        + handle.getY()
        + "]");
  }
}
