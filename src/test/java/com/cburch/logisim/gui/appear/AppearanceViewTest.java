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
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.AttributeSets;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.AttrTable;
import com.cburch.logisim.gui.main.AttrTableCircuitModel;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
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
  void middleButtonDragPansAppearanceCanvas() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var view = newAppearanceView();
          final var canvas = (AppearanceCanvas) view.getCanvas();

          canvas.processMouseEvent(
              mouse(canvas, MouseEvent.MOUSE_PRESSED, 40, 40, MouseEvent.BUTTON2));
          canvas.processMouseMotionEvent(
              mouse(canvas, MouseEvent.MOUSE_DRAGGED, 10, 20, MouseEvent.NOBUTTON, 0));

          // The camera follows the cursor: dragging left/up moves the view origin negative.
          assertEquals(new Point(-30, -20), canvas.getViewOrigin());
        });
  }

  @Test
  void ctrlWheelZoomKeepsAppearancePointUnderCursor() throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var view = newAppearanceView(Bounds.create(0, 0, 5000, 5000));
          final var canvas = (AppearanceCanvas) view.getCanvas();
          final var pane = view.getCanvasPane();
          pane.setSize(400, 300);
          pane.doLayout();
          view.getZoomModel().setZoomFactor(1.5);

          final var cursor = new Point(80, 140);
          final var zoomBefore = view.getZoomModel().getZoomFactor();
          final var originBefore = canvas.getViewOrigin();
          final var modelX = (cursor.x - originBefore.x) / zoomBefore;
          final var modelY = (cursor.y - originBefore.y) / zoomBefore;

          final var event =
              new MouseWheelEvent(
                  canvas,
                  MouseEvent.MOUSE_WHEEL,
                  0,
                  InputEvent.CTRL_DOWN_MASK,
                  cursor.x,
                  cursor.y,
                  0,
                  false,
                  MouseWheelEvent.WHEEL_UNIT_SCROLL,
                  1,
                  -1);
          canvas.dispatchEvent(event);

          final var zoomAfter = view.getZoomModel().getZoomFactor();
          final var originAfter = canvas.getViewOrigin();
          assertTrue(zoomAfter > zoomBefore);
          assertEquals(modelX, (cursor.x - originAfter.x) / zoomAfter, 1.0 / zoomAfter);
          assertEquals(modelY, (cursor.y - originAfter.y) / zoomAfter, 1.0 / zoomAfter);
        });
  }

  @Test
  void appearanceCanvasTracksViewportForInfinitePanning() {
    final var view = newAppearanceView();
    final var canvas = (AppearanceCanvas) view.getCanvas();

    assertTrue(canvas.getScrollableTracksViewportWidth());
    assertTrue(canvas.getScrollableTracksViewportHeight());
  }

  @Test
  void middleButtonPanUsesMoveCursorUntilRelease() {
    final var view = newAppearanceView();
    final var canvas = (AppearanceCanvas) view.getCanvas();
    final var initialCursor = canvas.getCursor();

    canvas.processMouseEvent(mouse(canvas, MouseEvent.MOUSE_PRESSED, 40, 40, MouseEvent.BUTTON2));

    assertEquals(Cursor.MOVE_CURSOR, canvas.getCursor().getType());

    canvas.processMouseEvent(mouse(canvas, MouseEvent.MOUSE_RELEASED, 40, 40, MouseEvent.BUTTON2));

    assertEquals(initialCursor.getType(), canvas.getCursor().getType());
  }

  private static AppearanceView newAppearanceView() {
    return newAppearanceView(Bounds.create(0, 0, 50, 50));
  }

  private static AppearanceView newAppearanceView(Bounds bounds) {
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
    when(appearance.getAbsoluteBounds()).thenReturn(bounds);
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
    return mouse(canvas, id, x, y, button, modifiers);
  }

  private static MouseEvent mouse(
      AppearanceCanvas canvas, int id, int x, int y, int button, int modifiers) {
    return new MouseEvent(canvas, id, 0, modifiers, x, y, x, y, 1, false, button);
  }
}
