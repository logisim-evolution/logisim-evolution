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

public class Adder extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Adder";

  static Value[] computeSum(BitWidth width, Value a, Value b, Value c_in) {
    int w = width.getWidth();
    if (c_in == Value.UNKNOWN || c_in == Value.NIL) c_in = Value.FALSE;
    if (a.isFullyDefined() && b.isFullyDefined() && c_in.isFullyDefined()) {
      if (w == 64) {
        long ax = a.toLongValue();
        long bx = b.toLongValue();
        long cx = c_in.toLongValue();
        long mask = ~(1L << 63);
        boolean a_last = (ax < 0);
        boolean b_last = (bx < 0);
        boolean cin_last = (((ax & mask) + (bx & mask) + cx) < 0);
        boolean cout = (a_last && b_last) || (a_last && cin_last) || (b_last && cin_last);
        long sum = a.toLongValue() + b.toLongValue() + c_in.toLongValue();
        return new Value[] {
          Value.createKnown(width, sum), cout ? Value.TRUE : Value.FALSE
        };
      } else {
        long sum = a.toLongValue() + b.toLongValue() + c_in.toLongValue();
        return new Value[] {
          Value.createKnown(width, sum), ((sum >> w) & 1) == 0 ? Value.FALSE : Value.TRUE
        };
      }
    } else {
      Value[] bits = new Value[w];
      Value carry = c_in;
      for (int i = 0; i < w; i++) {
        if (carry == Value.ERROR) {
          bits[i] = Value.ERROR;
        } else if (carry == Value.UNKNOWN) {
          bits[i] = Value.UNKNOWN;
        } else {
          Value ab = a.get(i);
          Value bb = b.get(i);
          if (ab == Value.ERROR || bb == Value.ERROR) {
            bits[i] = Value.ERROR;
            carry = Value.ERROR;
          } else if (ab == Value.UNKNOWN || bb == Value.UNKNOWN) {
            bits[i] = Value.UNKNOWN;
            carry = Value.UNKNOWN;
          } else {
            int sum =
                (ab == Value.TRUE ? 1 : 0)
                    + (bb == Value.TRUE ? 1 : 0)
                    + (carry == Value.TRUE ? 1 : 0);
            bits[i] = (sum & 1) == 1 ? Value.TRUE : Value.FALSE;
            carry = (sum >= 2) ? Value.TRUE : Value.FALSE;
          }
        }
      }
      return new Value[] {Value.create(bits), carry};
    }
  }

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int IN1 = 1;
  private static final int OUT = 2;
  private static final int C_IN = 3;

  private static final int C_OUT = 4;

  public Adder() {
    super(_ID, S.getter("adderComponent"));
    setAttributes(new Attribute[] {StdAttr.WIDTH}, new Object[] {BitWidth.create(8)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("+"));

    Port[] ps = new Port[5];
    ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.WIDTH);
    ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.WIDTH);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ps[C_IN] = new Port(-20, -20, Port.INPUT, 1);
    ps[C_OUT] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("adderInputTip"));
    ps[IN1].setToolTip(S.getter("adderInputTip"));
    ps[OUT].setToolTip(S.getter("adderOutputTip"));
    ps[C_IN].setToolTip(S.getter("adderCarryInTip"));
    ps[C_OUT].setToolTip(S.getter("adderCarryOutTip"));
    setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuilder CompleteName = new StringBuilder();
    if (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) CompleteName.append("FullAdder");
    else CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
    return CompleteName.toString();
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new AdderHDLGeneratorFactory();
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
    painter.drawPort(C_IN, "c in", Direction.NORTH);
    painter.drawPort(C_OUT, "c out", Direction.SOUTH);

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    g.drawLine(x - 15, y, x - 5, y);
    g.drawLine(x - 10, y - 5, x - 10, y + 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);

    // compute outputs
    Value a = state.getPortValue(IN0);
    Value b = state.getPortValue(IN1);
    Value c_in = state.getPortValue(C_IN);
    Value[] outs = Adder.computeSum(dataWidth, a, b, c_in);

    // propagate them
    int delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, outs[0], delay);
    state.setPort(C_OUT, outs[1], delay);
  }
}
