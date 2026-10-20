package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class RamByteEnableTest {

  @Test
  void writesBothBytesWhenBothEnabled() {
    assertWriteResult(true, true, 0xABCD);
  }

  @Test
  void writesLowByteOnly() {
    assertWriteResult(true, false, 0x12CD);
  }

  @Test
  void writesHighByteOnly() {
    assertWriteResult(false, true, 0xAB34);
  }

  @Test
  void writesNeitherByteWhenBothDisabled() {
    assertWriteResult(false, false, 0x1234);
  }

  @Test
  void writesOnFallingEdge() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_FALLING);
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    attrs.setValue(Mem.LINE_ATTR, Mem.SINGLE);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var ramState = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(ramState);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD));
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    // FALSE -> TRUE: not a falling edge.
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.TRUE);
    ram.propagate(state);

    assertEquals(0x1234, contents.get(0));

    // TRUE -> FALSE: falling edge, so write occurs.
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.FALSE);
    ram.propagate(state);

    assertEquals(0xABCD, contents.get(0));
  }

  @Test
  void writesWhenClockIsLow() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_LOW);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);
    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var data = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(data);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD));
    when(state.getPortValue(RamAppearance.getBEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getBEIndex(1, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    ram.propagate(state);

    assertEquals(0xABCD, contents.get(0));
  }

  @Test
  void writesWithHighLevelTrigger() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_HIGH);
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USELINEENABLES);
    attrs.setValue(Mem.LINE_ATTR, Mem.SINGLE);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var ramState = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(ramState);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD));
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    ram.propagate(state);

    assertEquals(0xABCD, contents.get(0));
  }

  @Test
  void byteEnableWritesCoverAllBusStyles() {
    for (var busStyle :
        new AttributeOption[] {
          RamAttributes.BUS_BIDIR, RamAttributes.BUS_SEP, RamAttributes.BUS_SEP_CONTROLLED
        }) {
      for (var enableMask : new int[] {0b0000, 0b0001, 0b0101, 0b1111}) {
        final var expected = expected32BitValue(enableMask);

        assertWriteResult32(busStyle, enableMask, 0xABCD1234, expected);
      }
    }
  }

  @Test
  void configuresBidirectionalDataBus() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(RamAttributes.ATTR_DBUS, RamAttributes.BUS_BIDIR);
    attrs.setValue(Mem.ENABLES_ATTR, Mem.USEBYTEENABLES);
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    assertEquals(0, RamAppearance.getNrDataInPorts(attrs));
    assertEquals(1, RamAppearance.getNrDataOutPorts(attrs));
    assertEquals(1, RamAppearance.getNrOEPorts(attrs));
    assertEquals(1, RamAppearance.getNrWEPorts(attrs));
    assertEquals(2, RamAppearance.getNrBEPorts(attrs));
  }

  @Test
  void readAfterWriteReturnsNewValue() {
    assertReadWriteBehavior(Mem.READAFTERWRITE, false, 0xABCD);
  }

  @Test
  void writeAfterReadReturnsOldValue() {
    assertReadWriteBehavior(Mem.WRITEAFTERREAD, false, 0x1234);
  }

  @Test
  void asyncReadReturnsCurrentValueWithoutClockEdge() {
    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(Mem.ASYNC_READ, true);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var ramState = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(ramState);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.FALSE);
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    final var output = new Value[1];

    doAnswer(
            invocation -> {
              output[0] = invocation.getArgument(1);
              return null;
            })
        .when(state)
        .setPort(anyInt(), any(Value.class), eq(Mem.DELAY));

    ram.propagate(state);

    assertEquals(0x1234, output[0].toLongValue());
  }

  private static void assertWriteResult(
      boolean lowByteEnabled, boolean highByteEnabled, int expected) {

    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var data = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(data);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD));

    when(state.getPortValue(RamAppearance.getBEIndex(0, attrs)))
        .thenReturn(lowByteEnabled ? Value.TRUE : Value.FALSE);
    when(state.getPortValue(RamAppearance.getBEIndex(1, attrs)))
        .thenReturn(highByteEnabled ? Value.TRUE : Value.FALSE);
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    ram.propagate(state);

    assertEquals(expected, contents.get(0));
  }

  private static void assertWriteResult32(
      AttributeOption busStyle, int byteEnableMask, int data, int expected) {

    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();
    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(32));
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(RamAttributes.ATTR_DBUS, busStyle);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var ramState = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x12345678);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(ramState);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), data));
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    for (int i = 0; i < 4; i++) {
      when(state.getPortValue(RamAppearance.getBEIndex(i, attrs)))
          .thenReturn((byteEnableMask & (1 << i)) != 0 ? Value.TRUE : Value.FALSE);
    }

    ram.propagate(state);

    assertEquals(
        expected & 0xFFFFFFFFL,
        contents.get(0) & 0xFFFFFFFFL,
        () -> String.format("Expected 0x%08X but got 0x%08X", expected, contents.get(0)));
  }

  private static int expected32BitValue(int enableMask) {
    int expected = 0x12345678;
    final int data = 0xABCD1234;

    for (var i = 0; i < 4; i++) {
      if ((enableMask & (1 << i)) != 0) {
        final int mask = 0xFF << (i * 8);
        expected = (expected & ~mask) | (data & mask);
      }
    }

    return expected;
  }

  private static void assertReadWriteBehavior(
      AttributeOption readBehavior, boolean asyncRead, int expectedOutput) {

    final var ram = new Ram();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(16));
    attrs.setValue(RamAttributes.ATTR_ByteEnables, RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(Mem.READ_ATTR, readBehavior);
    attrs.setValue(Mem.ASYNC_READ, asyncRead);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state = mock(InstanceState.class);
    final var instance = mock(Instance.class);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    final var ramState = new RamState(null, contents, new Mem.MemListener(instance));

    contents.set(0, 0x1234);

    when(instance.getAttributeSet()).thenReturn(attrs);
    when(state.getAttributeSet()).thenReturn(attrs);
    when(state.getData()).thenReturn(ramState);
    when(state.getInstance()).thenReturn(instance);
    when(state.getAttributeValue(Mem.DATA_ATTR)).thenReturn(attrs.getValue(Mem.DATA_ATTR));
    when(state.getAttributeValue(StdAttr.TRIGGER)).thenReturn(attrs.getValue(StdAttr.TRIGGER));

    when(state.getPortValue(RamAppearance.getAddrIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    when(state.getPortValue(RamAppearance.getWEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getClkIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getDataInIndex(0, attrs)))
        .thenReturn(Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD));

    when(state.getPortValue(RamAppearance.getBEIndex(0, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getBEIndex(1, attrs))).thenReturn(Value.TRUE);
    when(state.getPortValue(RamAppearance.getOEIndex(0, attrs))).thenReturn(Value.TRUE);

    final var output = new Value[1];

    doAnswer(
            invocation -> {
              output[0] = invocation.getArgument(1);
              return null;
            })
        .when(state)
        .setPort(anyInt(), any(Value.class), eq(Mem.DELAY));

    ram.propagate(state);

    assertEquals(0xABCD, contents.get(0));
    assertEquals(expectedOutput, output[0].toLongValue());
  }
}
