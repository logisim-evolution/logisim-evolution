/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.LedIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

public class RgbLed extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "RGBLED";

  public static class Logger extends InstanceLogger {
    static final BitWidth BITWIDTH = BitWidth.create(3);

    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public BitWidth getBitWidth(InstanceState state, Object option) {
      return BITWIDTH;
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      final var data = (InstanceDataSingleton) state.getData();
      if (data == null)
        return Value.createUnknown(BITWIDTH);
      else
        return Value.createKnown(BITWIDTH, (Integer) data.getValue());
    }
  }

  public static List<String> getLabels() {
    final var labelNames = new ArrayList<String>();
    for (var i = 0; i < 3; i++) labelNames.add("");
    labelNames.set(RED, "RED");
    labelNames.set(GREEN, "GREEN");
    labelNames.set(BLUE, "BLUE");
    return labelNames;
  }

  public static String getLabel(int id) {
    if (id < 0 || id > getLabels().size()) return "Undefined";
    return getLabels().get(id);
  }

  public static final int RED = 0;
  public static final int GREEN = 1;
  public static final int BLUE = 2;

  public RgbLed() {
    super(_ID, S.getter("RGBledComponent"), new AbstractSimpleIoHdlGeneratorFactory(false), true);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          IoLibrary.ATTR_ACTIVE,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.WEST,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(0, 3, 0, null, getLabels(), null)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new LedIcon(true));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstanceLogger(Logger.class);
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var ps = new Port[3];
    int cx = 0;
    int cy = 0;
    int dx = 0;
    int dy = 0;
    if (facing == Direction.NORTH) {
      cy = 10;
      dx = 10;
    } else if (facing == Direction.EAST) {
      cx = -10;
      dy = 10;
    } else if (facing == Direction.SOUTH) {
      cy = -10;
      dx = -10;
    } else {
      cx = 10;
      dy = -10;
    }
    ps[RED] = new Port(0, 0, Port.INPUT, 1);
    ps[GREEN] = new Port(cx + dx, cy + dy, Port.INPUT, 1);
    ps[BLUE] = new Port(cx - dx, cy - dy, Port.INPUT, 1);
    ps[RED].setToolTip(S.getter("RED"));
    ps[GREEN].setToolTip(S.getter("GREEN"));
    ps[BLUE].setToolTip(S.getter("BLUE"));
    instance.setPorts(ps);
  }

  @Override
  public boolean activeOnHigh(AttributeSet attrs) {
    return attrs.getValue(IoLibrary.ATTR_ACTIVE);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0, 0);
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
    int summ = (data == null ? 0 : (Integer) data.getValue());
    final var bds = painter.getBounds().expand(-1);

    final var g = painter.getGraphics();
    if (painter.getShowState()) {
      final var activ = painter.getAttributeValue(IoLibrary.ATTR_ACTIVE);
      final var mask = activ ? 0 : 7;
      summ ^= mask;
      final var red = ((summ >> RED) & 1) * 0xFF;
      final var green = ((summ >> GREEN) & 1) * 0xFF;
      final var blue = ((summ >> BLUE) & 1) * 0xFF;
      final var ledColor = new Color(red, green, blue);
      g.setColor(ledColor);
      g.fillOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    int summary = 0;
    for (var i = 0; i < 3; i++) {
      final var val = state.getPortValue(i);
      if (val == Value.TRUE) summary |= 1 << i;
    }
    Object value = summary;
    final var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(value));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new RgbLedShape(x, y, path);
  }
}
