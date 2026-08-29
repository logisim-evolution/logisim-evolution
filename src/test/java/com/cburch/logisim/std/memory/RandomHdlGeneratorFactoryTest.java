/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.fpga.hdlgenerator.HdlText.containsIgnoringCase;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class RandomHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void vhdlImplementsFullXoshiroStateTransitionAndClockEnable() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    assertTrue(
        containsIgnoringCase(hdl, "q                  <= s_result((nrOfBits - 1) DOWNTO 0);"));
    assertTrue(
        containsIgnoringCase(
            hdl, "s_rotatedSum       <= s_sum(40 DOWNTO 0)&s_sum(63 DOWNTO 41);"));
    assertTrue(
        containsIgnoringCase(hdl, "s_nextState3       <= s_state3XorState1(18 DOWNTO 0)&"));
    assertTrue(
        containsIgnoringCase(hdl, HdlPorts.getTickName(1) + " = '1' AND enable = '1'"));
    assertTrue(containsIgnoringCase(hdl, "CASE s_resetReg IS"));
    assertTrue(containsIgnoringCase(hdl, "WHEN OTHERS =>"));
    assertFalse(hdl.contains("5DEECE66D"));
    assertFalse(hdl.contains("s_currentSeed"));
  }

  @Test
  void verilogImplementsFullXoshiroStateTransitionAndClockEnable() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(hdl.contains("assign q                 = s_result;"));
    assertTrue(hdl.contains("assign s_rotatedSum      = {s_sum[40:0], s_sum[63:41]};"));
    assertTrue(
        hdl.contains(
            "assign s_nextState3      = {s_state3XorState1[18:0], "
                + "s_state3XorState1[63:19]};"));
    assertTrue(hdl.contains("else if (" + HdlPorts.getTickName(1) + " && enable)"));
    assertTrue(hdl.contains("case (s_resetReg)"));
    assertTrue(hdl.contains("default:"));
    assertFalse(hdl.contains("5DEECE66D"));
    assertFalse(hdl.contains("s_currentSeed"));
  }

  @Test
  void explicitSeedMapsToSplitMix64InitialStateInVhdl() {
    final var parameters = parameters(HdlGeneratorFactory.VHDL, 1);

    assertEquals("64", parameters.get("nrOfBits"));
    assertEquals("X\"910A2DEC89025CC1\"", parameters.get("initialState0"));
    assertEquals("X\"BEEB8DA1658EEC67\"", parameters.get("initialState1"));
    assertEquals("X\"F893A2EEFB32555E\"", parameters.get("initialState2"));
    assertEquals("X\"71C18690EE42C90B\"", parameters.get("initialState3"));
  }

  @Test
  void explicitSeedMapsToSplitMix64InitialStateInVerilog() {
    final var parameters = parameters(HdlGeneratorFactory.VERILOG, 1);

    assertEquals("64'h910A2DEC89025CC1", parameters.get("initialState0"));
    assertEquals("64'hBEEB8DA1658EEC67", parameters.get("initialState1"));
    assertEquals("64'hF893A2EEFB32555E", parameters.get("initialState2"));
    assertEquals("64'h71C18690EE42C90B", parameters.get("initialState3"));
  }

  @Test
  void zeroSeedUsesDeterministicHdlFallback() {
    final var parameters = parameters(HdlGeneratorFactory.VERILOG, 0);

    assertEquals("64'hC81A0D35C50EB982", parameters.get("initialState0"));
    assertEquals("64'h506E90F594419A89", parameters.get("initialState1"));
    assertEquals("64'h734D0E03E6F349E9", parameters.get("initialState2"));
    assertEquals("64'h8A4763566F4D6D62", parameters.get("initialState3"));
  }

  @Test
  void negativeSeedUsesItsUnsigned32BitPattern() {
    final var parameters = parameters(HdlGeneratorFactory.VERILOG, -1);

    assertEquals("64'h73B13BA2AFF181C0", parameters.get("initialState0"));
    assertEquals("64'h612043051340D3B4", parameters.get("initialState1"));
  }

  @Test
  void componentInstantiationUsesDerivedParameterMap() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var attrs = attributes(1);
    final var random = new Random();
    final var componentInfo =
        new netlistComponent(random.createComponent(Location.create(0, 0, true), attrs));
    final var mapping =
        String.join(
            "\n",
            new ParameterOnlyRandomHdlGeneratorFactory()
                .getComponentMap(null, 1L, componentInfo, "LogisimRNG")
                .get());

    assertTrue(mapping.contains(".initialState0(64'h910A2DEC89025CC1)"));
    assertTrue(mapping.contains(".initialState3(64'h71C18690EE42C90B)"));
  }

  @Test
  void verilogDeclaresFixedWidthStateParameters() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var attrs = attributes(1);
    final var nets = mock(Netlist.class);
    when(nets.projName()).thenReturn("test");
    final var module =
        String.join(
            "\n", new RandomHdlGeneratorFactory().getArchitecture(nets, attrs, "LogisimRNG"));

    assertTrue(module.contains("parameter [63:0] initialState0 = 1;"));
    assertFalse(module.contains("parameter [64:0] initialState0"));
  }

  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    return String.join(
        "\n",
        new RandomHdlGeneratorFactory().getModuleFunctionality(null, attributes(1)).get());
  }

  private static Map<String, String> parameters(String hdlType, int seed) {
    AppPreferences.HdlType.set(hdlType);
    return new ExposedRandomHdlGeneratorFactory().parameterMap(attributes(seed));
  }

  private static AttributeSet attributes(int seed) {
    final var attrs = new Random().createAttributeSet();
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(64));
    attrs.setValue(Random.ATTR_SEED, seed);
    return attrs;
  }

  private static class ExposedRandomHdlGeneratorFactory extends RandomHdlGeneratorFactory {
    Map<String, String> parameterMap(AttributeSet attrs) {
      return getParameterMap(attrs);
    }
  }

  private static class ParameterOnlyRandomHdlGeneratorFactory
      extends RandomHdlGeneratorFactory {
    @Override
    public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
      return new TreeMap<>();
    }
  }
}
