/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;

public class CircuitAction extends Action {
  private final StringGetter name;
  private final CircuitTransaction forward;
  private CircuitTransaction reverse;

  CircuitAction(StringGetter name, CircuitMutation forward) {
    this.name = name;
    this.forward = forward;
  }

  @Override
  public void doIt(Project proj) {
    final var result = forward.execute();
    if (result != null) {
      reverse = result.getReverseTransaction();
    }
  }

  @Override
  public String getName() {
    return name.toString();
  }

  @Override
  public void undo(Project proj) {
    if (reverse != null) {
      reverse.execute();
    }
  }
}
