/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.draw.model.Drawing;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.CircuitAppearance;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.main.AttrTableCircuitModel;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import org.junit.jupiter.api.Test;

class AppearanceViewTest {

  @Test
  void emptyAppearanceSelectionShowsCircuitAttributes() {
    final var project = mock(Project.class);
    final var circuitState = mock(CircuitState.class);
    final var circuit = mock(Circuit.class);
    final var appearance = mock(CircuitAppearance.class);
    final var logisimFile = mock(LogisimFile.class);
    final var view = new AppearanceView();
    final var table = new AttrTable(null);

    when(project.getLogisimFile()).thenReturn(logisimFile);
    when(logisimFile.contains(circuit)).thenReturn(true);
    when(circuitState.getCircuit()).thenReturn(circuit);
    when(circuit.getAppearance()).thenReturn(appearance);
    when(circuit.getName()).thenReturn("main");
    when(circuit.getStaticAttributes()).thenReturn(attributeSet("circuit"));
    when(appearance.getCustomAppearanceDrawing()).thenReturn(new Drawing());

    view.setCircuit(project, circuitState);
    view.getAttrTableDrawManager(table).attributesSelected();
    assertInstanceOf(AttrTableCircuitModel.class, table.getAttrTableModel());

    final var canvas = view.getCanvas();
    canvas.getSelection().setSelected(new Rectangle(0, 0, 10, 10), true);
    assertFalse(table.getAttrTableModel() instanceof AttrTableCircuitModel);
    assertTrue(table.getAttrTableModel().getRowCount() > 1);

    canvas.getSelection().clearSelected();
    assertInstanceOf(AttrTableCircuitModel.class, table.getAttrTableModel());
  }

  private static AttributeSet attributeSet(String label) {
    return AttributeSets.fixedSet(new Attribute<?>[] {StdAttr.LABEL}, new Object[] {label});
  }
}
