/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.main.Canvas;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.main.Selection;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import javax.swing.JPanel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class SelectToolTest {
  private static final Location TEXT_LOC = Location.create(33, 47, false);

  private static final int PRESS_X = 40;
  private static final int PRESS_Y = 40;

  private final Canvas canvas = mock(Canvas.class);
  private final Project proj = mock(Project.class);
  private final Selection sel = mock(Selection.class);
  private final Component text = mock(Component.class);
  private final ComponentDrawContext context = mock(ComponentDrawContext.class);
  private final Set<Component> selected = new HashSet<>();
  private final SelectTool tool = new SelectTool();

  @BeforeEach
  void setUp() {
    final var circuit = mock(Circuit.class);
    final var frame = mock(Frame.class);
    final var file = mock(LogisimFile.class);
    when(canvas.getProject()).thenReturn(proj);
    when(canvas.getCircuit()).thenReturn(circuit);
    when(proj.getSelection()).thenReturn(sel);
    when(proj.getFrame()).thenReturn(frame);
    when(frame.getCanvas()).thenReturn(canvas);
    when(proj.getLogisimFile()).thenReturn(file);
    when(file.contains(any(Circuit.class))).thenReturn(true);

    when(text.getLocation()).thenReturn(TEXT_LOC);
    when(circuit.getAllContaining(any(), any())).thenReturn(List.of(text));
    when(sel.getComponents()).thenAnswer(invocation -> selected);
    doAnswer(invocation -> selected.add(invocation.getArgument(0))).when(sel).add(any());
    when(sel.getComponentsContaining(any(), any()))
        .thenAnswer(invocation -> selected.contains(text) ? List.of(text) : List.of());
    when(sel.getBounds(any())).thenReturn(Bounds.create(20, 30, 30, 20));
  }

  @Test
  void ctrlDragSnapsSelectedTextToGrid() {
    selected.add(text);
    press();
    drag(44, 42, InputEvent.CTRL_DOWN_MASK);
    assertGhostShiftedBy(7, 3);

    release(44, 42, InputEvent.CTRL_DOWN_MASK);
    verify(sel).hasConflictWhenMoved(7, 3);
  }

  @Test
  void ctrlDragSnapsTextClickedOutsideSelection() {
    press();
    drag(44, 42, InputEvent.CTRL_DOWN_MASK);
    assertGhostShiftedBy(7, 3);
  }

  @Test
  void dragWithoutCtrlMovesTextFreely() {
    selected.add(text);
    press();
    drag(44, 42, 0);
    assertGhostShiftedBy(4, 2);

    release(44, 42, 0);
    verify(sel).hasConflictWhenMoved(4, 2);
  }

  @Test
  void ctrlDoesNotChangeSelectionsThatAlreadySnap() {
    when(sel.shouldSnap()).thenReturn(true);
    selected.add(text);
    press();
    drag(47, 54, InputEvent.CTRL_DOWN_MASK);
    assertGhostShiftedBy(10, 10);
  }

  @Test
  void pressingAndReleasingCtrlMidDragUpdatesWithoutMovingMouse() {
    selected.add(text);
    press();
    drag(44, 42, 0);
    assertGhostShiftedBy(4, 2);

    tool.keyPressed(canvas, key(KeyEvent.KEY_PRESSED, InputEvent.CTRL_DOWN_MASK));
    tool.draw(canvas, context);
    verify(sel).drawGhostsShifted(context, 7, 3);

    tool.keyReleased(canvas, key(KeyEvent.KEY_RELEASED, 0));
    tool.draw(canvas, context);
    verify(sel, times(2)).drawGhostsShifted(context, 4, 2);
  }

  private void assertGhostShiftedBy(int dx, int dy) {
    tool.draw(canvas, context);
    verify(sel).drawGhostsShifted(context, dx, dy);
  }

  private void press() {
    tool.mousePressed(canvas, null, mouse(MouseEvent.MOUSE_PRESSED, PRESS_X, PRESS_Y, 0));
  }

  private void drag(int x, int y, int mods) {
    tool.mouseDragged(canvas, null, mouse(MouseEvent.MOUSE_DRAGGED, x, y, mods | noConnect()));
  }

  private void release(int x, int y, int mods) {
    tool.mouseReleased(canvas, null, mouse(MouseEvent.MOUSE_RELEASED, x, y, mods | noConnect()));
  }

  private static int noConnect() {
    return AppPreferences.MOVE_KEEP_CONNECT.getBoolean() ? InputEvent.SHIFT_DOWN_MASK : 0;
  }

  private static MouseEvent mouse(int id, int x, int y, int mods) {
    final var buttons = id == MouseEvent.MOUSE_RELEASED ? 0 : InputEvent.BUTTON1_DOWN_MASK;
    return new MouseEvent(
        new JPanel(), id, 0, mods | buttons, x, y, 1, false, MouseEvent.BUTTON1);
  }

  private static KeyEvent key(int id, int mods) {
    return new KeyEvent(
        new JPanel(), id, 0, mods | noConnect(), KeyEvent.VK_CONTROL, KeyEvent.CHAR_UNDEFINED);
  }
}
