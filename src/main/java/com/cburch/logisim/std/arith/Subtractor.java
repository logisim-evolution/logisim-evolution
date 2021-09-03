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
import com.cburch.logisim.data.Location;
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
import java.awt.Graphics;

public class Subtractor extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Subtractor";

  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int OUT = 2;
  private static final int B_IN = 3;
  private static final int B_OUT = 4;

  public Subtractor() {
    super(_ID, S.getter("subtractorComponent"));
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("-"));

    Port[] ps = new Port[5];
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
    StringBuilder CompleteName = new StringBuilder();
    if (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) CompleteName.append("FullSubtractor");
    else CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new SubtractorHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    painter.drawBounds();

    g.setColor(Color.GRAY);
    painter.drawPort(IN0);
    painter.drawPort(IN1);
    painter.drawPort(OUT);
    painter.drawPort(B_IN, "b in", Direction.NORTH);
    painter.drawPort(B_OUT, "b out", Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    g.drawLine(x - 15, y, x - 5, y);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth data = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value b_in = state.getPortValue(B_IN);
    if (b_in == Value.UNKNOWN || b_in == Value.NIL) b_in = Value.FALSE;
    Value[] outs = Adder.computeSum(data, a, b.not(), b_in.not());

    // propagate them
    int delay = (data.getWidth() + 4) * Adder.PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(B_OUT, outs[1].not(), delay);
  }
}
