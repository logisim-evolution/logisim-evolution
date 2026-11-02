/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.io.ButtonKeyDispatcher;
import com.cburch.logisim.tools.PokeTool;
import com.cburch.logisim.tools.Tool;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import javax.swing.SwingUtilities;

/**
 * Delivers button trigger keys only while the Poke tool is selected and the layout canvas has
 * focus, as agreed in issue 961.
 */
final class ButtonKeyRouter implements KeyEventDispatcher, ProjectListener, AutoCloseable {
  private final Frame frame;
  private final Map<String, ButtonKeyDispatcher.HeldPress> keyboardHeld = new HashMap<>();
  private final PropertyChangeListener focusListener = event -> releaseIfContextLost();
  private boolean closed;

  ButtonKeyRouter(Frame frame) {
    this.frame = frame;
    final var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(this);
    manager.addPropertyChangeListener("focusOwner", focusListener);
    manager.addPropertyChangeListener("focusedWindow", focusListener);
    frame.getCanvas().getProject().addProjectListener(this);
  }

  @Override
  public void close() {
    if (closed) return;
    closed = true;
    final var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.removeKeyEventDispatcher(this);
    manager.removePropertyChangeListener("focusOwner", focusListener);
    manager.removePropertyChangeListener("focusedWindow", focusListener);
    frame.getCanvas().getProject().removeProjectListener(this);
    releaseHeld();
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    if (closed) return;
    final var action = event.getAction();
    if (action == ProjectEvent.ACTION_SET_CURRENT) {
      releaseHeld();
    } else if (action == ProjectEvent.ACTION_SET_TOOL
        && !allowsTool(frame.getCanvas().getProject().getTool())) {
      releaseHeld();
    }
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (closed || !eventIsInThisFrame()) return false;
    if (!allowsCurrentContext()) {
      if (event.getID() == KeyEvent.KEY_RELEASED) releaseHeld();
      return false;
    }
    final var canvas = frame.getCanvas();
    final var handled =
        switch (event.getID()) {
          case KeyEvent.KEY_PRESSED ->
              ButtonKeyDispatcher.dispatch(
                  canvas.getCircuit(), canvas.getCircuitState(), event, true, keyboardHeld);
          case KeyEvent.KEY_RELEASED ->
              ButtonKeyDispatcher.dispatch(
                  canvas.getCircuit(), canvas.getCircuitState(), event, false, keyboardHeld);
          case KeyEvent.KEY_TYPED ->
              ButtonKeyDispatcher.consumeTyped(canvas.getCircuit(), event);
          default -> false;
        };
    if (handled) canvas.getProject().repaintCanvas();
    return handled;
  }

  static boolean allowsTool(Tool tool) {
    return tool instanceof PokeTool;
  }

  static boolean allowsFocus(java.awt.Component focus, java.awt.Component canvas) {
    if (focus == null || canvas == null) return false;
    return focus == canvas || SwingUtilities.isDescendingFrom(focus, canvas);
  }

  private boolean allowsCurrentContext() {
    if (!Frame.EDIT_LAYOUT.equals(frame.getEditorView())) return false;
    final var canvas = frame.getCanvas();
    if (canvas.getCircuit() == null || !allowsTool(canvas.getProject().getTool())) return false;
    final var focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return allowsFocus(focus, canvas);
  }

  private boolean eventIsInThisFrame() {
    final var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    final var focus = manager.getFocusOwner();
    if (focus != null) return SwingUtilities.getWindowAncestor(focus) == frame;
    return manager.getFocusedWindow() == frame;
  }

  private void releaseIfContextLost() {
    if (closed || keyboardHeld.isEmpty()) return;
    if (!eventIsInThisFrame() || !allowsCurrentContext()) releaseHeld();
  }

  private void releaseHeld() {
    if (keyboardHeld.isEmpty()) return;
    ButtonKeyDispatcher.releaseHeld(keyboardHeld);
    frame.getCanvas().getProject().repaintCanvas();
  }
}
