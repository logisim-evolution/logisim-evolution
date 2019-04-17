/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class RevertAppearanceAction extends Action {
  private Circuit circuit;
  private ArrayList<CanvasObject> old;
  private boolean wasDefault;

  private class ActionTransaction extends CircuitTransaction {
    private boolean forward;

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
        CircuitAppearance appear = circuit.getAppearance();
        wasDefault = appear.isDefaultAppearance();
        old = new ArrayList<CanvasObject>(appear.getObjectsFromBottom());
        appear.setDefaultAppearance(true);
      } else {
        CircuitAppearance appear = circuit.getAppearance();
        appear.setObjectsForce(old);
        appear.setDefaultAppearance(wasDefault);
      }
    }
  }

  public RevertAppearanceAction(Circuit circuit) {
    this.circuit = circuit;
  }

  @Override
  public void doIt(Project proj) {
    ActionTransaction xn = new ActionTransaction(true);
    xn.execute();
  }

  @Override
  public String getName() {
    return S.get("revertAppearanceAction");
  }

  @Override
  public void undo(Project proj) {
    ActionTransaction xn = new ActionTransaction(false);
    xn.execute();
  }
}
