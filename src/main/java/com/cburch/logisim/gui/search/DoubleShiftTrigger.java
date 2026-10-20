/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.search;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.AWTEvent;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Toolkit;
import java.awt.event.AWTEventListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.SwingUtilities;

/**
 * Opens the omni-search when Shift is tapped twice in quick succession.
 *
 * <p>Shift is a working modifier throughout Logisim-evolution — it constrains wire drawing, extends
 * canvas selections and modifies gate placement — so the gesture is recognised only when both taps
 * are "clean": Shift pressed and released with no other key, no other modifier and no mouse button
 * in between. Anything else disarms the detector, which is what keeps Shift-dragging on the canvas
 * from popping the dialog open. The whole thing can still be switched off through
 * {@link AppPreferences#SEARCH_DOUBLE_SHIFT}.
 *
 * <p>Key events are only observed, never consumed, so this cannot interfere with any existing
 * binding.
 */
public final class DoubleShiftTrigger implements KeyEventDispatcher, AWTEventListener {

  /** Longest gap between the two taps that still counts as a double tap, in milliseconds. */
  private static final int DOUBLE_TAP_WINDOW_MS = 300;

  /** Modifiers that, held alongside Shift, mean the user is reaching for something else. */
  private static final int DISQUALIFYING_MODIFIERS =
      InputEvent.CTRL_DOWN_MASK
          | InputEvent.ALT_DOWN_MASK
          | InputEvent.ALT_GRAPH_DOWN_MASK
          | InputEvent.META_DOWN_MASK;

  private static DoubleShiftTrigger instance;

  private boolean shiftDown;
  private boolean shiftTainted;
  private boolean armed;
  private long lastTapAt;

  private DoubleShiftTrigger() {
    // Use install() instead.
  }

  /** Starts listening for the gesture. Safe to call more than once; only the first call acts. */
  public static synchronized void install() {
    if (instance != null) return;
    instance = new DoubleShiftTrigger();
    KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(instance);
    Toolkit.getDefaultToolkit().addAWTEventListener(instance, AWTEvent.MOUSE_EVENT_MASK);
  }

  @Override
  public boolean dispatchKeyEvent(KeyEvent event) {
    if (event.getKeyCode() == KeyEvent.VK_SHIFT) {
      handleShiftEvent(event);
    } else if (event.getID() == KeyEvent.KEY_PRESSED) {
      // Shift is being used as a modifier, not tapped on its own.
      shiftTainted = true;
      armed = false;
    }
    // Never consume: this is an observer, not a binding.
    return false;
  }

  private void handleShiftEvent(KeyEvent event) {
    switch (event.getID()) {
      case KeyEvent.KEY_PRESSED -> {
        // Auto-repeat while Shift is held re-enters here, so only the first press starts a tap.
        if (!shiftDown) {
          shiftDown = true;
          shiftTainted = false;
        }
        if ((event.getModifiersEx() & DISQUALIFYING_MODIFIERS) != 0) shiftTainted = true;
      }
      case KeyEvent.KEY_RELEASED -> {
        final var clean = shiftDown && !shiftTainted;
        shiftDown = false;
        shiftTainted = false;
        if (!clean) {
          armed = false;
          return;
        }
        final var now = event.getWhen();
        if (armed && now - lastTapAt <= DOUBLE_TAP_WINDOW_MS) {
          armed = false;
          fire();
        } else {
          armed = true;
          lastTapAt = now;
        }
      }
      default -> {
        // KEY_TYPED is not produced for Shift; nothing to do.
      }
    }
  }

  @Override
  public void eventDispatched(AWTEvent event) {
    // A click or a drag means Shift was a modifier for the mouse, not a tap of its own.
    if (event.getID() == MouseEvent.MOUSE_PRESSED) {
      shiftTainted = true;
      armed = false;
    }
  }

  private void fire() {
    if (!AppPreferences.SEARCH_DOUBLE_SHIFT.getBoolean()) return;
    // Let the key event finish dispatching before opening a modal dialog on top of it.
    SwingUtilities.invokeLater(
        () ->
            OmniSearchDialog.showForWindow(
                KeyboardFocusManager.getCurrentKeyboardFocusManager().getFocusedWindow()));
  }
}
