/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitAttributes;
import org.junit.jupiter.api.Test;

class CanvasActionAdapterTest {

  @Test
  void drawingEditSwitchesCircuitToCustomAndUndoRestoresPreviousAppearance() {
    final var circuit = new Circuit("main", null, null);
    circuit.getStaticAttributes()
        .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CLASSIC);
    final var model = circuit.getAppearance().getCustomAppearanceDrawing();
    final var rectangle = new Rectangle(10, 10, 30, 20);
    final var action = new CanvasActionAdapter(circuit, new ModelAddAction(model, rectangle));

    action.doIt(null);

    assertEquals(
        CircuitAttributes.APPEAR_CUSTOM,
        circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
    assertTrue(circuit.getAppearance().getCustomObjectsFromBottom().contains(rectangle));

    action.undo(null);

    assertEquals(
        CircuitAttributes.APPEAR_CLASSIC,
        circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
    assertFalse(circuit.getAppearance().getCustomObjectsFromBottom().contains(rectangle));

    action.doIt(null);

    assertEquals(
        CircuitAttributes.APPEAR_CUSTOM,
        circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
    assertTrue(circuit.getAppearance().getCustomObjectsFromBottom().contains(rectangle));
  }

  @Test
  void undoKeepsCustomAppearanceWhenCircuitWasAlreadyCustom() {
    final var circuit = new Circuit("main", null, null);
    circuit.getStaticAttributes()
        .setValue(CircuitAttributes.APPEARANCE_ATTR, CircuitAttributes.APPEAR_CUSTOM);
    final var model = circuit.getAppearance().getCustomAppearanceDrawing();
    final var rectangle = new Rectangle(10, 10, 30, 20);
    final var action = new CanvasActionAdapter(circuit, new ModelAddAction(model, rectangle));

    action.doIt(null);
    action.undo(null);

    assertEquals(
        CircuitAttributes.APPEAR_CUSTOM,
        circuit.getStaticAttributes().getValue(CircuitAttributes.APPEARANCE_ATTR));
    assertFalse(circuit.getAppearance().getCustomObjectsFromBottom().contains(rectangle));
  }
}
