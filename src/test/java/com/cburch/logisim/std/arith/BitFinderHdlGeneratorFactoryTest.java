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

class BitFinderHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void mapsAllFourFindModes() {
    assertAll(
        () -> assertEquals("0", parametersFor(8, BitFinder.LOW_ONE).get("findMode")),
        () -> assertEquals("1", parametersFor(8, BitFinder.HIGH_ONE).get("findMode")),
        () -> assertEquals("2", parametersFor(8, BitFinder.LOW_ZERO).get("findMode")),
        () -> assertEquals("3", parametersFor(8, BitFinder.HIGH_ZERO).get("findMode")));
  }

  @Test
  void derivesIndexWidthAcrossBoundariesAndNonPowerOfTwoInputs() {
    assertAll(
        () -> assertEquals(1, BitFinder.computeOutputBits(1)),
        () -> assertEquals(1, BitFinder.computeOutputBits(2)),
        () -> assertEquals(2, BitFinder.computeOutputBits(3)),
        () -> assertEquals(3, BitFinder.computeOutputBits(5)),
        () -> assertEquals(6, BitFinder.computeOutputBits(64)),
        () -> assertEquals("3", parametersFor(5, BitFinder.LOW_ONE).get("nrOfIndexBits")),
        () -> assertEquals("6", parametersFor(64, BitFinder.LOW_ONE).get("nrOfIndexBits")));
  }

  @Test
  void separatesScalarPortLayoutsFromParameterizedVectorLayout() {
    final var finder = new BitFinder();

    assertAll(
        () -> assertEquals("BitFinder_1_bit", finder.getHDLName(attributes(1, BitFinder.LOW_ONE))),
        () -> assertEquals("BitFinder_2_bit", finder.getHDLName(attributes(2, BitFinder.LOW_ONE))),
        () -> assertEquals(BitFinder._ID, finder.getHDLName(attributes(3, BitFinder.LOW_ONE))),
        () -> assertEquals(BitFinder._ID, finder.getHDLName(attributes(64, BitFinder.HIGH_ZERO))));
  }

  @Test
  void oneBitVhdlUsesScalarPortsWithoutZeroWidthVectors() {
    final var hdl = generatedHdl(HdlGeneratorFactory.VHDL, 1, BitFinder.LOW_ONE);

    assertAll(
        () -> assertTrue(containsIgnoringCase(hdl, "inputVector : in  std_logic")),
        () -> assertTrue(containsIgnoringCase(hdl, "index       : out std_logic")),
        () -> assertTrue(containsIgnoringCase(hdl, "present <= inputVector")),
        () -> assertFalse(containsIgnoringCase(hdl, "std_logic_vector( 0 downto 0 )")),
        () -> assertFalse(containsIgnoringCase(hdl, "std_logic_vector( -1 downto 0 )")));
  }

  @Test
  void twoBitVhdlKeepsTheIndexScalar() {
    final var hdl = generatedHdl(HdlGeneratorFactory.VHDL, 2, BitFinder.HIGH_ONE);

    assertAll(
        () ->
            assertTrue(
                containsIgnoringCase(
                    hdl,
                    "inputVector : in  std_logic_vector( (nrOfInputBits - 1) downto 0 )")),
        () -> assertTrue(containsIgnoringCase(hdl, "index       : out std_logic")),
        () -> assertTrue(containsIgnoringCase(hdl, "if indexValue = 0 then")));
  }

  @Test
  void parameterizedVhdlScansInBothDirections() {
    final var hdl = generatedHdl(HdlGeneratorFactory.VHDL, 5, BitFinder.LOW_ZERO);

    assertAll(
        () -> assertTrue(containsIgnoringCase(hdl, "for bitIndex in 0 to nrOfInputBits - 1 loop")),
        () ->
            assertTrue(
                containsIgnoringCase(
                    hdl, "for bitIndex in nrOfInputBits - 1 downto 0 loop")),
        () ->
            assertTrue(
                containsIgnoringCase(
                    hdl,
                    "s_index <= std_logic_vector(to_unsigned(indexValue, nrOfIndexBits));")),
        () -> assertTrue(containsIgnoringCase(hdl, "targetValue := '0';")));
  }

  @Test
  void oneBitVerilogUsesScalarPortsAndDirectLogic() {
    final var hdl = generatedHdl(HdlGeneratorFactory.VERILOG, 1, BitFinder.HIGH_ZERO);

    assertAll(
        () -> assertTrue(hdl.contains("input inputVector;")),
        () -> assertTrue(hdl.contains("output index;")),
        () -> assertTrue(hdl.contains("assign index = 1'b0;")),
        () -> assertFalse(hdl.contains("[-1:0]")));
  }

  @Test
  void parameterizedVerilogSupportsTheSixtyFourBitBoundary() {
    final var hdl = generatedHdl(HdlGeneratorFactory.VERILOG, 64, BitFinder.HIGH_ONE);

    assertAll(
        () -> assertTrue(hdl.contains("input [nrOfInputBits-1:0] inputVector;")),
        () -> assertTrue(hdl.contains("output [nrOfIndexBits-1:0] index;")),
        () -> assertTrue(hdl.contains("integer bitIndex;")),
        () ->
            assertTrue(
                hdl.contains(
                    "for (bitIndex = nrOfInputBits - 1; bitIndex >= 0; bitIndex = bitIndex - 1)")),
        () ->
            assertTrue(
                hdl.contains(
                    "for (bitIndex = 0; bitIndex < nrOfInputBits; bitIndex = bitIndex + 1)")),
        () -> assertFalse(hdl.contains("!s_present")),
        () -> assertEquals("64", parametersFor(64, BitFinder.HIGH_ONE).get("nrOfInputBits")),
        () -> assertEquals("6", parametersFor(64, BitFinder.HIGH_ONE).get("nrOfIndexBits")));
  }

  private static Map<String, String> parametersFor(int width, AttributeOption mode) {
    return new ExposedBitFinderHdlGeneratorFactory().parameterMap(attributes(width, mode));
  }

  private static AttributeSet attributes(int width, AttributeOption mode) {
    final var attrs = new BitFinder().createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
    attrs.setValue(BitFinder.TYPE, mode);
    return attrs;
  }

  private static String generatedHdl(String hdlType, int width, AttributeOption mode) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = attributes(width, mode);
    final var nets = mock(Netlist.class);
    when(nets.projName()).thenReturn("bit-finder-test");
    final var finder = new BitFinder();
    final var generator = new BitFinderHdlGeneratorFactory();
    final var componentName = finder.getHDLName(attrs);
    return String.join(
        "\n",
        generator.getEntity(nets, attrs, componentName))
        + "\n"
        + String.join("\n", generator.getArchitecture(nets, attrs, componentName));
  }

  private static class ExposedBitFinderHdlGeneratorFactory
      extends BitFinderHdlGeneratorFactory {
    Map<String, String> parameterMap(AttributeSet attrs) {
      return getParameterMap(attrs);
    }
  }
}
