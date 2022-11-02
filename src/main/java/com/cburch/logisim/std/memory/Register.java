/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.icons.FlipFlopIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;

import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;

public class Register extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Register";

  public static void drawRegisterClassic(
      InstancePainter painter,
      int x,
      int y,
      int nr_of_bits,
      boolean isLatch,
      boolean neg_active,
      boolean has_we,
      String value) {}

  public static void drawRegisterEvolution(
      InstancePainter painter,
      int x,
      int y,
      int nrOfBits,
      boolean isLatch,
      boolean negActive,
      boolean hasWE,
      Value value) {
    final var dq_widtdqWidth = (nrOfBits == 1) ? 2 : 5;
    final var len = (nrOfBits + 3) / 4;
    final var wid = 8 * len + 2;
    final var xoff = (60 - wid) / 2;
    final var g = painter.getGraphics();
    if (painter.getShowState() && (value != null)) {
      if (value.isFullyDefined()) g.setColor(Color.LIGHT_GRAY);
      else if (value.isErrorValue()) g.setColor(Color.RED);
      else g.setColor(Color.BLUE);
      g.fillRect(x + xoff, y + 1, wid, 16);
      if (value.isFullyDefined()) g.setColor(Color.DARK_GRAY);
      else g.setColor(Color.YELLOW);
      var str = "";
      if (value.isFullyDefined()) str = StringUtil.toHexString(nrOfBits, value.toLongValue());
      else {
        for (int i = 0; i < len; i++) str = str.concat("?");
      }
      GraphicsUtil.drawCenteredText(g, str, x + 30, y + 8);
    }
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.switchToWidth(g, 2);
    if (nrOfBits > 1) {
      g.drawLine(x + 15, y + 80, x + 15, y + 85);
      g.drawLine(x + 15, y + 85, x + 55, y + 85);
      g.drawLine(x + 55, y + 25, x + 55, y + 85);
      g.drawLine(x + 50, y + 25, x + 55, y + 25);
      if (nrOfBits > 2) {
        g.drawLine(x + 20, y + 85, x + 20, y + 90);
        g.drawLine(x + 20, y + 90, x + 60, y + 90);
        g.drawLine(x + 60, y + 30, x + 60, y + 90);
        g.drawLine(x + 55, y + 30, x + 60, y + 30);
      }
      g.setColor(Value.multiColor);
    }
    GraphicsUtil.switchToWidth(g, dq_widtdqWidth);
    g.drawLine(x, y + 30, x + 8, y + 30);
    g.drawLine(x + 52, y + 30, x + 60, y + 30);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x + 10, y + 20, 40, 60);
    GraphicsUtil.switchToWidth(g, 1);
    GraphicsUtil.drawCenteredText(g, "D", x + 18, y + 28);
    GraphicsUtil.drawCenteredText(g, "Q", x + 41, y + 28);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawLine(x + 30, y + 81, x + 30, y + 90);
    GraphicsUtil.switchToWidth(g, 1);
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    GraphicsUtil.drawCenteredText(g, "R", x + 30, y + 68);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    if (hasWE) {
      GraphicsUtil.drawCenteredText(g, "WE", x + 22, y + 48);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawLine(x, y + 50, x + 9, y + 50);
      GraphicsUtil.switchToWidth(g, 1);
    }
    if (!isLatch) {
      painter.drawClockSymbol(x + 10, y + 70);
    } else {
      GraphicsUtil.drawCenteredText(g, "E", x + 18, y + 68);
    }
    if (!negActive) {
      GraphicsUtil.switchToWidth(g, 2);
      g.drawLine(x, y + 70, x + 9, y + 70);
    } else {
      GraphicsUtil.switchToWidth(g, 2);
      g.drawOval(x, y + 65, 10, 10);
    }
    GraphicsUtil.switchToWidth(g, 1);
  }

  public void drawRegisterClassic(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    RegisterData state = (RegisterData) painter.getData();
    BitWidth widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    int width = widthVal == null ? 8 : widthVal.getWidth();

    // determine text to draw in label
    String a;
    String b = null;
    if (painter.getShowState()) {
      long val = state == null ? 0 : state.value.toLongValue();
      final var str = StringUtil.toHexString(width, val);
      if (str.length() <= 4) {
        a = str;
      } else {
        int split = str.length() - 4;
        a = str.substring(0, split);
        b = str.substring(split);
      }
    } else {
      a = S.get("registerLabel");
      b = S.get("registerWidthLabel", "" + widthVal.getWidth());
    }

    // draw boundary, label
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(painter.getAttributeValue(StdAttr.LABEL_COLOR));
    painter.drawLabel();

    // draw input and output ports
    if (b == null) {
      painter.drawPort(IN, "D", Direction.EAST);
      painter.drawPort(OUT, "Q", Direction.WEST);
    } else {
      painter.drawPort(IN);
      painter.drawPort(OUT);
    }
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(CLR, "0", Direction.SOUTH);
    painter.drawPort(EN, S.get("memEnableLabel"), Direction.EAST);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawClock(CK, Direction.NORTH);

    // draw contents
    if (b == null) {
      GraphicsUtil.drawText(
          g, a, bds.getX() + 15, bds.getY() + 4, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    } else {
      GraphicsUtil.drawText(
          g, a, bds.getX() + 15, bds.getY() + 3, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
      GraphicsUtil.drawText(
          g, b, bds.getX() + 15, bds.getY() + 15, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    }
  }

  static final int DELAY = 8;
  public static final int OUT = 0;
  public static final int IN = 1;
  public static final int CK = 2;
  public static final int CLR = 3;
  public static final int EN = 4;
  static final int Xsize = 60;
  static final int Ysize = 90;

  public static final Attribute<Boolean> ATTR_SHOW_IN_TAB =
      Attributes.forBoolean("showInTab", S.getter("registerShowInTab"));

  public Register() {
    super(_ID, S.getter("registerComponent"), new RegisterHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.WIDTH,
          StdAttr.TRIGGER,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          ATTR_SHOW_IN_TAB,
          StdAttr.APPEARANCE
        },
        new Object[] {
          BitWidth.create(8),
          StdAttr.TRIG_RISING,
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT,
          false,
          AppPreferences.getDefaultAppearance()
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(StdAttr.WIDTH),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
    setIcon(new FlipFlopIcon(FlipFlopIcon.REGISTER));
    setInstancePoker(RegisterPoker.class);
    setInstanceLogger(RegisterLogger.class);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      return Bounds.create(-30, -20, 30, 40);
    } else {
      return Bounds.create(0, 0, Xsize, Ysize);
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_SIDES);
  }

  private void updatePorts(Instance instance) {
    final var ps = new Port[5];
    if (instance.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
      ps[IN] = new Port(-30, 0, Port.INPUT, StdAttr.WIDTH);
      ps[CK] = new Port(-20, 20, Port.INPUT, 1);
      ps[CLR] = new Port(-10, 20, Port.INPUT, 1);
      ps[EN] = new Port(-30, 10, Port.INPUT, 1);
    } else {
      ps[OUT] = new Port(60, 30, Port.OUTPUT, StdAttr.WIDTH);
      ps[IN] = new Port(0, 30, Port.INPUT, StdAttr.WIDTH);
      ps[CK] = new Port(0, 70, Port.INPUT, 1);
      ps[CLR] = new Port(30, 90, Port.INPUT, 1);
      ps[EN] = new Port(0, 50, Port.INPUT, 1);
    }
    ps[OUT].setToolTip(S.getter("registerQTip"));
    ps[IN].setToolTip(S.getter("registerDTip"));
    ps[CK].setToolTip(S.getter("registerClkTip"));
    ps[CLR].setToolTip(S.getter("registerClrTip"));
    ps[EN].setToolTip(S.getter("registerEnableTip"));
    instance.setPorts(ps);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var CompleteName = new StringBuilder();
    CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()).toUpperCase());
    if ((attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_FALLING)
        || (attrs.getValue(StdAttr.TRIGGER) == StdAttr.TRIG_RISING)) {
      CompleteName.append("_FLIP_FLOP");
    } else {
      CompleteName.append("_LATCH");
    }
    return CompleteName.toString();
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      drawRegisterClassic(painter);
    } else {
      final var state = (RegisterData) painter.getData();
      final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
      final var width = widthVal == null ? 8 : widthVal.getWidth();
      final var loc = painter.getLocation();
      int x = loc.getX();
      int y = loc.getY();

      // determine text to draw in label
      Object Trigger = painter.getAttributeValue(StdAttr.TRIGGER);
      final var IsLatch = Trigger.equals(StdAttr.TRIG_HIGH) || Trigger.equals(StdAttr.TRIG_LOW);
      final var NegActive =
          Trigger.equals(StdAttr.TRIG_FALLING) || Trigger.equals(StdAttr.TRIG_LOW);

      drawRegisterEvolution(
          painter, x, y, width, IsLatch, NegActive, true, (state == null) ? null : state.value);
      painter.drawLabel();

      // draw input and output ports
      painter.drawPort(IN);
      painter.drawPort(OUT);
      painter.drawPort(CLR);
      painter.drawPort(EN);
      painter.drawPort(CK);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    Object triggerType = state.getAttributeValue(StdAttr.TRIGGER);
    var data = (RegisterData) state.getData();

    if (data == null) {
      data = new RegisterData(dataWidth);
      state.setData(data);
    }

    final var triggered = data.updateClock(state.getPortValue(CK), triggerType);

    if (state.getPortValue(CLR) == Value.TRUE) {
      data.value = Value.createKnown(dataWidth, 0);
    } else if (triggered && state.getPortValue(EN) != Value.FALSE) {
      data.value = state.getPortValue(IN);
    }

    state.setPort(OUT, data.value, DELAY);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return Netlist.isFlipFlop(comp.getComponent().getAttributeSet());
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {CK};
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.WIDTH || attr == StdAttr.APPEARANCE) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new RegisterShape(x, y, path);
  }
}
