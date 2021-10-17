/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.gray;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

/**
 * Manufactures a simple counter that iterates over the 4-bit Gray Code. This example illustrates
 * how a component can maintain its own internal state. All of the code relevant to state, though,
 * appears in CounterData class.
 */
class SimpleGrayCounter extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Gray Counter (Simple)";

  private static final BitWidth BIT_WIDTH = BitWidth.create(4);

  // Again, notice how we don't have any instance variables related to an
  // individual instance's state. We can't put that here, because only one
  // SimpleGrayCounter object is ever created, and its job is to manage all
  // instances that appear in any circuits.

  public SimpleGrayCounter() {
    super(_ID);
    setOffsetBounds(Bounds.create(-30, -15, 30, 30));
    setPorts(
        new Port[] {
          new Port(-30, 0, Port.INPUT, 1), new Port(0, 0, Port.OUTPUT, BIT_WIDTH.getWidth()),
        });
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    painter.drawBounds();
    painter.drawClock(0, Direction.EAST); // draw a triangle on port 0
    painter.drawPort(1); // draw port 1 as just a dot

    // Display the current counter value centered within the rectangle.
    // However, if the context says not to show state (as when generating
    // printer output), then skip this.
    if (painter.getShowState()) {
      final var state = CounterData.get(painter, BIT_WIDTH);
      final var bds = painter.getBounds();
      GraphicsUtil.drawCenteredText(
          painter.getGraphics(),
          StringUtil.toHexString(BIT_WIDTH.getWidth(), state.getValue().toLongValue()),
          bds.getX() + bds.getWidth() / 2,
          bds.getY() + bds.getHeight() / 2);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    // Here I retrieve the state associated with this component via a helper
    // method. In this case, the state is in a CounterData object, which is
    // also where the helper method is defined. This helper method will end
    // up creating a CounterData object if one doesn't already exist.
    final var cur = CounterData.get(state, BIT_WIDTH);

    final var trigger = cur.updateClock(state.getPortValue(0));
    if (trigger) cur.setValue(GrayIncrementer.nextGray(cur.getValue()));
    state.setPort(1, cur.getValue(), 9);

    // (You might be tempted to determine the counter's current value
    // via state.getPortValue(1). This is erroneous, though, because another
    // component may be pushing a value onto the same point, which would
    // "corrupt" the value found there. We really do need to store the
    // current value in the instance.)
  }
}
