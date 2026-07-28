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

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
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
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;

public class TwoPinLed extends InstanceFactory implements DynamicElementProvider {

  public static final String _ID = "TWOPINLED";

  public static final int ANODE = 0;
  public static final int CATHODE = 1;

  public static final int STATE_OFF = 0;
  public static final int STATE_FORWARD = 1;
  public static final int STATE_REVERSE = 2;

  public static final Attribute<Boolean> ATTR_BIPOLAR = Attributes.forBoolean("bipolar", S.getter("twoPinLedBipolar"));
  public static final Attribute<Color> ATTR_BIPOLAR_REVERSE_COLOR = Attributes.forColor("reversecolor",
      S.getter("twoPinLedReverseColor"));

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return BitWidth.ONE;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      final var data = (InstanceDataSingleton) state.getData();
      final var st = getState(data);
      return st != STATE_OFF ? Value.TRUE : Value.FALSE;
    }
  }

  static int getState(InstanceDataSingleton data) {
    if (data == null)
      return STATE_OFF;
    final var val = data.getValue();
    if (val instanceof Integer i)
      return i.intValue();
    if (val instanceof Boolean b)
      return b ? STATE_FORWARD : STATE_OFF;
    return STATE_OFF;
  }

  public TwoPinLed() {
    super(_ID, S.getter("twopinLEDComponent"));
    setAttributes(
        new Attribute[] {
            StdAttr.FACING,
            IoLibrary.ATTR_ON_COLOR,
            IoLibrary.ATTR_OFF_COLOR,
            ATTR_BIPOLAR,
            ATTR_BIPOLAR_REVERSE_COLOR,
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
            StdAttr.LABEL_COLOR,
            StdAttr.LABEL_VISIBILITY
        },
        new Object[] {
            Direction.WEST,
            new Color(240, 0, 0),
            Color.DARK_GRAY,
            Boolean.FALSE,
            new Color(0, 240, 0),
            "",
            Direction.EAST,
            StdAttr.DEFAULT_LABEL_FONT,
            StdAttr.DEFAULT_LABEL_COLOR,
            true
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new TwoPinLedIcon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstanceLogger(Logger.class);
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var ports = new Port[2];

    if (facing == Direction.WEST) {
      ports[ANODE] = new Port(-10, 0, Port.INPUT, 1);
      ports[CATHODE] = new Port(10, 0, Port.INPUT, 1);
    } else if (facing == Direction.EAST) {
      ports[ANODE] = new Port(10, 0, Port.INPUT, 1);
      ports[CATHODE] = new Port(-10, 0, Port.INPUT, 1);
    } else if (facing == Direction.NORTH) {
      ports[ANODE] = new Port(0, 10, Port.INPUT, 1);
      ports[CATHODE] = new Port(0, -10, Port.INPUT, 1);
    } else {
      ports[ANODE] = new Port(0, -10, Port.INPUT, 1);
      ports[CATHODE] = new Port(0, 10, Port.INPUT, 1);
    }

    ports[ANODE].setToolTip(S.getter("twoPinLedAnodeTip"));
    ports[CATHODE].setToolTip(S.getter("twoPinLedCathodeTip"));
    instance.setPorts(ports);
  }

  @Override
  public boolean activeOnHigh(AttributeSet attrs) {
    return true;
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(-10, -10, 20, 20);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 2, bds.getHeight() - 2);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var data = (InstanceDataSingleton) painter.getData();
    final var state = getState(data);
    final var bounds = painter.getBounds().expand(-1);
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var bipolar = painter.getAttributeValue(ATTR_BIPOLAR);

    final var g = painter.getGraphics();
    if (painter.getShowState()) {
      final var onColor = painter.getAttributeValue(IoLibrary.ATTR_ON_COLOR);
      final var offColor = painter.getAttributeValue(IoLibrary.ATTR_OFF_COLOR);
      Color ledColor;
      if (state == STATE_OFF) {
        ledColor = offColor;
      } else if (state == STATE_FORWARD) {
        ledColor = onColor;
      } else {
        ledColor = bipolar ? painter.getAttributeValue(ATTR_BIPOLAR_REVERSE_COLOR) : offColor;
      }
      g.setColor(ledColor);
      g.fillOval(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());
    }

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight());

    final var centerX = painter.getBounds().getX() + painter.getBounds().getWidth() / 2;
    final var centerY = painter.getBounds().getY() + painter.getBounds().getHeight() / 2;
    final var radius = painter.getBounds().getWidth() / 2;
    TwoPinLedShape.drawDiodeSymbol(g, centerX, centerY, radius, facing, Math.max(1, radius / 8));

    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var anodeVal = state.getPortValue(ANODE);
    final var cathodeVal = state.getPortValue(CATHODE);
    final var bipolar = state.getAttributeValue(ATTR_BIPOLAR) == Boolean.TRUE;
    final var forward = (anodeVal == Value.TRUE && cathodeVal == Value.FALSE);
    final var reverse = (anodeVal == Value.FALSE && cathodeVal == Value.TRUE);
    final var newState = (bipolar && reverse) ? STATE_REVERSE
        : (forward ? STATE_FORWARD : STATE_OFF);

    var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(newState));
    } else {
      data.setValue(newState);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new TwoPinLedShape(x, y, path);
  }
}
