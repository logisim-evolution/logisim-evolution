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
  private static final int IN0 = 0;
  private static final int NEG_INF = 1;
  private static final int NEG_NORMAL = 2;
  private static final int NEG_SUBNORMAL = 3;
  private static final int NEG_ZERO = 4;
  private static final int POS_ZERO = 5;
  private static final int POS_SUBNORMAL = 6;
  private static final int POS_NORMAL = 7;
  private static final int POS_INF = 8;
//private static final int SIGNALING_NAN = 9;
  private static final int QUIET_NAN = 9;

  public FpClassificator() {
    super(_ID, S.getter("fpClassificator"));
    setAttributes(new Attribute[] {StdAttr.FP_WIDTH}, new Object[] {BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -50, 40, 100));
    setIcon(new ArithmeticIcon("\u2630 ",3));

    final var ps = new Port[10];
    ps[IN0] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);

    ps[NEG_INF] = new Port(0, -40, Port.OUTPUT, 1);
    ps[NEG_NORMAL] = new Port(0, -30, Port.OUTPUT, 1);
    ps[NEG_SUBNORMAL] = new Port(0, -20, Port.OUTPUT, 1);
    ps[NEG_ZERO] = new Port(0, -10, Port.OUTPUT, 1);
    ps[POS_ZERO] = new Port(0, 0, Port.OUTPUT, 1);
    ps[POS_SUBNORMAL] = new Port(0, 10, Port.OUTPUT, 1);
    ps[POS_NORMAL] = new Port(0, 20, Port.OUTPUT, 1);
    ps[POS_INF] = new Port(0, 30, Port.OUTPUT, 1);
//  ps[SIGNALING_NAN] = new Port(0, 20, Port.OUTPUT, 1);
    ps[QUIET_NAN] = new Port(0, 40, Port.OUTPUT, 1);

    ps[IN0].setToolTip(S.getter("fpClassificatorInputTip"));

    ps[NEG_INF].setToolTip(S.getter("fpClassificatorNegativeInfinityTip"));
    ps[NEG_NORMAL].setToolTip(S.getter("fpClassificatorNegativeNormalTip"));
    ps[NEG_SUBNORMAL].setToolTip(S.getter("fpClassificatorNegativeSubnormalTip"));
    ps[NEG_ZERO].setToolTip(S.getter("fpClassificatorNegativeZeroTip"));
    ps[POS_ZERO].setToolTip(S.getter("fpClassificatorPositiveZeroTip"));
    ps[POS_SUBNORMAL].setToolTip(S.getter("fpClassificatorPositiveSubnormalTip"));
    ps[POS_NORMAL].setToolTip(S.getter("fpClassificatorPositiveNormalTip"));
    ps[POS_INF].setToolTip(S.getter("fpClassificatorPositiveInfinityTip"));
//  ps[SIGNALING_NAN].setToolTip(S.getter("fpClassificatorSignalingNaNTip"));
    ps[QUIET_NAN].setToolTip(S.getter("fpClassificatorQuietNaNTip"));

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
    painter.drawPort(IN0);
    painter.drawPort(NEG_INF, "-\u221E", Direction.WEST);
    painter.drawPort(NEG_NORMAL, "-N", Direction.WEST);
    painter.drawPort(NEG_SUBNORMAL, "-SN", Direction.WEST);
    painter.drawPort(NEG_ZERO, "-0", Direction.WEST);
    painter.drawPort(POS_ZERO, "+0", Direction.WEST);
    painter.drawPort(POS_SUBNORMAL, "+SN", Direction.WEST);
    painter.drawPort(POS_NORMAL, "+N", Direction.WEST);
    painter.drawPort(POS_INF, "+\u221E", Direction.WEST);
//  painter.drawPort(SIGNALING_NAN, "sNaN", Direction.WEST);
    painter.drawPort(QUIET_NAN, "NaN", Direction.WEST); //change to qNaN if sNaN is added

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawLine(x - 35, y - 45, x - 35, y - 25);
    g.drawLine(x - 35, y - 45, x - 25, y - 45);
    g.drawLine(x - 35, y - 35, x - 25, y - 35);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);

    // compute outputs
    final var a = state.getPortValue(IN0);

    final var isNegative = switch (dataWidth.getWidth()) {
      case 16 -> (a.toLongValue() & 0x8000) != 0;
      case 32 -> (a.toLongValue() & 0x80000000) != 0;
      case 64 -> (a.toLongValue() & 0x8000000000000000L) != 0;
      default -> false;
    };

    final var isInfinite = switch (dataWidth.getWidth()) {
      case 16 -> Float.isInfinite(a.toFloatValueFromFP16());
      case 32 -> Float.isInfinite(a.toFloatValue());
      case 64 -> Double.isInfinite(a.toDoubleValue());
      default -> false;
    };

    final var isNormal = switch (dataWidth.getWidth()) {
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
      case 16 -> a.toFloatValueFromFP16() == 0;
      case 32 -> a.toFloatValue() == 0;
      case 64 -> a.toDoubleValue() == 0;
      default -> false;
    };

    final var isNaN = switch (dataWidth.getWidth()) {
      case 16 -> Float.isNaN(a.toFloatValueFromFP16());
      case 32 -> Float.isNaN(a.toFloatValue());
      case 64 -> Double.isNaN(a.toDoubleValue());
      default -> false;
    };
    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;

    state.setPort(NEG_INF, isNegative && isInfinite ? Value.TRUE : Value.FALSE, delay);
    state.setPort(NEG_NORMAL, isNegative && isNormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(NEG_SUBNORMAL, isNegative && isSubnormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(NEG_ZERO, isNegative && isZero ? Value.TRUE : Value.FALSE, delay);
    state.setPort(POS_ZERO, !isNegative && isZero ? Value.TRUE : Value.FALSE, delay);
    state.setPort(POS_SUBNORMAL, !isNegative && isSubnormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(POS_NORMAL, !isNegative && isNormal ? Value.TRUE : Value.FALSE, delay);
    state.setPort(POS_INF, !isNegative && isInfinite ? Value.TRUE : Value.FALSE, delay);
//  state.setPort(SIGNALING_NAN, nan, delay);
    state.setPort(QUIET_NAN, isNaN ? Value.TRUE : Value.FALSE, delay);
  }
}
