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
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import javax.swing.SwingUtilities;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

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
          final var pane = view.getCanvasPane();

          canvas.setPreferredSize(new Dimension(1000, 1000));
          pane.setSize(200, 200);
          pane.getViewport().setViewSize(canvas.getPreferredSize());
          pane.doLayout();
          pane.getHorizontalScrollBar().setValues(80, 200, 0, 1000);
          pane.getVerticalScrollBar().setValues(90, 200, 0, 1000);
          final var initialX = pane.getHorizontalScrollBar().getValue();
          final var initialY = pane.getVerticalScrollBar().getValue();

          canvas.processMouseEvent(
              mouse(canvas, MouseEvent.MOUSE_PRESSED, 40, 40, MouseEvent.BUTTON2));
          canvas.processMouseMotionEvent(
              mouse(canvas, MouseEvent.MOUSE_DRAGGED, 10, 20, MouseEvent.NOBUTTON, 0));

          assertTrue(pane.getHorizontalScrollBar().getValue() > initialX);
          assertTrue(pane.getVerticalScrollBar().getValue() > initialY);
        });
  }

  @ParameterizedTest
  @CsvSource({
    "1000, 5000, 350, 1800, -1, false",
    "1000, 5000, 350, 1800, 1, false",
    "5000, 1000, 2200, 350, -1, false",
    "5000, 1000, 2200, 350, 1, false",
    "5000, 5000, 2200, 1800, -1, false",
    "5000, 5000, 2200, 1800, 1, false",
    "5000, 5000, 7600, 7600, -1, false",
    "5000, 5000, 7600, 7600, 1, false",
    "1000, 5000, 350, 1800, -1, true",
    "1000, 5000, 350, 1800, 1, true"
  })
  void mouseZoomKeepsScrolledAppearancePointUnderCursor(
      int width, int height, int scrollX, int scrollY, int rotation, boolean fromCanvas)
      throws Exception {
    SwingUtilities.invokeAndWait(
        () -> {
          final var view = newAppearanceView(Bounds.create(0, 0, width, height));
          final var canvas = (AppearanceCanvas) view.getCanvas();
          final var pane = view.getCanvasPane();
          final var viewport = pane.getViewport();
          view.getZoomModel().setZoomFactor(1.5);
          pane.setSize(400, 300);
          pane.doLayout();
          canvas.recomputeSize();
          viewport.doLayout();
          viewport.setViewPosition(new Point(scrollX, scrollY));

          final var cursor = new Point(80, 140);
          final var before = viewport.getViewPosition();
          final var pointX = (before.x + cursor.x) / view.getZoomModel().getZoomFactor();
          final var pointY = (before.y + cursor.y) / view.getZoomModel().getZoomFactor();
          final var source = fromCanvas ? canvas : pane;
          final var eventPoint = SwingUtilities.convertPoint(viewport, cursor, source);
          final var event =
              new MouseWheelEvent(
                  source,
                  MouseEvent.MOUSE_WHEEL,
                  0,
                  InputEvent.CTRL_DOWN_MASK,
                  eventPoint.x,
                  eventPoint.y,
                  0,
                  false,
                  MouseWheelEvent.WHEEL_UNIT_SCROLL,
                  1,
                  rotation);
          if (fromCanvas) {
            view.getZoomModel().setZoomFactor(1.5 - rotation * 0.1, event);
          } else {
            pane.dispatchEvent(event);
          }
          viewport.doLayout();

          final var zoom = view.getZoomModel().getZoomFactor();
          final var after = viewport.getViewPosition();
          assertEquals(1.5 - rotation * 0.1, zoom, 1.0e-6);
          assertEquals(pointX, (after.x + cursor.x) / zoom, 1.0 / zoom);
          assertEquals(pointY, (after.y + cursor.y) / zoom, 1.0 / zoom);
        });
  }

  @Test
  void appearanceCanvasLeavesViewportPaddingForMiddleButtonPanning() {
    final var view = newAppearanceView();
    final var canvas = (AppearanceCanvas) view.getCanvas();
    final var pane = view.getCanvasPane();

    pane.setSize(200, 200);
    pane.doLayout();
    canvas.recomputeSize();

    final var viewport = pane.getViewport().getExtentSize();
    final var preferred = canvas.getPreferredSize();
    assertTrue(preferred.width > viewport.width);
    assertTrue(preferred.height > viewport.height);
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
