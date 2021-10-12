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
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.icons.CounterIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.math.BigInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Counter extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Counter";

  public static int getSymbolWidth(int NrOfBits) {
    return 150 + ((NrOfBits - 8) / 5) * 10;
  }

  static final Logger logger = LoggerFactory.getLogger(Counter.class);

  static final AttributeOption ON_GOAL_WRAP =
      new AttributeOption("wrap", "wrap", S.getter("counterGoalWrap"));
  static final AttributeOption ON_GOAL_STAY =
      new AttributeOption("stay", "stay", S.getter("counterGoalStay"));
  static final AttributeOption ON_GOAL_CONT =
      new AttributeOption("continue", "continue", S.getter("counterGoalContinue"));

  static final AttributeOption ON_GOAL_LOAD =
      new AttributeOption("load", "load", S.getter("counterGoalLoad"));
  static final Attribute<Long> ATTR_MAX =
      Attributes.forHexLong("max", S.getter("counterMaxAttr"));

  static final Attribute<AttributeOption> ATTR_ON_GOAL =
      Attributes.forOption(
          "ongoal",
          S.getter("counterGoalAttr"),
          new AttributeOption[] {ON_GOAL_WRAP, ON_GOAL_STAY, ON_GOAL_CONT, ON_GOAL_LOAD});
  static final int DELAY = 8;
  public static final int OUT = 0;
  public static final int IN = 1;
  public static final int CK = 2;
  public static final int CLR = 3;
  public static final int LD = 4;
  public static final int UD = 5;
  public static final int EN = 6;

  static final int CARRY = 7;

  public Counter() {
    super(_ID, S.getter("counterComponent"), new CounterHdlGeneratorFactory());
    setOffsetBounds(Bounds.create(-30, -20, 30, 40));
    setIcon(new CounterIcon());
    setInstancePoker(CounterPoker.class);
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(StdAttr.WIDTH),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
    setInstanceLogger(RegisterLogger.class);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }

  private void configurePorts(Instance instance) {
    final var bds = instance.getBounds();
    final var widthVal = instance.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();
    final var ps = new Port[8];
    if (instance.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      ps[OUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
      ps[IN] = new Port(-30, 0, Port.INPUT, StdAttr.WIDTH);
      ps[CK] = new Port(-20, 20, Port.INPUT, 1);
      ps[CLR] = new Port(-10, 20, Port.INPUT, 1);
      ps[LD] = new Port(-30, -10, Port.INPUT, 1);
      ps[UD] = new Port(-20, -20, Port.INPUT, 1);
      ps[EN] = new Port(-30, 10, Port.INPUT, 1);
      ps[CARRY] = new Port(0, 10, Port.OUTPUT, 1);
    } else {
      if (width == 1) {
        ps[OUT] = new Port(getSymbolWidth(width) + 40, 120, Port.OUTPUT, StdAttr.WIDTH);
        ps[IN] = new Port(0, 120, Port.INPUT, StdAttr.WIDTH);
      } else {
        ps[OUT] = new Port(getSymbolWidth(width) + 40, 110, Port.OUTPUT, StdAttr.WIDTH);
        ps[IN] = new Port(0, 110, Port.INPUT, StdAttr.WIDTH);
      }
      ps[CK] = new Port(0, 80, Port.INPUT, 1);
      ps[CLR] = new Port(0, 20, Port.INPUT, 1);
      ps[LD] = new Port(0, 30, Port.INPUT, 1);
      ps[UD] = new Port(0, 50, Port.INPUT, 1);
      ps[EN] = new Port(0, 70, Port.INPUT, 1);
      ps[CARRY] = new Port(40 + getSymbolWidth(width), 50, Port.OUTPUT, 1);
    }
    ps[OUT].setToolTip(S.getter("counterQTip"));
    ps[IN].setToolTip(S.getter("counterDataTip"));
    ps[CK].setToolTip(S.getter("counterClockTip"));
    ps[CLR].setToolTip(S.getter("counterResetTip"));
    ps[LD].setToolTip(S.getter("counterLoadTip"));
    ps[UD].setToolTip(S.getter("counterUpDownTip"));
    ps[EN].setToolTip(S.getter("counterEnableTip"));
    ps[CARRY].setToolTip(S.getter("counterCarryTip"));
    instance.setPorts(ps);
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BASELINE);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new CounterAttributes();
  }

  private void drawControl(InstancePainter painter, int xpos, int ypos) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();
    final var symbolWidth = getSymbolWidth(width);
    // Draw top
    final var controlTopx = new int[8];
    controlTopx[0] = controlTopx[1] = xpos + 30;
    controlTopx[2] = controlTopx[3] = xpos + 20;
    controlTopx[4] = controlTopx[5] = xpos + 20 + symbolWidth;
    controlTopx[6] = controlTopx[7] = xpos + 10 + symbolWidth;

    final var controlTopy = new int[8];
    controlTopy[0] = controlTopy[7] = ypos + 110;
    controlTopy[1] = controlTopy[2] = controlTopy[5] = controlTopy[6] = ypos + 100;
    controlTopy[3] = controlTopy[4] = ypos;
    g.drawPolyline(controlTopx, controlTopy, controlTopx.length);
    // These are up here because they reset the width to 1 when done.
    painter.drawClockSymbol(xpos + 20, ypos + 80);
    painter.drawClockSymbol(xpos + 20, ypos + 90);
    /* Draw Label */

    long max = painter.getAttributeValue(ATTR_MAX);
    var isCTRm = (max == painter.getAttributeValue(StdAttr.WIDTH).getMask());
    Object onGoal = painter.getAttributeValue(ATTR_ON_GOAL);
    isCTRm |= onGoal == ON_GOAL_CONT;
    final var label = (isCTRm)
            ? "CTR" + painter.getAttributeValue(StdAttr.WIDTH).getWidth()
            : "CTR DIV0x" + Long.toHexString(max);
    GraphicsUtil.drawCenteredText(g, label, xpos + (getSymbolWidth(width) / 2) + 20, ypos + 5);
    GraphicsUtil.switchToWidth(g, GraphicsUtil.CONTROL_WIDTH);
    /* Draw Reset Input */
    g.drawLine(xpos, ypos + 20, xpos + 20, ypos + 20);
    GraphicsUtil.drawText(g, "R", xpos + 30, ypos + 20, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(CLR);
    /* Draw Load Input */
    g.drawLine(xpos, ypos + 30, xpos + 20, ypos + 30);
    g.drawLine(xpos + 5, ypos + 40, xpos + 12, ypos + 40);
    g.drawLine(xpos + 5, ypos + 30, xpos + 5, ypos + 40);
    g.drawOval(xpos + 12, ypos + 36, 8, 8);
    g.fillOval(xpos + 2, ypos + 27, 6, 6);
    painter.drawPort(LD);
    GraphicsUtil.drawText(g, "M2 [count]", xpos + 30, ypos + 40, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, "M1 [load]", xpos + 30, ypos + 30, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    /* Draw UpDn input */
    g.drawLine(xpos, ypos + 50, xpos + 20, ypos + 50);
    g.drawLine(xpos + 5, ypos + 60, xpos + 12, ypos + 60);
    g.drawLine(xpos + 5, ypos + 50, xpos + 5, ypos + 60);
    g.drawOval(xpos + 12, ypos + 56, 8, 8);
    g.fillOval(xpos + 2, ypos + 47, 6, 6);
    GraphicsUtil.drawText(g, "M3 [up]", xpos + 30, ypos + 50, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, "M4 [down]", xpos + 30, ypos + 60, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(UD);
    /* Draw Enable Port */
    g.drawLine(xpos, ypos + 70, xpos + 20, ypos + 70);
    GraphicsUtil.drawText(g, "G5", xpos + 30, ypos + 70, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(EN);
    /* Draw Clock */
    final var inverted = painter.getAttributeValue(StdAttr.EDGE_TRIGGER).equals(StdAttr.TRIG_FALLING);
    final var xend = (inverted) ? xpos + 12 : xpos + 20;
    g.drawLine(xpos, ypos + 80, xend, ypos + 80);
    g.drawLine(xpos + 5, ypos + 90, xend, ypos + 90);
    g.drawLine(xpos + 5, ypos + 80, xpos + 5, ypos + 90);
    g.fillOval(xpos + 2, ypos + 77, 6, 6);
    if (inverted) {
      g.drawOval(xend, ypos + 76, 8, 8);
      g.drawOval(xend, ypos + 86, 8, 8);
    }
    GraphicsUtil.drawText(g, "2,3,5+/C6", xpos + 30, ypos + 80, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(g, "2,4,5-", xpos + 30, ypos + 90, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(CK);
    /* Draw Carry */
    g.drawLine(xpos + 20 + getSymbolWidth(width), ypos + 50, xpos + 40 + getSymbolWidth(width), ypos + 50);
    g.drawLine(xpos + 20 + getSymbolWidth(width), ypos + 60, xpos + 35 + getSymbolWidth(width), ypos + 60);
    g.drawLine(xpos + 35 + getSymbolWidth(width), ypos + 50, xpos + 35 + getSymbolWidth(width), ypos + 60);
    g.fillOval(xpos + 32 + getSymbolWidth(width), ypos + 47, 6, 6);
    String maxVal =
        "3CT=0x"
            + Long.toHexString(painter.getAttributeValue(ATTR_MAX)).toUpperCase();
    GraphicsUtil.drawText(
        g,
        maxVal,
        xpos + 17 + getSymbolWidth(width),
        ypos + 50,
        GraphicsUtil.H_RIGHT,
        GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(
        g,
        "4CT=0",
        xpos + 17 + getSymbolWidth(width),
        ypos + 60,
        GraphicsUtil.H_RIGHT,
        GraphicsUtil.V_CENTER);
    painter.drawPort(CARRY);
    /* Draw counter Value */
    RegisterData state = (RegisterData) painter.getData();
    if (painter.getShowState() && (state != null)) {
      final var len = (width + 3) / 4;
      final var xcenter = getSymbolWidth(width) - 25;
      final var val = state.value;
      if (val.isFullyDefined()) g.setColor(Color.LIGHT_GRAY);
      else if (val.isErrorValue()) g.setColor(Color.RED);
      else g.setColor(Color.BLUE);
      g.fillRect(xpos + xcenter - len * 4, ypos + 22, len * 8, 16);
      var value = "";
      if (val.isFullyDefined()) {
        g.setColor(Color.DARK_GRAY);
        value = StringUtil.toHexString(width, val.toLongValue()).toUpperCase();
      } else {
        g.setColor(Color.YELLOW);
        for (var i = 0; i < StringUtil.toHexString(width, val.toLongValue()).length(); i++)
          value = (val.isUnknown()) ? value.concat("?") : value.concat("!");
      }
      GraphicsUtil.drawText(
          g,
          value,
          xpos + xcenter - len * 4 + 1,
          ypos + 30,
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
      g.setColor(Color.BLACK);
    }
  }

  private void drawDataBlock(InstancePainter painter, int xpos, int ypos, int bitNr, int nrOfBits) {
    final var realYpos = ypos + bitNr * 20;
    final var first = bitNr == 0;
    final var last = bitNr == (nrOfBits - 1);
    final var g = painter.getGraphics();
    final var font = g.getFont();
    g.setFont(font.deriveFont(7.0f));
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(xpos + 20, realYpos, getSymbolWidth(nrOfBits), 20);
    /* Input Line */
    if (nrOfBits > 1) {
      // Input Line
      int[] ixPoints = {xpos + 5, xpos + 10, xpos + 20};
      int[] iyPoints = {realYpos + 5, realYpos + 10, realYpos + 10};
      g.drawPolyline(ixPoints, iyPoints, 3);

      // Output Line
      int[] oxPoints = {
        xpos + 20 + getSymbolWidth(nrOfBits),
        xpos + 30 + getSymbolWidth(nrOfBits),
        xpos + 35 + getSymbolWidth(nrOfBits)
      };
      int[] oyPoints = {realYpos + 10, realYpos + 10, realYpos + 5};
      g.drawPolyline(oxPoints, oyPoints, 3);
    } else {
      // Input Line
      g.drawLine(xpos, realYpos + 10, xpos + 20, realYpos + 10);
      // Output Line
      g.drawLine(
          xpos + 20 + getSymbolWidth(nrOfBits),
          realYpos + 10,
          xpos + 40 + getSymbolWidth(nrOfBits),
          realYpos + 10);
    }

    g.setColor(Color.BLACK);
    if (nrOfBits > 1) {
      GraphicsUtil.drawText(
          g,
          Integer.toString(bitNr),
          xpos + 30 + getSymbolWidth(nrOfBits),
          realYpos + 8,
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_BASELINE);
      GraphicsUtil.drawText(
          g,
          Integer.toString(bitNr),
          xpos + 10,
          realYpos + 8,
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_BASELINE);
    }
    g.setFont(font);
    GraphicsUtil.drawText(
        g, "1,6D", xpos + 21, realYpos + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    final var LineWidth = (nrOfBits == 1) ? GraphicsUtil.DATA_SINGLE_WIDTH : GraphicsUtil.DATA_MULTI_WIDTH;
    GraphicsUtil.switchToWidth(g, LineWidth);
    if (first) {
      painter.drawPort(IN);
      painter.drawPort(OUT);
      if (nrOfBits > 1) {
        // Input Line
        int[] ixPoints = {xpos, xpos + 5, xpos + 5};
        int[] iyPoints = {realYpos, realYpos + 5, realYpos + 20};
        g.drawPolyline(ixPoints, iyPoints, 3);

        // Output Line
        int[] oxPoints = {
          xpos + 35 + getSymbolWidth(nrOfBits),
          xpos + 35 + getSymbolWidth(nrOfBits),
          xpos + 40 + getSymbolWidth(nrOfBits)
        };
        int[] oyPoints = {realYpos + 20, realYpos + 5, realYpos};
        g.drawPolyline(oxPoints, oyPoints, 3);
      }
    } else if (last) {
      g.drawLine(xpos + 5, realYpos, xpos + 5, realYpos + 5);
      g.drawLine(
          xpos + 35 + getSymbolWidth(nrOfBits),
          realYpos,
          xpos + 35 + getSymbolWidth(nrOfBits),
          realYpos + 5);
    } else {
      g.drawLine(xpos + 5, realYpos, xpos + 5, realYpos + 20);
      g.drawLine(
          xpos + 35 + getSymbolWidth(nrOfBits),
          realYpos,
          xpos + 35 + getSymbolWidth(nrOfBits),
          realYpos + 20);
    }
    GraphicsUtil.switchToWidth(g, 1);
    RegisterData state = (RegisterData) painter.getData();
    if (painter.getShowState() && (state != null)) {
      /* Here we draw the bit value */
      final var val = state.value;
      final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
      var width = widthVal == null ? 8 : widthVal.getWidth();
      var xcenter = (getSymbolWidth(width) / 2) + 10;
      var value = "";
      if (val.isFullyDefined()) {
        g.setColor(Color.LIGHT_GRAY);
        value = ((1L << bitNr) & val.toLongValue()) != 0 ? "1" : "0";
      } else if (val.isUnknown()) {
        g.setColor(Color.BLUE);
        value = "?";
      } else {
        g.setColor(Color.RED);
        value = "!";
      }
      g.fillRect(xpos + xcenter + 16, realYpos + 4, 8, 16);
      if (val.isFullyDefined()) g.setColor(Color.DARK_GRAY);
      else g.setColor(Color.YELLOW);
      GraphicsUtil.drawText(
          g,
          value,
          xpos + xcenter + 20,
          realYpos + 10,
          GraphicsUtil.H_CENTER,
          GraphicsUtil.V_CENTER);
      g.setColor(Color.BLACK);
    }
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return "LogisimCounter";
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var widthVal = attrs.getValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();
    return (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC)
        ? Bounds.create(-30, -20, 30, 40)
        : Bounds.create(0, 0, getSymbolWidth(width) + 40, 110 + 20 * width);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.WIDTH || attr == StdAttr.APPEARANCE) {
      instance.recomputeBounds();
      configurePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    }
  }

  public void drawCounterClassic(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var state = (RegisterData) painter.getData();
    final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();

    // determine text to draw in label
    String a;
    String b = null;
    if (painter.getShowState()) {
      final var val = state == null ? 0 : state.value.toLongValue();
      final var str = StringUtil.toHexString(width, val);
      if (str.length() <= 4) {
        a = str;
      } else {
        int split = str.length() - 4;
        a = str.substring(0, split);
        b = str.substring(split);
      }
    } else {
      a = S.get("counterLabel");
      b = S.get("registerWidthLabel", "" + widthVal.getWidth());
    }

    // draw boundary, label
    painter.drawBounds();
    painter.drawLabel();

    // draw input and output ports
    if (b == null) {
      painter.drawPort(IN, "D", Direction.EAST);
      painter.drawPort(OUT, "Q", Direction.WEST);
    } else {
      painter.drawPort(IN);
      painter.drawPort(OUT);
    }
    g.setColor(Color.GRAY);
    painter.drawPort(LD);
    painter.drawPort(UD);
    painter.drawPort(CARRY);
    painter.drawPort(CLR, "0", Direction.SOUTH);
    painter.drawPort(EN, S.get("counterEnableLabel"), Direction.EAST);
    g.setColor(Color.BLACK);
    painter.drawClock(CK, Direction.NORTH);

    // draw contents
    if (b == null) {
      GraphicsUtil.drawText(g, a, bds.getX() + 15, bds.getY() + 4, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    } else {
      GraphicsUtil.drawText(g, a, bds.getX() + 15, bds.getY() + 3, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
      GraphicsUtil.drawText(g, b, bds.getX() + 15, bds.getY() + 15, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      drawCounterClassic(painter);
      return;
    }
    final var Xpos = painter.getLocation().getX();
    final var Ypos = painter.getLocation().getY();
    painter.drawLabel();

    drawControl(painter, Xpos, Ypos);
    final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();
    for (var bit = 0; bit < width; bit++) {
      drawDataBlock(painter, Xpos, Ypos + 110, bit, width);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    var data = (RegisterData) state.getData();
    if (data == null) {
      data = new RegisterData(state.getAttributeValue(StdAttr.WIDTH));
      state.setData(data);
    }

    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
    final var max = new BigInteger(Long.toUnsignedString(state.getAttributeValue(ATTR_MAX)));
    final var clock = state.getPortValue(CK);
    final var triggered = data.updateClock(clock, triggerType);

    Value newValue;
    boolean carry;
    if (state.getPortValue(CLR) == Value.TRUE) {
      newValue = Value.createKnown(dataWidth, 0);
      carry = false;
    } else {
      final var ld = state.getPortValue(LD) == Value.TRUE;
      final var en = state.getPortValue(EN) != Value.FALSE;
      final var UpCount = state.getPortValue(UD) != Value.FALSE;
      final var oldVal = data.value;
      final var oldValue = new BigInteger(Long.toUnsignedString(oldVal.toLongValue()));
      final var loadValue = new BigInteger(Long.toUnsignedString(state.getPortValue(IN).toLongValue()));
      BigInteger newVal;
      if (!triggered) {
        newVal = new BigInteger(Long.toUnsignedString(oldVal.toLongValue()));
      } else if (ld) {
        newVal = loadValue;
        if (newVal.compareTo(max) > 0)
          newVal = newVal.and(max);
      } else if (!oldVal.isFullyDefined()) {
        newVal = null;
      } else if (en) {
        BigInteger goal = (UpCount) ? max : BigInteger.ZERO;
        if (oldValue.compareTo(goal) == 0) {
          Object onGoal = state.getAttributeValue(ATTR_ON_GOAL);
          if (onGoal == ON_GOAL_WRAP) {
            newVal = (UpCount) ? BigInteger.ZERO : max;
          } else if (onGoal == ON_GOAL_STAY) {
            newVal = oldValue;
          } else if (onGoal == ON_GOAL_LOAD) {
            newVal = loadValue;
            if (newVal.compareTo(max) > 0) newVal = newVal.and(max);
          } else if (onGoal == ON_GOAL_CONT) {
            newVal = (UpCount) ? oldValue.add(BigInteger.ONE) : oldValue.subtract(BigInteger.ONE);
          } else {
            logger.error("Invalid goal attribute {}", onGoal);
            newVal = ld ? max : BigInteger.ZERO;
          }
        } else {
          newVal = UpCount ? oldValue.add(BigInteger.ONE) : oldValue.subtract(BigInteger.ONE);
        }
      } else {
        newVal = oldValue;
      }
      newValue = newVal == null ? Value.createError(dataWidth) : Value.createKnown(dataWidth, newVal.longValue());
      final var compVal = (UpCount) ? max : BigInteger.ZERO;
      carry = newVal.compareTo(compVal) == 0;
      /*
       * I would want this if I were worried about the carry signal
       * outrunning the clock. But the component's delay should be enough
       * to take care of it. if (carry) { if (triggerType ==
       * StdAttr.TRIG_FALLING) { carry = clock == Value.TRUE; } else {
       * carry = clock == Value.FALSE; } }
       */
    }

    data.value = newValue;
    state.setPort(OUT, newValue, DELAY);
    state.setPort(CARRY, carry ? Value.TRUE : Value.FALSE, DELAY);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {CK};
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new CounterShape(x, y, path);
  }
}
