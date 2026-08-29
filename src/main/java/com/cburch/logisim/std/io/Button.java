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

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.ButtonIcon;
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
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Button extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Button";

  public static final AttributeOption BUTTON_PRESS_ACTIVE =
      new AttributeOption("active", S.getter("buttonPressActive"));
  public static final AttributeOption BUTTON_PRESS_PASSIVE =
      new AttributeOption("passive", S.getter("buttonPressPassive"));
  public static final Attribute<AttributeOption> ATTR_PRESS =
      Attributes.forOption(
          "press",
          S.getter("buttonPressAttr"),
          new AttributeOption[] {BUTTON_PRESS_ACTIVE, BUTTON_PRESS_PASSIVE});

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
      InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
      final var defaultButtonState =
          state.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_ACTIVE ? Value.FALSE : Value.TRUE;
      return data == null ? defaultButtonState : (Value) data.getValue();
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      return true;
    }
  }

  public static class Poker extends InstancePoker {
    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      setValue(
          state,
          state.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_PASSIVE ? Value.FALSE : Value.TRUE);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      setValue(
          state,
          state.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_PASSIVE ? Value.TRUE : Value.FALSE);
    }

    private void setValue(InstanceState state, Value val) {
      final var data = (InstanceDataSingleton) state.getData();
      if (data == null) {
        state.setData(new InstanceDataSingleton(val));
      } else {
        data.setValue(val);
      }
      state.getInstance().fireInvalidated();
    }
  }

  private static final int DEPTH = 3;

  public Button() {
    super(_ID, S.getter("buttonComponent"), new AbstractSimpleIoHdlGeneratorFactory(true), true);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          IoLibrary.ATTR_COLOR,
          ATTR_PRESS,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.EAST,
          Color.WHITE,
          BUTTON_PRESS_ACTIVE,
          "",
          Direction.WEST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(1, 0, 0)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new ButtonIcon());
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, 1)});
    setInstancePoker(Poker.class);
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(-20, -10, 20, 20).rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_CENTER | Instance.AVOID_LEFT);
    } else if (attr == ATTR_PRESS) {
      final var instanceImplementation = instance.getComponent().getInstanceStateImpl();
      if (instanceImplementation == null) return;
      final var circuitState = instanceImplementation.getCircuitState();
      if (circuitState == null) return;
      final var state = circuitState.getInstanceState(instance.getComponent());
      if (state == null) return;
      final var data = (InstanceDataSingleton) state.getData();
      if (data == null) {
        state.setData(
            new InstanceDataSingleton(
                state.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_PASSIVE
                    ? Value.TRUE
                    : Value.FALSE));
      } else {
        data.setValue(data.getValue() == Value.TRUE ? Value.FALSE : Value.TRUE);
      }
      state.getInstance().fireInvalidated();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var defaultButtonState =
        painter.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_ACTIVE ? Value.FALSE : Value.TRUE;
    final var bds = painter.getBounds();
    var x = bds.getX();
    var y = bds.getY();
    final var w = bds.getWidth();
    final var h = bds.getHeight();
    final var baseColor = new Color(AppPreferences.COMPONENT_COLOR.get());

    Value val;
    if (painter.getShowState()) {
      final var data = (InstanceDataSingleton) painter.getData();
      val = data == null ? defaultButtonState : (Value) data.getValue();
    } else {
      val = defaultButtonState;
    }

    var color = painter.getAttributeValue(IoLibrary.ATTR_COLOR);
    if (!painter.shouldDrawColor()) {
      int hue = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
      color = new Color(hue, hue, hue);
    }

    final var g = painter.getGraphics();
    int depress;
    if (val != defaultButtonState) {
      x += DEPTH;
      y += DEPTH;
      Object labelLoc = painter.getAttributeValue(StdAttr.LABEL_LOC);
      if (labelLoc == StdAttr.LABEL_CENTER
          || labelLoc == Direction.NORTH
          || labelLoc == Direction.WEST) {
        depress = DEPTH;
      } else {
        depress = 0;
      }

      Object facing = painter.getAttributeValue(StdAttr.FACING);
      if (facing == Direction.NORTH || facing == Direction.WEST) {
        final var p = painter.getLocation();
        int px = p.getX();
        int py = p.getY();
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        g.setColor(Value.trueColor);
        if (facing == Direction.NORTH) g.drawLine(px, py, px, py + 10);
        else g.drawLine(px, py, px + 10, py);
        GraphicsUtil.switchToWidth(g, 1);
      }

      g.setColor(color);
      g.fillRect(x, y, w - DEPTH, h - DEPTH);
      g.setColor(baseColor);
      g.drawRect(x, y, w - DEPTH, h - DEPTH);
    } else {
      depress = 0;
      int[] xp = new int[] {x, x + w - DEPTH, x + w, x + w, x + DEPTH, x};
      int[] yp = new int[] {y, y, y + DEPTH, y + h, y + h, y + h - DEPTH};
      g.setColor(color.darker());
      g.fillPolygon(xp, yp, xp.length);
      g.setColor(color);
      g.fillRect(x, y, w - DEPTH, h - DEPTH);
      g.setColor(baseColor);
      g.drawPolygon(xp, yp, xp.length);

      // The following two draws are supposed to be polygons, not polylines.
      // This is not a mistake, but rather a graphical trick which leverages
      // the implicit miter limit to produce lines that look like they have
      // "butt" style line caps, even though they are drawn with a system
      // that produces only "square" style line caps.
      final int farX = x + w - DEPTH;
      final int farY = y + h - DEPTH;
      xp = new int[] {farX, farX, x, farX};
      yp = new int[] {y, farY, farY, farY};
      g.drawPolygon(xp, yp, xp.length);
      xp = new int[] {farX, x + w};
      yp = new int[] {farY, y + h};
      g.drawPolygon(xp, yp, xp.length);
    }

    g.translate(depress, depress);
    painter.drawLabel();
    g.translate(-depress, -depress);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var data = (InstanceDataSingleton) state.getData();
    final var defaultButtonState =
        state.getAttributeValue(ATTR_PRESS) == BUTTON_PRESS_ACTIVE ? Value.FALSE : Value.TRUE;
    final var val = data == null ? defaultButtonState : (Value) data.getValue();
    state.setPort(0, val, 1);
  }
}
