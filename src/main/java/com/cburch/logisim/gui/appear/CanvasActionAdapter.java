/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import com.cburch.draw.actions.ModelAction;
import com.cburch.draw.undo.UndoAction;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;

public class CanvasActionAdapter extends com.cburch.logisim.proj.Action {
  private final Circuit circuit;
  private final UndoAction canvasAction;

  public CanvasActionAdapter(Circuit circuit, UndoAction action) {
    this.circuit = circuit;
    this.canvasAction = action;
  }

  private boolean affectsPorts() {
    if (canvasAction instanceof ModelAction action) {
      for (final var obj : action.getObjects()) {
        if (obj instanceof AppearanceElement) return true;
      }
    }
    return false;
  }

  @Override
  public void doIt(Project proj) {
    if (affectsPorts()) {
      final var xn = new ActionTransaction(true);
      xn.execute();
    } else {
      canvasAction.doIt();
    }
  }

  @Override
  public String getName() {
    return canvasAction.getName();
  }

  @Override
  public void undo(Project proj) {
    if (affectsPorts()) {
      final var xn = new ActionTransaction(false);
      xn.execute();
    } else {
      canvasAction.undo();
    }
  }

  private class ActionTransaction extends CircuitTransaction {
    private final boolean forward;

    ActionTransaction(boolean forward) {
      this.forward = forward;
    }

    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      final var accessMap = new HashMap<Circuit, Integer>();
      for (final var supercirc : circuit.getCircuitsUsingThis()) {
        accessMap.put(supercirc, READ_WRITE);
      }
      return accessMap;
    }

    @Override
    protected void run(CircuitMutator mutator) {
      if (forward) {
        canvasAction.doIt();
      } else {
        canvasAction.undo();
      }
    }
  }
}
