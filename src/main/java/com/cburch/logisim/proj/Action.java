/*
 * Logisim-evolution - digital logic design tool and simulator
 * © 2001 Logisim-evolution contributors
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.proj;

public abstract class Action {
  public Action append(Action other) {
    return new JoinedAction(this, other);
  }

  public abstract void doIt(Project proj);

  public abstract String getName();

  public boolean isModification() {
    return true;
  }

  public boolean shouldAppendTo(Action other) {
    return false;
  }

  public abstract void undo(Project proj);
}
