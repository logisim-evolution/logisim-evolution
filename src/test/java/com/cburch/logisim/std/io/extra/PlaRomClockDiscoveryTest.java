/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.log.LoggableContract;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import org.junit.jupiter.api.Test;

class PlaRomClockDiscoveryTest {
  private static final String LEGACY_CONTENTS = "1 2 0 1 1 0 0 1 ";

  private static final class Fixture {
    final Loader loader = new Loader(null);
    final LogisimFile file = LogisimFile.createNew(loader, null);
    final PlaRom factory;
    final Project project;

    Fixture() {
      final var library = loader.getBuiltin().getLibrary(ExtraIoLibrary._ID);
      file.addLibrary(library);
      factory =
          (PlaRom)
              library.getTools().stream()
                  .filter(tool -> tool instanceof AddTool addTool
                      && PlaRom._ID.equals(addTool.getFactory().getName()))
                  .map(tool -> ((AddTool) tool).getFactory())
                  .findFirst()
                  .orElseThrow();
      project = new Project(file);
      project.setCurrentCircuit(file.getMainCircuit());
    }

    Component createPlaRom() {
      final var attrs = factory.createAttributeSet();
      attrs.setValue(attribute(attrs, "inputs"), 2);
      attrs.setValue(attribute(attrs, "and"), 2);
      attrs.setValue(attribute(attrs, "outputs"), 2);
      attrs.setValue(attribute(attrs, "Contents"), LEGACY_CONTENTS);
      return factory.createComponent(Location.create(100, 100, true), attrs);
    }
  }

  @SuppressWarnings("unchecked")
  private static <T> Attribute<T> attribute(AttributeSet attrs, String name) {
    return (Attribute<T>) attrs.getAttributes().stream()
        .filter(attr -> name.equals(attr.getName()))
        .findFirst()
        .orElseThrow();
  }

  private static void add(Circuit circuit, Component component) {
    final var mutation = new CircuitMutation(circuit);
    mutation.add(component);
    mutation.execute();
  }

  /**
   * Clock discovery (ComponentSelector.findClocks) queries every loggable component for its bit
   * width through an InstanceLoggerAdapter whose InstanceState has no CircuitState. Reading the
   * PlaRom output width must therefore not require a CircuitState. Before the fix, PlaRom.Logger
   * routed through getPlaRomData(state) -> state.getData(), dereferencing the null CircuitState and
   * throwing a NullPointerException, which stopped the clock from working when a PlaRom was placed
   * in a subcircuit. See issue #1247.
   */
  @Test
  void loggerReportsOutputWidthWithoutCircuitState() {
    final var fixture = new Fixture();
    final var child = new Circuit("child", fixture.file, fixture.project);
    fixture.file.addCircuit(child);
    final var plaRom = fixture.createPlaRom();
    add(child, plaRom);
    final var subFactory = child.getSubcircuitFactory();
    final var sub =
        subFactory.createComponent(
            Location.create(200, 100, true), subFactory.createAttributeSet());
    add(fixture.file.getMainCircuit(), sub);

    final var log = (LoggableContract) plaRom.getFeature(LoggableContract.class);
    assertNotNull(log);
    // This is the exact call ComponentSelector.findClocks makes; it must not throw.
    final var bitWidth = log.getBitWidth(null);
    assertEquals(BitWidth.create(2), bitWidth);
  }
}
