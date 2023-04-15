/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.wiring;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;

public class BitExtender extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Bit Extender";

  private static final Attribute<BitWidth> ATTR_IN_WIDTH =
      Attributes.forBitWidth("in_width", S.getter("extenderInAttr"));
  private static final Attribute<BitWidth> ATTR_OUT_WIDTH =
      Attributes.forBitWidth("out_width", S.getter("extenderOutAttr"));
  static final Attribute<AttributeOption> ATTR_TYPE =
      Attributes.forOption(
          "type",
          S.getter("extenderTypeAttr"),
          new AttributeOption[] {
            new AttributeOption("zero", "zero", S.getter("extenderZeroType")),
            new AttributeOption("one", "one", S.getter("extenderOneType")),
            new AttributeOption("sign", "sign", S.getter("extenderSignType")),
            new AttributeOption("input", "input", S.getter("extenderInputType")),
          });

  public static final BitExtender FACTORY = new BitExtender();

  public BitExtender() {
    super(_ID, S.getter("extenderComponent"), new BitExtenderHdlGeneratorFactory());
    setIconName("extender.gif");
    setAttributes(
        new Attribute[] {ATTR_IN_WIDTH, ATTR_OUT_WIDTH, ATTR_TYPE},
        new Object[] {BitWidth.create(8), BitWidth.create(16), ATTR_TYPE.parse("sign")});
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(ATTR_OUT_WIDTH),
            new BitWidthConfigurator(ATTR_IN_WIDTH, 1, Value.MAX_WIDTH, 0)));
    setOffsetBounds(Bounds.create(-40, -20, 40, 40));
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }

  private void configurePorts(Instance instance) {
    Port p0 = new Port(0, 0, Port.OUTPUT, ATTR_OUT_WIDTH);
    Port p1 = new Port(-40, 0, Port.INPUT, ATTR_IN_WIDTH);
    String type = getType(instance.getAttributeSet());
    if (type.equals("input")) {
      instance.setPorts(new Port[] {p0, p1, new Port(-20, -20, Port.INPUT, 1)});
    } else {
      instance.setPorts(new Port[] {p0, p1});
    }
  }

  private String getType(AttributeSet attrs) {
    AttributeOption topt = attrs.getValue(ATTR_TYPE);
    return (String) topt.getValue();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_TYPE) {
      configurePorts(instance);
    }
    instance.fireInvalidated();
  }

  //
  // graphics methods
  //
  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    FontMetrics fm = g.getFontMetrics();
    int asc = fm.getAscent();

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();

    String s0;
    String type = getType(painter.getAttributeSet());
    s0 = switch (type) {
      case "zero" -> S.get("extenderZeroLabel");
      case "one" -> S.get("extenderOneLabel");
      case "sign" -> S.get("extenderSignLabel");
      case "input" -> S.get("extenderInputLabel");
      default -> "???"; // should never happen
    };
    String s1 = S.get("extenderMainLabel");
    Bounds bds = painter.getBounds();
    int x = bds.getX() + bds.getWidth() / 2;
    int y0 = bds.getY() + (bds.getHeight() / 2 + asc) / 2;
    int y1 = bds.getY() + (3 * bds.getHeight() / 2 + asc) / 2;
    GraphicsUtil.drawText(g, s0, x, y0, GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);
    GraphicsUtil.drawText(g, s1, x, y1, GraphicsUtil.H_CENTER, GraphicsUtil.V_BASELINE);

    BitWidth w0 = painter.getAttributeValue(ATTR_OUT_WIDTH);
    BitWidth w1 = painter.getAttributeValue(ATTR_IN_WIDTH);
    painter.drawPort(0, "" + w0.getWidth(), Direction.WEST);
    painter.drawPort(1, "" + w1.getWidth(), Direction.EAST);
    if (type.equals("input")) painter.drawPort(2);
  }

  @Override
  public void propagate(InstanceState state) {
    Value in = state.getPortValue(1);
    BitWidth wout = state.getAttributeValue(ATTR_OUT_WIDTH);
    String type = getType(state.getAttributeSet());
    Value extend;
    switch (type) {
      case "one" -> extend = Value.TRUE;
      case "sign" -> {
        int win = in.getWidth();
        extend = win > 0 ? in.get(win - 1) : Value.ERROR;
      }
      case "input" -> {
        extend = state.getPortValue(2);
        if (extend.getWidth() != 1)
          extend = Value.ERROR;
      }
      default -> extend = Value.FALSE;
    }

    Value out = in.extendWidth(wout.getWidth(), extend);
    state.setPort(0, out, 1);
  }
}
