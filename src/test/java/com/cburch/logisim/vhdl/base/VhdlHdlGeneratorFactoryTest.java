/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.base;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.TickComponentHdlGeneratorFactory;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.junit.jupiter.api.Test;

class VhdlHdlGeneratorFactoryTest {

  @Test
  void entityDeclarationKeepsEveryParsedGenericNameAndType() throws Exception {
    final var content = contentWithGenerics();
    final var attrs = new VhdlEntityAttributes(content);
    final var netlist = mock(Netlist.class);
    when(netlist.projName()).thenReturn("TestProject");

    final var source =
        normalized(
            new TestVhdlHdlGeneratorFactory()
                .getVhdlBlackBox(netlist, attrs, "generic_entry"));

    assertTrue(
        source.contains(
            "generic ( delay : time; integer_value : integer; natural_value : natural; positive_value : positive );"),
        source);
  }

  @Test
  void parameterMapUsesDefaultsAndPerInstanceOverrides() throws Exception {
    final var content = contentWithGenerics();
    final var defaultAttrs = new VhdlEntityAttributes(content);
    final var overriddenAttrs = new VhdlEntityAttributes(content);
    overriddenAttrs.setValue(genericAttribute(content, "natural_value"), 42);
    overriddenAttrs.setValue(genericAttribute(content, "delay"), 2_000_000);
    final var generator = new TestVhdlHdlGeneratorFactory();

    assertEquals(
        Map.of(
            "delay", "5000000 fs",
            "integer_value", "128",
            "natural_value", "7",
            "positive_value", "3"),
        generator.getTestParameterMap(defaultAttrs));
    assertEquals(
        Map.of(
            "delay", "2000000 fs",
            "integer_value", "128",
            "natural_value", "42",
            "positive_value", "3"),
        generator.getTestParameterMap(overriddenAttrs));
  }

  @Test
  void existingGeneratorParametersKeepTheirIntegerDeclarations() {
    final var netlist = mock(Netlist.class);
    when(netlist.projName()).thenReturn("TestProject");

    final var source =
        normalized(
            new TestTickComponentHdlGeneratorFactory(50_000_000L, 1_000.0)
                .getVhdlBlackBox(netlist, null, "logisimTickGenerator"));

    assertTrue(
        source.contains("generic ( nrofbits : integer; reloadvalue : integer );"), source);
  }

  private VhdlContent contentWithGenerics() throws Exception {
    final var parser =
        new VhdlParser(
            """
            entity generic_entry is
              generic (
                integer_value : integer := 128;
                natural_value : natural := 7;
                positive_value : positive := 3;
                delay : time := 5 ns
              );
            end entity generic_entry;

            architecture rtl of generic_entry is
            begin
            end architecture rtl;
            """);
    parser.parse();
    final var content = new VhdlContent("generic_entry", null);
    content.generics =
        parser.getGenerics().stream()
            .map(VhdlContent.Generic::new)
            .toArray(VhdlContent.Generic[]::new);
    return content;
  }

  private Attribute<Integer> genericAttribute(VhdlContent content, String name) {
    return content.getGenericAttributes().stream()
        .filter(
            attribute ->
                ((VhdlEntityAttributes.VhdlGenericAttribute) attribute)
                    .getGeneric()
                    .getName()
                    .equals(name))
        .findFirst()
        .orElseThrow();
  }

  private String normalized(List<String> lines) {
    return String.join(" ", lines)
        .replaceAll("\\s+", " ")
        .trim()
        .toLowerCase(Locale.ROOT);
  }

  private static class TestVhdlHdlGeneratorFactory extends VhdlHdlGeneratorFactory {
    private List<String> getVhdlBlackBox(
        Netlist netlist, AttributeSet attrs, String componentName) {
      return getVHDLBlackBox(netlist, attrs, componentName, true);
    }

    private Map<String, String> getTestParameterMap(AttributeSet attrs) {
      return getParameterMap(attrs);
    }
  }

  private static class TestTickComponentHdlGeneratorFactory
      extends TickComponentHdlGeneratorFactory {
    private TestTickComponentHdlGeneratorFactory(long clockFrequency, double tickFrequency) {
      super(clockFrequency, tickFrequency);
    }

    private List<String> getVhdlBlackBox(
        Netlist netlist, AttributeSet attrs, String componentName) {
      return getVHDLBlackBox(netlist, attrs, componentName, true);
    }
  }
}
