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

public class Demultiplexer extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Demultiplexer";

  public Demultiplexer() {
    super(_ID, S.getter("demultiplexerComponent"), new DemultiplexerHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.SELECT_LOC,
          PlexersLibrary.ATTR_SELECT,
          StdAttr.WIDTH,
          PlexersLibrary.ATTR_TRISTATE,
          PlexersLibrary.ATTR_DISABLED,
          PlexersLibrary.ATTR_ENABLE
        },
        new Object[] {
          Direction.EAST,
          StdAttr.SELECT_BOTTOM_LEFT,
          PlexersLibrary.DEFAULT_SELECT,
          BitWidth.ONE,
          PlexersLibrary.DEFAULT_TRISTATE,
          PlexersLibrary.DISABLED_ZERO,
          PlexersLibrary.DEFAULT_ENABLE
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(PlexersLibrary.ATTR_SELECT, 1, 5, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));
    setFacingAttribute(StdAttr.FACING);
    setIcon(new PlexerIcon(true, false));
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    // for backward compatibility, after 2.6.4 the enable pin was "enabled" by default upto and
    // until 3.6.1
    if (attr == PlexersLibrary.ATTR_ENABLE) {
      return ver.compareTo(new LogisimVersion(3, 6, 1)) <= 0;
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
  public boolean contains(Location loc, AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING).reverse();
    return PlexersLibrary.contains(loc, getOffsetBounds(attrs), facing);
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
    final var facing = attrs.getValue(StdAttr.FACING);
    final var select = attrs.getValue(PlexersLibrary.ATTR_SELECT);
    final var outputs = 1 << select.getWidth();
    final var bds =
        (outputs == 2)
            ? Bounds.create(0, -25, 30, 50)
            : Bounds.create(0, -(outputs / 2) * 10 - 10, 40, outputs * 10 + 20);
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
    } else if (attr == StdAttr.WIDTH || attr == PlexersLibrary.ATTR_ENABLE) {
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
    final var select = painter.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enable = painter.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    final var outputs = 1 << select.getWidth();

    // draw select and enable inputs
    GraphicsUtil.switchToWidth(g, 3);
    final var vertical = facing == Direction.NORTH || facing == Direction.SOUTH;
    Object selectLoc = painter.getAttributeValue(StdAttr.SELECT_LOC);
    final var selMult = selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 1 : -1;
    final var dx = vertical ? selMult : 0;
    final var dy = vertical ? 0 : -selMult;
    if (outputs == 2) { // draw select wire
      final var sel = painter.getInstance().getPortLocation(outputs);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(outputs).getColor());
      }
      g.drawLine(sel.getX(), sel.getY(), sel.getX() + 2 * dx, sel.getY() + 2 * dy);
    }
    if (enable) {
      final var en = painter.getInstance().getPortLocation(outputs + 1);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(outputs + 1).getColor());
      }
      int len = outputs == 2 ? 6 : 4;
      g.drawLine(en.getX(), en.getY(), en.getX() + len * dx, en.getY() + len * dy);
    }
    GraphicsUtil.switchToWidth(g, 1);

    // draw a circle indicating where the select input is located
    Multiplexer.drawSelectCircle(g, bds, painter.getInstance().getPortLocation(outputs));

    // draw "0" next to first input
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

    // draw trapezoid, "DMX" label, and ports
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
        g, "DMX", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    // get attributes
    BitWidth data = state.getAttributeValue(StdAttr.WIDTH);
    BitWidth select = state.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    Boolean threeState = state.getAttributeValue(PlexersLibrary.ATTR_TRISTATE);
    boolean enable = state.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    int outputs = 1 << select.getWidth();
    Value en = enable ? state.getPortValue(outputs + 1) : Value.TRUE;

    // determine output values
    Value others; // the default output
    if (threeState) {
      others = Value.createUnknown(data);
    } else {
      others = Value.createKnown(data, 0);
    }
    int outIndex = -1; // the special output
    Value out = null;
    if (en == Value.FALSE) {
      Object opt = state.getAttributeValue(PlexersLibrary.ATTR_DISABLED);
      Value base = opt == PlexersLibrary.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
      others = Value.repeat(base, data.getWidth());
    } else if (en == Value.ERROR && state.isPortConnected(outputs + 1)) {
      others = Value.createError(data);
    } else {
      final var sel = state.getPortValue(outputs);
      if (sel.isFullyDefined()) {
        outIndex = (int) sel.toLongValue();
        out = state.getPortValue(outputs + (enable ? 2 : 1));
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
    final var data = instance.getAttributeValue(StdAttr.WIDTH);
    final var select = instance.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    final var enable = instance.getAttributeValue(PlexersLibrary.ATTR_ENABLE);
    var outputs = 1 << select.getWidth();
    final var ps = new Port[outputs + (enable ? 3 : 2)];
    Location sel;
    int selMult = selectLoc == StdAttr.SELECT_BOTTOM_LEFT ? 1 : -1;
    if (outputs == 2) {
      Location end0;
      Location end1;
      if (facing == Direction.WEST) {
        end0 = Location.create(-30, -10, true);
        end1 = Location.create(-30, 10, true);
        sel = Location.create(-20, selMult * 20, true);
      } else if (facing == Direction.NORTH) {
        end0 = Location.create(-10, -30, true);
        end1 = Location.create(10, -30, true);
        sel = Location.create(selMult * -20, -20, true);
      } else if (facing == Direction.SOUTH) {
        end0 = Location.create(-10, 30, true);
        end1 = Location.create(10, 30, true);
        sel = Location.create(selMult * -20, 20, true);
      } else {
        end0 = Location.create(30, -10, true);
        end1 = Location.create(30, 10, true);
        sel = Location.create(20, selMult * 20, true);
      }
      ps[0] = new Port(end0.getX(), end0.getY(), Port.OUTPUT, data.getWidth());
      ps[1] = new Port(end1.getX(), end1.getY(), Port.OUTPUT, data.getWidth());
    } else {
      int dx = -(outputs / 2) * 10;
      var ddx = 10;
      var dy = dx;
      var ddy = 10;
      if (facing == Direction.WEST) {
        dx = -40;
        ddx = 0;
        sel = Location.create(-20, selMult * (dy + 10 * outputs), true);
      } else if (facing == Direction.NORTH) {
        dy = -40;
        ddy = 0;
        sel = Location.create(selMult * dx, -20, true);
      } else if (facing == Direction.SOUTH) {
        dy = 40;
        ddy = 0;
        sel = Location.create(selMult * dx, 20, true);
      } else {
        dx = 40;
        ddx = 0;
        sel = Location.create(20, selMult * (dy + 10 * outputs), true);
      }
      for (var i = 0; i < outputs; i++) {
        ps[i] = new Port(dx, dy, Port.OUTPUT, data.getWidth());
        dx += ddx;
        dy += ddy;
      }
    }
    final var en = sel.translate(facing, -10);
    ps[outputs] = new Port(sel.getX(), sel.getY(), Port.INPUT, select.getWidth());
    if (enable) {
      ps[outputs + 1] = new Port(en.getX(), en.getY(), Port.INPUT, BitWidth.ONE);
    }
    ps[ps.length - 1] = new Port(0, 0, Port.INPUT, data.getWidth());

    for (var i = 0; i < outputs; i++) {
      ps[i].setToolTip(S.getter("demultiplexerOutTip", "" + i));
    }
    ps[outputs].setToolTip(S.getter("demultiplexerSelectTip"));
    if (enable) {
      ps[outputs + 1].setToolTip(S.getter("demultiplexerEnableTip"));
    }
    ps[ps.length - 1].setToolTip(S.getter("demultiplexerInTip"));

    instance.setPorts(ps);
  }
}
