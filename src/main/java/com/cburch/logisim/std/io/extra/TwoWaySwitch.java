/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io.extra;

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
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class TwoWaySwitch extends InstanceFactory {
  public static final String _ID = "TwoWaySwitch";

  public static final AttributeOption MODE_1IN_2OUT =
      new AttributeOption("1in2out", S.getter("mode1In2Out"));
  public static final AttributeOption MODE_2IN_1OUT =
      new AttributeOption("2in1out", S.getter("mode2In1Out"));

  public static final Attribute<AttributeOption> ATTR_MODE =
      Attributes.forOption(
          "mode",
          S.getter("switchModeAttr"),
          new AttributeOption[] {MODE_1IN_2OUT, MODE_2IN_1OUT});

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.WIDTH);
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      int port = (option instanceof Integer) ? (Integer) option : 1;
      return state.getPortValue(port);
    }
  }

  public static class Poker extends InstancePoker {
    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
      boolean current = data != null && (Boolean) data.getValue();
      setActive(state, !current);
    }

    private void setActive(InstanceState state, boolean active) {
      InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
      if (data == null) {
        state.setData(new InstanceDataSingleton(active));
      } else {
        data.setValue(active);
      }
      state.getInstance().fireInvalidated();
    }
  }

  public TwoWaySwitch() {
    super(_ID, S.getter("twoWaySwitch"));
    setAttributes(
        new Attribute<?>[] {
          ATTR_MODE,
          StdAttr.FACING,
          StdAttr.WIDTH,
          IoLibrary.ATTR_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT
        },
        new Object[] {
          MODE_1IN_2OUT,
          Direction.EAST,
          BitWidth.ONE,
          Color.WHITE,
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new TwoWaySwitchIcon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstancePoker(Poker.class);
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(-20, -15, 40, 30).rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == ATTR_MODE) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
      updatePorts(instance);
    } else if (attr == StdAttr.WIDTH) {
      updatePorts(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paint(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    paint(painter, false);
  }

  private void paint(InstancePainter painter, boolean ghost) {
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    AttributeOption mode = painter.getAttributeValue(ATTR_MODE);
    boolean is1In2Out = (mode == MODE_1IN_2OUT);

    boolean active = false;
    if (painter.getShowState() && !ghost) {
      InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
      active = data != null && (Boolean) data.getValue();
    }

    Graphics2D g = (Graphics2D) painter.getGraphics().create();

    int cx = painter.getLocation().getX();
    int cy = painter.getLocation().getY();

    g.translate(cx, cy);
    if (facing == Direction.WEST) {
      g.rotate(Math.PI);
    } else if (facing == Direction.SOUTH) {
      g.rotate(Math.PI / 2);
    } else if (facing == Direction.NORTH) {
      g.rotate(-Math.PI / 2);
    }

    // Body
    Color bodyColor = painter.getAttributeValue(IoLibrary.ATTR_COLOR);
    if (!painter.shouldDrawColor()) {
      int hue = (bodyColor.getRed() + bodyColor.getGreen() + bodyColor.getBlue()) / 3;
      bodyColor = new Color(hue, hue, hue);
    }

    // Fill body only when a custom color is set (non-WHITE); WHITE means transparent (default)
    if (!ghost && !Color.WHITE.equals(bodyColor)) {
      g.setColor(bodyColor);
      g.fillRoundRect(-20, -15, 40, 30, 6, 6);
    }
    g.setColor(ghost ? Color.GRAY : new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.setStroke(new BasicStroke(1.0f));
    g.drawRoundRect(-20, -15, 40, 30, 6, 6);

    int cxCommon, cxPair, cyCommon, cyPair1, cyPair2;
    if (is1In2Out) {
      cxCommon = -17;
      cxPair   =  13;
    } else {
      cxCommon =  13;
      cxPair   = -17;
    }
    cyCommon = -2;
    cyPair1  = -12;
    cyPair2  =   8;

    g.fillOval(cxCommon, cyCommon, 4, 4);
    g.fillOval(cxPair, cyPair1, 4, 4);
    g.fillOval(cxPair, cyPair2, 4, 4);

    int leverStartX = cxCommon + 2;
    int leverStartY = cyCommon + 2;
    int leverEndX   = cxPair + 2;
    int leverEndY   = (active ? cyPair2 : cyPair1) + 2;

    g.setStroke(new BasicStroke(2.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
    g.drawLine(leverStartX, leverStartY, leverEndX, leverEndY);

    g.dispose();

    if (!ghost) {
      painter.drawLabel();
      painter.drawPorts();
    }
  }

  @Override
  public void propagate(InstanceState state) {
    InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
    boolean toSecond = data != null && (Boolean) data.getValue();
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    AttributeOption mode = state.getAttributeValue(ATTR_MODE);
    Value unknown = Value.createUnknown(width);

    if (mode == MODE_1IN_2OUT) {
      Value in = state.getPortValue(0);
      state.setPort(1, toSecond ? unknown : in, 1);
      state.setPort(2, toSecond ? in : unknown, 1);
    } else {
      Value selectedInput = state.getPortValue(toSecond ? 2 : 1);
      state.setPort(0, selectedInput, 1);
    }
  }

  private void updatePorts(Instance instance) {
    BitWidth b = instance.getAttributeValue(StdAttr.WIDTH);
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    AttributeOption mode = instance.getAttributeValue(ATTR_MODE);
    boolean is1In2Out = (mode == MODE_1IN_2OUT);

    int commonX = is1In2Out ? -20 : 20;
    int pairX   = is1In2Out ?  20 : -20;
    String commonDir = is1In2Out ? Port.INPUT  : Port.OUTPUT;
    String pairDir   = is1In2Out ? Port.OUTPUT : Port.INPUT;

    Port[] ps = new Port[3];
    if (dir == Direction.EAST) {
      ps[0] = new Port(commonX, 0, commonDir, b);
      ps[1] = new Port(pairX, -10, pairDir, b);
      ps[2] = new Port(pairX, 10, pairDir, b);
    } else if (dir == Direction.WEST) {
      ps[0] = new Port(-commonX, 0, commonDir, b);
      ps[1] = new Port(-pairX, -10, pairDir, b);
      ps[2] = new Port(-pairX, 10, pairDir, b);
    } else if (dir == Direction.SOUTH) {
      ps[0] = new Port(0, -commonX, commonDir, b);
      ps[1] = new Port(-10, -pairX, pairDir, b);
      ps[2] = new Port(10, -pairX, pairDir, b);
    } else { // NORTH
      ps[0] = new Port(0, commonX, commonDir, b);
      ps[1] = new Port(-10, pairX, pairDir, b);
      ps[2] = new Port(10, pairX, pairDir, b);
    }

    if (is1In2Out) {
      ps[0].setToolTip(S.getter("pinInputName"));
      ps[1].setToolTip(S.getter("pinOutputName"));
      ps[2].setToolTip(S.getter("pinOutputName"));
    } else {
      ps[0].setToolTip(S.getter("pinOutputName"));
      ps[1].setToolTip(S.getter("pinInputName"));
      ps[2].setToolTip(S.getter("pinInputName"));
    }
    instance.setPorts(ps);
  }
}

