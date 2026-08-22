/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.vhdl.sim;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.vhdl.base.VhdlContent;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import com.cburch.logisim.vhdl.base.VhdlParser;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;

class VhdlSimulatorGenerationTest {

  private static final String DEPENDENCY_SOURCE =
      """
      library ieee;
      use ieee.std_logic_1164.all;

      entity Dependency is
        port (
          dataIn : in std_logic;
          dataOut : out std_logic
        );
      end Dependency;

      architecture rtl of Dependency is
      begin
        dataOut <= dataIn;
      end rtl;
      """;

  private static final String PARENT_SOURCE =
      """
      library ieee;
      use ieee.std_logic_1164.all;

      entity Parent is
        port (
          dataIn : in std_logic;
          dataOut : out std_logic
        );
      end Parent;

      architecture rtl of Parent is
      begin
        dependency_instance : entity work.Dependency(rtl)
          port map (
            dataIn => dataIn,
            dataOut => dataOut
          );
      end rtl;
      """;

  @Test
  void repeatedEntityUsesOneCanonicalTypeAndUniqueBridgeNames() {
    final var ports =
        List.of(
            new VhdlParser.PortDescription("dataIn", Port.INPUT, 1),
            new VhdlParser.PortDescription("dataOut", Port.OUTPUT, 1));
    final var firstName = VhdlSimulatorTop.createSimulationName("Parent", 0);
    final var secondName = VhdlSimulatorTop.createSimulationName("Parent", 1);
    final var components =
        List.of(
            new VhdlSimulatorComponent("Parent", firstName, ports),
            new VhdlSimulatorComponent("Parent", secondName, ports));

    final var top = new VhdlSimulatorVhdlTop().buildTop(components);

    assertEquals(1, occurrences(top, "component Parent"));
    assertFalse(top.contains("component " + firstName));
    assertTrue(top.contains(firstName + "_map : Parent port map ("));
    assertTrue(top.contains(secondName + "_map : Parent port map ("));
    assertTrue(top.contains(firstName + "_dataIn"));
    assertTrue(top.contains(secondName + "_dataOut"));
    assertDoesNotThrow(() -> new VhdlParser(top).parse());
  }

  @Test
  void projectSourcesKeepLibraryOrderAndIncludeUnplacedDependencies() {
    final var dependency = VhdlContent.create("Dependency", null);
    final var parent = VhdlContent.create("Parent", null);
    final var placedParent = createComponent(parent, "visible_label", 100);

    final var sources =
        VhdlSimulatorTop.collectVhdlSources(
            List.of(dependency, parent), List.of(placedParent));
    final var script =
        new VhdlSimulatorTclComp()
            .buildCompileScript(sources.stream().map(VhdlContent::getName).toList());

    assertEquals(2, sources.size());
    assertSame(dependency, sources.get(0));
    assertSame(parent, sources.get(1));
    assertTrue(script.indexOf("Dependency.vhdl") < script.indexOf("Parent.vhdl"));
    assertEquals(3, occurrences(script, "if {[catch {"));
    assertEquals(3, occurrences(script, "Compilation error: $errmsg"));
  }

  @Test
  void visibleLabelsDoNotReplaceUniqueSimulationNames() {
    final var entity = VhdlContent.create("Worker", null);
    final var first = createComponent(entity, "same_label", 100);
    final var second = createComponent(entity, "same_label", 200);

    final var components =
        VhdlSimulatorTop.configureSimulationComponents(List.of(first, second));

    assertEquals("Worker", components.get(0).entityName());
    assertEquals(
        VhdlSimConstants.VHDL_COMPONENT_SIM_NAME + "Worker_0",
        components.get(0).simulationName());
    assertEquals(
        VhdlSimConstants.VHDL_COMPONENT_SIM_NAME + "Worker_1",
        components.get(1).simulationName());
    assertFalse(components.get(0).simulationName().contains("same_label"));
  }

  @Test
  void generatedFilesRetainCanonicalProjectEntities() throws Exception {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    project.setCurrentCircuit(circuit);

    final var dependency = VhdlContent.parse("Dependency", DEPENDENCY_SOURCE, file);
    file.addVhdlContent(dependency);
    final var parent = VhdlContent.parse("Parent", PARENT_SOURCE, file);
    file.addVhdlContent(parent);

    final var mutation = new CircuitMutation(circuit);
    mutation.add(createComponent(parent, "first_parent", 100));
    mutation.add(createComponent(parent, "second_parent", 200));
    mutation.execute();

    new VhdlSimulatorTop(project).generateFiles();

    final var sourceDirectory = Path.of(VhdlSimConstants.SIM_SRC_PATH);
    final var dependencyFile = sourceDirectory.resolve("Dependency.vhdl");
    final var parentFile = sourceDirectory.resolve("Parent.vhdl");
    final var top = Files.readString(sourceDirectory.resolve(VhdlSimConstants.SIM_TOP_FILENAME));
    final var compileScript =
        Files.readString(Path.of(VhdlSimConstants.SIM_PATH).resolve("comp.tcl"));

    assertEquals(DEPENDENCY_SOURCE, Files.readString(dependencyFile));
    assertEquals(PARENT_SOURCE, Files.readString(parentFile));
    assertTrue(compileScript.indexOf("Dependency.vhdl") < compileScript.indexOf("Parent.vhdl"));
    assertTrue(top.contains("component Parent"));
    assertEquals(1, occurrences(top, "component Parent"));
    assertTrue(top.contains("LogisimVhdlSimComp_Parent_0_map : Parent port map ("));
    assertTrue(top.contains("LogisimVhdlSimComp_Parent_1_map : Parent port map ("));
    assertDoesNotThrow(() -> new VhdlParser(top).parse());
  }

  private static Component createComponent(VhdlContent content, String label, int x) {
    final var factory = new VhdlEntity(content);
    final var attrs = factory.createAttributeSet();
    attrs.setValue(StdAttr.LABEL, label);
    return factory.createComponent(Location.create(x, 100, true), attrs);
  }

  private static int occurrences(String text, String needle) {
    var count = 0;
    var start = 0;
    while ((start = text.indexOf(needle, start)) >= 0) {
      count++;
      start += needle.length();
    }
    return count;
  }
}
