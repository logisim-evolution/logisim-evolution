/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

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
import java.util.ArrayList;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class NetlistDrcTest {
  private final String originalHdlType = AppPreferences.HdlType.get();

  @AfterEach
  void restoreHdlType() {
    AppPreferences.HdlType.set(originalHdlType);
  }

  @Test
  void caseDistinctPinLabelsStillFailVhdlDrcWhenTheyDifferOnlyByCase() {
    AppPreferences.HdlType.set(HdlGeneratorFactory.VERILOG);
    final var fixture = new Fixture();
    final var firstPin = pinWithLabel("A", 0);
    final var secondPin = pinWithLabel("a", 40);

    add(fixture.circuit, firstPin);
    add(fixture.circuit, secondPin);

    assertEquals("a", secondPin.getAttributeSet().getValue(StdAttr.LABEL));
    AppPreferences.HdlType.set(HdlGeneratorFactory.VHDL);
    final var result = fixture.circuit.getNetList().designRuleCheckResult(true, new ArrayList<>());
    assertTrue((result & Netlist.DRC_ERROR) != 0);
  }

  private static Component pinWithLabel(String label, int x) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(Location.create(x, 0, true), attrs);
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
