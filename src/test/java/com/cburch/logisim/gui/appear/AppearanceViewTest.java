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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.cburch.draw.model.Drawing;
import com.cburch.draw.shapes.Rectangle;
import com.cburch.draw.tools.DrawingAttributeSet;
import com.cburch.draw.tools.RectangleTool;
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
import java.awt.Dimension;
import java.awt.event.MouseEvent;
import org.junit.jupiter.api.Test;

class AppearanceViewTest {

  @Test
  void emptyAppearanceSelectionShowsCircuitAttributes() {
    final var view = newAppearanceView();
    final var table = new AttrTable(null);

    view.getAttrTableDrawManager(table).attributesSelected();
    assertInstanceOf(AttrTableCircuitModel.class, table.getAttrTableModel());

    final var canvas = view.getCanvas();
    canvas.getSelection().setSelected(new Rectangle(0, 0, 10, 10), true);
    assertFalse(table.getAttrTableModel() instanceof AttrTableCircuitModel);
    assertTrue(table.getAttrTableModel().getRowCount() > 1);

    canvas.getSelection().clearSelected();
    assertInstanceOf(AttrTableCircuitModel.class, table.getAttrTableModel());
  }

  @Test
  void createdAppearanceObjectShowsDrawingAttributes() {
    final var view = newAppearanceView();
    final var canvas = (AppearanceCanvas) view.getCanvas();
    final var table = new AttrTable(null);
    final var rectangleTool = new RectangleTool(new DrawingAttributeSet());

    view.getAttrTableDrawManager(table).attributesSelected();
    canvas.setTool(rectangleTool);
    assertTrue(table.getAttrTableModel().getRowCount() > 1);

    canvas.toolGestureComplete(rectangleTool, rectangleTool.createShape(0, 0, 10, 10));
    assertFalse(table.getAttrTableModel() instanceof AttrTableCircuitModel);
    assertTrue(table.getAttrTableModel().getRowCount() > 1);
  }

  @Test
  void middleButtonDragPansAppearanceCanvas() {
    final var view = newAppearanceView();
    final var canvas = (AppearanceCanvas) view.getCanvas();
    final var pane = view.getCanvasPane();

    canvas.setPreferredSize(new Dimension(1000, 1000));
    pane.setSize(200, 200);
    pane.doLayout();
    pane.getHorizontalScrollBar().setValues(80, 20, 0, 1000);
    pane.getVerticalScrollBar().setValues(90, 20, 0, 1000);

    canvas.processMouseEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, 40, 40, MouseEvent.BUTTON2));
    canvas.processMouseMotionEvent(
        mouse(canvas, MouseEvent.MOUSE_DRAGGED, 10, 20, MouseEvent.BUTTON2));

    assertEquals(110, pane.getHorizontalScrollBar().getValue());
    assertEquals(110, pane.getVerticalScrollBar().getValue());
  }

  private static AppearanceView newAppearanceView() {
    final var project = mock(Project.class);
    final var circuitState = mock(CircuitState.class);
    final var circuit = mock(Circuit.class);
    final var appearance = mock(CircuitAppearance.class);
    final var logisimFile = mock(LogisimFile.class);
    final var view = new AppearanceView();

    when(project.getLogisimFile()).thenReturn(logisimFile);
    when(logisimFile.contains(circuit)).thenReturn(true);
    when(circuitState.getCircuit()).thenReturn(circuit);
    when(circuit.getAppearance()).thenReturn(appearance);
    when(circuit.getName()).thenReturn("main");
    when(circuit.getStaticAttributes()).thenReturn(attributeSet("circuit"));
    when(appearance.getCustomAppearanceDrawing()).thenReturn(new Drawing());

    view.setCircuit(project, circuitState);
    return view;
  }

  private static AttributeSet attributeSet(String label) {
    return AttributeSets.fixedSet(new Attribute<?>[] {StdAttr.LABEL}, new Object[] {label});
  }

  private static MouseEvent mouse(
      AppearanceCanvas canvas, int id, int x, int y, int button) {
    final var modifiers =
        button == MouseEvent.BUTTON2 ? MouseEvent.BUTTON2_DOWN_MASK : MouseEvent.BUTTON1_DOWN_MASK;
    return new MouseEvent(canvas, id, 0, modifiers, x, y, x, y, 1, false, button);
  }
}
