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
import java.util.Map;

/** Routes letter and digit keys to buttons in the circuit where the key was pressed. */
public final class ButtonKeyDispatcher {
  private ButtonKeyDispatcher() {}

  /** A keyboard hold, remembered with the circuit state that was current when the key went down. */
  public static final class HeldPress {
    private final Component component;
    private final CircuitState state;

    private HeldPress(Component component, CircuitState state) {
      this.component = component;
      this.state = state;
    }
  }

  /**
   * Presses or releases the single button mapped to this key.
   *
   * @param keyboardHeld token to the button and circuit state captured on press
   * @return true when the event is a mapped key and must not reach the poke caret
   */
  public static boolean dispatch(
      Circuit circuit,
      CircuitState circuitState,
      KeyEvent event,
      boolean pressed,
      Map<String, HeldPress> keyboardHeld) {
    final var token = Button.tokenFor(event);
    if (token == null || circuit == null || event == null) return false;
    if (pressed) {
      if (isCommandChord(event) || circuitState == null) return false;
      final var button = findButton(circuit, token);
      if (button == null) return false;
      Button.setKeyPressed(circuitState.getInstanceState(button), true);
      keyboardHeld.put(token, new HeldPress(button, circuitState));
    } else {
      final var held = keyboardHeld.remove(token);
      if (held == null) {
        if (isCommandChord(event) || findButton(circuit, token) == null) return false;
      } else {
        Button.setKeyPressed(held.state.getInstanceState(held.component), false);
      }
    }
    event.consume();
    return true;
  }

  /** Consumes a typed character that belongs to a mapped key, without changing button state. */
  public static boolean consumeTyped(Circuit circuit, KeyEvent event) {
    if (circuit == null || event == null || isCommandChord(event)) return false;
    final var token = Button.tokenFor(event);
    if (token == null || findButton(circuit, token) == null) return false;
    event.consume();
    return true;
  }

  /** Releases keyboard holds in the circuit states where they were pressed. */
  public static void releaseHeld(Map<String, HeldPress> keyboardHeld) {
    for (final var held : keyboardHeld.values()) {
      if (held.component.getFactory() instanceof Button) {
        Button.setKeyPressed(held.state.getInstanceState(held.component), false);
      }
    }
    keyboardHeld.clear();
  }

  private static Component findButton(Circuit circuit, String token) {
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
