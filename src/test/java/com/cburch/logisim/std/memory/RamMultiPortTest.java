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
import static org.junit.jupiter.api.Assertions.assertNull;
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
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/** Tests for the configurable multi-port RAM component. */
class RamMultiPortTest {

  private static final BitWidth ADDR = BitWidth.create(4);
  private static final BitWidth DATA = BitWidth.create(8);

  /** Minimal {@link InstanceState} implementation, avoiding a Mockito dependency. */
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
    final Ram ram = new Ram();
    final RamAttributes attrs;
    final MemContents contents;
    final RamState ramState;
    final FakeState state;

    Rig(int ports) {
      attrs = (RamAttributes) ram.createAttributeSet();
      attrs.setValue(Mem.ADDR_ATTR, ADDR);
      attrs.setValue(Mem.DATA_ATTR, DATA);
      attrs.setValue(RamAttributes.ATTR_PORTS, ports);
      contents = MemContents.create(ADDR.getWidth(), DATA.getWidth(), false);
      ramState = new RamState(null, contents, new Mem.MemListener(null));
      state = new FakeState(attrs, ramState);
    }

    int addr(int p) {
      return RamMultiPortAppearance.getAddrIndex(p, attrs);
    }

    int we(int p) {
      return RamMultiPortAppearance.getWEIndex(p, attrs);
    }

    int oe(int p) {
      return RamMultiPortAppearance.getOEIndex(p, attrs);
    }

    int clk(int p) {
      return RamMultiPortAppearance.getClkIndex(p, attrs);
    }

    int dataIn(int p) {
      return RamMultiPortAppearance.getDataInIndex(p, 0, attrs);
    }

    int dataOut(int p) {
      return RamMultiPortAppearance.getDataOutIndex(p, 0, attrs);
    }

    void connectReadPort(int p, long address) {
      state.ports.put(addr(p), Value.createKnown(ADDR, address));
      state.ports.put(we(p), Value.FALSE);
      state.ports.put(oe(p), Value.TRUE);
      state.ports.put(clk(p), Value.TRUE);
    }

    void connectWritePort(int p, long address, long data) {
      state.ports.put(addr(p), Value.createKnown(ADDR, address));
      state.ports.put(we(p), Value.TRUE);
      state.ports.put(oe(p), Value.TRUE);
      state.ports.put(clk(p), Value.TRUE);
      state.ports.put(dataIn(p), Value.createKnown(DATA, data));
    }
  }

  @Test
  void layoutScalesWithPortCount() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(RamAttributes.ATTR_PORTS, 4);

    assertEquals(4, RamMultiPortAppearance.getNrAddrPorts(attrs));
    assertEquals(4, RamMultiPortAppearance.getNrDataOutPorts(attrs));
    assertEquals(4, RamMultiPortAppearance.getNrDataInPorts(attrs));
    assertEquals(4, RamMultiPortAppearance.getNrWEPorts(attrs));
    assertEquals(4, RamMultiPortAppearance.getNrClkPorts(attrs));
    assertEquals(4, RamMultiPortAppearance.getNrOEPorts(attrs));
    assertEquals(24, RamMultiPortAppearance.getNrOfPorts(attrs));

    final var single = (RamAttributes) ram.createAttributeSet();
    assertTrue(
        RamMultiPortAppearance.getBounds(attrs).getHeight()
            > RamAppearance.getBounds(single).getHeight());
  }

  @Test
  void twoPortsReadIndependentAddresses() {
    final var rig = new Rig(2);
    rig.contents.set(0, 0x11);
    rig.contents.set(1, 0x22);
    rig.connectReadPort(0, 0);
    rig.connectReadPort(1, 1);

    rig.ram.propagate(rig.state);

    assertEquals(
        Value.createKnown(DATA, 0x11), rig.state.outputs.get(rig.dataOut(0)));
    assertEquals(
        Value.createKnown(DATA, 0x22), rig.state.outputs.get(rig.dataOut(1)));
  }

  @Test
  void threePortsWriteAndReadIndependently() {
    final var rig = new Rig(3);
    rig.connectWritePort(0, 0, 0xA0);
    rig.connectWritePort(1, 1, 0xB1);
    rig.connectWritePort(2, 2, 0xC2);

    rig.ram.propagate(rig.state);

    assertEquals(0xA0, rig.contents.get(0));
    assertEquals(0xB1, rig.contents.get(1));
    assertEquals(0xC2, rig.contents.get(2));

    assertEquals(
        Value.createKnown(DATA, 0xA0), rig.state.outputs.get(rig.dataOut(0)));
    assertEquals(
        Value.createKnown(DATA, 0xB1), rig.state.outputs.get(rig.dataOut(1)));
    assertEquals(
        Value.createKnown(DATA, 0xC2), rig.state.outputs.get(rig.dataOut(2)));
  }

  @Test
  void highestPortIndexWinsWhenTwoPortsWriteSameAddress() {
    final var rig = new Rig(2);
    rig.connectWritePort(0, 0, 0xAA);
    rig.connectWritePort(1, 0, 0xBB);

    rig.ram.propagate(rig.state);

    // Ports are processed in ascending order, so the highest port index wins.
    assertEquals(0xBB, rig.contents.get(0));
  }

  @Test
  void eightPortsLayoutAndIndependentAccess() {
    final var layoutRam = new Ram();
    final var layoutAttrs = (RamAttributes) layoutRam.createAttributeSet();
    layoutAttrs.setValue(RamAttributes.ATTR_PORTS, 8);
    assertEquals(8, RamMultiPortAppearance.getNrAddrPorts(layoutAttrs));
    assertEquals(8, RamMultiPortAppearance.getNrDataOutPorts(layoutAttrs));
    assertEquals(8, RamMultiPortAppearance.getNrWEPorts(layoutAttrs));
    assertEquals(8, RamMultiPortAppearance.getNrClkPorts(layoutAttrs));
    assertEquals(8, RamMultiPortAppearance.getNrOEPorts(layoutAttrs));
    assertEquals(48, RamMultiPortAppearance.getNrOfPorts(layoutAttrs));

    final var rig = new Rig(8);
    for (var p = 0; p < 8; p++) {
      rig.connectWritePort(p, p, 0x10 + p);
    }
    rig.ram.propagate(rig.state);
    for (var p = 0; p < 8; p++) {
      assertEquals(0x10 + p, rig.contents.get(p));
      assertEquals(
          Value.createKnown(DATA, 0x10 + p), rig.state.outputs.get(rig.dataOut(p)));
    }
  }

  @Test
  void singlePortKeepsOriginalPortCount() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    assertEquals(1, RamAttributes.getNrPorts(attrs));
    assertEquals(1, RamAppearance.getNrAddrPorts(attrs));
  }

  @Test
  void dualRamDoesNotExposeThePortsAttribute() {
    final var dualRam = new DualRam();
    final var attrs = dualRam.createAttributeSet();
    assertNull(attrs.getAttribute("ports"));
  }
}
