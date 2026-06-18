/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.std.wiring.Tunnel;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class CircuitLabelValidationTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void rejectsLabelsThatOnlyDifferByCaseFromAnotherComponent() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertFalse(
        Circuit.isCorrectLabel(
            "main", "a", components, Pin.FACTORY.createAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void relaxedModeAllowsLabelsThatDifferOnlyByCase() {
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertTrue(
        Circuit.isCorrectLabel(
            "main",
            "a",
            components,
            Pin.FACTORY.createAttributeSet(),
            Pin.FACTORY,
            CircuitLabelValidator.LabelIdentity.CASE_SENSITIVE,
            false));
  }

  @Test
  void relaxedModeStillRejectsExactDuplicateLabels() {
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertFalse(
        Circuit.isCorrectLabel(
            "main",
            "A",
            components,
            Pin.FACTORY.createAttributeSet(),
            Pin.FACTORY,
            CircuitLabelValidator.LabelIdentity.CASE_SENSITIVE,
            false));
  }

  @Test
  void relaxedModeStillRejectsPinLabelThatCollidesWithCircuitNameByCase() {
    final var components = componentSet(pinWithLabel("b"));

    assertFalse(
        Circuit.isCorrectLabel(
            "Main",
            "main",
            components,
            Pin.FACTORY.createAttributeSet(),
            Pin.FACTORY,
            CircuitLabelValidator.LabelIdentity.CASE_SENSITIVE,
            false));
  }

  @Test
  void vhdlClearsCaseOnlyDuplicateLabelWhenAddingComponent() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var fixture = new Fixture();
    final var firstPin = pinWithLabel("A", 0);
    final var secondPin = pinWithLabel("a", 40);

    add(fixture.circuit, firstPin);
    add(fixture.circuit, secondPin);

    assertEquals("", secondPin.getAttributeSet().getValue(StdAttr.LABEL));
  }

  @Test
  void verilogKeepsCaseOnlyDuplicateLabelWhenAddingComponent() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var fixture = new Fixture();
    final var firstPin = pinWithLabel("A", 0);
    final var secondPin = pinWithLabel("a", 40);

    add(fixture.circuit, firstPin);
    add(fixture.circuit, secondPin);

    assertEquals("a", secondPin.getAttributeSet().getValue(StdAttr.LABEL));
  }

  @Test
  void noHdlKeepsCaseOnlyDuplicateLabelWhenAddingComponent() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.NONE);
    final var fixture = new Fixture();
    final var firstPin = pinWithLabel("A", 0);
    final var secondPin = pinWithLabel("a", 40);

    add(fixture.circuit, firstPin);
    add(fixture.circuit, secondPin);

    assertEquals("a", secondPin.getAttributeSet().getValue(StdAttr.LABEL));
  }

  @Test
  void allowsChangingOnlyTheCaseOfTheSameComponentLabel() {
    final var existingPin = pinWithLabel("A");
    final var components = componentSet(existingPin);

    assertTrue(
        Circuit.isCorrectLabel(
            "main", "a", components, existingPin.getAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void rejectsPinLabelsThatOnlyDifferByCaseFromCircuitName() {
    assertFalse(
        Circuit.isCorrectLabel(
            "Display", "display", Set.of(), Pin.FACTORY.createAttributeSet(), Pin.FACTORY, false));
  }

  @Test
  void allowsTunnelLabelsToReuseComponentLabels() {
    final var existingPin = pinWithLabel("A");
    final var tunnelAttrs = Tunnel.FACTORY.createAttributeSet();

    assertTrue(
        Circuit.isCorrectLabel(
            "main", "a", componentSet(existingPin), tunnelAttrs, Tunnel.FACTORY, false));
  }

  private static Component pinWithLabel(String label) {
    return pinWithLabel(label, 0);
  }

  private static Component pinWithLabel(String label, int x) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(Location.create(x, 0, true), attrs);
  }

  private static Set<Component> componentSet(Component component) {
    final var components = new HashSet<Component>();
    components.add(component);
    return components;
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  private static final class Fixture {
    private final Circuit circuit;

    private Fixture() {
      final var file = LogisimFile.createNew(new Loader(null), null);
      final var project = new Project(file);
      circuit = file.getMainCircuit();
      circuit.setProject(project);
      project.setCurrentCircuit(circuit);
    }
  }
}
