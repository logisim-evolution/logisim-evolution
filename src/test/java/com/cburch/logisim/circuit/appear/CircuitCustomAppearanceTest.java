/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.circuit.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.draw.actions.ModelTranslateAction;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.Loader;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.appear.CanvasActionAdapter;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.wiring.Pin;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class CircuitCustomAppearanceTest {

  @Test
  void getObjectsInUsesCustomObjectsEvenWhenParentShowsDefaultAppearance() {
    final var parent = mock(CircuitAppearance.class);
    final var customShape = new Rectangle(10, 10, 20, 20);
    final var defaultShape = new Rectangle(100, 100, 20, 20);
    final var query = Bounds.create(0, 0, 40, 40);
    final var model = new CircuitCustomAppearance(parent);

    when(parent.getCustomObjectsFromBottom()).thenReturn(List.of(customShape));
    when(parent.getObjectsIn(query)).thenReturn(List.of(defaultShape));

    final var result = model.getObjectsIn(query);

    assertEquals(1, result.size());
    assertSame(customShape, result.iterator().next());
  }

  @Test
  void addingPinsWithoutAppearanceEditsKeepsDefaultCustomAppearance() {
    final var circuit = new Circuit("main", null, null);
    assertFalse(circuit.getAppearance().hasCustomAppearance());

    addPin(circuit, "A", 100);
    assertFalse(circuit.getAppearance().hasCustomAppearance());

    addPin(circuit, "B", 200);
    assertFalse(circuit.getAppearance().hasCustomAppearance());
    circuit.getStaticAttributes()
        .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CUSTOM);
    assertFalse(circuit.getAppearance().hasCustomAppearance());
  }

  @Test
  void changingOnlyAnchorFacingCountsAsCustomAppearance() {
    final var circuit = new Circuit("main", null, null);
    final var anchor = circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearanceAnchor.class::isInstance)
        .map(AppearanceAnchor.class::cast)
        .findFirst().orElseThrow();

    anchor.setValue(AppearanceAnchor.FACING, Direction.WEST);
    assertTrue(circuit.getAppearance().hasCustomAppearance());

    anchor.setValue(AppearanceAnchor.FACING, Direction.EAST);
    assertFalse(circuit.getAppearance().hasCustomAppearance());
  }

  @Test
  void addingPinPreservesPreviouslyEditedPortPosition() {
    final var circuit = new Circuit("main", null, null);
    addPin(circuit, "A", 100);
    final var port = circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearancePort.class::isInstance)
        .map(AppearancePort.class::cast)
        .findFirst().orElseThrow();
    final var expected = port.getLocation().translate(70, 130);
    new CanvasActionAdapter(circuit,
        new ModelTranslateAction(
            circuit.getAppearance().getCustomAppearanceDrawing(), List.of(port), 70, 130))
        .doIt(null);

    addPin(circuit, "B", 200);

    assertTrue(circuit.getAppearance().hasCustomAppearance());
    final var retainedPort = circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearancePort.class::isInstance)
        .map(AppearancePort.class::cast)
        .filter(candidate -> "A".equals(candidate.getPin().getAttributeValue(StdAttr.LABEL)))
        .findFirst().orElseThrow();
    assertEquals(expected, retainedPort.getLocation());
  }

  @ParameterizedTest
  @ValueSource(booleans = {false, true})
  void replacingSchematicPinPreservesAppearanceClassificationAndPortPosition(boolean edited) {
    final var file = LogisimFile.createNew(new Loader(null), null);
    final var project = new Project(file);
    final var circuit = file.getMainCircuit();
    circuit.setProject(project);
    project.setCurrentCircuit(circuit);
    addPin(circuit, "A", 100);
    final var oldPin = circuit.getNonWires().iterator().next();
    final var port = circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearancePort.class::isInstance)
        .map(AppearancePort.class::cast)
        .findFirst().orElseThrow();
    if (edited) {
      new CanvasActionAdapter(circuit,
          new ModelTranslateAction(
              circuit.getAppearance().getCustomAppearanceDrawing(), List.of(port), 70, 130))
          .doIt(null);
    }
    final var expected = port.getLocation();
    final var attributes = Pin.FACTORY.createAttributeSet();
    attributes.setValue(StdAttr.LABEL, "A");
    final var newPin = Pin.FACTORY.createComponent(
        Location.create(100, 200, true), attributes);
    final var replacePin = new CircuitMutation(circuit);
    replacePin.replace(oldPin, newPin);
    replacePin.execute();

    assertEquals(edited, circuit.getAppearance().hasCustomAppearance());
    final var retainedPort = circuit.getAppearance().getCustomObjectsFromBottom().stream()
        .filter(AppearancePort.class::isInstance)
        .map(AppearancePort.class::cast)
        .findFirst().orElseThrow();
    assertEquals(expected, retainedPort.getLocation());
    assertSame(Instance.getInstanceFor(newPin), retainedPort.getPin());
  }

  private static void addPin(Circuit circuit, String label, int y) {
    final var attributes = Pin.FACTORY.createAttributeSet();
    attributes.setValue(StdAttr.LABEL, label);
    final var addPin = new CircuitMutation(circuit);
    addPin.add(Pin.FACTORY.createComponent(Location.create(100, y, true), attributes));
    addPin.execute();
  }
}
