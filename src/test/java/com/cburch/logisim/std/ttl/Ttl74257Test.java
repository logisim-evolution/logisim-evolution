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

class Ttl74257Test {
  /** Each row is source 0, source 1, and the output of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {Ttl74257.L1_I0, Ttl74257.L1_I1, Ttl74257.L1_Y},
    {Ttl74257.L2_I0, Ttl74257.L2_I1, Ttl74257.L2_Y},
    {Ttl74257.L3_I0, Ttl74257.L3_I1, Ttl74257.L3_Y},
    {Ttl74257.L4_I0, Ttl74257.L4_I1, Ttl74257.L4_Y}
  };

  private static final int GND_PORT = 14;
  private static final int VCC_PORT = 15;

  @Test
  void logicalPortsFollowTheDatasheetPinout() {
    final var mux = new Ttl74257();
    final var hiddenPower = createInstance(mux, false);

    assertEquals(14, hiddenPower.getPorts().size());
    assertPort(hiddenPower, Ttl74257.SELECT, 10, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L1_I0, 30, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L1_I1, 50, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L1_Y, 70, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L2_I0, 90, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L2_I1, 110, 30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L2_Y, 130, 30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L3_Y, 150, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L3_I1, 130, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L3_I0, 110, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L4_Y, 90, -30, EndData.OUTPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L4_I1, 70, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.L4_I0, 50, -30, EndData.INPUT_ONLY);
    assertPort(hiddenPower, Ttl74257.OE, 30, -30, EndData.INPUT_ONLY);

    final var shownPower = createInstance(mux, true);
    assertEquals(16, shownPower.getPorts().size());
    assertEquals(Location.create(150, 30, false), shownPower.getPortLocation(GND_PORT));
    assertEquals(Location.create(10, -30, false), shownPower.getPortLocation(VCC_PORT));
  }

  @Test
  void eachChannelPassesTheSelectedSourceAndIgnoresTheOther() {
    final var mux = new Ttl74257();

    for (var pattern = 0; pattern < 16; pattern++) {
      for (final var selectHigh : new boolean[] {false, true}) {
        final var state = enabled(mux, selectHigh);
        for (var channel = 0; channel < CHANNELS.length; channel++) {
          final var source0 = ((pattern >> channel) & 1) == 1;
          set(state, CHANNELS[channel][0], source0 ? Value.TRUE : Value.FALSE);
          set(state, CHANNELS[channel][1], source0 ? Value.FALSE : Value.TRUE);
        }
        mux.propagate(state);

        for (var channel = 0; channel < CHANNELS.length; channel++) {
          final var source0 = ((pattern >> channel) & 1) == 1;
          final var expected = selectHigh ? !source0 : source0;
          assertEquals(
              expected ? Value.TRUE : Value.FALSE,
              output(state, CHANNELS[channel][2]),
              "channel " + channel + " select " + (selectHigh ? 1 : 0));
        }
      }
    }
  }

  @Test
  void highOutputEnableReleasesEveryOutput() {
    final var mux = new Ttl74257();
    final var state = enabled(mux, true);
    set(state, Ttl74257.OE, Value.TRUE);
    for (final var channel : CHANNELS) {
      set(state, channel[0], Value.ERROR);
      set(state, channel[1], Value.TRUE);
    }
    set(state, Ttl74257.SELECT, Value.UNKNOWN);
    mux.propagate(state);

    for (final var channel : CHANNELS) {
      assertEquals(Value.UNKNOWN, output(state, channel[2]));
    }
  }

  @Test
  void unknownSelectPassesTheValueBothSourcesShare() {
    final var mux = new Ttl74257();
    final var state = enabled(mux, false);
    set(state, Ttl74257.SELECT, Value.UNKNOWN);
    for (final var channel : CHANNELS) {
      set(state, channel[0], Value.TRUE);
      set(state, channel[1], Value.TRUE);
    }
    mux.propagate(state);

    for (final var channel : CHANNELS) {
      assertEquals(Value.TRUE, output(state, channel[2]));
    }
  }

  @Test
  void unknownSelectReleasesTheOutputWhenTheSourcesDiffer() {
    final var mux = new Ttl74257();
    final var state = enabled(mux, false);
    set(state, Ttl74257.SELECT, Value.UNKNOWN);
    set(state, Ttl74257.L1_I0, Value.FALSE);
    set(state, Ttl74257.L1_I1, Value.TRUE);
    mux.propagate(state);

    assertEquals(Value.UNKNOWN, output(state, Ttl74257.L1_Y));
  }

  @Test
  void errorOnSelectMattersOnlyWhenTheSourcesDiffer() {
    final var mux = new Ttl74257();
    final var agreed = enabled(mux, false);
    set(stateSources(agreed, Value.TRUE, Value.TRUE), Ttl74257.SELECT, Value.ERROR);
    mux.propagate(agreed);
    assertEquals(Value.TRUE, output(agreed, Ttl74257.L2_Y));

    final var split = enabled(mux, false);
    set(stateSources(split, Value.FALSE, Value.TRUE), Ttl74257.SELECT, Value.ERROR);
    mux.propagate(split);
    assertEquals(Value.ERROR, output(split, Ttl74257.L2_Y));
  }

  @Test
  void selectedUnknownOrErrorPassesThroughAndTheOtherSourceDoesNot() {
    final var mux = new Ttl74257();
    final var fromSource1 = enabled(mux, true);
    set(fromSource1, Ttl74257.L3_I0, Value.ERROR);
    set(fromSource1, Ttl74257.L3_I1, Value.UNKNOWN);
    mux.propagate(fromSource1);
    assertEquals(Value.UNKNOWN, output(fromSource1, Ttl74257.L3_Y));

    final var fromSource0 = enabled(mux, false);
    set(fromSource0, Ttl74257.L4_I0, Value.FALSE);
    set(fromSource0, Ttl74257.L4_I1, Value.ERROR);
    mux.propagate(fromSource0);
    assertEquals(Value.FALSE, output(fromSource0, Ttl74257.L4_Y));

    final var errorSelected = enabled(mux, true);
    set(errorSelected, Ttl74257.L1_I0, Value.FALSE);
    set(errorSelected, Ttl74257.L1_I1, Value.ERROR);
    mux.propagate(errorSelected);
    assertEquals(Value.ERROR, output(errorSelected, Ttl74257.L1_Y));
  }

  @Test
  void uncertainOutputEnableFollowsTheDisagreementOfFloatAndData() {
    final var mux = new Ttl74257();
    final var unknownEnable = enabled(mux, false);
    set(unknownEnable, Ttl74257.OE, Value.UNKNOWN);
    set(unknownEnable, Ttl74257.L1_I0, Value.TRUE);
    mux.propagate(unknownEnable);
    assertEquals(Value.UNKNOWN, output(unknownEnable, Ttl74257.L1_Y));

    final var errorEnable = enabled(mux, false);
    set(errorEnable, Ttl74257.OE, Value.ERROR);
    set(errorEnable, Ttl74257.L1_I0, Value.TRUE);
    mux.propagate(errorEnable);
    assertEquals(Value.ERROR, output(errorEnable, Ttl74257.L1_Y));

    final var errorData = enabled(mux, false);
    set(errorData, Ttl74257.OE, Value.UNKNOWN);
    set(errorData, Ttl74257.L1_I0, Value.ERROR);
    mux.propagate(errorData);
    assertEquals(Value.ERROR, output(errorData, Ttl74257.L1_Y));
  }

  @Test
  void invalidExposedPowerInputsMakeOutputsUnknown() {
    final var mux = new Ttl74257();
    final var state = new TtlTestInstanceState(mux, true);
    state.setPortValue(GND_PORT, Value.FALSE);
    state.setPortValue(VCC_PORT, Value.TRUE);
    set(state, Ttl74257.OE, Value.FALSE);
    set(state, Ttl74257.SELECT, Value.FALSE);
    set(state, Ttl74257.L1_I0, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74257.L1_Y));

    state.setPortValue(VCC_PORT, Value.FALSE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74257.L1_Y));

    state.setPortValue(VCC_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.TRUE, output(state, Ttl74257.L1_Y));

    state.setPortValue(GND_PORT, Value.TRUE);
    mux.propagate(state);
    assertEquals(Value.UNKNOWN, output(state, Ttl74257.L1_Y));
  }

  /** Enables the device and drives every data input low before a test overrides some of them. */
  private static TtlTestInstanceState enabled(Ttl74257 mux, boolean selectHigh) {
    final var state = new TtlTestInstanceState(mux, false);
    set(state, Ttl74257.OE, Value.FALSE);
    set(state, Ttl74257.SELECT, selectHigh ? Value.TRUE : Value.FALSE);
    for (final var channel : CHANNELS) {
      set(state, channel[0], Value.FALSE);
      set(state, channel[1], Value.FALSE);
    }
    return state;
  }

  private static TtlTestInstanceState stateSources(TtlTestInstanceState state, Value source0, Value source1) {
    set(state, Ttl74257.L2_I0, source0);
    set(state, Ttl74257.L2_I1, source1);
    return state;
  }

  private static void assertPort(Instance instance, byte dsPinNr, int x, int y, int type) {
    final var port = Ttl74257.pinNrToPortNr(dsPinNr);
    assertEquals(Location.create(x, y, false), instance.getPortLocation(port));
    assertEquals(type, instance.getPorts().get(port).getType());
  }

  private static void set(TtlTestInstanceState state, byte dsPinNr, Value value) {
    state.setPortValue(Ttl74257.pinNrToPortNr(dsPinNr), value);
  }

  private static Value output(TtlTestInstanceState state, byte dsPinNr) {
    return state.getPortValue(Ttl74257.pinNrToPortNr(dsPinNr));
  }
}
