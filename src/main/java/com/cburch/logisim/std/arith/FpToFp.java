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
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;

public class FpToFp extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPToFP";

  static final int PER_DELAY = 1;
  private static final int IN = 0;
  private static final int OUT = 1;
  private static final int ERR = 2;

  Attribute<BitWidth> FP_WIDTH_IN =
      Attributes.forOption(
          "fpwidthin",
          S.getter("fpToFpDataWidthIn"),
          new BitWidth[] {BitWidth.create(8), BitWidth.create(16), BitWidth.create(32),
            BitWidth.create(64)});
  Attribute<BitWidth> FP_WIDTH_OUT =
      Attributes.forOption(
          "fpwidthout",
          S.getter("fpToFpDataWidthOut"),
          new BitWidth[] {BitWidth.create(8), BitWidth.create(16), BitWidth.create(32),
            BitWidth.create(64)});

  public FpToFp() {
    super(_ID, S.getter("fpToFpComponent"));
    setAttributes(
        new Attribute[] {FP_WIDTH_IN, FP_WIDTH_OUT},
        new Object[] {BitWidth.create(32), BitWidth.create(32)});
    setKeyConfigurator(new BitWidthConfigurator(FP_WIDTH_IN));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
    setIcon(new ArithmeticIcon("FP\u2192FP", 3));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == FP_WIDTH_IN || attr == FP_WIDTH_OUT) {
      configurePorts(instance);
    }
  }
  private void configurePorts(Instance instance) {
    final var ps = new Port[3];
    ps[IN] = new Port(-40, 0, Port.INPUT, FP_WIDTH_IN);
    ps[OUT] = new Port(0, 0, Port.OUTPUT, FP_WIDTH_OUT);
    ps[ERR] = new Port(-20, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("fpToFpInputTip"));
    ps[OUT].setToolTip(S.getter("fpToFpOutputTip"));
    ps[ERR].setToolTip(S.getter("fpErrorTip"));
    instance.setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();

    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    GraphicsUtil.switchToWidth(g, 2);
    var font = g.getFont().deriveFont(20f);
    GraphicsUtil.drawText(g, font, "\u2192", x - 20, y, GraphicsUtil.H_CENTER,
      GraphicsUtil.V_CENTER_OVERALL);
    GraphicsUtil.switchToWidth(g, 1);

    final var inputWidth = painter.getAttributeValue(FP_WIDTH_IN);
    final var outputWidth = painter.getAttributeValue(FP_WIDTH_OUT);

    final String inputChar = switch(inputWidth.getWidth()){
      case 8 -> "M";
      case 16 -> "H";
      case 32 -> "F";
      case 64 -> "D";
      default -> "E";
    };
    final String outputChar = switch(outputWidth.getWidth()){
      case 8 -> "M";
      case 16 -> "H";
      case 32 -> "F";
      case 64 -> "D";
      default -> "E";
    };
    painter.drawPort(IN, inputChar, Direction.EAST);
    painter.drawPort(OUT, outputChar, Direction.WEST);
    painter.drawPort(ERR);
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var dataWidthOut = state.getAttributeValue(FP_WIDTH_OUT);

    // compute outputs
    final var a = state.getPortValue(IN);
    final var a_val = a.toDoubleValueFromAnyFloat();

    final var out = Value.createKnown(dataWidthOut, a_val);

    // propagate them
    final var delay = (dataWidthOut.getWidth() + 2) * PER_DELAY;
    state.setPort(OUT, out, delay);
    state.setPort(ERR, Value.createKnown(BitWidth.create(1), Double.isNaN(a_val) ? 1 : 0), delay);
  }
}
