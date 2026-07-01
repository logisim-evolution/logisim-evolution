/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class AutoLabelTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void verilogAutoLabelAllowsCaseDistinctComponentLabels() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var fixture = new Fixture();
    add(fixture.circuit, pinWithLabel("A"));

    final var labeler = new AutoLabel("a", fixture.circuit);

    assertEquals("a", labeler.getCurrent(fixture.circuit, Pin.FACTORY));
  }

  private static Component pinWithLabel(String label) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(Location.create(0, 0, true), attrs);
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
