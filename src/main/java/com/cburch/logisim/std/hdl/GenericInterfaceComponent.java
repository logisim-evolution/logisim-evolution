/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import java.awt.Color;

import com.cburch.hdl.HdlModel.PortDescription;
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

    final var labelVis = painter.getAttributeValue(StdAttr.LABEL_VISIBILITY);
    if (labelVis != null && labelVis.booleanValue()) {
      final var glbLabel = painter.getAttributeValue(StdAttr.LABEL);
      if (glbLabel != null) {
        final var font = g.getFont();
        g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
        GraphicsUtil.drawCenteredText(
            g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
        g.setFont(font);
      }
    }

    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    final var inputs = getGIAttributesInputs(attrs);
    final var outputs = getGIAttributesOutputs(attrs);

    for (var i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getName(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (var i = 0; i < outputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(outputs[i].getName(), metric, (WIDTH / 2) - X_PADDING),
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
  protected abstract PortDescription[] getGIAttributesInputs(AttributeSet attrs);

  /**
   * Get the output ports for the given attributes.
   * The returned value is not to be modified.
   */
  protected abstract PortDescription[] getGIAttributesOutputs(AttributeSet attrs);

  /**
   * Called by subclasses when something happens which changes anything affecting getGIAttributes* functions.
   */
  protected void updatePorts(Instance instance) {
    PortDescription[] inputs = getGIAttributesInputs(instance.getAttributeSet());
    PortDescription[] outputs = getGIAttributesOutputs(instance.getAttributeSet());
    Port[] result = new Port[inputs.length + outputs.length];
    int resultIndex = 0;

    int i = 0;
    for (var desc : inputs) {
      result[resultIndex] =
          new Port(
              0,
              (i * HdlCircuitComponent.PORT_GAP) + HdlCircuitComponent.HEIGHT,
              desc.getType(),
              desc.getWidth());
      result[resultIndex].setToolTip(S.getter(desc.getName()));
      resultIndex++;
      i++;
    }

    i = 0;
    for (var desc : outputs) {
      result[resultIndex] =
          new Port(
              HdlCircuitComponent.WIDTH,
              (i * HdlCircuitComponent.PORT_GAP) + HdlCircuitComponent.HEIGHT,
              desc.getType(),
              desc.getWidth());
      result[resultIndex].setToolTip(S.getter(desc.getName()));
      resultIndex++;
      i++;
    }

    instance.setPorts(result);
    instance.recomputeBounds();
  }

}
