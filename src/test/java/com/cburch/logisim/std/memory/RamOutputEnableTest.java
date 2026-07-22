/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class RamOutputEnableTest {

  @Test
  void separateDataBusRetainsOutputWhenOutputEnableIsFalse() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    final var state = ramState(attrs);

    ram.propagate(state);

    verify(state, never())
        .setPort(eq(RamAppearance.getDataOutIndex(0, attrs)), any(Value.class), eq(Mem.DELAY));
  }

  @Test
  void controlledSeparateDataBusDrivesUnknownWhenOutputEnableIsFalse() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP_CONTROLLED);
    final var dataBits = attrs.getValue(Mem.DATA_ATTR);
    final var state = ramState(attrs);

    ram.propagate(state);

    verify(state)
        .setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createUnknown(dataBits), Mem.DELAY);
  }

  @Test
  void controlledLineEnableRamDrivesUnknownWhenOutputEnableIsFalse() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP_CONTROLLED);
    final var dataBits = attrs.getValue(Mem.DATA_ATTR);
    final var state = ramState(attrs);

    ram.propagate(state);

    verify(state)
        .setPort(RamAppearance.getDataOutIndex(0, attrs), Value.createUnknown(dataBits), Mem.DELAY);
  }

  @Test
  void dualRamSeparateDataBusRetainsOutputWhenOutputEnableIsFalse() {
    final var ram = new DualRam();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    final var state = dualRamState(attrs);

    ram.propagate(state);

    verify(state, never())
        .setPort(eq(DualRamAppearance.getDataOutIndex(0, attrs)), any(Value.class), eq(Mem.DELAY));
  }

  @Test
  void dualRamControlledSeparateDataBusDrivesUnknownWhenOutputEnableIsFalse() {
    final var ram = new DualRam();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP_CONTROLLED);
    final var dataBits = attrs.getValue(Mem.DATA_ATTR);
    final var state = dualRamState(attrs);

    ram.propagate(state);

    verify(state)
        .setPort(DualRamAppearance.getDataOutIndex(0, attrs), Value.createUnknown(dataBits), Mem.DELAY);
  }

  @Test
  void controlledSeparateDataBusIsNotHdlSupported() {
    final var attrs = (RamAttributes) new Ram().createAttributeSet();
    final var hdl = new RamHdlGeneratorFactory();

    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_SEP_CONTROLLED);
    assertFalse(hdl.isHdlSupportedTarget(attrs));

    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    assertFalse(hdl.isHdlSupportedTarget(attrs));
  }

  private static InstanceState ramState(RamAttributes attrs) {
    final var state = mock(InstanceState.class);
    final var contents = MemContents.create(
        attrs.getValue(Mem.ADDR_ATTR).getWidth(), attrs.getValue(Mem.DATA_ATTR).getWidth(), false);
    final var instance = mock(Instance.class);
    final var data = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x5a);
    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(data);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));
    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.FALSE);
    return state;
  }

  private static InstanceState dualRamState(RamAttributes attrs) {
    final var state = mock(InstanceState.class);
    final var contents = MemContents.create(
        attrs.getValue(Mem.ADDR_ATTR).getWidth(), attrs.getValue(Mem.DATA_ATTR).getWidth(), false);
    final var instance = mock(Instance.class);
    final var data = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x5a);
    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(data);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));
    when(state.getPortValue(DualRamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(DualRamAppearance.getAddrIndex(1, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(DualRamAppearance.getWEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(DualRamAppearance.getWEIndex(1, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(DualRamAppearance.getOEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(DualRamAppearance.getOEIndex(1, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(DualRamAppearance.getClkIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(DualRamAppearance.getClkIndex(1, attrs))).thenReturn(Value.FALSE);
    return state;
  }
}
