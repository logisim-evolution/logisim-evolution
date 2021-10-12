/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools.move;

class MoveRequest {
  private final MoveGesture gesture;
  private final int dx;
  private final int dy;

  public MoveRequest(MoveGesture gesture, int dx, int dy) {
    this.gesture = gesture;
    this.dx = dx;
    this.dy = dy;
  }

  @Override
  public boolean equals(Object other) {
    if (other instanceof MoveRequest o) {
      return this.gesture == o.gesture && this.dx == o.dx && this.dy == o.dy;
    } else {
      return false;
    }
  }

  public int getDeltaX() {
    return dx;
  }

  public int getDeltaY() {
    return dy;
  }

  public MoveGesture getMoveGesture() {
    return gesture;
  }

  @Override
  public int hashCode() {
    return (gesture.hashCode() * 31 + dx) * 31 + dy;
  }
}
