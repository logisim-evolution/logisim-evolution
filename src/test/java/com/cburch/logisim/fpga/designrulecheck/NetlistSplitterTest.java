/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.designrulecheck;

import static com.cburch.logisim.fpga.Strings.S;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.SplitterAttributes;
import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.fpga.gui.FpgaReportTabbedPane;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.memory.Register;
import com.cburch.logisim.std.wiring.Constant;
import com.cburch.logisim.std.wiring.Pin;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;

class NetlistSplitterTest {
  private final String originalHdlType = AppPreferences.HdlType.get();
  private final List<Project> projects = new ArrayList<>();

  @AfterEach
  void restoreState() {
    for (final var project : projects) project.getSimulator().shutDown();
    Reporter.report.setGuiLogger(null);
    AppPreferences.HdlType.set(originalHdlType);
  }

  @ParameterizedTest
  @ValueSource(strings = {"VHDL", "Verilog"})
  void partialSplittersCanJoinDifferentSourcesIntoDifferentBusBits(String hdlType)
      throws Exception {
    AppPreferences.HdlType.set(hdlType);
    final var project = loadReportedCircuit();
    final var circuit = project.getCurrentCircuit();
    final var input = pinWithLabel(circuit, "x");
    final var output = pinWithLabel(circuit, "y");
    final var state = CircuitState.createRootState(project, circuit, Thread.currentThread());
    final var expected = new int[] {0, 1, 3, 2};
    for (var value = 0; value < expected.length; value++) {
      final var inputState = state.getInstanceState(input);
      Pin.FACTORY.driveInputPin(inputState, Value.createKnown(BitWidth.create(2), value));
      Pin.FACTORY.propagate(inputState);
      state.getPropagator().propagate();
      assertEquals(
          Value.createKnown(BitWidth.create(2), expected[value]),
          Pin.FACTORY.getValue(state.getInstanceState(output)));
    }

    assertEquals(Netlist.DRC_PASSED, check(circuit));
  }

  @ParameterizedTest
  @ValueSource(strings = {"VHDL", "Verilog"})
  void additionalDriverOnSplitterBranchStillFailsDrc(String hdlType) throws Exception {
    AppPreferences.HdlType.set(hdlType);
    final var circuit = loadReportedCircuit().getCurrentCircuit();
    add(
        circuit,
        Constant.FACTORY.createComponent(
            Location.create(290, 440, true), Constant.FACTORY.createAttributeSet()));

    assertEquals(Netlist.DRC_ERROR, check(circuit));
  }

  @ParameterizedTest
  @ValueSource(strings = {"VHDL", "Verilog"})
  void joiningDifferentBitsOfTheSameInputStillFailsDrc(String hdlType) throws Exception {
    AppPreferences.HdlType.set(hdlType);
    final var circuit = loadReportedCircuit().getCurrentCircuit();
    add(
        circuit,
        Wire.create(Location.create(280, 380, true), Location.create(300, 380, true)));

    assertEquals(Netlist.DRC_ERROR, check(circuit));
  }

  @ParameterizedTest
  @ValueSource(strings = {"VHDL", "Verilog"})
  void disconnectedBitDoesNotHideUndrivenSinkWarnings(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var circuit = createProject(file).getCurrentCircuit();
    final var highBit = partialSplitter(1, Direction.WEST);
    final var lowBit = partialSplitter(0, Direction.SOUTH);
    final var source = Constant.FACTORY.createComponent(
        highBit.getEnd(1).getLocation(), Constant.FACTORY.createAttributeSet());
    final var busOutput = output("y", 2, Location.create(190, 100, true));
    final var bitOutput = output("z", 1, lowBit.getEnd(1).getLocation().translate(0, 40));
    add(
        circuit,
        highBit,
        lowBit,
        source,
        busOutput,
        bitOutput,
        Wire.create(highBit.getLocation(), busOutput.getLocation()),
        Wire.create(lowBit.getEnd(1).getLocation(), bitOutput.getLocation()));
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);

