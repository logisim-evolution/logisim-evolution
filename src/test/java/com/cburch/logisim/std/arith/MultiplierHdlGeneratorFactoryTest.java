/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.prefs.AppPreferences;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class MultiplierHdlGeneratorFactoryTest {

  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void unsignedNumericTypeSetsUnsignedMultiplierGeneric() {
    final var parameters = parametersFor(Multiplier.UNSIGNED_OPTION);

    assertEquals("0", parameters.get("unsignedMultiplier"));
  }

  @Test
  void signedUnsignedNumericTypeSetsSignedUnsignedMultiplierGeneric() {
    final var parameters = parametersFor(Multiplier.SIGNED_UNSIGNED_OPTION);

    assertEquals("1", parameters.get("unsignedMultiplier"));
  }

  @Test
  void twosComplementNumericTypeClearsUnsignedMultiplierGeneric() {
    final var parameters = parametersFor(Multiplier.SIGNED_OPTION);

    assertEquals("2", parameters.get("unsignedMultiplier"));
  }

  @Test
  void vhdlSelectsProductAndCarryExtensionMatchingTheGeneric() {
    final var hdl = functionality(HdlGeneratorFactory.VHDL);

    // A ';' inside a conditional signal assignment ends it early and is a syntax error.
    assertFalse(hdl.matches("(?s).*;\\s*when.*"));
    // numeric_std has no signed * unsigned operator.
    assertFalse(hdl.contains("vector(signed(inputa)*unsigned(inputb)"));
    assertFalse(hdl.contains("vector(signed(s_multresult) + unsigned(s_extendedcarryin)"));
    assertTrue(
        hdl.contains(
            "s_multresult <= std_logic_vector(unsigned(inputa)*unsigned(inputb)) "
                + "when unsignedmultiplier= 0 else "
                + "std_logic_vector(resize(signed(inputa)*signed('0'&inputb), calcbits)) "
                + "when unsignedmultiplier= 1 else "
                + "std_logic_vector(signed(inputa)*signed(inputb));"));
    assertTrue(hdl.contains("(others => '0') when unsignedmultiplier = 0 else"));
    assertTrue(
        hdl.contains(
            "s_newresult <= "
                + "std_logic_vector(unsigned(s_multresult) + unsigned(s_extendedcarryin)) "
                + "when unsignedmultiplier= 0 else "
                + "std_logic_vector(signed(s_multresult) + signed(s_extendedcarryin));"));
  }

  @Test
  void verilogSelectsProductAndCarryExtensionMatchingTheGeneric() {
    final var hdl = functionality(HdlGeneratorFactory.VERILOG);

    assertTrue(
        hdl.contains(
            "if (unsignedmultiplier == 0) begin s_carryin[calcbits-1:nrofbits] = 0; "
                + "s_multunsigned = $unsigned(inputa) * $unsigned(inputb);"));
    assertTrue(
        hdl.contains(
            "else if (unsignedmultiplier== 1) begin if (carryin[nrofbits-1] == 1) "
                + "s_carryin[calcbits-1:nrofbits] = -1; "
                + "else s_carryin[calcbits-1:nrofbits] = 0; "
                + "s_multsigned = $signed(inputa) * $signed({1'b0, inputb});"));
    // Mixing a signed and an unsigned operand makes the whole Verilog expression unsigned.
    assertFalse(hdl.contains("$signed(inputa) * $unsigned(inputb)"));
  }

  /** Generated module body, lower-cased and with whitespace collapsed to single spaces. */
  private static String functionality(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var attrs = new Multiplier().createAttributeSet();
    final var lines = new MultiplierHdlGeneratorFactory().getModuleFunctionality(null, attrs).get();
    return String.join(" ", lines).replaceAll("\\s+", " ").toLowerCase(Locale.ROOT);
  }

  private static Map<String, String> parametersFor(AttributeOption numericType) {
    final var attrs = new Multiplier().createAttributeSet();
    attrs.setValue(Multiplier.MODE_ATTR, numericType);
    return new ExposedMultiplierHdlGeneratorFactory().parameterMap(attrs);
  }

  private static class ExposedMultiplierHdlGeneratorFactory extends MultiplierHdlGeneratorFactory {
    Map<String, String> parameterMap(AttributeSet attrs) {
      return getParameterMap(attrs);
    }
  }
}
