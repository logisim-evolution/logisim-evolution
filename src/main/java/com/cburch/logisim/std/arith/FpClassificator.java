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
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
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
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;

public class FpClassificator extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPClassificator";

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int NEGATIVE = 1;
  private static final int ZERO = 2;
  private static final int SUBNORMAL = 3;
  private static final int NORMAL = 4;
  private static final int INFINITE = 5;

//private static final int SIGNALING_NAN = 9;
  private static final int QUIET_NAN = 6;

  public FpClassificator() {
    super(_ID, S.getter("fpClassificatorComponent"));
    setAttributes(new Attribute[] {StdAttr.FP_WIDTH}, new Object[] {BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -40, 40, 80));
    setIcon(new ArithmeticIcon("\u2630 ",3));

    final var ps = new Port[7];
    ps[IN] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);

    ps[NEGATIVE] = new Port(0, -30, Port.OUTPUT, 1);
    ps[ZERO] = new Port(0, -20, Port.OUTPUT, 1);
    ps[SUBNORMAL] = new Port(0, -10, Port.OUTPUT, 1);
    ps[NORMAL] = new Port(0, 0, Port.OUTPUT, 1);
    ps[INFINITE] = new Port(0, 10, Port.OUTPUT, 1);
//  ps[SIGNALING_NAN] = new Port(0, 20, Port.OUTPUT, 1);
    ps[QUIET_NAN] = new Port(0, 30, Port.OUTPUT, 1);

    ps[IN].setToolTip(S.getter("fpClassificatorInputTip"));

    ps[NEGATIVE].setToolTip(S.getter("fpClassificatorNegativeTip"));
    ps[ZERO].setToolTip(S.getter("fpClassificatorZeroTip"));
    ps[SUBNORMAL].setToolTip(S.getter("fpClassificatorSubnormalTip"));
    ps[NORMAL].setToolTip(S.getter("fpClassificatorNormalTip"));
    ps[INFINITE].setToolTip(S.getter("fpClassificatorInfiniteTip"));
//  ps[SIGNALING_NAN].setToolTip(S.getter("fpClassificatorSignalingNaNTip"));
    ps[QUIET_NAN].setToolTip(S.getter("fpClassificatorNaNTip"));

    setPorts(ps);
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    instance.fireInvalidated();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPort(IN);
    painter.drawPort(NEGATIVE, "-", Direction.WEST);
    painter.drawPort(ZERO, "0", Direction.WEST);
    painter.drawPort(SUBNORMAL, "sn", Direction.WEST);
    painter.drawPort(NORMAL, "n", Direction.WEST);
    painter.drawPort(INFINITE, "\u221E", Direction.WEST);
//  painter.drawPort(SIGNALING_NAN, "sNaN", Direction.WEST);
    painter.drawPort(QUIET_NAN, "NaN", Direction.WEST); //change to qNaN if sNaN is added

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawLine(x - 35, y - 35, x - 35, y - 15);
    g.drawLine(x - 35, y - 35, x - 25, y - 35);
    g.drawLine(x - 35, y - 25, x - 25, y - 25);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);

    // compute outputs
    final var a = state.getPortValue(IN);

    final var isNegative = (a.toLongValue() & (1L << (dataWidth.getWidth() - 1))) != 0;

    final var isInfinite = switch (dataWidth.getWidth()) {
      case 8 -> Float.isInfinite(a.toFloatValueFromFP8());
      case 16 -> Float.isInfinite(a.toFloatValueFromFP16());
      case 32 -> Float.isInfinite(a.toFloatValue());
      case 64 -> Double.isInfinite(a.toDoubleValue());
      default -> false;
    };

    final var isNormal = switch (dataWidth.getWidth()) {
      case 8 -> {
          int exponent = (int)((a.toLongValue() >>> 3) & 0xF);
          yield exponent > 0 && exponent < 0xF;
      }
      case 16 -> {
          int exponent = (int)((a.toLongValue() >>> 10) & 0x1F);
          yield exponent > 0 && exponent < 0x1F;
      }
      case 32 -> {
          int exponent = (int)((a.toLongValue() >>> 23) & 0xFF);
          yield exponent > 0 && exponent < 0xFF;
      }
      case 64 -> {
          long exponent = (a.toLongValue() >>> 52) & 0x7FFL;
          yield exponent > 0 && exponent < 0x7FFL;
      }
      default -> false;
    };

    final var isSubnormal = switch (dataWidth.getWidth()) {
      case 8 -> {
          long bits = a.toLongValue();
          int exponent = (int)((bits >>> 3) & 0xF);
          int fraction = (int)(bits & 0x7);
          yield exponent == 0 && fraction != 0;
      }
      case 16 -> {
          long bits = a.toLongValue();
          int exponent = (int)((bits >>> 10) & 0x1F);
          int fraction = (int)(bits & 0x3FF);
          yield exponent == 0 && fraction != 0;
      }
      case 32 -> {
          long bits = a.toLongValue();
          int exponent = (int)((bits >>> 23) & 0xFF);
          int fraction = (int)(bits & 0x7FFFFF);
          yield exponent == 0 && fraction != 0;
      }
      case 64 -> {
          long bits = a.toLongValue();
          long exponent = (bits >>> 52) & 0x7FFL;
          long fraction = bits & 0xFFFFFFFFFFFFFL;
          yield exponent == 0 && fraction != 0;
      }
      default -> false;
    };

    final var isZero = switch (dataWidth.getWidth()) {
      case 8 -> a.toFloatValueFromFP8() == 0;
      case 16 -> a.toFloatValueFromFP16() == 0;
      case 32 -> a.toFloatValue() == 0;
      case 64 -> a.toDoubleValue() == 0;
      default -> false;
    };

    final var isNaN = switch (dataWidth.getWidth()) {
      case 8 -> Float.isNaN(a.toFloatValueFromFP8());
      case 16 -> Float.isNaN(a.toFloatValueFromFP16());
      case 32 -> Float.isNaN(a.toFloatValue());
      case 64 -> Double.isNaN(a.toDoubleValue());
      default -> false;
    };

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;

    state.setPort(NEGATIVE, isNegative ? Value.TRUE : Value.FALSE, delay);
    state.setPort(ZERO, isZero ? Value.TRUE : Value.FALSE, delay);
    state.setPort(SUBNORMAL, isSubnormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(NORMAL, isNormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(INFINITE, isInfinite ? Value.TRUE : Value.FALSE, delay);
//  state.setPort(SIGNALING_NAN, nan, delay);
    state.setPort(QUIET_NAN, isNaN ? Value.TRUE : Value.FALSE, delay);
  }
}
