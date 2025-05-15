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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.icons.RandomIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;

public class Random extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Random";

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      final var ret = state.getAttributeValue(StdAttr.LABEL);
      return ret != null && !ret.equals("") ? ret : null;
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.WIDTH);
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
      if (dataWidth == null) dataWidth = BitWidth.create(0);
      final var data = (StateData) state.getData();
      if (data == null) return Value.createKnown(dataWidth, 0);
      return Value.createKnown(dataWidth, data.value);
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      return true;
    }
  }

  private static class StateData extends ClockState implements InstanceData {
    private static final long MULTIPLIER = 0x5DEECE66DL;
    private static final long ADDEND = 0xBL;
    private static final long MASK = (1L << 48) - 1;

    private long initSeed;
    private long curSeed;
    private int value;
    private long resetValue;
    private Value oldReset;

    public StateData(Object seed) {
      resetValue = this.initSeed = this.curSeed = getRandomSeed(seed);
      this.value = (int) this.initSeed;
      oldReset = Value.UNKNOWN;
    }

    private void propagateReset(Value reset, Object seed) {
      if (oldReset == Value.FALSE && reset == Value.TRUE) {
        resetValue = getRandomSeed(seed);
      }
      oldReset = reset;
    }

    public void reset(Object seed) {
      this.initSeed = resetValue;
      this.curSeed = resetValue;
      this.value = (int) resetValue;
    }

    private long getRandomSeed(Object seed) {
      long retValue = seed instanceof Integer ? (Integer) seed : 0;
      if (retValue == 0) {
        // Prior to 2.7.0, this would reset to the seed at the time of
        // the StateData's creation. It seems more likely that what
        // would be intended was starting a new sequence entirely...
        retValue = (System.currentTimeMillis() ^ MULTIPLIER) & MASK;
        if (retValue == initSeed) {
          retValue = (retValue + MULTIPLIER) & MASK;
        }
      }
      return retValue;
    }

    void step() {
      long v = curSeed;
      v = (v * MULTIPLIER + ADDEND) & MASK;
      curSeed = v;
      value = (int) (v >> 12);
    }
  }

  static final Attribute<Integer> ATTR_SEED =
      Attributes.forInteger("seed", S.getter("randomSeedAttr"));
  public static final int OUT = 0;
  public static final int CK = 1;
  public static final int NXT = 2;
  public static final int RST = 3;

  public Random() {
    super(_ID, S.getter("randomComponent"), new RandomHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.WIDTH,
          ATTR_SEED,
          StdAttr.EDGE_TRIGGER,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.APPEARANCE
        },
        new Object[] {
          BitWidth.create(8),
          0,
          StdAttr.TRIG_RISING,
          "",
          StdAttr.DEFAULT_LABEL_FONT,
          AppPreferences.getDefaultAppearance()
        });
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));

    setOffsetBounds(Bounds.create(0, 0, 80, 90));
    setIcon(new RandomIcon());
    setInstanceLogger(Logger.class);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      return Bounds.create(0, 0, 40, 40);
    } else {
      return Bounds.create(0, 0, 80, 90);
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    final var bds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BASELINE);
  }

  private void updatePorts(Instance instance) {
    final var ps = new Port[4];
    if (instance.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      ps[OUT] = new Port(40, 20, Port.OUTPUT, StdAttr.WIDTH);
      ps[CK] = new Port(10, 40, Port.INPUT, 1);
      ps[NXT] = new Port(0, 30, Port.INPUT, 1);
      ps[RST] = new Port(30, 40, Port.INPUT, 1);
    } else {
      ps[OUT] = new Port(80, 80, Port.OUTPUT, StdAttr.WIDTH);
      ps[CK] = new Port(0, 50, Port.INPUT, 1);
      ps[NXT] = new Port(0, 40, Port.INPUT, 1);
      ps[RST] = new Port(0, 30, Port.INPUT, 1);
    }
    ps[OUT].setToolTip(S.getter("randomQTip"));
    ps[CK].setToolTip(S.getter("randomClockTip"));
    ps[NXT].setToolTip(S.getter("randomNextTip"));
    ps[RST].setToolTip(S.getter("randomResetTip"));
    instance.setPorts(ps);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.APPEARANCE) {
      instance.recomputeBounds();
      updatePorts(instance);
    }
  }

  private void drawControl(InstancePainter painter, int xpos, int ypos, int nrOfBits) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(xpos + 10, ypos, xpos + 70, ypos);
    g.drawLine(xpos + 10, ypos, xpos + 10, ypos + 60);
    g.drawLine(xpos + 70, ypos, xpos + 70, ypos + 60);
    g.drawLine(xpos + 10, ypos + 60, xpos + 20, ypos + 60);
    g.drawLine(xpos + 60, ypos + 60, xpos + 70, ypos + 60);
    g.drawLine(xpos + 20, ypos + 60, xpos + 20, ypos + 70);
    g.drawLine(xpos + 60, ypos + 60, xpos + 60, ypos + 70);
    final var Name = "RNG" + nrOfBits;
    GraphicsUtil.drawText(
        g, Name, xpos + 40, ypos + 8, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER);
    g.drawLine(xpos, ypos + 30, xpos + 10, ypos + 30);
    GraphicsUtil.drawText(g, "R", xpos + 20, ypos + 30, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(RST);
    g.drawLine(xpos, ypos + 40, xpos + 10, ypos + 40);
    GraphicsUtil.drawText(
        g, "EN", xpos + 20, ypos + 40, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(NXT);
    painter.drawClockSymbol(xpos + 10, ypos + 50);
    GraphicsUtil.switchToWidth(g, 2);
    if (painter.getAttributeValue(StdAttr.EDGE_TRIGGER).equals(StdAttr.TRIG_FALLING)) {
      g.drawOval(xpos, ypos + 45, 10, 10);
    } else {
      g.drawLine(xpos, ypos + 50, xpos + 10, ypos + 50);
    }
    painter.drawPort(CK);
    GraphicsUtil.switchToWidth(g, 1);
  }

  private void drawData(InstancePainter painter, int xpos, int ypos, int nrOfBits, int value) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(xpos, ypos, 80, 20);
    if (painter.getShowState()) {
      final var str = StringUtil.toHexString(nrOfBits, value);
      GraphicsUtil.drawCenteredText(g, str, xpos + 40, ypos + 10);
    }
    painter.drawPort(OUT);
    GraphicsUtil.switchToWidth(g, 1);
  }

  private void paintInstanceClassic(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var state = (StateData) painter.getData();
    final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();

    // determine text to draw in label
    String a;
    String b = null;
    if (painter.getShowState()) {
      int val = state == null ? 0 : state.value;
      final var str = StringUtil.toHexString(width, val);
      if (str.length() <= 4) {
        a = str;
      } else {
        final var split = str.length() - 4;
        a = str.substring(0, split);
        b = str.substring(split);
      }
    } else {
      a = S.get("randomLabel");
      b = S.get("randomWidthLabel", "" + widthVal.getWidth());
    }

    // draw boundary, label
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    g.setColor(painter.getAttributeValue(StdAttr.LABEL_COLOR));
    painter.drawLabel();

    // draw input and output ports
    if (b == null) painter.drawPort(OUT, "Q", Direction.WEST);
    else painter.drawPort(OUT);
    g.setColor(new Color(AppPreferences.COMPONENT_SECONDARY_COLOR.get()));
    painter.drawPort(RST, "0", Direction.SOUTH);
    painter.drawPort(NXT, S.get("memEnableLabel"), Direction.EAST);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawClock(CK, Direction.NORTH);

    // draw contents
    if (b == null) {
      GraphicsUtil.drawText(
          g, a, bds.getX() + 20, bds.getY() + 4, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    } else {
      GraphicsUtil.drawText(
          g, a, bds.getX() + 20, bds.getY() + 3, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
      GraphicsUtil.drawText(
          g, b, bds.getX() + 20, bds.getY() + 15, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
    }
  }

  private void paintInstanceEvolution(InstancePainter painter) {
    final var bds = painter.getBounds();
    final var x = bds.getX();
    final var y = bds.getY();
    final var state = (StateData) painter.getData();
    final var val = state == null ? 0 : state.value;
    final var widthVal = painter.getAttributeValue(StdAttr.WIDTH);
    final var width = widthVal == null ? 8 : widthVal.getWidth();

    painter.drawLabel();
    drawControl(painter, x, y, width);
    drawData(painter, x, y + 70, width, val);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return "LogisimRNG";
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC)
      paintInstanceClassic(painter);
    else paintInstanceEvolution(painter);
  }

  @Override
  public void propagate(InstanceState state) {
    var data = (StateData) state.getData();
    if (data == null) {
      data = new StateData(state.getAttributeValue(ATTR_SEED));
      state.setData(data);
    }

    final var dataWidth = state.getAttributeValue(StdAttr.WIDTH);
    Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
    final var triggered = data.updateClock(state.getPortValue(CK), triggerType);

    data.propagateReset(state.getPortValue(RST), state.getAttributeValue(ATTR_SEED));
    if (state.getPortValue(RST) == Value.TRUE) {
      data.reset(state.getAttributeValue(ATTR_SEED));
    } else if (triggered && state.getPortValue(NXT) != Value.FALSE) {
      data.step();
    }

    state.setPort(OUT, Value.createKnown(dataWidth, data.value), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {CK};
  }
}
