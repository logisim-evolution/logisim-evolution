/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74365Test {
  /** Data inputs in channel order. */
  private static final byte[] INPUTS = {
    Ttl74365.A1, Ttl74365.A2, Ttl74365.A3, Ttl74365.A4, Ttl74365.A5, Ttl74365.A6
  };

  /** Data outputs in channel order. */
  private static final byte[] OUTPUTS = {
    Ttl74365.Y1, Ttl74365.Y2, Ttl74365.Y3, Ttl74365.Y4, Ttl74365.Y5, Ttl74365.Y6
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var buffer = new Ttl74365();
    final var hiddenPower = createInstance(buffer, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74365.OE1, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A1, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y1, 50, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A2, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y2, 90, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A3, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y3, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y4, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A4, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y5, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A5, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.Y6, 70, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.A6, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74365.OE2, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(buffer, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void enabledDeviceCopiesEveryDataPattern() {
    final var buffer = new Ttl74365();

    for (var pattern = 0; pattern < (1 << INPUTS.length); pattern++) {
      final var state = enabled(buffer);
      setData(state, pattern);
      buffer.propagate(state);
      assertCopied(state, pattern);
    }
  }

  @Test
  void eitherEnableHighReleasesEveryOutput() {
    final var buffer = new Ttl74365();

    for (var pattern = 0; pattern < (1 << INPUTS.length); pattern++) {
      final var outputEnable1High = enabled(buffer);
      setData(outputEnable1High, pattern);
      setHigh(outputEnable1High, Ttl74365.OE1);
      buffer.propagate(outputEnable1High);
      assertReleased(outputEnable1High);

      final var outputEnable2High = enabled(buffer);
      setData(outputEnable2High, pattern);
      setHigh(outputEnable2High, Ttl74365.OE2);
      buffer.propagate(outputEnable2High);
      assertReleased(outputEnable2High);

      final var bothEnablesHigh = enabled(buffer);
      setData(bothEnablesHigh, pattern);
      setHigh(bothEnablesHigh, Ttl74365.OE1);
      setHigh(bothEnablesHigh, Ttl74365.OE2);
      buffer.propagate(bothEnablesHigh);
      assertReleased(bothEnablesHigh);
    }
  }

  @Test
  void unknownOrErrorEnableReleasesEveryOutput() {
    final var buffer = new Ttl74365();

    final var unknownEnable = enabled(buffer);
    setData(unknownEnable, 0x15);
    unknownEnable.setPortValue(Ttl74365.pinNrToPortNr(Ttl74365.OE1), Value.UNKNOWN);
    buffer.propagate(unknownEnable);
    assertReleased(unknownEnable);

    final var errorEnable = enabled(buffer);
    setData(errorEnable, 0x2A);
    errorEnable.setPortValue(Ttl74365.pinNrToPortNr(Ttl74365.OE2), Value.ERROR);
    buffer.propagate(errorEnable);
    assertReleased(errorEnable);
  }

  @Test
  void enabledChannelPassesUnknownAndErrorOnItsOwnInput() {
    final var buffer = new Ttl74365();
    final var state = enabled(buffer);
    setData(state, 0x11);
    state.setPortValue(Ttl74365.pinNrToPortNr(Ttl74365.A3), Value.UNKNOWN);
    state.setPortValue(Ttl74365.pinNrToPortNr(Ttl74365.A5), Value.ERROR);
    buffer.propagate(state);

    assertEquals(Value.TRUE, output(state, Ttl74365.Y1));
    assertEquals(Value.FALSE, output(state, Ttl74365.Y2));
    assertEquals(Value.UNKNOWN, output(state, Ttl74365.Y3));
    assertEquals(Value.FALSE, output(state, Ttl74365.Y4));
    assertEquals(Value.ERROR, output(state, Ttl74365.Y5));
    assertEquals(Value.FALSE, output(state, Ttl74365.Y6));
  }

  @Test
  void invalidExposedPowerInputsReleaseEveryOutput() {
    final var buffer = new Ttl74365();
    final var state = new TtlTestInstanceState(buffer, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74365.OE1);
    setLow(state, Ttl74365.OE2);
    setData(state, 0x2A);
    buffer.propagate(state);
    assertCopied(state, 0x2A);

    state.setPortValue(VCC_PORT, Value.FALSE);
    buffer.propagate(state);
    assertReleased(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    buffer.propagate(state);
    assertCopied(state, 0x2A);

    state.setPortValue(GND_PORT, Value.TRUE);
    buffer.propagate(state);
    assertReleased(state);
  }

  /** Creates a device with both active-low enables asserted. */
  private static TtlTestInstanceState enabled(Ttl74365 buffer) {
    final var state = new TtlTestInstanceState(buffer, false);
    setLow(state, Ttl74365.OE1);
    setLow(state, Ttl74365.OE2);
    return state;
  }

  private static void setData(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < INPUTS.length; index++) {
      setLevel(state, INPUTS[index], (pattern & (1 << index)) != 0);
    }
  }

  private static void assertCopied(TtlTestInstanceState state, int pattern) {
    for (var index = 0; index < OUTPUTS.length; index++) {
      final var expected = (pattern & (1 << index)) != 0 ? Value.TRUE : Value.FALSE;
      assertEquals(expected, output(state, OUTPUTS[index]));
    }
  }

  private static void assertReleased(TtlTestInstanceState state) {
    for (final var pin : OUTPUTS) {
      assertEquals(Value.UNKNOWN, output(state, pin));
    }
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74365.pinNrToPortNr(dsPinNr));
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74365.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLevel(TtlTestInstanceState state, byte dsPinNr, boolean high) {
    state.setPortValue(Ttl74365.pinNrToPortNr(dsPinNr), high ? Value.TRUE : Value.FALSE);
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, false);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    setLevel(state, dsPinNr, true);
  }
}
