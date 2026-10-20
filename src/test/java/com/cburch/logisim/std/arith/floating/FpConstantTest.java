/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import org.junit.jupiter.api.Test;

class FpConstantTest {
  @Test
  void libraryContainsFloatingPointConstant() {
    assertNotNull(new FPArithmeticLibrary().getTool(FpConstant._ID));
  }

  @Test
  void encodesValueForEachSupportedFloatingPointWidth() {
    assertEquals(0x3CL, FpConstant.createValue(BitWidth.create(8), 1.5).toLongValue());
    assertEquals(0x3E00L, FpConstant.createValue(BitWidth.create(16), 1.5).toLongValue());
    assertEquals(0x3FC00000L, FpConstant.createValue(BitWidth.create(32), 1.5).toLongValue());
    assertEquals(
        0x3FF8000000000000L,
        FpConstant.createValue(BitWidth.create(64), 1.5).toLongValue());
  }

  @Test
  void propagatesEncodedFloatingPointValue() {
    final var width = BitWidth.create(16);
    final var state = mock(InstanceState.class);
    when(state.getAttributeValue(StdAttr.FP_WIDTH)).thenReturn(width);
    when(state.getAttributeValue(FpConstant.ATTR_VALUE)).thenReturn(-2.25);

    new FpConstant().propagate(state);

    verify(state).setPort(0, Value.createKnown(width, -2.25), 1);
  }

  @Test
  void componentUsesFloatingPointWidthForItsOutputAndHdlGenerator() {
    final var constant = new FpConstant();
    final var attrs = constant.createAttributeSet();
    final var width = BitWidth.create(64);
    attrs.setValue(StdAttr.FP_WIDTH, width);
    attrs.setValue(FpConstant.ATTR_VALUE, -2.25);

    final var component = constant.createComponent(Location.create(0, 0, false), attrs);

    assertEquals(width, component.getEnd(0).getWidth());
    assertNotNull(constant.getHDLGenerator(attrs));
  }

  @Test
  void hdlGeneratorUsesEncodedFloatingPointBits() {
    final var originalHdlType = AppPreferences.HdlType.get();
    try {
      AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
      final var constant = new FpConstant();
      final var attrs = constant.createAttributeSet();
      attrs.setValue(StdAttr.FP_WIDTH, BitWidth.create(16));
      attrs.setValue(FpConstant.ATTR_VALUE, 1.5);
      final var component = constant.createComponent(Location.create(0, 0, false), attrs);
      final var componentInfo = mock(netlistComponent.class);
      final var nets = mock(Netlist.class);
      when(componentInfo.getComponent()).thenReturn(component);
      when(componentInfo.isEndConnected(0)).thenReturn(true);
      when(nets.isContinuesBus(componentInfo, 0)).thenReturn(true);

      try (final var hdl = mockStatic(Hdl.class, CALLS_REAL_METHODS)) {
        hdl.when(() -> Hdl.getBusNameContinues(componentInfo, 0, nets)).thenReturn("outputBus");

        final var code =
            String.join(
                "\n",
                constant
                    .getHDLGenerator(attrs)
                    .getInlinedCode(nets, 1L, componentInfo, "main")
                    .get());

        assertTrue(code.replaceAll("\\s+", "").contains("outputBus<=X\"3E00\";"), code);
      }
    } finally {
      AppPreferences.HdlType.set(originalHdlType);
    }
  }
}
