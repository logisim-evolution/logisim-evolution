/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RevertAppearanceAction extends Action {
  private final Circuit circuit;
  private ArrayList<CanvasObject> old;

  public RevertAppearanceAction(Circuit circuit) {
    this.circuit = circuit;
  }

  @Override
  public void doIt(Project proj) {
    final var xn = new ActionTransaction(true);
    xn.execute();
  }

  @Override
  public String getName() {
    return S.get("revertAppearanceAction");
  }

  @Override
  public void undo(Project proj) {
    final var xn = new ActionTransaction(false);
    xn.execute();
  }

  private class ActionTransaction extends CircuitTransaction {
    private final boolean forward;

    ActionTransaction(boolean forward) {
      this.forward = forward;
    }

    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      final var accessMap = new HashMap<Circuit, Integer>();
      for (final var superCircuit : circuit.getCircuitsUsingThis()) {
        accessMap.put(superCircuit, READ_WRITE);
      }
      return accessMap;
    }

    @Override
    protected void run(CircuitMutator mutator) {
      final var appear = circuit.getAppearance();
      if (forward) {
        old = new ArrayList<>(appear.getObjectsFromBottom());
      } else {
        appear.setObjectsForce(old);
      }
    }
  }
}
