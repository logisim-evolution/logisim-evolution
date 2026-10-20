/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for the configurable read-only multi-port ROM component. */
class RomMultiPortTest {

  private static final BitWidth ADDR = BitWidth.create(4);
  private static final BitWidth DATA = BitWidth.create(8);

  private static final class FakeState implements InstanceState {
    private final AttributeSet attrs;
    private final InstanceData data;
    private final Map<Integer, Value> ports = new HashMap<>();
    private final Map<Integer, Value> outputs = new HashMap<>();

    FakeState(AttributeSet attrs, InstanceData data) {
      this.attrs = attrs;
      this.data = data;
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
      return null;
    }

    @Override
    public Instance getInstance() {
      return null;
    }

    @Override
    public int getPortIndex(Port port) {
      return -1;
    }

    @Override
    public Value getPortValue(int portIndex) {
      return ports.getOrDefault(portIndex, Value.FALSE);
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
      return true;
    }

    @Override
    public CircuitState createCircuitSubstateFor(Circuit circ) {
      return null;
    }

    @Override
    public void setData(InstanceData value) {}

    @Override
    public void setPort(int portIndex, Value value, int delay) {
      outputs.put(portIndex, value);
    }
  }

  private static final class Rig {
    final Rom rom = new Rom();
    final RomAttributes attrs;
    final MemContents contents;
    final FakeState state;

    Rig(int ports) {
      attrs = (RomAttributes) rom.createAttributeSet();
      attrs.setValue(Mem.ADDR_ATTR, ADDR);
      attrs.setValue(Mem.DATA_ATTR, DATA);
      attrs.setValue(RamAttributes.ATTR_PORTS, ports);
      contents = (MemContents) attrs.getValue(Rom.CONTENTS_ATTR);
      state = new FakeState(attrs, new MemState(contents));
    }

    void connectReadPort(int p, long address) {
      state.ports.put(
          RomMultiPortAppearance.getAddrIndex(p, attrs), Value.createKnown(ADDR, address));
    }

    int out(int p) {
      return RomMultiPortAppearance.getDataOutIndex(p, 0, attrs);
    }
  }

  @Test
  void layoutScalesWithPortCount() {
    final var rom = new Rom();
    final var attrs = (RomAttributes) rom.createAttributeSet();
    attrs.setValue(RamAttributes.ATTR_PORTS, 4);

    assertEquals(4, RomMultiPortAppearance.getNrAddrPorts(attrs));
    assertEquals(4, RomMultiPortAppearance.getNrDataOutPorts(attrs));
    assertEquals(8, RomMultiPortAppearance.getNrOfPorts(attrs));
    assertTrue(RomMultiPortAppearance.getBounds(attrs).getHeight() > 0);
  }

  @Test
  void threePortsReadIndependently() {
    final var rig = new Rig(3);
    rig.contents.set(0, 0x11);
    rig.contents.set(1, 0x22);
    rig.contents.set(2, 0x33);
    rig.connectReadPort(0, 0);
    rig.connectReadPort(1, 1);
    rig.connectReadPort(2, 2);

    rig.rom.propagate(rig.state);

    assertEquals(Value.createKnown(DATA, 0x11), rig.state.outputs.get(rig.out(0)));
    assertEquals(Value.createKnown(DATA, 0x22), rig.state.outputs.get(rig.out(1)));
    assertEquals(Value.createKnown(DATA, 0x33), rig.state.outputs.get(rig.out(2)));
  }

  @Test
  void errorAndUnknownAddressesAreReported() {
    final var rig = new Rig(3);
    rig.contents.set(2, 0x33);
    rig.state.ports.put(RomMultiPortAppearance.getAddrIndex(0, rig.attrs), Value.createError(ADDR));
    rig.state.ports.put(
        RomMultiPortAppearance.getAddrIndex(1, rig.attrs), Value.createUnknown(ADDR));
    rig.connectReadPort(2, 2);

    rig.rom.propagate(rig.state);

    assertEquals(Value.createError(DATA), rig.state.outputs.get(rig.out(0)));
    assertEquals(Value.createUnknown(DATA), rig.state.outputs.get(rig.out(1)));
    assertEquals(Value.createKnown(DATA, 0x33), rig.state.outputs.get(rig.out(2)));
  }

  @Test
  void eightPortsLayoutAndIndependentReads() {
    final var layoutRom = new Rom();
    final var layoutAttrs = (RomAttributes) layoutRom.createAttributeSet();
    layoutAttrs.setValue(RamAttributes.ATTR_PORTS, 8);
    assertEquals(8, RomMultiPortAppearance.getNrAddrPorts(layoutAttrs));
    assertEquals(8, RomMultiPortAppearance.getNrDataOutPorts(layoutAttrs));
    assertEquals(16, RomMultiPortAppearance.getNrOfPorts(layoutAttrs));

    final var rig = new Rig(8);
    for (var p = 0; p < 8; p++) {
      rig.contents.set(p, 0x10 + p);
      rig.connectReadPort(p, p);
    }
    rig.rom.propagate(rig.state);
    for (var p = 0; p < 8; p++) {
      assertEquals(Value.createKnown(DATA, 0x10 + p), rig.state.outputs.get(rig.out(p)));
    }
  }

  @Test
  void singlePortKeepsOriginalPortCount() {
    final var rom = new Rom();
    final var attrs = (RomAttributes) rom.createAttributeSet();
    assertEquals(1, RamAttributes.getNrPorts(attrs));
    assertEquals(1, RamAppearance.getAddrIndex(0, attrs) + 1);
  }
}
