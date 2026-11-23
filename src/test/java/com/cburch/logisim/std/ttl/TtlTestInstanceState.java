/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;

/**
 * An {@link InstanceState} backed by a plain port-value map, so that TTL components can be
 * propagated from tests without building a circuit around them.
 */
final class TtlTestInstanceState implements InstanceState {
  private final AttributeSet attrs;
  private final Instance instance;
  private final Map<Integer, Value> portValues = new HashMap<>();
  private InstanceData data;

  TtlTestInstanceState(InstanceFactory factory, boolean showPowerPins) {
    attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    instance =
        Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
  }

  /** Creates a component instance without the port-value bookkeeping, for port-layout tests. */
  static Instance createInstance(InstanceFactory factory, boolean showPowerPins) {
    final var attrs = factory.createAttributeSet();
    attrs.setValue(TtlLibrary.VCC_GND, showPowerPins);
    return Instance.getInstanceFor(factory.createComponent(Location.create(0, 0, false), attrs));
  }

  void setPortValue(int portIndex, Value value) {
    portValues.put(portIndex, value);
  }

  @Override
  public void fireInvalidated() {}

  @Override
  public AttributeSet getAttributeSet() {
    return attrs;
  }

  @Override
  public <E> E getAttributeValue(Attribute<E> attr) {
    return attrs.getValue(attr);
  }

  @Override
  public InstanceData getData() {
    return data;
  }

  @Override
  public InstanceFactory getFactory() {
    return instance.getFactory();
  }

  @Override
  public Instance getInstance() {
    return instance;
  }

  @Override
  public int getPortIndex(Port port) {
    return instance.getPorts().indexOf(port);
  }

  @Override
  public Value getPortValue(int portIndex) {
    return portValues.getOrDefault(portIndex, Value.UNKNOWN);
  }

  @Override
  public Project getProject() {
    return null;
  }

  @Override
  public int getTickCount() {
    return 0;
  }

  @Override
  public boolean isCircuitRoot() {
    return true;
  }

  @Override
  public boolean isPortConnected(int portIndex) {
    return false;
  }

  @Override
  public CircuitState createCircuitSubstateFor(Circuit circ) {
    return null;
  }

  @Override
  public void setData(InstanceData value) {
    data = value;
  }

  @Override
  public void setPort(int portIndex, Value value, int delay) {
    portValues.put(portIndex, value);
  }
}
