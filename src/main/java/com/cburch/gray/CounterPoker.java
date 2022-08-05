/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.gray;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

/**
 * When the user clicks a counter using the Poke Tool, a CounterPoker object is created, and that
 * object will handle all user events. Note that CounterPoker is a class specific to GrayCounter,
 * and that it must be a subclass of InstancePoker in the com.cburch.logisim.instance package.
 */
public class CounterPoker extends InstancePoker {
  public CounterPoker() {
    // dummy
  }

  /** Determines whether the location the mouse was pressed should result in initiating a poke. */
  @Override
  public boolean init(InstanceState state, MouseEvent e) {
    return state.getInstance().getBounds().contains(e.getX(), e.getY());
    // Anywhere in the main rectangle initiates the poke. The user might
    // have clicked within a label, but that will be outside the bounds.
  }

  /** Processes a key by just adding it onto the end of the current value. */
  @Override
  public void keyTyped(InstanceState state, KeyEvent e) {
    // convert it to a hex digit; if it isn't a hex digit, abort.
    final var val = Character.digit(e.getKeyChar(), 16);
    final var width = state.getAttributeValue(StdAttr.WIDTH);
    if (val < 0 || (val & width.getMask()) != val) return;

    // compute the next value
    final var cur = CounterData.get(state, width);
    final var newVal = (cur.getValue().toLongValue() * 16 + val) & width.getMask();
    final var newValue = Value.createKnown(width, newVal);
    cur.setValue(newValue);
    state.fireInvalidated();

    // You might be tempted to propagate the value immediately here, using
    // state.setPort. However, the circuit may currently be propagating in
    // another thread, and invoking setPort directly could interfere with
    // that. Using fireInvalidated notifies the propagation thread to
    // invoke propagate on the counter at its next opportunity.
  }

  /**
   * Draws an indicator that the caret is being selected. Here, we'll draw a red rectangle around
   * the value.
   */
  @Override
  public void paint(InstancePainter painter) {
    final var bds = painter.getBounds();
    final var len = (painter.getAttributeValue(StdAttr.WIDTH).getWidth() + 3) / 4;

    final var gfx = painter.getGraphics();
    gfx.setColor(Color.RED);
    final var width = 7 * len + 2; // width of caret rectangle
    final var height = 16; // height of caret rectangle
    gfx.drawRect(
        bds.getX() + (bds.getWidth() - width) / 2,
        bds.getY() + (bds.getHeight() - height) / 2,
        width,
        height);
    gfx.setColor(Color.BLACK);
  }
}
