/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import com.cburch.logisim.comp.Component;
import com.cburch.logisim.std.io.ButtonKeyDispatcher;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.TextTool;
import com.cburch.logisim.tools.Tool;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeListener;
import java.util.HashSet;
import java.util.Set;
import javax.swing.JComboBox;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.text.JTextComponent;

/**
 * Delivers button trigger keys while the circuit layout is showing.
 *
 * <p>The properties table keeps focus after a trigger key is chosen, and the pointer tool, not the
 * Poke tool, is usually selected. Listening only to the canvas KeyListener misses that case.
 */
final class ButtonKeyRouter implements KeyEventDispatcher, AutoCloseable {
  private final Frame frame;
  private final Set<Component> keyboardHeld = new HashSet<>();
  private final PropertyChangeListener focusListener = event -> releaseIfContextLost();
  private boolean closed;

  ButtonKeyRouter(Frame frame) {
    this.frame = frame;
    final var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.addKeyEventDispatcher(this);
    manager.addPropertyChangeListener("focusOwner", focusListener);
    manager.addPropertyChangeListener("focusedWindow", focusListener);
  }

  @Override
  public void close() {
    if (closed) return;
    closed = true;
    final var manager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
    manager.removeKeyEventDispatcher(this);
    manager.removePropertyChangeListener("focusOwner", focusListener);
    manager.removePropertyChangeListener("focusedWindow", focusListener);
    releaseHeld();
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (closed || !eventIsInThisFrame()) return false;
    if (!allowsCurrentContext()) {
      if (event.getID() == KeyEvent.KEY_RELEASED) releaseHeld();
      return false;
    }
    final var canvas = frame.getCanvas();
    final var circuit = canvas.getCircuit();
    final var state = canvas.getCircuitState();
    final var handled =
        switch (event.getID()) {
          case KeyEvent.KEY_PRESSED ->
              ButtonKeyDispatcher.dispatch(circuit, state, event, true, keyboardHeld);
          case KeyEvent.KEY_RELEASED ->
              ButtonKeyDispatcher.dispatch(circuit, state, event, false, keyboardHeld);
          case KeyEvent.KEY_TYPED -> ButtonKeyDispatcher.consumeTyped(circuit, event);
          default -> false;
        };
    if (handled) canvas.getProject().repaintCanvas();
    return handled;
  }

  static boolean allowsTool(Tool tool) {
    return tool != null && !(tool instanceof TextTool) && !(tool instanceof AddTool);
  }

  static boolean allowsFocus(java.awt.Component focus) {
    if (focus instanceof JTextComponent) return false;
    if (focus != null && SwingUtilities.getAncestorOfClass(JPopupMenu.class, focus) != null) {
      return false;
    }
    return !(focus instanceof JComboBox<?> combo && combo.isPopupVisible());
  }

  private boolean allowsCurrentContext() {
    if (!Frame.EDIT_LAYOUT.equals(frame.getEditorView())) return false;
    final var canvas = frame.getCanvas();
    if (canvas.getCircuit() == null || !allowsTool(canvas.getProject().getTool())) return false;
    final var focus = KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusOwner();
    return allowsFocus(focus);
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
    final var canvas = frame.getCanvas();
    ButtonKeyDispatcher.releaseHeld(canvas.getCircuitState(), keyboardHeld);
    canvas.getProject().repaintCanvas();
  }
}
