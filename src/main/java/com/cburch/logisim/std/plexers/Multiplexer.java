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
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class Multiplexer extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Multiplexer";

  static void drawSelectCircle(Graphics g, Bounds bds, Location loc) {
    if (Math.min(bds.getHeight(), bds.getWidth()) <= 20) return; // no dot for narrow mode
    final var locDelta = Math.max(bds.getHeight(), bds.getWidth()) <= 50 ? 8 : 6;
    Location circLoc;
    if (bds.getHeight() >= bds.getWidth()) { // vertically oriented
      if (loc.getY() < bds.getY() + bds.getHeight() / 2) { // at top
        circLoc = loc.translate(0, locDelta);
      } else { // at bottom
        circLoc = loc.translate(0, -locDelta);
      }
      if (loc.getX() >= bds.getX() + bds.getWidth()) loc.translate(-4, 0);
      else if (loc.getX() <= bds.getX()) loc.translate(4, 0);
    } else {
      if (loc.getX() < bds.getX() + bds.getWidth() / 2) { // at left
        circLoc = loc.translate(locDelta, 0);
      } else { // at right
        circLoc = loc.translate(-locDelta, 0);
      }
      if (loc.getY() >= bds.getY() + bds.getHeight()) loc.translate(0, -4);
      else if (loc.getY() <= bds.getY()) loc.translate(0, 4);
    }
    g.setColor(Color.LIGHT_GRAY);
    g.fillOval(circLoc.getX() - 3, circLoc.getY() - 3, 6, 6);
  }

  public Multiplexer() {
    super(_ID, S.getter("multiplexerComponent"), new MultiplexerHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
            StdAttr.FACING,
            PlexersLibrary.ATTR_SIZE,
            StdAttr.SELECT_LOC,
            PlexersLibrary.ATTR_SELECT,
            StdAttr.WIDTH,
            PlexersLibrary.ATTR_DISABLED,
            PlexersLibrary.ATTR_ENABLE_TYPE,
        },
        new Object[] {
          Direction.EAST, PlexersLibrary.SIZE_WIDE, StdAttr.SELECT_BOTTOM_LEFT,
          PlexersLibrary.DEFAULT_SELECT, BitWidth.ONE, PlexersLibrary.DISABLED_ZERO,
          PlexersLibrary.DEFAULT_ENABLE_TYPE,
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(PlexersLibrary.ATTR_SELECT, 1, 5, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));
    setIcon(new PlexerIcon(false, false));
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    // for backward compatibility, after 2.6.4 the enable pin was "enabled" by default upto and
    // until 3.6.1
    if (attr == PlexersLibrary.ATTR_ENABLE_TYPE) {
      return ver.compareTo(new LogisimVersion(3, 6, 1)) <= 0
          ? PlexersLibrary.WITH_ENABLE
          : PlexersLibrary.WITHOUT_ENABLE;
    } else {
      return super.getDefaultAttributeValue(attr, ver);
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var completeName = new StringBuilder();
    completeName.append(CorrectLabel.getCorrectLabel(this.getName()));
    if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) completeName.append("_bus");
    completeName.append("_").append(1 << attrs.getValue(PlexersLibrary.ATTR_SELECT).getWidth());
    return completeName.toString();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Object size = attrs.getValue(PlexersLibrary.ATTR_SIZE);
    final var wide = size == PlexersLibrary.SIZE_WIDE;
    final var dir = attrs.getValue(StdAttr.FACING);
    final var select = attrs.getValue(PlexersLibrary.ATTR_SELECT);
    final var inputs = 1 << select.getWidth();
    if (inputs == 2) {
      final var w = (wide ? 30 : 20);
      return Bounds.create(-w, -20, w, 40).rotate(Direction.EAST, dir, 0, 0);
    } else {
      final var w = (wide ? 40 : 20);
      final var lengthAdjust = (wide ? 0 : -5);
      var offs = -(inputs / 2) * 10 - 10;
      final var length = inputs * 10 + 20 + lengthAdjust;
      // narrow isn't symmetrical when switchinng selector sides, rotating
      if (!wide && (dir == Direction.SOUTH || dir == Direction.WEST)) offs += 5;
      return Bounds.create(-w, offs, w, length).rotate(Direction.EAST, dir, 0, 0);
    }
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    return (attrs.getValue(PlexersLibrary.ATTR_DISABLED) == PlexersLibrary.DISABLED_FLOATING);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING
        || attr == StdAttr.SELECT_LOC
        || attr == PlexersLibrary.ATTR_SELECT
        || attr == PlexersLibrary.ATTR_SIZE || attr == StdAttr.WIDTH
        || attr == PlexersLibrary.ATTR_ENABLE_TYPE) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == PlexersLibrary.ATTR_DISABLED) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Object size = painter.getAttributeValue(PlexersLibrary.ATTR_SIZE);
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var select = painter.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var bds = painter.getBounds();
    final var lean =
        (select.getWidth() == 1)
            ? (size == PlexersLibrary.SIZE_NARROW ? 7 : 10)
            : (size == PlexersLibrary.SIZE_NARROW ? 10 : 20);
    PlexersLibrary.drawTrapezoid(painter.getGraphics(), bds, facing, lean);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    Object size = painter.getAttributeValue(PlexersLibrary.ATTR_SIZE);
    final var wide = size == PlexersLibrary.SIZE_WIDE;
    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var select = painter.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enableType = painter.getAttributeValue(PlexersLibrary.ATTR_ENABLE_TYPE);
    final var enable = enableType != PlexersLibrary.WITHOUT_ENABLE;
    final var invertEnable = enableType == PlexersLibrary.WITH_INVERT_ENABLE;
    int inputs = 1 << select.getWidth();

    // draw stubs for select/enable inputs that aren't on instance boundary
    GraphicsUtil.switchToWidth(g, 3);
    final var vertical = facing != Direction.NORTH && facing != Direction.SOUTH;
    Object selectLoc = painter.getAttributeValue(StdAttr.SELECT_LOC);
    final var selMult = selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 1 : -1;
    final var oddside = (vertical == (selMult < 0));
    int dx, dy;
    if (wide) {
      dx = vertical ? 0 : -2 * selMult;
      dy = vertical ? 2 * selMult : 0;
    } else if (vertical) {
      dx = (facing == Direction.EAST ? 1 : -1);
      dy = 2 * selMult;
    } else {
      dx = -2 * selMult;
      dy = (facing == Direction.SOUTH ? 1 : -1);
    }
    if (inputs == 2 || (!wide && oddside)) { // draw select wire
      final var pt = painter.getInstance().getPortLocation(inputs);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(inputs).getColor());
      }
      final var len = (wide ? 2 : 1);
      g.drawLine(pt.getX() - len * dx, pt.getY() - len * dy, pt.getX(), pt.getY());
    }
    if (enable) {
      final var en = painter.getInstance().getPortLocation(inputs + 1);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(inputs + 1).getColor());
      }
      final var len = (inputs == 2) ? 3 : wide ? 2 : oddside ? 4 : 2;
      g.drawLine(en.getX() - len * dx, en.getY() - len * dy, en.getX(), en.getY());

      g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

      if (invertEnable) {
        int ovalX, ovalY;
        int radius = 8;
        int halfRadius = radius / 2;
        if (vertical) {
          ovalX = en.getX() - halfRadius;
          ovalY = en.getY() - len - radius - halfRadius;
        }
        else {
          ovalX = en.getX() + len + radius - halfRadius;
          ovalY = en.getY() - halfRadius;
        }

        GraphicsUtil.switchToWidth(g, 2);
        g.drawOval(ovalX, ovalY, radius, radius);
      }
    }
    GraphicsUtil.switchToWidth(g, 1);

    // draw a circle indicating where the select input is located
    Multiplexer.drawSelectCircle(g, bds, painter.getInstance().getPortLocation(inputs));

    // draw a 0 indicating where the numbering starts for inputs
    int x0;
    int y0;
    int halign;
    if (facing == Direction.WEST) {
      x0 = bds.getX() + bds.getWidth() - 3;
      y0 = bds.getY() + 15;
      halign = GraphicsUtil.H_RIGHT;
    } else if (facing == Direction.NORTH) {
      x0 = bds.getX() + 10;
      y0 = bds.getY() + bds.getHeight() - 2;
      halign = GraphicsUtil.H_CENTER;
    } else if (facing == Direction.SOUTH) {
      x0 = bds.getX() + 10;
      y0 = bds.getY() + 12;
      halign = GraphicsUtil.H_CENTER;
    } else {
      x0 = bds.getX() + 3;
      y0 = bds.getY() + 15;
      halign = GraphicsUtil.H_LEFT;
    }
    g.setColor(Color.GRAY);
    GraphicsUtil.drawText(g, "0", x0, y0, halign, GraphicsUtil.V_BASELINE);

    // draw the trapezoid, "MUX" string, the individual ports
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    final var lean =
        (inputs == 2)
            ? (size == PlexersLibrary.SIZE_NARROW ? 7 : 10)
            : (size == PlexersLibrary.SIZE_NARROW ? 10 : 20);
    PlexersLibrary.drawTrapezoid(g, bds, facing, lean);
    if (size == PlexersLibrary.SIZE_WIDE)
      GraphicsUtil.drawCenteredText(
          g, "MUX", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var data = state.getAttributeValue(StdAttr.WIDTH);
    final var select = state.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enableType = state.getAttributeValue(PlexersLibrary.ATTR_ENABLE_TYPE);
    final var enable = enableType != PlexersLibrary.WITHOUT_ENABLE;
    final var invertEnable = enableType == PlexersLibrary.WITH_INVERT_ENABLE;
    final var inputs = 1 << select.getWidth();
    final var enablePort = state.getPortValue(inputs + 1);
    final var en = enable ? (invertEnable ? enablePort.not() : enablePort) : Value.TRUE;
    Value out;
    if (en == Value.FALSE) {
      Object opt = state.getAttributeValue(PlexersLibrary.ATTR_DISABLED);
      final var base = opt == PlexersLibrary.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
      out = Value.repeat(base, data.getWidth());
    } else if (en == Value.ERROR && state.isPortConnected(inputs + 1)) {
      out = Value.createError(data);
    } else {
      final var sel = state.getPortValue(inputs);
      if (sel.isFullyDefined()) {
        out = state.getPortValue((int) sel.toLongValue());
      } else if (sel.isErrorValue()) {
        out = Value.createError(data);
      } else {
        out = Value.createUnknown(data);
      }
    }
    state.setPort(inputs + (enable ? 2 : 1), out, PlexersLibrary.DELAY);
  }

  private void updatePorts(Instance instance) {
    Object size = instance.getAttributeValue(PlexersLibrary.ATTR_SIZE);
    final var wide = size == PlexersLibrary.SIZE_WIDE;
    final var dir = instance.getAttributeValue(StdAttr.FACING);
    final var vertical = dir != Direction.NORTH && dir != Direction.SOUTH;
    final var selectLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    final var botLeft = selectLoc == StdAttr.SELECT_BOTTOM_LEFT;
    final var selMult = botLeft ? 1 : -1;
    final var data = instance.getAttributeValue(StdAttr.WIDTH);
    final var select = instance.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enableType = instance.getAttributeValue(PlexersLibrary.ATTR_ENABLE_TYPE);
    final var enable = enableType != PlexersLibrary.WITHOUT_ENABLE;
    final var invertEnable = enableType == PlexersLibrary.WITH_INVERT_ENABLE;

    final var inputs = 1 << select.getWidth();
    final var ps = new Port[inputs + (enable ? 3 : 2)];
    Location sel;
    int w, s;
    if (inputs == 2) {
      w = (size == PlexersLibrary.SIZE_NARROW ? 20 : 30);
      s = (size == PlexersLibrary.SIZE_NARROW ? 10 : 20);
      Location end0;
      Location end1;
      if (dir == Direction.WEST) {
        end0 = Location.create(w, -10, true);
        end1 = Location.create(w, 10, true);
        sel = Location.create(s, selMult * 20, true);
      } else if (dir == Direction.NORTH) {
        end0 = Location.create(-10, w, true);
        end1 = Location.create(10, w, true);
        sel = Location.create(selMult * -20, s, true);
      } else if (dir == Direction.SOUTH) {
        end0 = Location.create(-10, -w, true);
        end1 = Location.create(10, -w, true);
        sel = Location.create(selMult * -20, -s, true);
      } else {
        end0 = Location.create(-w, -10, true);
        end1 = Location.create(-w, 10, true);
        sel = Location.create(-s, selMult * 20, true);
      }
      ps[0] = new Port(end0.getX(), end0.getY(), Port.INPUT, data.getWidth());
      ps[1] = new Port(end1.getX(), end1.getY(), Port.INPUT, data.getWidth());
    } else {
      w = (size == PlexersLibrary.SIZE_NARROW ? 20 : 40);
      s = (size == PlexersLibrary.SIZE_NARROW ? 10 : 20);
      var dx = -(inputs / 2) * 10;
      var ddx = 10;
      var dy = -(inputs / 2) * 10;
      var ddy = 10;
      if (dir == Direction.WEST) {
        dx = w;
        ddx = 0;
        sel = Location.create(s, selMult * (dy + 10 * inputs), true);
      } else if (dir == Direction.NORTH) {
        dy = w;
        ddy = 0;
        sel = Location.create(selMult * dx, s, true);
      } else if (dir == Direction.SOUTH) {
        dy = -w;
        ddy = 0;
        sel = Location.create(selMult * dx, -s, true);
      } else {
        dx = -w;
        ddx = 0;
        sel = Location.create(-s, selMult * (dy + 10 * inputs), true);
      }
      for (var i = 0; i < inputs; i++) {
        ps[i] = new Port(dx, dy, Port.INPUT, data.getWidth());
        dx += ddx;
        dy += ddy;
      }
    }
    if (!wide && !vertical && botLeft && inputs > 2)
      sel = sel.translate(-10, 0); // left side, adjust selector left
    else if (!wide && vertical && !botLeft && inputs > 2)
      sel = sel.translate(0, -10); // top side, adjust selector up
    var en = sel.translate(dir, 10);
    ps[inputs] = new Port(sel.getX(), sel.getY(), Port.INPUT, select.getWidth());
    if (enable) {
      if (invertEnable) {
        en = vertical ? en.translate(0, 10) : en.translate(-10, 0);
      }
      ps[inputs + 1] = new Port(en.getX(), en.getY(), Port.INPUT, BitWidth.ONE);
    }
    ps[ps.length - 1] = new Port(0, 0, Port.OUTPUT, data.getWidth());

    for (var i = 0; i < inputs; i++) {
      ps[i].setToolTip(S.getter("multiplexerInTip", "" + i));
    }
    ps[inputs].setToolTip(S.getter("multiplexerSelectTip"));
    if (enable) {
      ps[inputs + 1].setToolTip(S.getter("multiplexerEnableTip"));
    }
    ps[ps.length - 1].setToolTip(S.getter("multiplexerOutTip"));

    instance.setPorts(ps);
  }
}
