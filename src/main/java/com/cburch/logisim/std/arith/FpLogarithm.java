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
import com.cburch.logisim.data.Attributes;
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

public class FpLogarithm extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPLogarithm";

  static final AttributeOption LOGXY =
      new AttributeOption("logxy", S.getter("fpLogarithmArbitraryBase"));
  static final AttributeOption LOG1P =
      new AttributeOption("log1p", S.getter("fpLogarithmNaturalLogPlusOne"));
  static final AttributeOption LOG =
      new AttributeOption("log", S.getter("fpLogarithmNaturalLog"));
  static final AttributeOption LOG10 =
      new AttributeOption("log10", S.getter("fpLogarithmLog10"));
  static final Attribute<AttributeOption> LOG_MODE =
      Attributes.forOption(
          "mode",
          S.getter("fpExponentModeAttr"),
          new AttributeOption[] {
            LOGXY,
            LOG,
            LOG10,
            LOG1P
          });

  static final int PER_DELAY = 1;
  private static final int IN0 = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;
  private static final int IN1 = 3;

  public FpLogarithm() {
    super(_ID, S.getter("fpLogarithmComponent"));
    setAttributes(
      new Attribute[] {StdAttr.FP_WIDTH, LOG_MODE},
      new Object[] {BitWidth.create(32), LOGXY});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("Log",3));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == LOG_MODE) {
      configurePorts(instance);
    }
  }
  private void configurePorts(Instance instance) {
    final var isSingleInput = instance.getAttributeValue(LOG_MODE) != LOGXY;
    final Port[] ps;
    if(isSingleInput) {
      ps = new Port[3];

      ps[IN0] = new Port(-40, 0, Port.INPUT, StdAttr.FP_WIDTH);

    } else {
      ps = new Port[4];

      ps[IN0] = new Port(-40, -10, Port.INPUT, StdAttr.FP_WIDTH);
      ps[IN1] = new Port(-40, 10, Port.INPUT, StdAttr.FP_WIDTH);
      ps[IN1].setToolTip(S.getter("logarithmInputBTip"));
    }
    ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN0].setToolTip(S.getter("logarithmInputATip"));
    ps[OUT].setToolTip(S.getter("fpLogarithmOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));

    instance.setPorts(ps);
  }
  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();

    final var mode = painter.getAttributeValue(LOG_MODE);
    if(mode == LOGXY) {
      painter.drawPort(OUT,"log\u1D67x",Direction.WEST);
      g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
      painter.drawPort(IN0);
      painter.drawPort(IN1);
    } else {
      if(mode == LOG) {
        painter.drawPort(OUT, "log", Direction.WEST);
      } else if (mode == LOG10) {
        painter.drawPort(OUT, "log\u2081\u2080", Direction.WEST);
      } else {
        painter.drawPort(OUT, "log+1", Direction.WEST);
      }

      g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
      painter.drawPort(IN0);
    }

    painter.drawPort(ERR);

    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(x - 35, y - 15, x - 35, y + 5);
    g.drawLine(x - 35, y - 15, x - 25, y - 15);
    g.drawLine(x - 35, y - 5, x - 25, y - 5);
    GraphicsUtil.switchToWidth(g, 1);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidth = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var mode = state.getAttributeValue(LOG_MODE);

    // compute outputs
    final var a = state.getPortValue(IN0);

    final double out_val;
    final var a_val = a.toDoubleValueFromAnyFloat();

    if(mode == LOGXY) {
      final var b = state.getPortValue(IN1);
      final var b_val = b.toDoubleValueFromAnyFloat();
      out_val = Math.log(a_val) / Math.log(b_val);
    } else if(mode == LOG) {
      out_val = Math.log(a_val);
    } else if(mode == LOG10) {
      out_val = Math.log10(a_val);
    } else {
      out_val = Math.log1p(a_val);
    }

    final var out = Value.createKnown(dataWidth, out_val);

    // propagate them
    final var delay = (dataWidth.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(out_val) ? 1 : 0), delay);
  }
}
