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

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class RamLineEnableTest {

  private static final BitWidth ADDR_WIDTH = BitWidth.create(4);
  private static final BitWidth DATA_WIDTH = BitWidth.create(8);

  @Test
  void ramOctoLineWritesOnlyLinesWithTrueLineEnable() {
    final var ram = new Ram();
    final var attrs = ramAttrs(Mem.OCTO);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.setPortValue(RamAppearance.getAddrIndex(0, attrs), known(ADDR_WIDTH, 0));
    state.setPortValue(RamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getClkIndex(0, attrs), Value.TRUE);
    for (var i = 0; i < 8; i++) {
      state.setPortValue(RamAppearance.getDataInIndex(i, attrs), known(DATA_WIDTH, 0x20 + i));
    }
    state.setPortValue(RamAppearance.getLEIndex(0, attrs), Value.TRUE);

    ram.propagate(state);

    assertEquals(0x20, contents.get(0));
    for (var i = 1; i < 8; i++) {
      assertEquals(0, contents.get(i), "line " + i + " must not be written by unknown LE" + i);
    }
  }

  @Test
  void ramSingleLineStillWritesWithoutLineEnablePins() {
    final var ram = new Ram();
    final var attrs = ramAttrs(Mem.SINGLE);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.setPortValue(RamAppearance.getAddrIndex(0, attrs), known(ADDR_WIDTH, 3));
    state.setPortValue(RamAppearance.getDataInIndex(0, attrs), known(DATA_WIDTH, 0x5A));
    state.setPortValue(RamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getClkIndex(0, attrs), Value.TRUE);

    ram.propagate(state);

    assertEquals(0x5A, contents.get(3));
  }

  @Test
  void ramOctoLineAllowMisalignedWritesOnlyTrueLineAtArbitraryAddress() {
    final var ram = new Ram();
    final var attrs = ramAttrs(Mem.OCTO);
    attrs.setValue(Mem.ALLOW_MISALIGNED, true);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.connectPort(RamAppearance.getDataOutIndex(0, attrs));
    state.setPortValue(RamAppearance.getAddrIndex(0, attrs), known(ADDR_WIDTH, 3));
    state.setPortValue(RamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getClkIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getDataInIndex(0, attrs), known(DATA_WIDTH, 0x5A));
    state.setPortValue(RamAppearance.getLEIndex(0, attrs), Value.TRUE);
    for (var i = 1; i < 8; i++) {
      state.setPortValue(RamAppearance.getDataInIndex(i, attrs), known(DATA_WIDTH, 0x70 + i));
      if (i < 4) {
        state.setPortValue(RamAppearance.getLEIndex(i, attrs), Value.FALSE);
      }
    }

    ram.propagate(state);

    assertEquals(0x5A, contents.get(3));
    assertEquals(known(DATA_WIDTH, 0x5A), state.getPortValue(RamAppearance.getDataOutIndex(0, attrs)));
    for (var i = 4; i < 11; i++) {
      assertEquals(0, contents.get(i), "disabled octo line must not write address " + i);
    }
  }

  @Test
  void ramOctoLineDisallowedMisalignmentKeepsFixedLineAlignment() {
    final var ram = new Ram();
    final var attrs = ramAttrs(Mem.OCTO);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.connectPort(RamAppearance.getDataOutIndex(0, attrs));
    state.setPortValue(RamAppearance.getAddrIndex(0, attrs), known(ADDR_WIDTH, 3));
    state.setPortValue(RamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getClkIndex(0, attrs), Value.TRUE);
    state.setPortValue(RamAppearance.getDataInIndex(0, attrs), known(DATA_WIDTH, 0x6C));
    state.setPortValue(RamAppearance.getLEIndex(0, attrs), Value.TRUE);

    ram.propagate(state);

    assertEquals(0, contents.get(3));
    assertEquals(Value.createError(DATA_WIDTH), state.getPortValue(RamAppearance.getDataOutIndex(0, attrs)));
  }

  @Test
  void dualRamOctoLinePortAWritesOnlyLinesWithTrueLineEnable() {
    verifyDualRamOctoLineWritesOnlyEnabledLine(0, 0, 0x40);
  }

  @Test
  void dualRamOctoLinePortBWritesOnlyLinesWithTrueLineEnable() {
    verifyDualRamOctoLineWritesOnlyEnabledLine(1, 8, 0x50);
  }

  @Test
  void dualRamOctoLineAllowMisalignedWritesOnlyFirstPortALineAtArbitraryAddress() {
    verifyDualRamOctoLineFirstLineActsLikeSingleLine(0, 3, 0x6A);
  }

  @Test
  void dualRamOctoLineAllowMisalignedWritesOnlyFirstPortBLineAtArbitraryAddress() {
    verifyDualRamOctoLineFirstLineActsLikeSingleLine(1, 5, 0x7B);
  }

  @Test
  void dualRamOctoLineDisallowedMisalignmentKeepsFixedLineAlignment() {
    final var ram = new DualRam();
    final var attrs = dualRamAttrs(Mem.OCTO);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.connectPort(DualRamAppearance.getDataOutIndex(0, attrs));
    state.setPortValue(DualRamAppearance.getAddrIndex(0, attrs), known(ADDR_WIDTH, 3));
    state.setPortValue(DualRamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(DualRamAppearance.getClkIndex(0, attrs), Value.TRUE);
    state.setPortValue(DualRamAppearance.getDataInIndex(0, attrs), known(DATA_WIDTH, 0x7C));
    state.setPortValue(DualRamAppearance.getLEIndex(0, attrs), Value.TRUE);

    ram.propagate(state);

    assertEquals(0, contents.get(3));
    assertEquals(Value.createError(DATA_WIDTH), state.getPortValue(DualRamAppearance.getDataOutIndex(0, attrs)));
  }

  private static void verifyDualRamOctoLineWritesOnlyEnabledLine(int port, int baseAddress, int dataBase) {
    final var ram = new DualRam();
    final var attrs = dualRamAttrs(Mem.OCTO);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    state.setPortValue(DualRamAppearance.getAddrIndex(port, attrs), known(ADDR_WIDTH, baseAddress));
    state.setPortValue(DualRamAppearance.getWEIndex(port, attrs), Value.TRUE);
    state.setPortValue(DualRamAppearance.getClkIndex(port, attrs), Value.TRUE);

    final var dataLines = DualRamAppearance.getNrLEPorts(attrs) / 2;
    for (var i = 0; i < dataLines; i++) {
      final var absoluteLine = port * dataLines + i;
      state.setPortValue(DualRamAppearance.getDataInIndex(absoluteLine, attrs), known(DATA_WIDTH, dataBase + i));
    }
    state.setPortValue(DualRamAppearance.getLEIndex(port * dataLines, attrs), Value.TRUE);

    ram.propagate(state);

    assertEquals(dataBase, contents.get(baseAddress));
    for (var i = 1; i < dataLines; i++) {
      assertEquals(
          0,
          contents.get(baseAddress + i),
          "port " + port + " line " + i + " must not be written by unknown LE");
    }
  }

  private static void verifyDualRamOctoLineFirstLineActsLikeSingleLine(int port, int address, int value) {
    final var ram = new DualRam();
    final var attrs = dualRamAttrs(Mem.OCTO);
    attrs.setValue(Mem.ALLOW_MISALIGNED, true);
    final var state = new TestInstanceState(ram, attrs);
    final var contents = MemContents.create(ADDR_WIDTH.getWidth(), DATA_WIDTH.getWidth(), false);
    state.setData(new RamState(state.getInstance(), contents, new Mem.MemListener(state.getInstance())));

    final var dataLines = DualRamAppearance.getNrLEPorts(attrs) / 2;
    final var baseLine = port * dataLines;

    state.connectPort(DualRamAppearance.getDataOutIndex(baseLine, attrs));
    state.setPortValue(DualRamAppearance.getAddrIndex(port, attrs), known(ADDR_WIDTH, address));
    state.setPortValue(DualRamAppearance.getWEIndex(port, attrs), Value.TRUE);
    state.setPortValue(DualRamAppearance.getClkIndex(port, attrs), Value.TRUE);
    state.setPortValue(DualRamAppearance.getDataInIndex(baseLine, attrs), known(DATA_WIDTH, value));
    state.setPortValue(DualRamAppearance.getLEIndex(baseLine, attrs), Value.TRUE);
    for (var i = 1; i < dataLines; i++) {
      final var absIndex = baseLine + i;
      state.setPortValue(DualRamAppearance.getDataInIndex(absIndex, attrs), known(DATA_WIDTH, 0x20 + i));
      if (i < 4) {
        state.setPortValue(DualRamAppearance.getLEIndex(absIndex, attrs), Value.FALSE);
      }
    }

    ram.propagate(state);

    assertEquals(value, contents.get(address));
    assertEquals(known(DATA_WIDTH, value), state.getPortValue(DualRamAppearance.getDataOutIndex(baseLine, attrs)));
    for (var i = 1; i < dataLines; i++) {
      assertEquals(0, contents.get(address + i), "disabled dual RAM line must not write address " + (address + i));
    }
  }

  private static AttributeSet ramAttrs(AttributeOption lineSize) {
    final var attrs = new Ram().createAttributeSet();
    attrs.setValue(Mem.ADDR_ATTR, ADDR_WIDTH);
    attrs.setValue(Mem.DATA_ATTR, DATA_WIDTH);
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    attrs.setValue(Mem.LINE_ATTR, lineSize);
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);
    return attrs;
  }

  private static AttributeSet dualRamAttrs(AttributeOption lineSize) {
    final var attrs = new DualRam().createAttributeSet();
    attrs.setValue(Mem.ADDR_ATTR, ADDR_WIDTH);
    attrs.setValue(Mem.DATA_ATTR, DATA_WIDTH);
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    attrs.setValue(Mem.LINE_ATTR, lineSize);
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);
    return attrs;
  }

  private static Value known(BitWidth width, long value) {
    return Value.createKnown(width, value);
  }

  private static final class TestInstanceState implements com.cburch.logisim.instance.InstanceState {
    private final AttributeSet attrs;
    private final Instance instance;
    private final Map<Integer, Value> portValues = new HashMap<>();
    private final Set<Integer> connectedPorts = new HashSet<>();
    private InstanceData data;

    private TestInstanceState(InstanceFactory factory, AttributeSet attrs) {
      this.attrs = attrs;
      final var component = factory.createComponent(Location.create(0, 0, false), attrs);
      this.instance = Instance.getInstanceFor(component);
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
      return connectedPorts.contains(portIndex);
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

    private void setPortValue(int portIndex, Value value) {
      portValues.put(portIndex, value);
      connectPort(portIndex);
    }

    private void connectPort(int portIndex) {
      connectedPorts.add(portIndex);
    }
  }
}
