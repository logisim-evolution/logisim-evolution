/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.instance;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;
import lombok.Getter;
import lombok.val;

public class InstanceStateImpl implements InstanceState {
  @Getter private CircuitState circuitState;
  private Component component;

  public InstanceStateImpl(CircuitState circuitState, Component component) {
    this.circuitState = circuitState;
    this.component = component;

    if (component instanceof InstanceComponent) {
      ((InstanceComponent) component).setInstanceStateImpl(this);
    }
  }

  @Override
  public void fireInvalidated() {
    if (component instanceof InstanceComponent) {
      ((InstanceComponent) component).fireInvalidated();
    }
  }

  @Override
  public AttributeSet getAttributeSet() {
    return component.getAttributeSet();
  }

  @Override
  public <E> E getAttributeValue(Attribute<E> attr) {
    return component.getAttributeSet().getValue(attr);
  }

  @Override
  public InstanceData getData() {
    return ((InstanceData) circuitState.getData(component));
  }

  @Override
  public InstanceFactory getFactory() {
    if (component instanceof InstanceComponent) {
      InstanceComponent comp = (InstanceComponent) component;
      return (InstanceFactory) comp.getFactory();
    }
    return null;
  }

  @Override
  public Instance getInstance() {
    if (component instanceof InstanceComponent) {
      return ((InstanceComponent) component).getInstance();
    }
    return null;
  }

  @Override
  public int getPortIndex(Port port) {
    return this.getInstance().getPorts().indexOf(port);
  }

  @Override
  public Value getPortValue(int portIndex) {
    EndData data = component.getEnd(portIndex);
    return circuitState.getValue(data.getLocation());
  }

  @Override
  public Project getProject() {
    return circuitState.getProject();
  }

  @Override
  public int getTickCount() {
    return circuitState.getPropagator().getTickCount();
  }

  @Override
  public boolean isCircuitRoot() {
    return !circuitState.isSubstate();
  }

  @Override
  public boolean isPortConnected(int index) {
    val circ = circuitState.getCircuit();
    val loc = component.getEnd(index).getLocation();
    return circ.isConnected(loc, component);
  }

  public void repurpose(CircuitState circuitState, Component component) {
    this.circuitState = circuitState;
    this.component = component;
  }

  @Override
  public void setData(InstanceData value) {
    circuitState.setData(component, value);
  }

  @Override
  public void setPort(int portIndex, Value value, int delay) {
    val end = component.getEnd(portIndex);
    circuitState.setValue(end.getLocation(), value, component, delay);
  }
}
