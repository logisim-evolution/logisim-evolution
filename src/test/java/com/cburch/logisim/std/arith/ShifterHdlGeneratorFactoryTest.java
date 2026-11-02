/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ShifterHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void mapsAllFiveShiftModes() {
    assertAll(
        () -> assertEquals("0", parametersFor(8, Shifter.SHIFT_LOGICAL_LEFT).get("shifterMode")),
        () -> assertEquals("1", parametersFor(8, Shifter.SHIFT_ROLL_LEFT).get("shifterMode")),
        () -> assertEquals("2", parametersFor(8, Shifter.SHIFT_LOGICAL_RIGHT).get("shifterMode")),
        () -> assertEquals("3", parametersFor(8, Shifter.SHIFT_ARITHMETIC_RIGHT).get("shifterMode")),
        () -> assertEquals("4", parametersFor(8, Shifter.SHIFT_ROLL_RIGHT).get("shifterMode")));
  }

  @Test
  void verilogStage0UsesCorrectMsbAndShiftModeComparisons() {
    final var hdl8 = generatedHdl(HdlGeneratorFactory.VERILOG, 8, Shifter.SHIFT_LOGICAL_LEFT);

    assertAll(
        () -> assertTrue(hdl8.contains("assign s_stage0ShiftIn = ((shifterMode == 1) || (shifterMode == 3))")),
        () -> assertTrue(hdl8.contains("? dataA[7] : (shifterMode == 4) ? dataA[0] : 0;")),
        () -> assertFalse(hdl8.contains("dataA[shifterMode]")),
        () -> assertFalse(hdl8.contains("(7 == 4)")),
        () -> assertTrue(hdl8.contains("assign s_stage0Result  = (shiftAmount[0] == 0)")));

    final var hdl5 = generatedHdl(HdlGeneratorFactory.VERILOG, 5, Shifter.SHIFT_LOGICAL_LEFT);
    assertAll(
        () -> assertTrue(hdl5.contains("? dataA[4] : (shifterMode == 4) ? dataA[0] : 0;")),
        () -> assertFalse(hdl5.contains("(4 == 4)")),
        () -> assertTrue(hdl5.contains("assign s_stage0Result  = (shiftAmount[0] == 0)")));

    final var hdl2 = generatedHdl(HdlGeneratorFactory.VERILOG, 2, Shifter.SHIFT_LOGICAL_LEFT);
    assertAll(
        () -> assertTrue(hdl2.contains("? dataA[1] : (shifterMode == 4) ? dataA[0] : 0;")),
        () -> assertTrue(hdl2.contains("assign s_stage0Result  = (shiftAmount == 0)")));
  }

  @Test
  void vhdlStagesAboveZeroUseMsbForArithmeticShiftRight() {
    final var hdl8 = generatedHdl(HdlGeneratorFactory.VHDL, 8, Shifter.SHIFT_ARITHMETIC_RIGHT);

    assertAll(
        () -> assertTrue(containsIgnoringCase(hdl8, "s_stage0Result(7)")),
        () -> assertTrue(containsIgnoringCase(hdl8, "s_stage1Result(7)")),
        () -> assertFalse(containsIgnoringCase(hdl8, "s_stage0Result(0)")),
        () -> assertFalse(containsIgnoringCase(hdl8, "s_stage1Result(1)")));

    final var hdl5 = generatedHdl(HdlGeneratorFactory.VHDL, 5, Shifter.SHIFT_ARITHMETIC_RIGHT);
    assertAll(
        () -> assertTrue(containsIgnoringCase(hdl5, "s_stage0Result(4)")),
        () -> assertTrue(containsIgnoringCase(hdl5, "s_stage1Result(4)")));
  }

  private static Map<String, String> parametersFor(int width, AttributeOption mode) {
    return new ExposedShifterHdlGeneratorFactory().parameterMap(attributes(width, mode));
  }

  private static AttributeSet attributes(int width, AttributeOption mode) {
    final var attrs = new Shifter().createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
    attrs.setValue(Shifter.ATTR_SHIFT, mode);
    var shift = 1;
    while ((1 << shift) < width) shift++;
    attrs.setValue(Shifter.SHIFT_BITS_ATTR, shift);
    return attrs;
  }

  private static String generatedHdl(String hdlType, int width, AttributeOption mode) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = attributes(width, mode);
    final var nets = mock(Netlist.class);
    when(nets.projName()).thenReturn("shifter-test");
    final var shifter = new Shifter();
    final var generator = new ShifterHdlGeneratorFactory();
    final var componentName = shifter.getHDLName(attrs);
    return String.join("\n", generator.getEntity(nets, attrs, componentName))
        + "\n"
        + String.join("\n", generator.getArchitecture(nets, attrs, componentName));
  }

  private static class ExposedShifterHdlGeneratorFactory extends ShifterHdlGeneratorFactory {
    Map<String, String> parameterMap(AttributeSet attrs) {
      return getParameterMap(attrs);
    }
  }
}
