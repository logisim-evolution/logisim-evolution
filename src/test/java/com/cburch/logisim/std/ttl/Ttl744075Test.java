/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static com.cburch.logisim.std.ttl.TtlTestInstanceState.createInstance;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

/** Tests for the TTL 744075 triple 3-input OR gate. */
class Ttl744075Test {
  private static final int[][] GATES = {
    {
      Ttl744075.PORT_INDEX_1A,
      Ttl744075.PORT_INDEX_1B,
      Ttl744075.PORT_INDEX_1C,
      Ttl744075.PORT_INDEX_1Y
    },
    {
      Ttl744075.PORT_INDEX_2A,
      Ttl744075.PORT_INDEX_2B,
      Ttl744075.PORT_INDEX_2C,
      Ttl744075.PORT_INDEX_2Y
    },
    {
      Ttl744075.PORT_INDEX_3A,
      Ttl744075.PORT_INDEX_3B,
      Ttl744075.PORT_INDEX_3C,
      Ttl744075.PORT_INDEX_3Y
    }
  };

  private static final int[] OUTPUTS = {
    Ttl744075.PORT_INDEX_1Y, Ttl744075.PORT_INDEX_2Y, Ttl744075.PORT_INDEX_3Y
  };

  private static final int GND_PORT = 12;
  private static final int VCC_PORT = 13;

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void usesPhysicalPinoutAndKeepsPowerPortsLast() {
    final var gate = new Ttl744075();
    final var hiddenPower = createInstance(gate, false);

    assertEquals(12, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_2A, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_2B, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_1A, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_1B, 70, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_1C, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_1Y, 110, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_2C, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_2Y, 110, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_3Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_3A, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_3B, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl744075.PORT_INDEX_3C, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(gate, true);
    assertEquals(14, shownPower.getPorts().size());
    assertPort(shownPower, Ttl744075.PORT_INDEX_2A, 10, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, Ttl744075.PORT_INDEX_3C, 30, -30, EndData.INPUT_ONLY);
    assertPort(shownPower, GND_PORT, 130, 30, EndData.INPUT_ONLY);
    assertPort(shownPower, VCC_PORT, 10, -30, EndData.INPUT_ONLY);
  }

  @Test
  void eachGateFollowsTheOrTruthTable() {
    final var gate = new Ttl744075();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var pins : GATES) {
      for (var pattern = 0; pattern < 8; pattern++) {
        driveAllLow(state);
        final var inputA = bit(pattern, 0);
        final var inputB = bit(pattern, 1);
        final var inputC = bit(pattern, 2);
        state.setPortValue(pins[0], inputA);
        state.setPortValue(pins[1], inputB);
        state.setPortValue(pins[2], inputC);
        gate.propagate(state);
        assertEquals(or(inputA, inputB, inputC), state.getPortValue(pins[3]));
        assertOtherOutputs(state, pins[3], Value.FALSE);
      }
    }
  }

  @Test
  void gatesComputeIndependently() {
    final var gate = new Ttl744075();
    final var state = new TtlTestInstanceState(gate, false);

    for (var pattern = 0; pattern < 512; pattern++) {
      final var inputs = new Value[] {
        bit(pattern, 0), bit(pattern, 1), bit(pattern, 2),
        bit(pattern, 3), bit(pattern, 4), bit(pattern, 5),
        bit(pattern, 6), bit(pattern, 7), bit(pattern, 8)
      };
      for (var gateIndex = 0; gateIndex < GATES.length; gateIndex++) {
        final var pins = GATES[gateIndex];
        state.setPortValue(pins[0], inputs[gateIndex * 3]);
        state.setPortValue(pins[1], inputs[gateIndex * 3 + 1]);
        state.setPortValue(pins[2], inputs[gateIndex * 3 + 2]);
      }
      gate.propagate(state);
      for (var gateIndex = 0; gateIndex < GATES.length; gateIndex++) {
        final var pins = GATES[gateIndex];
        assertEquals(
            or(inputs[gateIndex * 3], inputs[gateIndex * 3 + 1], inputs[gateIndex * 3 + 2]),
            state.getPortValue(pins[3]));
      }
    }
  }

