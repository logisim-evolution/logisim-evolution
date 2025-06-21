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
import com.cburch.logisim.gui.icons.ShifterIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.IntegerConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;

public class ShiftRegister extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Shift Register";

  static final Attribute<Integer> ATTR_LENGTH =
      Attributes.forIntegerRange("length", S.getter("shiftRegLengthAttr"), 1, 64);
  static final Attribute<Boolean> ATTR_LOAD =
      Attributes.forBoolean("parallel", S.getter("shiftRegParallelAttr"));

  static final int IN = 0;
  static final int SH = 1;
  public static final int CK = 2;
  static final int CLR = 3;
  static final int OUT = 4;
  static final int LD = 5;
  static final int symbolWidth = 100;

  public ShiftRegister() {
    super(_ID, S.getter("shiftRegisterComponent"), new ShiftRegisterHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.WIDTH,
          ATTR_LENGTH,
          ATTR_LOAD,
          StdAttr.EDGE_TRIGGER,
          StdAttr.LABEL,
          StdAttr.LABEL_FONT,
          StdAttr.APPEARANCE
        },
        new Object[] {
          BitWidth.ONE,
          8,
          Boolean.TRUE,
          StdAttr.TRIG_RISING,
          "",
          StdAttr.DEFAULT_LABEL_FONT,
          AppPreferences.getDefaultAppearance()
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new IntegerConfigurator(ATTR_LENGTH, 1, 64, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));

    setIcon(new ShifterIcon());
    setInstanceLogger(ShiftRegisterLogger.class);
    setInstancePoker(ShiftRegisterPoker.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    configurePorts(instance);
    instance.addAttributeListener();
  }

  private void configurePorts(Instance instance) {
    final var widthObj = instance.getAttributeValue(StdAttr.WIDTH);
    final var width = widthObj.getWidth();
    final var parallelObj = instance.getAttributeValue(ATTR_LOAD);
    final var bds = instance.getBounds();
    Port[] ps;
    final var lenObj = instance.getAttributeValue(ATTR_LENGTH);
    final var len = lenObj == null ? 8 : lenObj;
    if (instance.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      if (parallelObj == null || parallelObj) {
        ps = new Port[6 + 2 * len];
        ps[LD] = new Port(10, -20, Port.INPUT, 1);
        ps[LD].setToolTip(S.getter("shiftRegLoadTip"));
        for (var i = 0; i < len; i++) {
          ps[6 + 2 * i] = new Port(20 + 10 * i, -20, Port.INPUT, width);
          ps[6 + 2 * i + 1] = new Port(20 + 10 * i, 20, Port.OUTPUT, width);
        }
      } else {
        ps = new Port[5];
      }
      ps[OUT] = new Port(bds.getWidth(), 0, Port.OUTPUT, width);
      ps[IN] = new Port(0, 0, Port.INPUT, width);
      ps[SH] = new Port(0, -10, Port.INPUT, 1);
      ps[CK] = new Port(0, 10, Port.INPUT, 1);
      ps[CLR] = new Port(10, 20, Port.INPUT, 1);
    } else {
      if (parallelObj == null || parallelObj) {
        ps = new Port[6 + 2 * len - 1];
        ps[LD] = new Port(0, 30, Port.INPUT, 1);
        ps[LD].setToolTip(S.getter("shiftRegLoadTip"));
        for (var i = 0; i < len; i++) {
          ps[6 + 2 * i] = new Port(0, 90 + i * 20, Port.INPUT, width);
          if (i < (len - 1))
            ps[6 + 2 * i + 1] = new Port(symbolWidth + 20, 90 + i * 20, Port.OUTPUT, width);
        }
      } else {
        ps = new Port[5];
      }
      ps[OUT] = new Port(symbolWidth + 20, 70 + len * 20, Port.OUTPUT, width);
      ps[IN] = new Port(0, 80, Port.INPUT, width);
      ps[SH] = new Port(0, 40, Port.INPUT, 1);
      ps[CK] = new Port(0, 50, Port.INPUT, 1);
      ps[CLR] = new Port(0, 20, Port.INPUT, 1);
    }
    ps[OUT].setToolTip(S.getter("shiftRegOutTip"));
    ps[SH].setToolTip(S.getter("shiftRegShiftTip"));
    ps[IN].setToolTip(S.getter("shiftRegInTip"));
    ps[CK].setToolTip(S.getter("shiftRegClockTip"));
    ps[CLR].setToolTip(S.getter("shiftRegClearTip"));
    instance.setPorts(ps);
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BASELINE);
  }

  private void drawControl(
      InstancePainter painter,
      int xpos,
      int ypos,
      int nr_of_stages,
      int nr_of_bits,
      boolean has_load,
      boolean active_low_clock) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    final var blockwidth = symbolWidth;
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawLine(xpos + 10, ypos, xpos + blockwidth + 10, ypos);
    g.drawLine(xpos + 10, ypos, xpos + 10, ypos + 60);
    g.drawLine(xpos + blockwidth + 10, ypos, xpos + blockwidth + 10, ypos + 60);
    g.drawLine(xpos + 10, ypos + 60, xpos + 20, ypos + 60);
    g.drawLine(xpos + blockwidth, ypos + 60, xpos + blockwidth + 10, ypos + 60);
    g.drawLine(xpos + 20, ypos + 60, xpos + 20, ypos + 70);
    g.drawLine(xpos + blockwidth, ypos + 60, xpos + blockwidth, ypos + 70);
    if (nr_of_bits > 1) {
      g.drawLine(xpos + blockwidth + 10, ypos + 5, xpos + blockwidth + 15, ypos + 5);
      g.drawLine(xpos + blockwidth + 15, ypos + 5, xpos + blockwidth + 15, ypos + 65);
      g.drawLine(xpos + blockwidth + 5, ypos + 65, xpos + blockwidth + 15, ypos + 65);
      g.drawLine(xpos + blockwidth + 5, ypos + 65, xpos + blockwidth + 5, ypos + 70);
      if (nr_of_bits > 2) {
        g.drawLine(xpos + blockwidth + 15, ypos + 10, xpos + blockwidth + 20, ypos + 10);
        g.drawLine(xpos + blockwidth + 20, ypos + 10, xpos + blockwidth + 20, ypos + 70);
        g.drawLine(xpos + blockwidth + 10, ypos + 70, xpos + blockwidth + 20, ypos + 70);
      }
    }
    final var Identifier = "SRG" + nr_of_stages;
    GraphicsUtil.drawCenteredText(g, Identifier, xpos + (symbolWidth / 2) + 10, ypos + 5);
    /* Draw the clock input */
    painter.drawClockSymbol(xpos + 10, ypos + 50);
    GraphicsUtil.switchToWidth(g, 2);
    if (active_low_clock) g.drawOval(xpos, ypos + 45, 10, 10);
    else g.drawLine(xpos, ypos + 50, xpos + 10, ypos + 50);
    painter.drawPort(CK);
    final var cntrl = "1\u2192/C3";
    GraphicsUtil.drawText(
        g, cntrl, xpos + 20, ypos + 50, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    /* draw shift input */
    g.drawLine(xpos, ypos + 40, xpos + 10, ypos + 40);
    GraphicsUtil.drawText(
        g, "M1 [shift]", xpos + 20, ypos + 40, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(SH);
    /* draw load input */
    if (has_load) {
      g.drawLine(xpos, ypos + 30, xpos + 10, ypos + 30);
      GraphicsUtil.drawText(
          g, "M2 [load]", xpos + 20, ypos + 30, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
      painter.drawPort(LD);
    }
    /* draw reset */
    g.drawLine(xpos, ypos + 20, xpos + 10, ypos + 20);
    GraphicsUtil.drawText(g, "R", xpos + 20, ypos + 20, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    painter.drawPort(CLR);
    GraphicsUtil.switchToWidth(g, 1);
  }

  private void drawDataBlock(
      InstancePainter painter,
      int xpos,
      int ypos,
      int nrOfStages,
      int nrOfBits,
      int currentStage,
      Value data_value,
      boolean hasLoad) {
    var realYpos = ypos + 70 + currentStage * 20;
    if (currentStage > 0) realYpos += 10;
    final var realXpos = xpos + 10;
    final var dataWidth = (nrOfBits == 1) ? 2 : 5;
    final var lineFix = (nrOfBits == 1) ? 1 : 2;
    final var componentColor = new Color(AppPreferences.COMPONENT_COLOR.get());
    final var inOutputConectionColor = (nrOfBits == 1) ? componentColor : Value.multiColor;
    int height = (currentStage == 0) ? 30 : 20;
    final var lastBlock = (currentStage == (nrOfStages - 1));
    final var blockWidth = symbolWidth;
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(realXpos, realYpos, blockWidth, height);
    if (nrOfBits > 1) {
      g.drawLine(realXpos + blockWidth, realYpos + 5, realXpos + blockWidth + 5, realYpos + 5);
      g.drawLine(
          realXpos + blockWidth + 5,
          realYpos + 5,
          realXpos + blockWidth + 5,
          realYpos + height + 5);
      if (lastBlock) {
        g.drawLine(
            realXpos + 5, realYpos + height + 5, realXpos + blockWidth + 5, realYpos + height + 5);
        g.drawLine(realXpos + 5, realYpos + height, realXpos + 5, realYpos + height + 5);
      }
      if (nrOfBits > 2) {
        g.drawLine(
            realXpos + blockWidth + 5, realYpos + 10, realXpos + blockWidth + 10, realYpos + 10);
        g.drawLine(
            realXpos + blockWidth + 10,
            realYpos + 10,
            realXpos + blockWidth + 10,
            realYpos + height + 10);
        if (lastBlock) {
          g.drawLine(
              realXpos + 10,
              realYpos + height + 10,
              realXpos + blockWidth + 10,
              realYpos + height + 10);
          g.drawLine(realXpos + 10, realYpos + height + 5, realXpos + 10, realYpos + height + 10);
        }
      }
    }
    /* Draw the Inputs */
    if (currentStage == 0 || hasLoad) {
      GraphicsUtil.switchToWidth(g, dataWidth);
      g.setColor(inOutputConectionColor);
      g.drawLine(realXpos - 10, realYpos + 10, realXpos - lineFix, realYpos + 10);
      g.setColor(componentColor);
      if (currentStage == 0) {
        painter.drawPort(IN);
        GraphicsUtil.drawText(
            g, "1,3D", realXpos + 1, realYpos + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
        if (hasLoad) {
          g.setColor(inOutputConectionColor);
          g.drawLine(realXpos - 10, realYpos + 20, realXpos - lineFix, realYpos + 20);
          g.setColor(componentColor);
          GraphicsUtil.drawText(
              g, "2,3D", realXpos + 1, realYpos + 20, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
        }
      } else {
        GraphicsUtil.drawText(
            g, "2,3D", realXpos + 1, realYpos + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
      }

      if (hasLoad) painter.drawPort(6 + 2 * currentStage);
      GraphicsUtil.switchToWidth(g, 1);
    }
    GraphicsUtil.switchToWidth(g, 1);
    /* Draw the outputs */
    GraphicsUtil.switchToWidth(g, dataWidth);
    g.setColor(inOutputConectionColor);
    if (hasLoad || lastBlock) {
      if (currentStage == 0) {
        g.drawLine(
            realXpos + blockWidth + lineFix,
            realYpos + 20,
            realXpos + blockWidth + 10,
            realYpos + 20);
      } else {
        g.drawLine(
            realXpos + blockWidth + lineFix,
            realYpos + 10,
            realXpos + blockWidth + 10,
            realYpos + 10);
      }
    }
    if (lastBlock) {
      painter.drawPort(OUT);
    } else if (hasLoad) {
      painter.drawPort(6 + 2 * currentStage + 1);
    }
    GraphicsUtil.switchToWidth(g, 1);

    /* Draw stage value */
    g.setColor(componentColor);
    if (painter.getShowState() && (data_value != null)) {
      if (data_value.isFullyDefined()) g.setColor(Color.LIGHT_GRAY);
      else if (data_value.isErrorValue()) g.setColor(Color.RED);
      else g.setColor(Color.BLUE);
      final var yoff = (currentStage == 0) ? 10 : 0;
      final var len = (nrOfBits + 3) / 4;
      final var boxXpos = ((blockWidth - 30) / 2 + 30) - (len * 4);
      g.fillRect(realXpos + boxXpos, realYpos + yoff + 2, 2 + len * 8, 16);
      String value;
      if (data_value.isFullyDefined()) {
        g.setColor(Color.DARK_GRAY);
        value = StringUtil.toHexString(nrOfBits, data_value.toLongValue());
      } else {
        g.setColor(Color.YELLOW);
        value = (data_value.isUnknown()) ? "?" : "!";
      }
      GraphicsUtil.drawText(
          g,
          value,
          realXpos + boxXpos + 1,
          realYpos + yoff + 10,
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
      g.setColor(componentColor);
    }
  }

  private ShiftRegisterData getData(InstanceState state) {
    final var width = state.getAttributeValue(StdAttr.WIDTH);
    final var lenObj = state.getAttributeValue(ATTR_LENGTH);
    final var length = lenObj == null ? 8 : lenObj;
    var data = (ShiftRegisterData) state.getData();
    if (data == null) {
      data = new ShiftRegisterData(width, length);
      state.setData(data);
    } else {
      data.setDimensions(width, length);
    }
    return data;
  }

  private void updateData(Instance instance) {
    final var comp = instance.getComponent().getInstanceStateImpl();
    if (comp == null) return;
    final var circuitState = comp.getCircuitState();
    if (circuitState == null) return;
    final var state = circuitState.getInstanceState(instance);
    if (state == null) return;
    final var data = (ShiftRegisterData) state.getData();
    if (data == null) return;
    final var lenObj = state.getAttributeValue(ATTR_LENGTH);
    data.setDimensions(state.getAttributeValue(StdAttr.WIDTH), lenObj == null ? 8 : lenObj);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      Object parallel = attrs.getValue(ATTR_LOAD);
      if (parallel == null || (Boolean) parallel) {
        return Bounds.create(0, -20, 20 + 10 * attrs.getValue(ATTR_LENGTH), 40);
      } else {
        return Bounds.create(0, -20, 30, 40);
      }
    } else {
      return Bounds.create(0, 0, symbolWidth + 20, 80 + 20 * attrs.getValue(ATTR_LENGTH));
    }
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == ATTR_LOAD
        || attr == ATTR_LENGTH
        || attr == StdAttr.WIDTH
        || attr == StdAttr.APPEARANCE) {
      instance.recomputeBounds();
      configurePorts(instance);
      updateData(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      paintInstanceClassic(painter);
    } else {
      paintInstanceEvolution(painter);
    }
  }

  private void paintInstanceEvolution(InstancePainter painter) {
    // draw boundary, label
    painter.drawLabel();
    final var xpos = painter.getLocation().getX();
    final var ypos = painter.getLocation().getY();
    final var wid = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
    final var lenObj = painter.getAttributeValue(ATTR_LENGTH);
    final var len = lenObj == null ? 8 : lenObj;
    final var parallelObj = painter.getAttributeValue(ATTR_LOAD);
    final var negEdge =
        painter.getAttributeValue(StdAttr.EDGE_TRIGGER).equals(StdAttr.TRIG_FALLING);
    drawControl(painter, xpos, ypos, len, wid, parallelObj, negEdge);
    final var data = (ShiftRegisterData) painter.getData();

    // In the case data is null we assume that the different value are null. This allow the user to
    // instantiate the shift register without simulation mode
    if (data == null) {
      for (var stage = 0; stage < len; stage++) {
        drawDataBlock(painter, xpos, ypos, len, wid, stage, null, parallelObj);
      }
    } else {
      for (var stage = 0; stage < len; stage++)
        drawDataBlock(painter, xpos, ypos, len, wid, stage, data.get(len - stage - 1), parallelObj);
    }
  }

  private void paintInstanceClassic(InstancePainter painter) {
    // draw boundary, label
    painter.drawBounds();
    painter.drawLabel();

    // draw state
    boolean parallel = painter.getAttributeValue(ATTR_LOAD);
    if (parallel) {
      final var widObj = painter.getAttributeValue(StdAttr.WIDTH);
      final var wid = widObj.getWidth();
      final var lenObj = painter.getAttributeValue(ATTR_LENGTH);
      final var len = lenObj == null ? 8 : lenObj;
      if (painter.getShowState()) {
        if (wid <= 4) {
          final var data = getData(painter);
          final var bds = painter.getBounds();
          var x = bds.getX() + 20;
          var y = bds.getY();
          Object label = painter.getAttributeValue(StdAttr.LABEL);
          if (label == null || label.equals("")) {
            y += bds.getHeight() / 2;
          } else {
            y += 3 * bds.getHeight() / 4;
          }
          final var g = painter.getGraphics();
          for (var i = 0; i < len; i++) {
            if (data != null && data.get(len - 1 - i) != null) {
              final var s = data.get(len - 1 - i).toHexString();
              GraphicsUtil.drawCenteredText(g, s, x, y);
            }
            x += 10;
          }
        }
      } else {
        final var bds = painter.getBounds();
        final var x = bds.getX() + bds.getWidth() / 2;
        final var y = bds.getY();
        final var h = bds.getHeight();
        final var g = painter.getGraphics();
        Object label = painter.getAttributeValue(StdAttr.LABEL);
        if (label == null || label.equals("")) {
          final var a = S.get("shiftRegisterLabel1");
          GraphicsUtil.drawCenteredText(g, a, x, y + h / 4);
        }
        var b = S.get("shiftRegisterLabel2", "" + len, "" + wid);
        GraphicsUtil.drawCenteredText(g, b, x, y + 3 * h / 4);
      }
    }

    // draw input and output ports
    final var ports = painter.getInstance().getPorts().size();
    for (var i = 0; i < ports; i++) {
      if (i != CK) painter.drawPort(i);
    }
    painter.drawClock(CK, Direction.EAST);
  }

  @Override
  public void propagate(InstanceState state) {
    Object triggerType = state.getAttributeValue(StdAttr.EDGE_TRIGGER);
    final var parallel = state.getAttributeValue(ATTR_LOAD);
    ShiftRegisterData data = getData(state);
    final var len = data.getLength();

    final var triggered = data.updateClock(state.getPortValue(CK), triggerType);
    if (state.getPortValue(CLR) == Value.TRUE) {
      data.clear();
    } else if (triggered) {
      if (parallel && state.getPortValue(LD) == Value.TRUE) {
        data.clear();
        for (int i = len - 1; i >= 0; i--) {
          data.push(state.getPortValue(6 + 2 * i));
        }
      } else if (state.getPortValue(SH) != Value.FALSE) {
        data.push(state.getPortValue(IN));
      }
    }

    state.setPort(OUT, data.get(0), 4);
    if (parallel) {
      final var nrOfBits =  (state.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) ? len : len - 1;
      for (var i = 0; i < nrOfBits; i++) {
        state.setPort(6 + 2 * i + 1, data.get(len - 1 - i), 4);
      }
    }
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
  public String getHDLName(AttributeSet attrs) {
    final var nrOfStages = attrs.getValue(ShiftRegister.ATTR_LENGTH);
    final var extension = (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) ? "Classic" : "Evolution";
    return String.format("SHIFTREG_%d_%s", nrOfStages, extension);
  }

}
