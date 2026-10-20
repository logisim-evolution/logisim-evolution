package com.cburch.logisim.std.memory;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.StdAttr;
import org.junit.jupiter.api.Test;

class DualRamByteEnableTest {

  @Test
  void portAWritesSelectedBytes() {
    assertWriteResult(0, 0b0101, 0xABCD1234, 0x12CD5634);
  }

  @Test
  void portBWritesSelectedBytes() {
    assertWriteResult(1, 0b1010, 0xABCD1234, 0xAB341278L);
  }

  @Test
  void noBytesEnabledLeavesMemoryUnchanged() {
    assertWriteResult(0, 0b0000, 0xABCD1234, 0x12345678);
  }

  @Test
  void bothPortsCanUseDifferentByteEnables() {
    final var ram = new DualRam();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(32));
    attrs.setValue(
        RamAttributes.ATTR_ByteEnables,
        RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state = new RamLineEnableTest.TestInstanceState(ram, attrs);
    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    contents.set(0, 0x12345678);
    contents.set(1, 0x12345678);

    state.setData(
        new RamState(
            state.getInstance(),
            contents,
            new Mem.MemListener(state.getInstance())));

    for (int port = 0; port < 2; port++) {
      state.setPortValue(
          DualRamAppearance.getAddrIndex(port, attrs),
          Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), port));
      state.setPortValue(
          DualRamAppearance.getWEIndex(port, attrs), Value.FALSE);
      state.setPortValue(
          DualRamAppearance.getClkIndex(port, attrs), Value.FALSE);
      state.setPortValue(
          DualRamAppearance.getOEIndex(port, attrs), Value.TRUE);
    }

    // Port A: write low byte.
    state.setPortValue(
        DualRamAppearance.getWEIndex(0, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getClkIndex(0, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getDataInIndex(0, attrs),
        Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD1234));

    // Port B: write the second byte.
    state.setPortValue(
        DualRamAppearance.getWEIndex(1, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getClkIndex(1, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getDataInIndex(1, attrs),
        Value.createKnown(attrs.getValue(Mem.DATA_ATTR), 0xABCD1234));

    final int bytesPerWord = DualRamAppearance.getNrBEPorts(attrs) / 2;

    for (int i = 0; i < bytesPerWord; i++) {
      state.setPortValue(
          DualRamAppearance.getBEIndex(i, attrs),
          i == 0 ? Value.TRUE : Value.FALSE);

      state.setPortValue(
          DualRamAppearance.getBEIndex(bytesPerWord + i, attrs),
          i == 1 ? Value.TRUE : Value.FALSE);
    }

    ram.propagate(state);

    assertEquals(0x12345634, contents.get(0));
    assertEquals(0x12341278L, contents.get(1));
  }

  private static void assertWriteResult(
      int port, int byteEnableMask, long data, long expected) {

    final var ram = new DualRam();
    final var attrs = (RamAttributes) ram.createAttributeSet();

    attrs.setValue(Mem.DATA_ATTR, BitWidth.create(32));
    attrs.setValue(
        RamAttributes.ATTR_ByteEnables,
        RamAttributes.BUS_WITH_BYTEENABLES);
    attrs.setValue(StdAttr.TRIGGER, StdAttr.TRIG_RISING);

    final var state =
        new RamLineEnableTest.TestInstanceState(ram, attrs);

    final var contents =
        MemContents.create(
            attrs.getValue(Mem.ADDR_ATTR).getWidth(),
            attrs.getValue(Mem.DATA_ATTR).getWidth(),
            false);

    contents.set(0, 0x12345678);

    state.setData(
        new RamState(
            state.getInstance(),
            contents,
            new Mem.MemListener(state.getInstance())));

    state.setPortValue(
        DualRamAppearance.getAddrIndex(port, attrs),
        Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    state.setPortValue(
        DualRamAppearance.getWEIndex(port, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getClkIndex(port, attrs), Value.TRUE);
    state.setPortValue(
        DualRamAppearance.getDataInIndex(port, attrs),
        Value.createKnown(attrs.getValue(Mem.DATA_ATTR), data));
    state.setPortValue(
        DualRamAppearance.getOEIndex(port, attrs), Value.TRUE);

    final int bytesPerWord = DualRamAppearance.getNrBEPorts(attrs) / 2;

    for (int i = 0; i < bytesPerWord; i++) {
      state.setPortValue(
          DualRamAppearance.getBEIndex(port * bytesPerWord + i, attrs),
          (byteEnableMask & (1 << i)) != 0
              ? Value.TRUE
              : Value.FALSE);
    }

    // Give the inactive port valid inputs so it doesn't accidentally
    // interfere with the test.
    final int otherPort = 1 - port;

    state.setPortValue(
        DualRamAppearance.getAddrIndex(otherPort, attrs),
        Value.createKnown(attrs.getValue(Mem.ADDR_ATTR), 0));
    state.setPortValue(
        DualRamAppearance.getWEIndex(otherPort, attrs), Value.FALSE);
    state.setPortValue(
        DualRamAppearance.getClkIndex(otherPort, attrs), Value.FALSE);
    state.setPortValue(
        DualRamAppearance.getOEIndex(otherPort, attrs), Value.FALSE);

    ram.propagate(state);

    assertEquals(
        expected,
        contents.get(0),
        () ->
            String.format(
                "Port %d: expected 0x%08X but got 0x%08X",
                port,
                expected,
                contents.get(0)));
  }
}
