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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import org.junit.jupiter.api.Test;

class PlaRomStateTest {
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

  @Test
  void legacyContentsSurviveCircSaveAndReload() throws Exception {
    final var fixture = new Fixture();
    add(fixture.file.getMainCircuit(), fixture.createPlaRom());
    final var output = new ByteArrayOutputStream();
    fixture.file.write(output, fixture.loader);

    final var reloader = new Loader(null);
    final var loaded =
        LogisimFile.load(new ByteArrayInputStream(output.toByteArray()), reloader);
    final var component = loaded.getMainCircuit().getNonWires().iterator().next();
    final var attrs = component.getAttributeSet();
    assertEquals(Integer.valueOf(2), attrs.getValue(attribute(attrs, "inputs")));
    assertEquals(Integer.valueOf(2), attrs.getValue(attribute(attrs, "and")));
    assertEquals(Integer.valueOf(2), attrs.getValue(attribute(attrs, "outputs")));
    assertEquals(LEGACY_CONTENTS, attrs.getValue(attribute(attrs, "Contents")));

    final var project = new Project(loaded);
    final var state = CircuitState.createRootState(project, loaded.getMainCircuit());
    final var data = PlaRom.getPlaRomData(state.getInstanceState(component));
    assertTrue(data.getInputAndValue(0, 0));
    assertTrue(data.getInputAndValue(0, 3));
    assertTrue(data.getAndOutputValue(0, 0));
    assertTrue(data.getAndOutputValue(1, 1));

    final var outputAgain = new ByteArrayOutputStream();
    loaded.write(outputAgain, reloader);
    final var reloaded =
        LogisimFile.load(new ByteArrayInputStream(outputAgain.toByteArray()), new Loader(null));
    final var reloadedComponent = reloaded.getMainCircuit().getNonWires().iterator().next();
    final var reloadedAttrs = reloadedComponent.getAttributeSet();
    assertEquals(LEGACY_CONTENTS, reloadedAttrs.getValue(attribute(reloadedAttrs, "Contents")));
  }

  @Test
  void nestedInstancesRefreshContentsWithoutSharingSimulationValues() {
    final var fixture = new Fixture();
    final var child = new Circuit("child", fixture.file, fixture.project);
    fixture.file.addCircuit(child);
    final var plaRom = fixture.createPlaRom();
    add(child, plaRom);
    final var subFactory = child.getSubcircuitFactory();
    final var first =
        subFactory.createComponent(Location.create(200, 100, true), subFactory.createAttributeSet());
    final var second =
        subFactory.createComponent(Location.create(300, 100, true), subFactory.createAttributeSet());
    add(fixture.file.getMainCircuit(), first);
    add(fixture.file.getMainCircuit(), second);

    final var rootState = fixture.project.getCircuitState();
    final var firstState = subFactory.getSubstate(rootState, first);
    final var secondState = subFactory.getSubstate(rootState, second);
    final var firstData = PlaRom.getPlaRomData(firstState.getInstanceState(plaRom));
    final var secondData = PlaRom.getPlaRomData(secondState.getInstanceState(plaRom));
    assertNotSame(firstData, secondData);
    assertTrue(firstData.getInputAndValue(0, 0));
    assertTrue(secondData.getInputAndValue(0, 0));

    firstData.setInputsValue(new Value[] {Value.TRUE, Value.FALSE});
    assertEquals(Value.UNKNOWN, secondData.getInputValue((byte) 0));

    final var clonedRoot = rootState.cloneAsNewRootState();
    final var clonedFirstState = subFactory.getSubstate(clonedRoot, first);
    final var clonedData = PlaRom.getPlaRomData(clonedFirstState.getInstanceState(plaRom));
    assertNotSame(firstData, clonedData);
    clonedData.setInputAndValue(0, 0, false);
    assertTrue(firstData.getInputAndValue(0, 0));

    final var attrs = plaRom.getAttributeSet();
    final var resize = new CircuitMutation(child);
    resize.set(plaRom, attribute(attrs, "inputs"), 3);
    resize.execute();
    assertEquals(3, PlaRom.getPlaRomData(firstState.getInstanceState(plaRom)).getInputs());
    assertEquals(3, PlaRom.getPlaRomData(secondState.getInstanceState(plaRom)).getInputs());
    assertTrue(PlaRom.getPlaRomData(secondState.getInstanceState(plaRom)).getInputAndValue(0, 0));
    firstData.setInputsValue(new Value[] {Value.TRUE, Value.FALSE, Value.TRUE});

    final var updateContents = new CircuitMutation(child);
    updateContents.set(plaRom, attribute(attrs, "Contents"), "0*10 ");
    updateContents.execute();
    assertFalse(PlaRom.getPlaRomData(firstState.getInstanceState(plaRom)).getInputAndValue(0, 0));
    assertFalse(PlaRom.getPlaRomData(secondState.getInstanceState(plaRom)).getInputAndValue(0, 0));
    assertEquals(Value.TRUE, firstData.getInputValue((byte) 0));
  }
}
