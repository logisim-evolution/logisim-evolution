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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.IconsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import javax.swing.Icon;

public class Clock extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Clock";

  public static class ClockLogger extends InstanceLogger {
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
      ClockState s = getState(state);
      return s.sending;
    }

    @Override
    public boolean isInput(InstanceState state, Object option) {
      return true;
    }
  }

  public static class ClockPoker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      Bounds bds = state.getInstance().getBounds();
      return bds.contains(e.getX(), e.getY());
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (isPressed && isInside(state, e))
        state.getProject().getSimulator().tick(1); // all clocks tick together
      isPressed = false;
    }
  }

  private static class ClockState implements InstanceData, Cloneable {
    Value sending = Value.UNKNOWN;

    ClockState(int curTick, AttributeSet attrs) {
      updateTick(curTick, attrs);
    }

    boolean updateTick(int ticks, AttributeSet attrs) {
      int durationHigh = attrs.getValue(ATTR_HIGH);
      int durationLow = attrs.getValue(ATTR_LOW);
      int cycle = durationHigh + durationLow;
      int phase = ((attrs.getValue(ATTR_PHASE) % cycle) + cycle) % cycle;
      boolean isLow = ((ticks + phase) % cycle) < durationLow;
      Value desired = (isLow ? Value.FALSE : Value.TRUE);
      if (sending.equals(desired))
        return false;
      sending = desired;
      return true;
    }

    @Override
    public ClockState clone() {
      try {
        return (ClockState) super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  private static ClockState getState(InstanceState state) {
    ClockState ret = (ClockState) state.getData();
    if (ret == null) {
      ret = new ClockState(state.getTickCount(), state.getAttributeSet());
      state.setData(ret);
    }
    return ret;
  }

  public static boolean tick(CircuitState circState, int ticks, Component comp) {
    AttributeSet attrs = comp.getAttributeSet();
    ClockState state = (ClockState) circState.getData(comp);
    boolean dirty = false;
    if (state == null) {
      state = new ClockState(ticks, attrs);
      circState.setData(comp, state);
      dirty = true;
    } else {
      dirty = state.updateTick(ticks, attrs);
    }
    if (dirty)
      Instance.getInstanceFor(comp).fireInvalidated();
    return true;
  }

  public static final Attribute<Integer> ATTR_HIGH =
      new DurationAttribute("highDuration", S.getter("clockHighAttr"), 1, Integer.MAX_VALUE, true);

  public static final Attribute<Integer> ATTR_LOW =
      new DurationAttribute("lowDuration", S.getter("clockLowAttr"), 1, Integer.MAX_VALUE, true);

  public static final Attribute<Integer> ATTR_PHASE = new DurationAttribute(
      "phaseOffset", S.getter("clockPhaseAttr"), 0, Integer.MAX_VALUE, true);

  public static final Clock FACTORY = new Clock();

  private static final Icon toolIcon = IconsUtil.getIcon("clock.gif");

  public Clock() {
    super(_ID, S.getter("clockComponent"), new ClockHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.FACING, ATTR_HIGH, ATTR_LOW, ATTR_PHASE, StdAttr.LABEL, StdAttr.LABEL_LOC, StdAttr.LABEL_FONT
        },
        new Object[] {
          Direction.EAST,
            1,
            1,
            0,
          "",
          Direction.WEST,
          StdAttr.DEFAULT_LABEL_FONT
        });
    setFacingAttribute(StdAttr.FACING);
    setInstanceLogger(ClockLogger.class);
    setInstancePoker(ClockPoker.class);
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, BitWidth.ONE)});
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return "LogisimClockComponent";
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Probe.getOffsetBounds(
        attrs.getValue(StdAttr.FACING), BitWidth.ONE, RadixOption.RADIX_2, false, false);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  //
  // graphics methods
  //
  @Override
  public void paintIcon(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    if (toolIcon != null) {
      toolIcon.paintIcon(painter.getDestination(), g, 2, 2);
    } else {
      g.drawRect(4, 4, 13, 13);
      g.setColor(Value.FALSE.getColor());
      g.drawPolyline(new int[] {6, 6, 10, 10, 14, 14}, new int[] {10, 6, 6, 14, 14, 10}, 6);
    }

    Direction dir = painter.getAttributeValue(StdAttr.FACING);
    int pinx = 15;
    int piny = 8;
    if (dir == Direction.EAST) { // keep defaults
    } else if (dir == Direction.WEST) {
      pinx = 3;
    } else if (dir == Direction.NORTH) {
      pinx = 8;
      piny = 3;
    } else if (dir == Direction.SOUTH) {
      pinx = 8;
      piny = 15;
    }
    g.setColor(Value.TRUE.getColor());
    g.fillOval(pinx, piny, 3, 3);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    java.awt.Graphics g = painter.getGraphics();
    Bounds bds = painter.getInstance().getBounds(); // intentionally with no
    // graphics object - we
    // don't want label
    // included
    int x = bds.getX();
    int y = bds.getY();
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.BLACK);
    g.drawRect(x, y, bds.getWidth(), bds.getHeight());

    painter.drawLabel();

    boolean drawUp;
    if (painter.getShowState()) {
      ClockState state = getState(painter);
      g.setColor(state.sending.getColor());
      drawUp = state.sending == Value.TRUE;
    } else {
      g.setColor(Color.BLACK);
      drawUp = true;
    }
    x += 10;
    y += 10;
    int[] xs = {x - 6, x - 6, x, x, x + 6, x + 6};
    int[] ys;
    if (drawUp) {
      ys = new int[] {y, y - 4, y - 4, y + 4, y + 4, y};
    } else {
      ys = new int[] {y, y + 4, y + 4, y - 4, y - 4, y};
    }
    g.drawPolyline(xs, ys, xs.length);

    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    Value val = state.getPortValue(0);
    ClockState q = getState(state);
    if (!val.equals(q.sending)) { // ignore if no change
      state.setPort(0, q.sending, 1);
    }
  }
}
