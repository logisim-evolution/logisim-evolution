/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.plexers;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.icons.PlexerIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;

public class Decoder extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Decoder";

  public Decoder() {
    super(_ID, S.getter("decoderComponent"), new DecoderHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.SELECT_LOC,
          PlexersLibrary.ATTR_SELECT,
          PlexersLibrary.ATTR_TRISTATE,
          PlexersLibrary.ATTR_DISABLED,
          PlexersLibrary.ATTR_ENABLE
        },
        new Object[] {
          Direction.EAST,
          StdAttr.SELECT_BOTTOM_LEFT,
          PlexersLibrary.DEFAULT_SELECT,
          PlexersLibrary.DEFAULT_TRISTATE,
          PlexersLibrary.DISABLED_ZERO,
          Boolean.TRUE
        });
    setKeyConfigurator(new BitWidthConfigurator(PlexersLibrary.ATTR_SELECT, 1, 5, 0));
    setIcon(new PlexerIcon(true, false));
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public boolean contains(Location loc, AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING).reverse();
    return PlexersLibrary.contains(loc, getOffsetBounds(attrs), facing);
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    if (attr == PlexersLibrary.ATTR_ENABLE) {
      int newer = ver.compareTo(new LogisimVersion(2, 6, 4));
      return newer >= 0;
    } else {
      return super.getDefaultAttributeValue(attr, ver);
    }
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return CorrectLabel.getCorrectLabel(this.getName())
        + "_"
        + (1 << attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth());
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    final var selectLoc = attrs.getValue(StdAttr.SELECT_LOC);
    final var select = attrs.getValue(PlexersLibrary.ATTR_SELECT);
    final var outputs = 1 << select.getWidth();
    Bounds bds;
    var reversed = facing == Direction.WEST || facing == Direction.NORTH;
    if (selectLoc == StdAttr.SELECT_TOP_RIGHT) reversed = !reversed;
    if (outputs == 2) {
      int y = reversed ? 0 : -40;
      bds = Bounds.create(-20, y - 5, 30, 40 + 10);
    } else {
      int x = -20;
      int y = reversed ? -10 : -(outputs * 10 + 10);
      bds = Bounds.create(x, y, 40, outputs * 10 + 20);
    }
    return bds.rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    return (attrs.getValue(PlexersLibrary.ATTR_TRISTATE)
        || (attrs.getValue(PlexersLibrary.ATTR_DISABLED) == PlexersLibrary.DISABLED_FLOATING));
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING
        || attr == StdAttr.SELECT_LOC
        || attr == PlexersLibrary.ATTR_SELECT) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == PlexersLibrary.ATTR_ENABLE) {
      updatePorts(instance);
    } else if (attr == PlexersLibrary.ATTR_TRISTATE || attr == PlexersLibrary.ATTR_DISABLED) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var select = painter.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var bds = painter.getBounds();

    if (select.getWidth() == 1) {
      if (facing == Direction.EAST || facing == Direction.WEST) {
        PlexersLibrary.drawTrapezoid(
            painter.getGraphics(),
            Bounds.create(bds.getX(), bds.getY() + 5, bds.getWidth(), bds.getHeight() - 10),
            facing.reverse(),
            10);
      } else {
        PlexersLibrary.drawTrapezoid(
            painter.getGraphics(),
            Bounds.create(bds.getX() + 5, bds.getY(), bds.getWidth() - 10, bds.getHeight()),
            facing.reverse(),
            10);
      }
    } else {
      PlexersLibrary.drawTrapezoid(painter.getGraphics(), bds, facing.reverse(), 20);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    Object selectLoc = painter.getAttributeValue(StdAttr.SELECT_LOC);
    final var select = painter.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    boolean enable = painter.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    int selMult = selectLoc == StdAttr.SELECT_TOP_RIGHT ? -1 : 1;
    int outputs = 1 << select.getWidth();

    // draw stubs for select and enable ports
    GraphicsUtil.switchToWidth(g, 3);
    final var vertical = facing == Direction.NORTH || facing == Direction.SOUTH;
    final var dx = vertical ? selMult : 0;
    final var dy = vertical ? 0 : -selMult;
    if (outputs == 2) { // draw select wire
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(outputs).getColor());
      }
      final var pt = painter.getInstance().getPortLocation(outputs);
      g.drawLine(pt.getX(), pt.getY(), pt.getX() + 2 * dx, pt.getY() + 2 * dy);
    }
    if (enable) {
      final var en = painter.getInstance().getPortLocation(outputs + 1);
      int len = outputs == 2 ? 6 : 4;
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(outputs + 1).getColor());
      }
      g.drawLine(en.getX(), en.getY(), en.getX() + len * dx, en.getY() + len * dy);
    }
    GraphicsUtil.switchToWidth(g, 1);

    // draw a circle indicating where the select input is located
    Multiplexer.drawSelectCircle(g, bds, painter.getInstance().getPortLocation(outputs));

    // draw "0"
    int x0;
    int y0;
    int halign;
    if (facing == Direction.WEST) {
      x0 = 3;
      y0 = 15 + (outputs == 2 ? 5 : 0);
      halign = GraphicsUtil.H_LEFT;
    } else if (facing == Direction.NORTH) {
      x0 = 10 + (outputs == 2 ? 5 : 0);
      y0 = 15;
      halign = GraphicsUtil.H_CENTER;
    } else if (facing == Direction.SOUTH) {
      x0 = 10 + (outputs == 2 ? 5 : 0);
      y0 = bds.getHeight() - 3;
      halign = GraphicsUtil.H_CENTER;
    } else {
      x0 = bds.getWidth() - 3;
      y0 = 15 + (outputs == 2 ? 5 : 0);
      halign = GraphicsUtil.H_RIGHT;
    }
    g.setColor(Color.GRAY);
    GraphicsUtil.drawText(
        g, "0", bds.getX() + x0, bds.getY() + y0, halign, GraphicsUtil.V_BASELINE);

    // draw trapezoid, "Decd", and ports
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    if (outputs == 2) {
      if (facing == Direction.EAST || facing == Direction.WEST) {
        PlexersLibrary.drawTrapezoid(
            g,
            Bounds.create(bds.getX(), bds.getY() + 5, bds.getWidth(), bds.getHeight() - 10),
            facing.reverse(),
            10);
      } else {
        PlexersLibrary.drawTrapezoid(
            g,
            Bounds.create(bds.getX() + 5, bds.getY(), bds.getWidth() - 10, bds.getHeight()),
            facing.reverse(),
            10);
      }
    } else {
      PlexersLibrary.drawTrapezoid(g, bds, facing.reverse(), 20);
    }
    GraphicsUtil.drawCenteredText(
        g, "Decd", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    final var data = BitWidth.ONE;
    final var select = state.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var threeState = state.getAttributeValue(PlexersLibrary.ATTR_TRISTATE);
    final var enable = state.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    var outputs = 1 << select.getWidth();

    // determine default output values
    Value others; // the default output
    if (threeState) {
      others = Value.UNKNOWN;
    } else {
      others = Value.FALSE;
    }

    // determine selected output value
    var outIndex = -1; // the special output
    Value out = null;
    final var en = enable ? state.getPortValue(outputs + 1) : Value.TRUE;
    if (en == Value.FALSE) {
      Object opt = state.getAttributeValue(PlexersLibrary.ATTR_DISABLED);
      final var base = opt == PlexersLibrary.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
      others = Value.repeat(base, data.getWidth());
    } else if (en == Value.ERROR && state.isPortConnected(outputs + 1)) {
      others = Value.createError(data);
    } else {
      final var sel = state.getPortValue(outputs);
      if (sel.isFullyDefined()) {
        outIndex = (int) sel.toLongValue();
        out = Value.TRUE;
      } else if (sel.isErrorValue()) {
        others = Value.createError(data);
      } else {
        others = Value.createUnknown(data);
      }
    }

    // now propagate them
    for (var i = 0; i < outputs; i++) {
      state.setPort(i, i == outIndex ? out : others, PlexersLibrary.DELAY);
    }
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    Object selectLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    final var select = instance.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enable = instance.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    var outputs = 1 << select.getWidth();
    final var ps = new Port[outputs + (enable ? 2 : 1)];
    if (outputs == 2) {
      Location end0;
      Location end1;
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        int y = facing == Direction.NORTH ? -10 : 10;
        if (selectLoc == StdAttr.SELECT_TOP_RIGHT) {
          end0 = Location.create(-30, y, true);
          end1 = Location.create(-10, y, true);
        } else {
          end0 = Location.create(10, y, true);
          end1 = Location.create(30, y, true);
        }
      } else {
        int x = facing == Direction.WEST ? -10 : 10;
        if (selectLoc == StdAttr.SELECT_TOP_RIGHT) {
          end0 = Location.create(x, 10, true);
          end1 = Location.create(x, 30, true);
        } else {
          end0 = Location.create(x, -30, true);
          end1 = Location.create(x, -10, true);
        }
      }
      ps[0] = new Port(end0.getX(), end0.getY(), Port.OUTPUT, 1);
      ps[1] = new Port(end1.getX(), end1.getY(), Port.OUTPUT, 1);
    } else {
      int dx;
      int ddx;
      int dy;
      int ddy;
      if (facing == Direction.NORTH || facing == Direction.SOUTH) {
        dy = facing == Direction.NORTH ? -20 : 20;
        ddy = 0;
        dx = selectLoc == StdAttr.SELECT_TOP_RIGHT ? -10 * outputs : 0;
        ddx = 10;
      } else {
        dx = facing == Direction.WEST ? -20 : 20;
        ddx = 0;
        dy = selectLoc == StdAttr.SELECT_TOP_RIGHT ? 0 : -10 * outputs;
        ddy = 10;
      }
      for (var i = 0; i < outputs; i++) {
        ps[i] = new Port(dx, dy, Port.OUTPUT, 1);
        dx += ddx;
        dy += ddy;
      }
    }
    final var en = Location.create(0, 0, true).translate(facing, -10);
    ps[outputs] = new Port(0, 0, Port.INPUT, select.getWidth());
    if (enable) {
      ps[outputs + 1] = new Port(en.getX(), en.getY(), Port.INPUT, BitWidth.ONE);
    }
    for (var i = 0; i < outputs; i++) {
      ps[i].setToolTip(S.getter("decoderOutTip", "" + i));
    }
    ps[outputs].setToolTip(S.getter("decoderSelectTip"));
    if (enable) {
      ps[outputs + 1].setToolTip(S.getter("decoderEnableTip"));
    }
    instance.setPorts(ps);
  }
}
