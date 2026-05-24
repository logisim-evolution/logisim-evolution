/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import org.junit.jupiter.api.Test;

class AttrTableSelectionModelTest {

  @Test
  void updatesAttributeSetWhenSelectionChangesBetweenEmptyAndSelected() {
    final var project = mock(Project.class);
    final var frame = mock(Frame.class);
    final var canvas = mock(Canvas.class);
    final var selection = mock(Selection.class);
    final var circuit = mock(Circuit.class);
    final var circuitAttrs = attributeSet("circuit");
    final var selectionAttrs = attributeSet("selection");

    when(frame.getCanvas()).thenReturn(canvas);
    when(canvas.getCircuit()).thenReturn(circuit);
    when(canvas.getSelection()).thenReturn(selection);
    when(circuit.getStaticAttributes()).thenReturn(circuitAttrs);
    when(selection.getAttributeSet()).thenReturn(selectionAttrs);

    when(selection.isEmpty()).thenReturn(true);
    final var model = new AttrTableSelectionModel(project, frame);
    assertSame(circuitAttrs, model.getAttributeSet());

    when(selection.isEmpty()).thenReturn(false);
    model.updateAttributeSet();
    assertSame(selectionAttrs, model.getAttributeSet());

    when(selection.isEmpty()).thenReturn(true);
    model.updateAttributeSet();
    assertSame(circuitAttrs, model.getAttributeSet());
  }

  private static AttributeSet attributeSet(String label) {
    return AttributeSets.fixedSet(new Attribute<?>[] {StdAttr.LABEL}, new Object[] {label});
  }
}
