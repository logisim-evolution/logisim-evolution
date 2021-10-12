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

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
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
import com.cburch.logisim.std.io.IoLibrary;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;

public class Switch extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Switch";

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
      return state.getPortValue(1);
    }
  }

  public static class Poker extends InstancePoker {
    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
      setActive(state, data == null || !((Boolean) data.getValue()));
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

  private static final int DEPTH = 3;

  public Switch() {
    super(_ID, S.getter("switchComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.WIDTH,
          IoLibrary.ATTR_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT
        },
        new Object[] {
          Direction.EAST, BitWidth.ONE, Color.WHITE, "", Direction.NORTH, StdAttr.DEFAULT_LABEL_FONT
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("switch.gif");
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstancePoker(Poker.class);
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
    updateports(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(-20, -15, 20, 30).rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
      updateports(instance);
    } else if (attr == StdAttr.WIDTH) {
      updateports(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_RIGHT | Instance.AVOID_LEFT);
    }
  }

  private void paint(InstancePainter painter, boolean ghost) {
    // draw
    Bounds bds = painter.getBounds();
    int x = bds.getX(); // x position
    int y = bds.getY(); // y position
    int w = bds.getWidth(); // width
    int h = bds.getHeight(); // height
    byte circle = 4; // 0 symbol radius
    int[] xp;
    int[] yp;
    int[] xr;
    int[] yr;
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    if (!ghost)
      // draw the first port here because it has to be under the drawing
      painter.drawPort((facing == Direction.SOUTH || facing == Direction.EAST ? 0 : 1));
    boolean active = false;
    if (painter.getShowState() && !ghost) {
      InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
      active = data != null && (Boolean) data.getValue();
    }

    Color color = painter.getAttributeValue(IoLibrary.ATTR_COLOR);
    if (!painter.shouldDrawColor()) {
      int hue = (color.getRed() + color.getGreen() + color.getBlue()) / 3;
      color = new Color(hue, hue, hue);
    }

    Graphics g = painter.getGraphics();
    if (active) { // case true output
      if (facing == Direction.NORTH || facing == Direction.WEST) {
        Location p = painter.getLocation();
        int px = p.getX();
        int py = p.getY();
        GraphicsUtil.switchToWidth(g, Wire.WIDTH);
        g.setColor(Value.trueColor);
        if (facing == Direction.NORTH) g.drawLine(px, py, px, py + 10);
        else g.drawLine(px, py, px + 10, py);
        GraphicsUtil.switchToWidth(g, 1);
      }

      if (facing == Direction.NORTH || facing == Direction.SOUTH) { // horizontal
        // grey polygon x points
        xp = new int[] {x, x + w - DEPTH, x + w, x + w, x};
        // grey polygon y points
        yp = new int[] {y + DEPTH, y, y + DEPTH, y + h, y + h};
        // white polygon x points
        xr = new int[] {x, x + w - DEPTH, x + w - DEPTH, x};
        // white polygon y points
        yr = new int[] {y + DEPTH, y, y + h - DEPTH, y + h};

      } else { // vertical
        xp = new int[] {x + DEPTH, x + w, x + w, x + DEPTH, x};
        yp = new int[] {y, y, y + h, y + h, y + DEPTH};
        xr = new int[] {x, x + w - DEPTH, x + w, x + DEPTH};
        yr = new int[] {y + DEPTH, y + DEPTH, y + h, y + h};
      }
      if (!ghost) {
        g.setColor(color.darker());
        g.fillPolygon(xp, yp, xp.length);
        g.setColor(color);
        g.fillPolygon(xr, yr, xr.length);
        g.setColor(Color.BLACK);
      }
      g.drawPolygon(xp, yp, xp.length);
      g.drawPolygon(xr, yr, xr.length);
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        g.drawLine(
            x + ((w - DEPTH) / 2),
            y + (DEPTH / 2) + 1,
            x + ((w - DEPTH) / 2),
            y + h - (DEPTH / 2) - 1);
        g.drawLine(x + w - DEPTH, y + h - DEPTH, x + w, y + h);
        g.drawLine(
            x + ((w - DEPTH) / 6),
            y + ((h - DEPTH) / 2) + (DEPTH - DEPTH / 6),
            x + ((w - DEPTH) / 3),
            y + ((h - DEPTH) / 2) + (DEPTH - DEPTH / 3));
        g.drawOval(
            x + ((w - DEPTH) * 3 / 4) - (circle / 2),
            y + ((h - DEPTH) / 2 - (circle / 2)) + 1 + (DEPTH / 4),
            circle,
            circle - 1);
      } else {
        g.drawLine(x + w - DEPTH, y + DEPTH, x + w, y);
        g.drawLine(
            x + (DEPTH / 2) + 1,
            y + ((h - DEPTH) / 2) + DEPTH,
            x + w - (DEPTH / 2) - 1,
            y + ((h - DEPTH) / 2) + DEPTH);
        g.drawLine(
            x + ((w - DEPTH) / 2) + (DEPTH - DEPTH / 6),
            y + ((h - DEPTH) * 5 / 6) + DEPTH,
            x + ((w - DEPTH) / 2) + (DEPTH - DEPTH / 3),
            y + ((h - DEPTH) * 2 / 3) + DEPTH);
        g.drawOval(
            x + (DEPTH / 4) + ((w - DEPTH - circle) / 2) + 1,
            y + ((h - DEPTH) / 4 - (circle / 2)) + DEPTH,
            circle - 1,
            circle);
      }
    } else { // case false output
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        xp = new int[] {x, x + DEPTH, x + w, x + w, x};
        yp = new int[] {y + DEPTH, y, y + DEPTH, y + h, y + h};
        xr = new int[] {x + DEPTH, x + w, x + w, x + DEPTH};
        yr = new int[] {y, y + DEPTH, y + h, y + h - DEPTH};
      } else {
        xp = new int[] {x + DEPTH, x + w, x + w, x + DEPTH, x};
        yp = new int[] {y, y, y + h, y + h, y + h - DEPTH};
        xr = new int[] {x + DEPTH, x + w, x + w - DEPTH, x};
        yr = new int[] {y, y, y + h - DEPTH, y + h - DEPTH};
      }
      if (!ghost) {
        g.setColor(color.darker());
        g.fillPolygon(xp, yp, xp.length);
        g.setColor(color);
        g.fillPolygon(xr, yr, xr.length);
        g.setColor(Color.BLACK);
      }
      g.drawPolygon(xp, yp, xp.length);
      g.drawPolygon(xr, yr, xr.length);
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        // diagonal line inside gray polygon
        g.drawLine(x + DEPTH, y + h - DEPTH, x, y + h);
        g.drawLine(
            x + ((w - DEPTH) / 2) + DEPTH,
            y + (DEPTH / 2) + 1,
            x + ((w - DEPTH) / 2) + DEPTH,
            y + h - (DEPTH / 2) - 1);
        g.drawLine(
            x + DEPTH + (w / 6),
            y + ((h - DEPTH) / 2) + (DEPTH / 6),
            x + DEPTH + (w / 3),
            y + ((h - DEPTH) / 2) + (DEPTH / 3));
        g.drawOval(
            x + ((w - DEPTH) * 3 / 4) - (circle / 2) + DEPTH,
            y + ((h - DEPTH) / 2 - (circle / 2)) + (DEPTH * 3 / 4) + 1,
            circle,
            circle - 1);
      } else {
        g.drawLine(
            x + (DEPTH / 2) + 1,
            y + ((h - DEPTH) / 2),
            x + w - (DEPTH / 2) - 1,
            y + ((h - DEPTH) / 2));
        g.drawLine(x + w - DEPTH, y + h - DEPTH, x + w, y + h);
        g.drawLine(
            x + ((w - DEPTH) / 2) + (DEPTH / 6),
            y + ((h - DEPTH) * 5 / 6),
            x + ((w - DEPTH) / 2) + (DEPTH / 3),
            y + ((h - DEPTH) * 2 / 3));
        g.drawOval(
            x + (DEPTH * 3 / 4) + ((w - DEPTH - circle) / 2) + 1,
            y + ((h - DEPTH) / 4 - (circle / 2)),
            circle - 1,
            circle);
      }
    }
    if (!ghost) {
      painter.drawLabel();
      painter.drawPort((facing == Direction.SOUTH || facing == Direction.EAST ? 1 : 0));
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

  @Override
  public void propagate(InstanceState state) {
    InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
    Value val =
        (data == null || !(Boolean) data.getValue())
            ? Value.createUnknown(state.getAttributeValue(StdAttr.WIDTH))
            : state.getPortValue(0);
    state.setPort(1, val, 1);
  }

  private void updateports(Instance instance) {
    Port[] ps = new Port[2];
    BitWidth b = instance.getAttributeValue(StdAttr.WIDTH);
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    ps[0] =
        dir == Direction.EAST
            ? new Port(-20, 0, Port.INPUT, b)
            : dir == Direction.WEST
                ? new Port(20, 0, Port.INPUT, b)
                : dir == Direction.NORTH
                    ? new Port(0, 20, Port.INPUT, b)
                    : new Port(0, -20, Port.INPUT, b);
    ps[0].setToolTip(S.getter("pinInputName"));
    ps[1] = new Port(0, 0, Port.OUTPUT, b);
    ps[1].setToolTip(S.getter("pinOutputName"));
    instance.setPorts(ps);
  }
}
