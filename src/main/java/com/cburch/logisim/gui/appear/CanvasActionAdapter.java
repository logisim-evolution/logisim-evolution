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
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.undo.Action;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.appear.AppearanceElement;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;

public class CanvasActionAdapter extends com.cburch.logisim.proj.Action {
  private final Circuit circuit;
  private final Action canvasAction;
  private boolean wasDefault;

  public CanvasActionAdapter(Circuit circuit, Action action) {
    this.circuit = circuit;
    this.canvasAction = action;
  }

  private boolean affectsPorts() {
    if (canvasAction instanceof ModelAction) {
      for (CanvasObject o : ((ModelAction) canvasAction).getObjects()) {
        if (o instanceof AppearanceElement) {
          return true;
        }
      }
    }
    return false;
  }

  @Override
  public void doIt(Project proj) {
    wasDefault = circuit.getAppearance().isDefaultAppearance();
    if (affectsPorts()) {
      ActionTransaction xn = new ActionTransaction(true);
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
      ActionTransaction xn = new ActionTransaction(false);
      xn.execute();
    } else {
      canvasAction.undo();
    }
    circuit.getAppearance().setDefaultAppearance(wasDefault);
  }

  private class ActionTransaction extends CircuitTransaction {
    private final boolean forward;

    ActionTransaction(boolean forward) {
      this.forward = forward;
    }

    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      Map<Circuit, Integer> accessMap = new HashMap<>();
      for (Circuit supercirc : circuit.getCircuitsUsingThis()) {
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
