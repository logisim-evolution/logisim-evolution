/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutator;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
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
    AttributeSet attrs = tool.getAttributeSet();
    KeyConfigurationEvent e = new KeyConfigurationEvent(0, attrs, null, null);
    KeyConfigurationResult r = new KeyConfigurationResult(e, attr, value);
    return new ToolAttributeAction(r);
  }

  @Override
  public String getName() {
    return S.get("changeToolAttrAction");
  }

  @Override
  public void doIt(Project proj) {
    if (affectsAppearance()) {
      ActionTransaction xn = new ActionTransaction(true);
      xn.execute();
    } else {
      execute(true);
    }
  }

  @Override
  public void undo(Project proj) {
    if (affectsAppearance()) {
      ActionTransaction xn = new ActionTransaction(true);
      xn.execute();
    } else {
      execute(false);
    }
  }

  boolean affectsAppearance() {
    AttributeSet attrs = config.getEvent().getAttributeSet();
    if (attrs instanceof FactoryAttributes factoryAttributes) {
      final var factory = factoryAttributes.getFactory();
      if (factory instanceof SubcircuitFactory) {
        for (final var attr : config.getAttributeValues().keySet()) {
          if (attr == CircuitAttributes.APPEARANCE_ATTR) return true;
        }
      } else if (factory instanceof VhdlEntity) {
        for (final var attr : config.getAttributeValues().keySet()) {
          if (attr == StdAttr.APPEARANCE) return true;
        }
      }
    }
    return false;
  }

  private void execute(boolean forward) {
    if (forward) {
      AttributeSet attrs = config.getEvent().getAttributeSet();
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
      AttributeSet attrs = config.getEvent().getAttributeSet();
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
      Map<Circuit, Integer> accessMap = new HashMap<>();
      AttributeSet attrs = config.getEvent().getAttributeSet();
      if (attrs instanceof FactoryAttributes factoryAttributes) {
        final var factory = factoryAttributes.getFactory();
        if (factory instanceof SubcircuitFactory sub) {
          final var circuit = sub.getSubcircuit();
          for (final var supercirc : circuit.getCircuitsUsingThis()) {
            accessMap.put(supercirc, READ_WRITE);
          }
        } else if (factory instanceof VhdlEntity vhdl) {
          for (final var supercirc : vhdl.getCircuitsUsingThis()) {
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
