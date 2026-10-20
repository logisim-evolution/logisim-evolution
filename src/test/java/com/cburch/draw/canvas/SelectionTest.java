/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.canvas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.cburch.draw.model.Drawing;
import com.cburch.draw.shapes.Rectangle;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class SelectionTest {

  @Test
  void modelRemovalOfSelectedObjectNotifiesSelectionListeners() {
    final var canvas = new Canvas();
    final var drawing = new Drawing();
    final var selected = new Rectangle(0, 0, 10, 10);
    final var unselected = new Rectangle(20, 20, 10, 10);
    drawing.addObjects(0, List.of(selected, unselected));
    canvas.setModel(drawing, null);

    final var events = new ArrayList<SelectionEvent>();
    canvas.getSelection().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(SelectionEvent e) {
        events.add(e);
      }
    });
    canvas.getSelection().setSelected(selected, true);
    events.clear();

    drawing.removeObjects(List.of(selected, unselected));

    assertTrue(canvas.getSelection().isEmpty());
    assertEquals(1, events.size());
    assertEquals(SelectionEvent.ACTION_REMOVED, events.get(0).getAction());
    assertEquals(List.of(selected), List.copyOf(events.get(0).getAffected()));
  }

  @Test
  void modelRemovalOfUnselectedObjectDoesNotNotifySelectionListeners() {
    final var canvas = new Canvas();
    final var drawing = new Drawing();
    final var selected = new Rectangle(0, 0, 10, 10);
    final var unselected = new Rectangle(20, 20, 10, 10);
    drawing.addObjects(0, List.of(selected, unselected));
    canvas.setModel(drawing, null);

    final var events = new ArrayList<SelectionEvent>();
    canvas.getSelection().addSelectionListener(new SelectionListener() {
      @Override
      public void selectionChanged(SelectionEvent e) {
        events.add(e);
      }
    });
    canvas.getSelection().setSelected(selected, true);
    events.clear();

    drawing.removeObjects(List.of(unselected));

    assertFalse(canvas.getSelection().isEmpty());
    assertTrue(canvas.getSelection().isSelected(selected));
    assertTrue(events.isEmpty());
  }
}
