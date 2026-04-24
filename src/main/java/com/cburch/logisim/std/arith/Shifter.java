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
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import java.awt.Color;
import java.awt.Graphics;
import java.math.BigInteger;
import java.util.Arrays;

public class Shifter extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Shifter";

  static final AttributeOption SHIFT_LOGICAL_LEFT =
      new AttributeOption("ll", S.getter("shiftLogicalLeft"));
  static final AttributeOption SHIFT_LOGICAL_RIGHT =
      new AttributeOption("lr", S.getter("shiftLogicalRight"));
  static final AttributeOption SHIFT_ARITHMETIC_RIGHT =
      new AttributeOption("ar", S.getter("shiftArithmeticRight"));
  static final AttributeOption SHIFT_ROLL_LEFT =
      new AttributeOption("rl", S.getter("shiftRollLeft"));
  static final AttributeOption SHIFT_ROLL_RIGHT =
      new AttributeOption("rr", S.getter("shiftRollRight"));
  static final Attribute<AttributeOption> ATTR_SHIFT =
      Attributes.forOption(
          "shift",
          S.getter("shifterShiftAttr"),
          new AttributeOption[] {
            SHIFT_LOGICAL_LEFT,
            SHIFT_LOGICAL_RIGHT,
            SHIFT_ARITHMETIC_RIGHT,
            SHIFT_ROLL_LEFT,
            SHIFT_ROLL_RIGHT
          });
  public static final Attribute<Integer> SHIFT_BITS_ATTR = Attributes.forNoSave();

  static final int IN0 = 0;
  static final int IN1 = 1;
  static final int OUT = 2;

  public Shifter() {
    super(_ID, S.getter("shifterComponent"), new ShifterHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {StdAttr.WIDTH, ATTR_SHIFT, SHIFT_BITS_ATTR},
        new Object[] {BitWidth.create(8), SHIFT_LOGICAL_LEFT, 4});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("\u2b05"));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }

  private void configurePorts(Instance instance) {
    BitWidth dataWid = instance.getAttributeValue(StdAttr.WIDTH);
    int data = dataWid == null ? 32 : dataWid.getWidth();
    int shift = 1;
    while ((1 << shift) < data) shift++;
    instance.getAttributeSet().setValue(SHIFT_BITS_ATTR, shift);

    Port[] ps = new Port[3];
    ps[IN0] = new Port(-40, -10, Port.INPUT, data);
    ps[IN1] = new Port(-40, 10, Port.INPUT, shift);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, data);
    ps[IN0].setToolTip(S.getter("shifterInputTip"));
    ps[IN1].setToolTip(S.getter("shifterDistanceTip"));
    ps[OUT].setToolTip(S.getter("shifterOutputTip"));
    instance.setPorts(ps);
  }

  private void drawArrow(Graphics g, int x, int y, int d) {
    int[] px = {x + d, x, x + d};
    int[] py = {y + d, y, y - d};
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.fillPolygon(px, py, 3);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return "Shifter_" + attrs.getValue(StdAttr.WIDTH).getWidth() + "_bit";
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.WIDTH) {
      configurePorts(instance);
    } else if (attr == ATTR_SHIFT) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPorts();
    Location loc = painter.getLocation();
    int x = loc.getX() - 15;
    int y = loc.getY();
    Object shift = painter.getAttributeValue(ATTR_SHIFT);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    if (shift == SHIFT_LOGICAL_RIGHT) {
      g.fillRect(x, y - 1, 8, 3);
      drawArrow(g, x + 10, y, -4);
    } else if (shift == SHIFT_ARITHMETIC_RIGHT) {
      g.fillRect(x, y - 1, 2, 3);
      g.fillRect(x + 3, y - 1, 5, 3);
      drawArrow(g, x + 10, y, -4);
    } else if (shift == SHIFT_ROLL_RIGHT) {
      g.fillRect(x, y - 1, 5, 3);
      g.fillRect(x + 8, y - 7, 2, 8);
      g.fillRect(x, y - 7, 2, 8);
      g.fillRect(x, y - 7, 10, 2);
      drawArrow(g, x + 8, y, -4);
    } else if (shift == SHIFT_ROLL_LEFT) {
      g.fillRect(x + 6, y - 1, 4, 3);
      g.fillRect(x + 8, y - 7, 2, 8);
      g.fillRect(x, y - 7, 2, 8);
      g.fillRect(x, y - 7, 10, 2);
      drawArrow(g, x + 3, y, 4);
    } else { // SHIFT_LOGICAL_LEFT
      g.fillRect(x + 2, y - 1, 8, 3);
      drawArrow(g, x, y, 4);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    // compute output
    BitWidth dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    int width = dataWidth == null ? 32 : dataWidth.getWidth();

    Value input = state.getPortValue(IN0);
    Value shift = state.getPortValue(IN1);
    Value output;

    if (shift.isFullyDefined() && input.getWidth() == width) {
      int s = (int) shift.toLongValue();
      var shiftType = state.getAttributeValue(ATTR_SHIFT);
      if (s == 0) {
        output = input;
      } else if (input.isFullyDefined()) {
        if(width <= 64) {
          long in = input.toLongValue();
          long out;
          if (shiftType == SHIFT_LOGICAL_RIGHT) {
            out = in >>> s;
          } else if (shiftType == SHIFT_ARITHMETIC_RIGHT) {
            if (s >= width) s = width - 1;
            out = in >> s | ((in << (64 - width)) >> (64 - width + s));
          } else if (shiftType == SHIFT_ROLL_RIGHT) {
            if (s >= width) s -= width;
            out = (in >>> s) | (in << (width - s));
          } else if (shiftType == SHIFT_ROLL_LEFT) {
            if (s >= width) s -= width;
            out = (in << s) | (in >>> (width - s));
          } else { // SHIFT_LOGICAL_LEFT
            out = in << s;
          }
          output = Value.createKnown(dataWidth, out);
        } else {
          BigInteger in = input.toBigInteger(true);
          BigInteger out;
          if (shiftType == SHIFT_LOGICAL_RIGHT) {
            out = in.shiftRight(s);
            if (in.signum() < 0) {
                BigInteger mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
                out = out.and(mask);
            }
          } else if (shiftType == SHIFT_ARITHMETIC_RIGHT) {
            if (s >= width) s = width - 1;
            out = in.shiftRight(s);
          } else if (shiftType == SHIFT_ROLL_RIGHT) {
            s %= width; // normalize shift
            BigInteger mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
            BigInteger right = in.shiftRight(s);
            BigInteger left = in.shiftLeft(width - s).and(mask);
            out = right.or(left);
          } else if (shiftType == SHIFT_ROLL_LEFT) {
            s %= width;
            BigInteger mask = BigInteger.ONE.shiftLeft(width).subtract(BigInteger.ONE);
            BigInteger right = in.shiftRight(width - s);
            BigInteger left = in.shiftLeft(s).and(mask);
            out = right.or(left);
          } else { // SHIFT_LOGICAL_LEFT
            out = in.shiftLeft(s);
          }
          output = Value.createKnown(dataWidth, out);
        }
      } else {
        Value[] in = input.getAll();
        Value[] out = new Value[width];
        if (shiftType == SHIFT_LOGICAL_RIGHT) {
          if (s >= width) s = width;
          System.arraycopy(in, s, out, 0, width - s);
          Arrays.fill(out, width - s, width, Value.FALSE);
        } else if (shiftType == SHIFT_ARITHMETIC_RIGHT) {
          if (s >= width) s = width;
          System.arraycopy(in, s, out, 0, in.length - s);
          Arrays.fill(out, width - s, out.length, in[width - 1]);
        } else if (shiftType == SHIFT_ROLL_RIGHT) {
          if (s >= width) s -= width;
          System.arraycopy(in, s, out, 0, width - s);
          System.arraycopy(in, 0, out, width - s, s);
        } else if (shiftType == SHIFT_ROLL_LEFT) {
          if (s >= width) s -= width;
          System.arraycopy(in, in.length - s, out, 0, s);
          System.arraycopy(in, 0, out, s, width - s);
        } else { // SHIFT_LOGICAL_LEFT
          if (s >= width) s = width;
          Arrays.fill(out, 0, s, Value.FALSE);
          System.arraycopy(in, 0, out, s, width - s);
        }
        output = Value.create(out);
      }
    } else {
      output = Value.createError(dataWidth);
    }

    // propagate them
    int delay = dataWidth.getWidth() * (3 * Adder.PER_DELAY);
    state.setPort(OUT, output, delay);
  }
}
