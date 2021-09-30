/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.util.Collection;

public class CircuitChange {
  public static CircuitChange add(Circuit circuit, Component comp) {
    return new CircuitChange(circuit, ADD, comp);
  }

  public static CircuitChange addAll(Circuit circuit, Collection<? extends Component> comps) {
    return new CircuitChange(circuit, ADD_ALL, comps);
  }

  public static CircuitChange clear(Circuit circuit, Collection<Component> oldComponents) {
    return new CircuitChange(circuit, CLEAR, oldComponents);
  }

  public static CircuitChange remove(Circuit circuit, Component comp) {
    return new CircuitChange(circuit, REMOVE, comp);
  }

  public static CircuitChange removeAll(Circuit circuit, Collection<? extends Component> comps) {
    return new CircuitChange(circuit, REMOVE_ALL, comps);
  }

  public static CircuitChange replace(Circuit circuit, ReplacementMap replMap) {
    return new CircuitChange(circuit, REPLACE, null, null, null, replMap);
  }

  public static CircuitChange set(
      Circuit circuit, Component comp, Attribute<?> attr, Object value) {
    return new CircuitChange(circuit, SET, comp, attr, null, value);
  }

  public static CircuitChange set(
      Circuit circuit, Component comp, Attribute<?> attr, Object oldValue, Object newValue) {
    return new CircuitChange(circuit, SET, comp, attr, oldValue, newValue);
  }

  public static CircuitChange setForCircuit(Circuit circuit, Attribute<?> attr, Object v) {
    return new CircuitChange(circuit, SET_FOR_CIRCUIT, null, attr, null, v);
  }

  public static CircuitChange setForCircuit(
      Circuit circuit, Attribute<?> attr, Object oldValue, Object newValue) {
    return new CircuitChange(circuit, SET_FOR_CIRCUIT, null, attr, oldValue, newValue);
  }

  static final int CLEAR = 0;

  static final int ADD = 1;

  static final int ADD_ALL = 2;

  static final int REMOVE = 3;

  static final int REMOVE_ALL = 4;

  static final int REPLACE = 5;

  static final int SET = 6;

  static final int SET_FOR_CIRCUIT = 7;

  private final Circuit circuit;
  private final int type;
  private final Component comp;
  private Collection<? extends Component> comps;
  private final Attribute<?> attr;
  private final Object oldValue;
  private final Object newValue;

  private CircuitChange(Circuit circuit, int type, Collection<? extends Component> comps) {
    this(circuit, type, null, null, null, null);
    this.comps = comps;
  }

  private CircuitChange(Circuit circuit, int type, Component comp) {
    this(circuit, type, comp, null, null, null);
  }

  private CircuitChange(
      Circuit circuit,
      int type,
      Component comp,
      Attribute<?> attr,
      Object oldValue,
      Object newValue) {
    this.circuit = circuit;
    this.type = type;
    this.comp = comp;
    this.attr = attr;
    this.oldValue = oldValue;
    this.newValue = newValue;
  }

  boolean concernsSupercircuit() {
    switch (type) {
      case CLEAR:
        return true;
      case ADD, REMOVE:
        return comp.getFactory() instanceof Pin;
      case ADD_ALL, REMOVE_ALL:
        for (final var comp : comps) {
          if (comp.getFactory() instanceof Pin) return true;
        }
        return false;
      case REPLACE:
        final var repl = (ReplacementMap) newValue;
        for (final var comp : repl.getRemovals()) {
          if (comp.getFactory() instanceof Pin) return true;
        }
        for (final var comp : repl.getAdditions()) {
          if (comp.getFactory() instanceof Pin) return true;
        }
        return false;
      case SET:
        return comp.getFactory() instanceof Pin
            && (attr == StdAttr.WIDTH || attr == Pin.ATTR_TYPE || attr == StdAttr.LABEL);
      case SET_FOR_CIRCUIT:
        return (attr == CircuitAttributes.NAME_ATTR
            || attr == CircuitAttributes.NAMED_CIRCUIT_BOX_FIXED_SIZE
            || attr == CircuitAttributes.APPEARANCE_ATTR);
      default:
        return false;
    }
  }

  boolean concernsSiblingComponents() {
    if (type == SET) {
      return (comp.getFactory() instanceof SubcircuitFactory
          && attr == CircuitAttributes.APPEARANCE_ATTR)
          || (comp.getFactory() instanceof VhdlEntity && attr == StdAttr.APPEARANCE);
    }
    return false;
  }

  void execute(CircuitMutator mutator, ReplacementMap prevReplacements) {
    switch (type) {
      case CLEAR:
        mutator.clear(circuit);
        prevReplacements.reset();
        break;
      case ADD:
        prevReplacements.add(comp);
        break;
      case ADD_ALL:
        for (final var comp : comps) prevReplacements.add(comp);
        break;
      case REMOVE:
        prevReplacements.remove(comp);
        break;
      case REMOVE_ALL:
        for (final var comp : comps) prevReplacements.remove(comp);
        break;
      case REPLACE:
        prevReplacements.append((ReplacementMap) newValue);
        break;
      case SET:
        mutator.replace(circuit, prevReplacements);
        prevReplacements.reset();
        mutator.set(circuit, comp, attr, newValue);
        break;
      case SET_FOR_CIRCUIT:
        mutator.replace(circuit, prevReplacements);
        prevReplacements.reset();
        mutator.setForCircuit(circuit, attr, newValue);
        break;
      default:
        throw new IllegalArgumentException("unknown change type " + type);
    }
  }

  public Attribute<?> getAttribute() {
    return attr;
  }

  public Circuit getCircuit() {
    return circuit;
  }

  public Component getComponent() {
    return comp;
  }

  public Object getNewValue() {
    return newValue;
  }

  public Object getOldValue() {
    return oldValue;
  }

  CircuitChange getReverseChange() {
    return switch (type) {
      case CLEAR -> CircuitChange.addAll(circuit, comps);
      case ADD -> CircuitChange.remove(circuit, comp);
      case ADD_ALL -> CircuitChange.removeAll(circuit, comps);
      case REMOVE -> CircuitChange.add(circuit, comp);
      case REMOVE_ALL -> CircuitChange.addAll(circuit, comps);
      case SET -> CircuitChange.set(circuit, comp, attr, newValue, oldValue);
      case SET_FOR_CIRCUIT -> CircuitChange.setForCircuit(circuit, attr, newValue, oldValue);
      case REPLACE -> CircuitChange.replace(circuit, ((ReplacementMap) newValue).getInverseMap());
      default -> throw new IllegalArgumentException("unknown change type " + type);
    };
  }

  public int getType() {
    return type;
  }
}
