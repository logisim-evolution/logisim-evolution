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
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import org.junit.jupiter.api.Test;

class Ttl74148Test {
  /** Data inputs ordered by the number they encode. */
  private static final byte[] DATA_INPUTS = {
    Ttl74148.I0, Ttl74148.I1, Ttl74148.I2, Ttl74148.I3,
    Ttl74148.I4, Ttl74148.I5, Ttl74148.I6, Ttl74148.I7
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var encoder = new Ttl74148();
    final var hiddenPower = createInstance(encoder, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74148.I4, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I5, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I6, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I7, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.EI, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.A2, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.A1, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.A0, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I0, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I1, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I2, 90, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.I3, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.GS, 50, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74148.EO, 30, -30, EndData.OUTPUT_ONLY);

    final var shownPower = createInstance(encoder, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachDataInputEncodesTheComplementOfItsNumber() {
    final var encoder = new Ttl74148();

    for (var number = 0; number < DATA_INPUTS.length; number++) {
      final var state = enabledAndIdle(encoder);
      setLow(state, DATA_INPUTS[number]);
      encoder.propagate(state);

      assertEquals(number, encodedNumber(state));
      assertTrue(isLow(state, Ttl74148.GS));
      assertTrue(isHigh(state, Ttl74148.EO));
    }
  }

  @Test
  void theHighestNumberedAssertedInputWins() {
    final var encoder = new Ttl74148();

    for (var expected = 0; expected < DATA_INPUTS.length; expected++) {
      final var state = enabledAndIdle(encoder);
      for (var number = 0; number <= expected; number++) {
        setLow(state, DATA_INPUTS[number]);
      }
      encoder.propagate(state);

      assertEquals(expected, encodedNumber(state));
    }
  }

  @Test
  void idleDeviceKeepsTheCodeHighAndAssertsTheEnableOutput() {
    final var encoder = new Ttl74148();
    final var state = enabledAndIdle(encoder);
    encoder.propagate(state);

    assertEquals(0, encodedNumber(state));
    assertTrue(isHigh(state, Ttl74148.GS));
    assertTrue(isLow(state, Ttl74148.EO));
  }

  @Test
  void disabledDeviceIgnoresItsDataInputs() {
    final var encoder = new Ttl74148();
    final var state = enabledAndIdle(encoder);
    setHigh(state, Ttl74148.EI);
    for (final var input : DATA_INPUTS) {
      setLow(state, input);
    }
    encoder.propagate(state);

    assertEquals(0, encodedNumber(state));
    assertTrue(isHigh(state, Ttl74148.GS));
    assertTrue(isHigh(state, Ttl74148.EO));
  }

  @Test
  void unknownInputsAreTreatedAsTheInactiveLevel() {
    final var encoder = new Ttl74148();
    final var state = new TtlTestInstanceState(encoder, false);
    encoder.propagate(state);

    assertEquals(0, encodedNumber(state));
    assertTrue(isHigh(state, Ttl74148.GS));
    assertTrue(isHigh(state, Ttl74148.EO));
  }

  @Test
  void enableOutputCascadesIntoTheEnableInputOfTheNextDevice() {
    final var encoder = new Ttl74148();
    final var highOrder = enabledAndIdle(encoder);
    final var lowOrder = enabledAndIdle(encoder);
    setLow(lowOrder, Ttl74148.I5);

    // With nothing asserted, the high-order device hands the encoding over to the low-order one.
    encoder.propagate(highOrder);
    assertTrue(isLow(highOrder, Ttl74148.EO));
    cascade(highOrder, lowOrder);
    encoder.propagate(lowOrder);
    assertEquals(5, encodedNumber(lowOrder));
    assertTrue(isLow(lowOrder, Ttl74148.GS));

    // As soon as the high-order device encodes, the low-order one must fall silent.
    setLow(highOrder, Ttl74148.I2);
    encoder.propagate(highOrder);
    assertEquals(2, encodedNumber(highOrder));
    assertTrue(isHigh(highOrder, Ttl74148.EO));
    cascade(highOrder, lowOrder);
    encoder.propagate(lowOrder);
    assertEquals(0, encodedNumber(lowOrder));
    assertTrue(isHigh(lowOrder, Ttl74148.GS));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var encoder = new Ttl74148();
    final var state = new TtlTestInstanceState(encoder, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    setLow(state, Ttl74148.EI);
    for (final var input : DATA_INPUTS) {
      setHigh(state, input);
    }
    setLow(state, Ttl74148.I3);
    encoder.propagate(state);
    assertEquals(3, encodedNumber(state));

    state.setPortValue(VCC_PORT, Value.FALSE);
    encoder.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    encoder.propagate(state);
    assertEquals(3, encodedNumber(state));

    state.setPortValue(GND_PORT, Value.TRUE);
    encoder.propagate(state);
    assertUnknownOutputs(state);
  }

  /** Creates an enabled device whose data inputs are all held at the inactive level. */
  private static TtlTestInstanceState enabledAndIdle(Ttl74148 encoder) {
    final var state = new TtlTestInstanceState(encoder, false);
    setLow(state, Ttl74148.EI);
    for (final var input : DATA_INPUTS) {
      setHigh(state, input);
    }
    return state;
  }

  private static void cascade(TtlTestInstanceState from, TtlTestInstanceState to) {
    to.setPortValue(
        Ttl74148.pinNrToPortNr(Ttl74148.EI),
        from.getPortValue(Ttl74148.pinNrToPortNr(Ttl74148.EO)));
  }

  /** Converts the active-low code outputs back into the data input number they encode. */
  private static int encodedNumber(TtlTestInstanceState state) {
    return (isLow(state, Ttl74148.A2) ? 4 : 0)
        + (isLow(state, Ttl74148.A1) ? 2 : 0)
        + (isLow(state, Ttl74148.A0) ? 1 : 0);
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : new byte[] {
        Ttl74148.A2, Ttl74148.A1, Ttl74148.A0, Ttl74148.GS, Ttl74148.EO}) {
      assertEquals(Value.UNKNOWN, state.getPortValue(Ttl74148.pinNrToPortNr(output)));
    }
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74148.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void setLow(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74148.pinNrToPortNr(dsPinNr), Value.FALSE);
  }

  private static void setHigh(TtlTestInstanceState state, byte dsPinNr) {
    state.setPortValue(Ttl74148.pinNrToPortNr(dsPinNr), Value.TRUE);
  }

  private static boolean isLow(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74148.pinNrToPortNr(dsPinNr)) == Value.FALSE;
  }

  private static boolean isHigh(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74148.pinNrToPortNr(dsPinNr)) == Value.TRUE;
  }
}
