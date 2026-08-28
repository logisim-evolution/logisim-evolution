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
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.io.PortIo;
import com.cburch.logisim.std.wiring.Constant;
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
  void constantCanDriveOutputOnlyPortIoWithoutShortCircuit() {
    final var fixture = new Fixture();
    final var width = BitWidth.create(8);

    final var constantAttrs = Constant.FACTORY.createAttributeSet();
    constantAttrs.setValue(StdAttr.WIDTH, width);
    constantAttrs.setValue(Constant.ATTR_VALUE, 0xffL);
    final var constant =
        Constant.FACTORY.createComponent(Location.create(340, 280, true), constantAttrs);

    final var portFactory = new PortIo();
    final var portAttrs = portFactory.createAttributeSet();
    portAttrs.setValue(PortIo.ATTR_DIR, PortIo.OUTPUT);
    portAttrs.setValue(PortIo.ATTR_SIZE, width);
    portAttrs.setValue(StdAttr.FACING, Direction.NORTH);
    portAttrs.setValue(StdAttr.LABEL, "interface");
    final var portIo =
        portFactory.createComponent(Location.create(390, 290, true), portAttrs);

    add(
        fixture.circuit,
        constant,
        portIo,
        Wire.create(constant.getLocation(), portIo.getEnd(0).getLocation()));

    final var result = fixture.circuit.getNetList().designRuleCheckResult(true, new ArrayList<>());

    assertEquals(Netlist.DRC_PASSED, result);
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

  private static void add(Circuit circuit, Component... components) {
    final var mutation = new CircuitMutation(circuit);
    for (final var component : components) mutation.add(component);
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
