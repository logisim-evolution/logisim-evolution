/*
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

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.FactoryAttributes;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.tools.key.KeyConfigurationEvent;
import com.cburch.logisim.tools.key.KeyConfigurationResult;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.util.HashMap;
import java.util.Map;
import lombok.val;

public class ToolAttributeAction extends Action {
  private final KeyConfigurationResult config;
  private Map<Attribute<?>, Object> oldValues;

  private ToolAttributeAction(KeyConfigurationResult config) {
    this.config = config;
    this.oldValues = new HashMap<>(2);
  }

  public static Action create(KeyConfigurationResult results) {
    return new ToolAttributeAction(results);
  }

  public static Action create(Tool tool, Attribute<?> attr, Object value) {
    val attrs = tool.getAttributeSet();
    val event  = new KeyConfigurationEvent(0, attrs, null, null);
    val result = new KeyConfigurationResult(event, attr, value);
    return new ToolAttributeAction(result);
  }

  @Override
  public String getName() {
    return S.get("changeToolAttrAction");
  }

  @Override
  public void doIt(Project proj) {
    if (affectsAppearance()) {
      val xn = new ActionTransaction(true);
      xn.execute();
    } else {
      execute(true);
    }
  }

  @Override
  public void undo(Project proj) {
    if (affectsAppearance()) {
      val xn = new ActionTransaction(true);
      xn.execute();
    } else {
      execute(false);
    }
  }

  boolean affectsAppearance() {
    val attrs = config.getEvent().getAttributeSet();
    if (attrs instanceof FactoryAttributes) {
      val factory = ((FactoryAttributes) attrs).getFactory();
      if (factory instanceof SubcircuitFactory) {
        for (Attribute<?> attr : config.getAttributeValues().keySet()) {
          if (attr == CircuitAttributes.APPEARANCE_ATTR) return true;
        }
      } else if (factory instanceof VhdlEntity) {
        for (Attribute<?> attr : config.getAttributeValues().keySet()) {
          if (attr == StdAttr.APPEARANCE) return true;
        }
      }
    }
    return false;
  }

  private void execute(boolean forward) {
    if (forward) {
      val attrs = config.getEvent().getAttributeSet();
      Map<Attribute<?>, Object> newValues = config.getAttributeValues();
      Map<Attribute<?>, Object> oldValues = new HashMap<>(newValues.size());
      for (Map.Entry<Attribute<?>, Object> entry : newValues.entrySet()) {
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) entry.getKey();
        oldValues.put(attr, attrs.getValue(attr));
        attrs.setValue(attr, entry.getValue());
      }
      this.oldValues = oldValues;
    } else {
      val attrs = config.getEvent().getAttributeSet();
      Map<Attribute<?>, Object> oldValues = this.oldValues;
      for (Map.Entry<Attribute<?>, Object> entry : oldValues.entrySet()) {
        @SuppressWarnings("unchecked")
        Attribute<Object> attr = (Attribute<Object>) entry.getKey();
        attrs.setValue(attr, entry.getValue());
      }
    }
  }

  private class ActionTransaction extends CircuitTransaction {
    private final boolean forward;

    ActionTransaction(boolean forward) {
      this.forward = forward;
    }

    @Override
    protected Map<Circuit, Integer> getAccessedCircuits() {
      val accessMap = new HashMap<Circuit, Integer>();
      val attrs = config.getEvent().getAttributeSet();
      if (attrs instanceof FactoryAttributes) {
        val factory = ((FactoryAttributes) attrs).getFactory();
        if (factory instanceof SubcircuitFactory) {
          val circuit = ((SubcircuitFactory) factory).getSubcircuit();
          for (val supercirc : circuit.getCircuitsUsingThis()) {
            accessMap.put(supercirc, READ_WRITE);
          }
        } else if (factory instanceof VhdlEntity) {
          val vhdl = (VhdlEntity) factory;
          for (val supercirc : vhdl.getCircuitsUsingThis()) {
            accessMap.put(supercirc, READ_WRITE);
          }
        }
      }
      return accessMap;
    }

    @Override
    protected void run(CircuitMutator mutator) {
      ToolAttributeAction.this.execute(forward);
    }
  }
}
