/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;

public class Subtractor extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Subtractor";

  public static final int IN0 = 0;
  public static final int IN1 = 1;
  public static final int OUT = 2;
  public static final int B_IN = 3;
  public static final int B_OUT = 4;

  public Subtractor() {
    super(_ID, S.getter("subtractorComponent"), new SubtractorHdlGeneratorFactory());
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("-"));

    final var ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[B_IN] = new Port(-20, -20, Port.INPUT, 1);
    ps[B_OUT] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("subtractorMinuendTip"));
    ps[IN1].setToolTip(S.getter("subtractorSubtrahendTip"));
    ps[OUT].setToolTip(S.getter("subtractorOutputTip"));
    ps[B_IN].setToolTip(S.getter("subtractorBorrowInTip"));
    ps[B_OUT].setToolTip(S.getter("subtractorBorrowOutTip"));
    setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var fullName = new StringBuilder();
    if (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) fullName.append("FullSubtractor");
    else fullName.append(CorrectLabel.getCorrectLabel(this.getName()));
    return fullName.toString();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var gfx = painter.getGraphics();
    painter.drawBounds();

    gfx.setColor(Color.GRAY);
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(B_IN, "b in", Direction.NORTH);
    painter.drawPort(B_OUT, "b out", Direction.SOUTH);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(gfx, 2);
    gfx.setColor(Color.BLACK);
    gfx.drawLine(x - 15, y, x - 5, y);
    GraphicsUtil.switchToWidth(gfx, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth data = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    final var a = state.getPortValue(IN0);
    final var b = state.getPortValue(IN1);
    var bIn = state.getPortValue(B_IN);
    if (bIn == Value.UNKNOWN || bIn == Value.NIL) bIn = Value.FALSE;
    final var outs = Adder.computeSum(data, a, b.not(), bIn.not());

    // propagate them
    final var delay = (data.getWidth() + 4) * Adder.PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(B_OUT, outs[1].not(), delay);
  }
}
