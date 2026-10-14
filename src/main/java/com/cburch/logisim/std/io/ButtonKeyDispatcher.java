/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.Set;

/** Routes letter and digit keys to buttons in the circuit currently on the canvas. */
public final class ButtonKeyDispatcher {
  private ButtonKeyDispatcher() {}

  /**
   * Presses or releases the single button mapped to this key.
   *
   * @param keyboardHeld buttons currently held by the keyboard; mouse-only presses stay out of it
   * @return true when the event is a mapped key and must not reach the poke caret
   */
  public static boolean dispatch(
      Circuit circuit,
      CircuitState circuitState,
      KeyEvent event,
      boolean pressed,
      Set<Component> keyboardHeld) {
    final var button = mappedButton(circuit, event, true);
    if (button == null || circuitState == null) return false;
    final var state = circuitState.getInstanceState(button);
    if (pressed) {
      Button.setPressed(state, true);
      keyboardHeld.add(button);
    } else if (keyboardHeld.remove(button)) {
      Button.setPressed(state, false);
    }
    event.consume();
    return true;
  }

  /** Consumes a typed character that belongs to a mapped key, without changing button state. */
  public static boolean consumeTyped(Circuit circuit, KeyEvent event) {
    final var button = mappedButton(circuit, event, false);
    if (button == null) return false;
    event.consume();
    return true;
  }

  /** Releases buttons held from the keyboard. Buttons held only by the mouse are left pressed. */
  public static void releaseHeld(CircuitState circuitState, Set<Component> keyboardHeld) {
    if (circuitState != null) {
      for (final var button : keyboardHeld) {
        if (button.getFactory() instanceof Button) {
          Button.setPressed(circuitState.getInstanceState(button), false);
        }
      }
    }
    keyboardHeld.clear();
  }

  private static Component mappedButton(Circuit circuit, KeyEvent event, boolean fromKeyCode) {
    if (circuit == null || event == null || isCommandChord(event)) return null;
    final var token =
        fromKeyCode
            ? Button.tokenForKeyCode(event.getKeyCode())
            : Button.tokenForKeyChar(event.getKeyChar());
    if (token == null) return null;
    Component match = null;
    for (final var comp : circuit.getNonWires()) {
      if (!(comp.getFactory() instanceof Button)) continue;
      final var key = comp.getAttributeSet().getValue(Button.ATTR_KEY);
      if (key == null || !token.equals(key.toString())) continue;
      if (match != null) return null;
      match = comp;
    }
    return match;
  }

  private static boolean isCommandChord(KeyEvent event) {
    final var modifiers = event.getModifiersEx();
    return (modifiers
            & (InputEvent.CTRL_DOWN_MASK | InputEvent.ALT_DOWN_MASK | InputEvent.META_DOWN_MASK))
        != 0;
  }
}
