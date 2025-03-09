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
import com.cburch.logisim.data.Value;
import com.cburch.logisim.proj.Project;

public class InstanceStateImpl implements InstanceState {
  private CircuitState circuitState;
  private Component component;

  //static int cnt = 0;
  public InstanceStateImpl(CircuitState circuitState, Component component) {
    //int n = ++cnt; // System.out.println("alloc " + (++cnt));
    //if (n % 10000 == 0) try { throw new Exception(); } catch (Exception e) { e.printStackTrace(); }
    this.circuitState = circuitState;
    this.component = component;

    if (component instanceof InstanceComponent instComp) {
      instComp.setInstanceStateImpl(this);
    }
  }

  @Override
  public void fireInvalidated() {
    if (component instanceof InstanceComponent instComp) {
      instComp.fireInvalidated();
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

  public CircuitState getCircuitState() {
    return circuitState;
  }

  public CircuitState createCircuitSubstateFor(Circuit circ) {
    return circuitState.createCircuitSubstateFor(component, circ);
  }

  public Component getComponent() {
    return component;
  }

  @Override
  public InstanceData getData() {
    return ((InstanceData) circuitState.getData(component));
  }

  @Override
  public InstanceFactory getFactory() {
    if (component instanceof InstanceComponent instComp) {
      return (InstanceFactory) instComp.getFactory();
    }
    return null;
  }

  @Override
  public Instance getInstance() {
    return (component instanceof InstanceComponent instComp)
           ? instComp.getInstance()
           : null;
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
    final var circ = circuitState.getCircuit();
    final var loc = component.getEnd(index).getLocation();
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
    final var end = component.getEnd(portIndex);
    circuitState.setValue(end.getLocation(), value, component, delay);
  }
}
