/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import java.awt.Color;
import java.util.Arrays;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;

/**
 * Base class.
 * This base class provides a standard port layout and drawing code for generic logic interfaces (like for VHDL).
 * It does not assume much about the target; the goal is simply to keep the port layout code in one place.
 */
public abstract class GenericInterfaceComponent extends InstanceFactory {
  public GenericInterfaceComponent(String name,
      StringGetter displayName,
      HdlGeneratorFactory generator,
      boolean requiresGlobalClock) {
    super(name, displayName, generator, requiresGlobalClock);
  }

  static final int WIDTH = 140;
  static final int HEIGHT = 40;
  static final int PORT_GAP = 10;

  static final int X_PADDING = 5;

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    int nbInputs = getGIAttributesInputs(attrs).length;
    int nbOutputs = getGIAttributesOutputs(attrs).length;
    return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs) * PORT_GAP + HEIGHT);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    AttributeSet attrs = painter.getAttributeSet();
    final var g = painter.getGraphics();
    var metric = g.getFontMetrics();

    final var bds = painter.getBounds();
    final var x0 = bds.getX() + (bds.getWidth() / 2);
    final var y0 = bds.getY() + metric.getHeight() + 12;
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(getGIAttributesName(attrs), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    final var glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      final var font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(
          g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    final var inputs = getGIAttributesInputs(attrs);
    final var outputs = getGIAttributesOutputs(attrs);

    for (var i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (var i = 0; i < outputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(outputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + WIDTH - 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_CENTER);

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawPorts();
  }

  /**
   * Get the chip name for the given attributes.
   */
  protected abstract String getGIAttributesName(AttributeSet attrs);

  /**
   * Get the input ports for the given attributes.
   * The returned value is not to be modified.
   */
  protected abstract Port[] getGIAttributesInputs(AttributeSet attrs);

  /**
   * Get the output ports for the given attributes.
   * The returned value is not to be modified.
   */
  protected abstract Port[] getGIAttributesOutputs(AttributeSet attrs);

  /**
   * Called by subclasses when something happens which changes anything affecting getGIAttributes* functions.
   */
  protected void updatePorts(Instance instance) {
    Port[] inputs = getGIAttributesInputs(instance.getAttributeSet());
    Port[] outputs = getGIAttributesOutputs(instance.getAttributeSet());
    Port[] result = Arrays.copyOf(inputs, inputs.length + outputs.length);
    System.arraycopy(outputs, 0, result, inputs.length, outputs.length);
    instance.setPorts(result);
    instance.recomputeBounds();
  }

}
