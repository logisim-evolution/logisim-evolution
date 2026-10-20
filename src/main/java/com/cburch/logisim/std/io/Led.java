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
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;

public class Led extends InstanceFactory implements DynamicElementProvider {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "LED";

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
      if (data == null) return Value.FALSE;
      return data.getValue() == Value.TRUE ? Value.TRUE : Value.FALSE;
    }
  }

  public Led() {
    super(_ID, S.getter("ledComponent"), new AbstractSimpleIoHdlGeneratorFactory(false), true);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          IoLibrary.ATTR_ON_COLOR,
          IoLibrary.ATTR_OFF_COLOR,
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
          new Color(240, 0, 0),
          Color.DARK_GRAY,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(0, 1, 0)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new LedIcon(false));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setPorts(new Port[] {new Port(0, 0, Port.INPUT, 1)});
    setInstanceLogger(Logger.class);
  }

  @Override
  public boolean activeOnHigh(AttributeSet attrs) {
    return attrs.getValue(IoLibrary.ATTR_ACTIVE);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
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
    final var val = data == null ? Value.FALSE : (Value) data.getValue();
    final var bds = painter.getBounds().expand(-1);

    final var g = painter.getGraphics();
    if (painter.getShowState()) {
      final var onColor = painter.getAttributeValue(IoLibrary.ATTR_ON_COLOR);
      final var offColor = painter.getAttributeValue(IoLibrary.ATTR_OFF_COLOR);
      final var activ = painter.getAttributeValue(IoLibrary.ATTR_ACTIVE);
      final var desired = activ ? Value.TRUE : Value.FALSE;
      g.setColor(val == desired ? onColor : offColor);
      g.fillOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var val = state.getPortValue(0);
    final var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(val));
    } else {
      data.setValue(val);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new LedShape(x, y, path);
  }
}
