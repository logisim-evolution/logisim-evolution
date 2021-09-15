/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public class InstanceStateImpl implements InstanceState {
  private CircuitState circuitState;
  private Component component;

  public InstanceStateImpl(CircuitState circuitState, Component component) {
    this.circuitState = circuitState;
    this.component = component;

    if (component instanceof InstanceComponent) {
      ((InstanceComponent) component).setInstanceStateImpl(this);
    }
  }

  public void fireInvalidated() {
    if (component instanceof InstanceComponent) {
      ((InstanceComponent) component).fireInvalidated();
    }
  }

  public AttributeSet getAttributeSet() {
    return component.getAttributeSet();
  }

  public <E> E getAttributeValue(Attribute<E> attr) {
    return component.getAttributeSet().getValue(attr);
  }

  public CircuitState getCircuitState() {
    return circuitState;
  }

  public InstanceData getData() {
    return ((InstanceData) circuitState.getData(component));
  }

  public InstanceFactory getFactory() {
    if (component instanceof InstanceComponent) {
      InstanceComponent comp = (InstanceComponent) component;
      return (InstanceFactory) comp.getFactory();
    } else {
      return null;
    }
  }

  public Instance getInstance() {
    if (component instanceof InstanceComponent) {
      return ((InstanceComponent) component).getInstance();
    } else {
      return null;
    }
  }

  public int getPortIndex(Port port) {
    return this.getInstance().getPorts().indexOf(port);
  }

  public Value getPortValue(int portIndex) {
    EndData data = component.getEnd(portIndex);
    return circuitState.getValue(data.getLocation());
  }

  public Project getProject() {
    return circuitState.getProject();
  }

  public int getTickCount() {
    return circuitState.getPropagator().getTickCount();
  }

  public boolean isCircuitRoot() {
    return !circuitState.isSubstate();
  }

  public boolean isPortConnected(int index) {
    Circuit circ = circuitState.getCircuit();
    Location loc = component.getEnd(index).getLocation();
    return circ.isConnected(loc, component);
  }

  public void repurpose(CircuitState circuitState, Component component) {
    this.circuitState = circuitState;
    this.component = component;
  }

  public void setData(InstanceData value) {
    circuitState.setData(component, value);
  }

  public void setPort(int portIndex, Value value, int delay) {
    EndData end = component.getEnd(portIndex);
    circuitState.setValue(end.getLocation(), value, component, delay);
  }
}
