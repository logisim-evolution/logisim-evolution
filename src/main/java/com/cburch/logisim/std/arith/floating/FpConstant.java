/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.AbstractConstantHdlGeneratorFactory;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;

public class FpConstant extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "FPConstant";

  public static final Attribute<Double> ATTR_VALUE =
      Attributes.forDouble("value", S.getter("constantValueAttr"));

  private static final int OUT = 0;
  private static final int PER_DELAY = 1;
  private static final Font DEFAULT_FONT = new Font("monospaced", Font.PLAIN, 12);

  private static class FpConstantHdlGeneratorFactory
      extends AbstractConstantHdlGeneratorFactory {
    @Override
    public long getConstant(AttributeSet attrs) {
      return createValue(attrs.getValue(StdAttr.FP_WIDTH), attrs.getValue(ATTR_VALUE))
          .toLongValue();
    }
  }

  public FpConstant() {
    super(_ID, S.getter("fpConstantComponent"), new FpConstantHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {StdAttr.FP_WIDTH, ATTR_VALUE},
        new Object[] {BitWidth.create(32), 0.0});
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.FP_WIDTH));
    setIcon(new ArithmeticIcon("FP"));
  }

  static Value createValue(BitWidth width, double value) {
    return Value.createKnown(width, value);
  }

  private static String getDisplayValue(AttributeSet attrs) {
    return createValue(attrs.getValue(StdAttr.FP_WIDTH), attrs.getValue(ATTR_VALUE))
        .toStringFromFloatValue();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var chars = getDisplayValue(attrs).length();
    final var width = Math.max(30, 8 + 7 * chars);
    return Bounds.create(-width, -10, width, 20);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FP_WIDTH) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.fireInvalidated();
    } else if (attr == ATTR_VALUE) {
      instance.recomputeBounds();
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var bounds = getOffsetBounds(painter.getAttributeSet());
    final var graphics = painter.getGraphics();
    graphics.drawRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    graphics.setFont(DEFAULT_FONT);
    GraphicsUtil.drawCenteredText(
        graphics,
        getDisplayValue(painter.getAttributeSet()),
        bounds.getX() + bounds.getWidth() / 2,
        bounds.getY() + bounds.getHeight() / 2 - 2);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var graphics = painter.getGraphics();
    graphics.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();

    final var bounds = painter.getOffsetBounds();
    final var location = painter.getLocation();
    graphics.setFont(DEFAULT_FONT);
    GraphicsUtil.drawCenteredText(
        graphics,
        getDisplayValue(painter.getAttributeSet()),
        location.getX() + bounds.getX() + bounds.getWidth() / 2,
        location.getY() + bounds.getY() + bounds.getHeight() / 2 - 2);
    painter.drawPort(OUT);
  }

  @Override
  public void propagate(InstanceState state) {
    final var width = state.getAttributeValue(StdAttr.FP_WIDTH);
    final var value = state.getAttributeValue(ATTR_VALUE);
    state.setPort(OUT, createValue(width, value), PER_DELAY);
  }

  private void updatePorts(Instance instance) {
    final var output = new Port(0, 0, Port.OUTPUT, StdAttr.FP_WIDTH);
    output.setToolTip(S.getter("fpConstantOutputTip"));
    instance.setPorts(new Port[] {output});
  }
}