    assertEquals(Netlist.DRC_PASSED, check(circuit));
    final var warnings = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui, atLeastOnce()).addWarning(warnings.capture());
    final var undriven = warnings.getAllValues().stream()
        .filter(warning -> warning.toString().equals(S.get("NetList_UnsourcedSink")))
        .count();
    assertEquals(2, undriven, "The output bus bit 0 and the one-bit output have no driver");
  }

  private Project loadReportedCircuit() throws Exception {
    try (final var input =
        getClass().getResourceAsStream("/fpga/splitter-unconnected-bits.circ")) {
      assertNotNull(input);
      final var file = LogisimFile.load(input, new Loader(null));
      assertNotNull(file);
      return createProject(file);
    }
  }

  @ParameterizedTest
  @ValueSource(strings = {"VHDL", "Verilog"})
  void disconnectedBitDoesNotProvideAClockSourceFromAnotherBit(String hdlType) {
    AppPreferences.HdlType.set(hdlType);
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var circuit = createProject(file).getCurrentCircuit();
    final var lowBit = partialSplitter(0, Direction.WEST);
    final var highBit = partialSplitter(1, Direction.SOUTH);
    final var source = Constant.FACTORY.createComponent(
        lowBit.getEnd(1).getLocation(), Constant.FACTORY.createAttributeSet());
    final var registerFactory = new Register();
    final var register = registerFactory.createComponent(
        Location.create(300, 240, true), registerFactory.createAttributeSet());
    final var clock = register.getEnd(Register.CK).getLocation();
    final var branch = highBit.getEnd(1).getLocation();
    final var corner = Location.create(branch.getX(), clock.getY(), true);
    final var busOutput = output("y", 2, Location.create(190, 100, true));
    add(
        circuit,
        lowBit,
        highBit,
        source,
        register,
        busOutput,
        Wire.create(lowBit.getLocation(), busOutput.getLocation()),
        Wire.create(branch, corner),
        Wire.create(corner, clock));
    final var reporterGui = mock(FpgaReportTabbedPane.class);
    Reporter.report.setGuiLogger(reporterGui);

    assertEquals(Netlist.DRC_PASSED, check(circuit));
    final var warnings = ArgumentCaptor.forClass(Object.class);
    verify(reporterGui, atLeastOnce()).addWarning(warnings.capture());
    assertEquals(
        1,
        warnings.getAllValues().stream()
            .filter(warning -> warning.toString().equals(S.get("NetList_NoClockConnection")))
            .count(),
        "A driver on bus bit 0 must not be treated as the clock source on bit 1");
  }

  private Project createProject(LogisimFile file) {
    final var project = new Project(file);
    projects.add(project);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    project.setCurrentCircuit(circuit);
    return project;
  }

  private static int check(Circuit circuit) {
    return circuit.getNetList().designRuleCheckResult(true, new ArrayList<>());
  }

  private static Component pinWithLabel(Circuit circuit, String label) {
    return circuit.getNonWires().stream()
        .filter(component -> component.getFactory() == Pin.FACTORY
            && label.equals(component.getAttributeSet().getValue(StdAttr.LABEL)))
        .findFirst()
        .orElseThrow();
  }

  @SuppressWarnings("unchecked")
  private static Component partialSplitter(int selectedBit, Direction facing) {
    final var attrs = (SplitterAttributes) SplitterFactory.instance.createAttributeSet();
    attrs.setValue(StdAttr.FACING, facing);
    attrs.setValue(SplitterAttributes.ATTR_FANOUT, 1);
    attrs.setValue(SplitterAttributes.ATTR_APPEARANCE, SplitterAttributes.APPEAR_CENTER);
    for (var bit = 0; bit < 2; bit++) {
      final var name = "bit" + bit;
      final var bitAttribute = attrs.getAttributes().stream()
          .filter(attribute -> attribute.getName().equals(name))
          .findFirst()
          .orElseThrow();
      attrs.setValue(
          (Attribute<Integer>) bitAttribute, bit == selectedBit ? 1 : 0);
    }
    return SplitterFactory.instance.createComponent(Location.create(150, 100, true), attrs);
  }

  private static Component output(String label, int width, Location location) {
    final var attrs = Pin.FACTORY.createAttributeSet();
    attrs.setValue(Pin.ATTR_TYPE, Pin.OUTPUT);
    attrs.setValue(StdAttr.WIDTH, BitWidth.create(width));
    attrs.setValue(StdAttr.LABEL, label);
    return Pin.FACTORY.createComponent(location, attrs);
  }

  private static void add(Circuit circuit, Component... components) {
    final var mutation = new CircuitMutation(circuit);
    for (final var component : components) mutation.add(component);
    mutation.execute();
  }
}