  @Test
  void trueInputOverridesUnsettledInputs() {
    final var gate = new Ttl744075();
    final var state = new TtlTestInstanceState(gate, false);

    for (final var pins : GATES) {
      for (final var unsettled : new Value[] {Value.UNKNOWN, Value.ERROR}) {
        driveAllLow(state);
        state.setPortValue(pins[2], unsettled);
        gate.propagate(state);
        assertEquals(Value.ERROR, state.getPortValue(pins[3]));
        assertOtherOutputs(state, pins[3], Value.FALSE);

        driveAllLow(state);
        state.setPortValue(pins[0], Value.TRUE);
        state.setPortValue(pins[1], unsettled);
        state.setPortValue(pins[2], Value.ERROR);
        gate.propagate(state);
        assertEquals(Value.TRUE, state.getPortValue(pins[3]));
        assertOtherOutputs(state, pins[3], Value.FALSE);
      }
    }
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var gate = new Ttl744075();
    final var state = new TtlTestInstanceState(gate, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    driveAllLow(state);
    state.setPortValue(Ttl744075.PORT_INDEX_1A, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744075.PORT_INDEX_1Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    gate.propagate(state);
    assertUnknownOutputs(state);

    state.setPortValue(VCC_PORT, Value.TRUE);
    driveAllLow(state);
    state.setPortValue(Ttl744075.PORT_INDEX_2B, Value.TRUE);
    gate.propagate(state);
    assertEquals(Value.TRUE, state.getPortValue(Ttl744075.PORT_INDEX_2Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    gate.propagate(state);
    assertUnknownOutputs(state);
  }

  @Test
  void hdlEmitsOrForEachGate() {
    for (final var index : new int[] {1, 2, 3}) {
      final var vhdl = functionality(HdlGeneratorFactory.VHDL).replaceAll("\\s+", " ");
      assertTrue(containsIgnoringCase(
          vhdl, "Y" + index + " <= (A" + index + " OR B" + index + " OR C" + index + ")"));

      final var verilog = functionality(HdlGeneratorFactory.VERILOG);
      assertTrue(verilog.contains(
          "assign Y" + index + " = (A" + index + "|" + "B" + index + "|" + "C" + index + ");"));
    }
  }

  @Test
  void exposedPowerPinsAreNotAnHdlTarget() {
    final var gate = new Ttl744075();
    final var attrs = gate.createAttributeSet();

    attrs.setValue(TtlLibrary.VCC_GND, false);
    assertTrue(gate.isHDLSupportedComponent(attrs));
    final var generator = gate.getHDLGenerator(attrs);
    assertFalse(generator.isHdlSupportedTarget(null));

    attrs.setValue(TtlLibrary.VCC_GND, true);
    assertFalse(gate.isHDLSupportedComponent(attrs));
    assertNull(gate.getHDLGenerator(attrs));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var gate = new Ttl744075();
    final var attrs = gate.createAttributeSet();
    final var generator = (AbstractHdlGeneratorFactory) gate.getHDLGenerator(attrs);
    return String.join("\n", generator.getModuleFunctionality(null, attrs).get());
  }

  private static void assertPort(Instance instance, int index, int x, int y, int type) {
    assertEquals(Location.create(x, y, false), instance.getPortLocation(index));
    assertEquals(type, instance.getPorts().get(index).getType());
  }

  private static Value bit(int pattern, int index) {
    return ((pattern >> index) & 1) == 1 ? Value.TRUE : Value.FALSE;
  }

  private static Value or(Value inputA, Value inputB, Value inputC) {
    return inputA.or(inputB).or(inputC);
  }

  private static void driveAllLow(TtlTestInstanceState state) {
    for (final var pins : GATES) {
      state.setPortValue(pins[0], Value.FALSE);
      state.setPortValue(pins[1], Value.FALSE);
      state.setPortValue(pins[2], Value.FALSE);
    }
  }

  private static void assertOtherOutputs(TtlTestInstanceState state, int driven, Value expected) {
    for (final var output : OUTPUTS) {
      if (output != driven) {
        assertEquals(expected, state.getPortValue(output));
      }
    }
  }

  private static void assertUnknownOutputs(TtlTestInstanceState state) {
    for (final var output : OUTPUTS) {
      assertEquals(Value.UNKNOWN, state.getPortValue(output));
    }
  }
}
