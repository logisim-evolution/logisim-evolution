/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class PortHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void mapInformationMatchesConfiguredDirectionWhenComponentIsCreated() {
    assertMapInformation(PortIo.INPUT, 4, 4, 0, 0);
    assertMapInformation(PortIo.OUTPUT, 4, 0, 4, 0);
    assertMapInformation(PortIo.INOUTSE, 4, 0, 0, 4);
    assertMapInformation(PortIo.INOUTME, 4, 0, 0, 4);
  }

  @Test
  void outputOnlyPortsUseOutputBubbleRange() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var nets = mock(Netlist.class);
    final var componentInfo = portComponent(PortIo.OUTPUT, 4);
    componentInfo.setLocalBubbleID(3, 2, 8, 4, 20, 5);

    try (final var hdl = mockStatic(Hdl.class, CALLS_REAL_METHODS)) {
      hdl.when(() -> Hdl.getBusName(componentInfo, 0, nets)).thenReturn("portData");

      final var code =
          String.join(
              "\n",
              new PortHdlGeneratorFactory()
                  .getInlinedCode(nets, 1L, componentInfo, "main")
                  .get());

      assertTrue(code.contains("logisimOutputBubbles(11 DOWNTO 8) <= portData;"));
      assertFalse(code.contains("logisimOutputBubbles(6 DOWNTO 3)"));
    }
  }

  @Test
  void inOutPortsUseInOutBubbleRange() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var nets = mock(Netlist.class);
    final var componentInfo = portComponent(PortIo.INOUTSE, 4);
    componentInfo.setLocalBubbleID(3, 2, 8, 4, 20, 5);

    try (final var hdl = mockStatic(Hdl.class, CALLS_REAL_METHODS)) {
      hdl.when(() -> Hdl.getBusName(componentInfo, 2, nets)).thenReturn("readData");
      hdl.when(() -> Hdl.getBusName(componentInfo, 1, nets)).thenReturn("writeData");
      hdl.when(() -> Hdl.getNetName(componentInfo, 0, true, nets)).thenReturn("enable");

      final var code =
          String.join(
              "\n",
              new PortHdlGeneratorFactory()
                  .getInlinedCode(nets, 1L, componentInfo, "main")
                  .get());

      assertTrue(code.contains("readData <= logisimInOutBubbles(23 DOWNTO 20);"));
      assertTrue(
          code.contains(
              "logisimInOutBubbles(23 DOWNTO 20) <= writeData WHEN enable = '1' "
                  + "ELSE (OTHERS => 'Z');"));
      assertFalse(code.contains("logisimInOutBubbles(6 DOWNTO 3)"));
    }
  }

  private static netlistComponent portComponent(AttributeOption direction, int width) {
    final var port = new PortIo();
    final var attrs = port.createAttributeSet();
    attrs.setValue(PortIo.ATTR_DIR, direction);
    attrs.setValue(PortIo.ATTR_SIZE, BitWidth.create(width));
    return new netlistComponent(port.createComponent(Location.create(0, 0, true), attrs));
  }

  private static void assertMapInformation(
      AttributeOption direction, int width, int inputs, int outputs, int inOuts) {
    final var mapInfo = portComponent(direction, width).getMapInformationContainer();
    assertEquals(inputs, mapInfo.getNrOfInPorts());
    assertEquals(outputs, mapInfo.getNrOfOutPorts());
    assertEquals(inOuts, mapInfo.getNrOfInOutPorts());
  }
}
