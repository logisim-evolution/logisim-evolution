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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.JoystickIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.MouseEvent;

public class Joystick extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Joystick";

  public static class Poker extends InstancePoker {
    @Override
    public void mouseDragged(InstanceState state, MouseEvent e) {
      final var loc = state.getInstance().getLocation();
      final var cx = loc.getX() - 15;
      final var cy = loc.getY() + 5;
      updateState(state, e.getX() - cx, e.getY() - cy);
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      mouseDragged(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      updateState(state, 0, 0);
    }

    @Override
    public void paint(InstancePainter painter) {
      var state = (State) painter.getData();
      if (state == null) {
        state = new State(0, 0);
        painter.setData(state);
      }
      final var loc = painter.getLocation();
      final var x = loc.getX();
      final var y = loc.getY();
      final var g = painter.getGraphics();
      g.fillOval(x - 19, y + 1, 8, 8);
      GraphicsUtil.switchToWidth(g, 3);
      final var dx = state.xPos;
      final var dy = state.yPos;
      final var x0 = x - 15 + (dx > 5 ? 1 : dx < -5 ? -1 : 0);
      final var y0 = y + 5 + (dy > 5 ? 1 : dy < 0 ? -1 : 0);
      final var x1 = x - 15 + dx;
      final var y1 = y + 5 + dy;
      g.drawLine(x0, y0, x1, y1);
      final var ballColor = painter.getAttributeValue(IoLibrary.ATTR_COLOR);
      Joystick.drawBall(g, x1, y1, ballColor, true);
    }

    private void updateState(InstanceState state, int dx, int dy) {
      var s = (State) state.getData();
      if (dx < -14) dx = -14;
      if (dy < -14) dy = -14;
      if (dx > 14) dx = 14;
      if (dy > 14) dy = 14;
      if (s == null) {
        s = new State(dx, dy);
        state.setData(s);
      } else {
        s.xPos = dx;
        s.yPos = dy;
      }
      state.getInstance().fireInvalidated();
    }
  }

  private static class State implements InstanceData, Cloneable {
    private int xPos;
    private int yPos;

    public State(int x, int y) {
      xPos = x;
      yPos = y;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  private static void drawBall(Graphics g, int x, int y, Color c, boolean inColor) {
    if (inColor) {
      g.setColor(c == null ? Color.RED : c);
    } else {
      int hue = c == null ? 128 : (c.getRed() + c.getGreen() + c.getBlue()) / 3;
      g.setColor(new Color(hue, hue, hue));
    }
    GraphicsUtil.switchToWidth(g, 1);
    g.fillOval(x - 4, y - 4, 8, 8);
    g.setColor(Color.BLACK);
    g.drawOval(x - 4, y - 4, 8, 8);
  }

  static final Attribute<BitWidth> ATTR_WIDTH =
      Attributes.forBitWidth("bits", S.getter("ioBitWidthAttr"), 2, 5);

  public Joystick() {
    super(_ID, S.getter("joystickComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          ATTR_WIDTH,
          IoLibrary.ATTR_COLOR,
          IoLibrary.ATTR_BACKGROUND
        },
        new Object[] {
          Direction.EAST,
          BitWidth.create(4),
          Color.RED,
          IoLibrary.DEFAULT_BACKGROUND
        });
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(ATTR_WIDTH, 2, 5));
    setOffsetBounds(Bounds.create(-30, -10, 30, 30));
    setIcon(new JoystickIcon());
    setPorts(
        new Port[] {
          new Port(0, 0, Port.OUTPUT, ATTR_WIDTH), new Port(0, 10, Port.OUTPUT, ATTR_WIDTH),
        });
    setInstancePoker(Poker.class);
  }
  
  @Override
  protected void configureNewInstance(Instance instance) {
    updatePorts(instance);
    instance.addAttributeListener();
  }
  
  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    int x0, y0;
    int x1, y1;
    if (facing == Direction.NORTH) {
      x0 = -20;
      y0 = -10;
      x1 = -10;
      y1 = -10;
    } else if (facing == Direction.SOUTH) {
      x0 = -20;
      y0 = 20;
      x1 = -10;
      y1 = 20;
    } else if (facing == Direction.WEST) {
      x0 = -30;
      y0 = 0;
      x1 = -30;
      y1 = 10;
    } else {
      x0 = 0;
      y0 = 0;
      x1 = 0;
      y1 = 10;
    }
    final var ports = new Port[2];
    ports[0] = new Port(x0, y0, Port.OUTPUT, ATTR_WIDTH);
    ports[0].setToolTip(S.getter("joystickCoordinateX"));
    ports[1] = new Port(x1, y1, Port.OUTPUT, ATTR_WIDTH);
    ports[1].setToolTip(S.getter("joystickCoordinateY"));
    instance.setPorts(ports);
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var g = painter.getGraphics();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRoundRect(-30, -10, 30, 30, 8, 8);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var loc = painter.getLocation();
    final var x = loc.getX();
    final var y = loc.getY();

    final var g = painter.getGraphics();
    g.setColor(painter.getAttributeValue(IoLibrary.ATTR_BACKGROUND));
    g.fillRoundRect(x - 30, y - 10, 30, 30, 8, 8);
    g.setColor(Color.BLACK);
    g.drawRoundRect(x - 30, y - 10, 30, 30, 8, 8);
    g.drawRoundRect(x - 28, y - 8, 26, 26, 4, 4);
    drawBall(g, x - 15, y + 5, painter.getAttributeValue(IoLibrary.ATTR_COLOR), painter.shouldDrawColor());
    painter.drawPorts();
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      updatePorts(instance);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var bits = state.getAttributeValue(ATTR_WIDTH);
    int dx;
    int dy;
    State s = (State) state.getData();
    if (s == null) {
      dx = 0;
      dy = 0;
    } else {
      dx = s.xPos;
      dy = s.yPos;
    }

    int steps = (1 << bits.getWidth()) - 1;
    dx = (dx + 14) * steps / 29 + 1;
    dy = (dy + 14) * steps / 29 + 1;
    if (bits.getWidth() > 4) {
      if (dx >= steps / 2) dx++;
      if (dy >= steps / 2) dy++;
    }
    state.setPort(0, Value.createKnown(bits, dx), 1);
    state.setPort(1, Value.createKnown(bits, dy), 1);
  }
}
