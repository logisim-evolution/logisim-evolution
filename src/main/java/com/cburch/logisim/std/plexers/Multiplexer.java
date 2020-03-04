/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;

public class Multiplexer extends InstanceFactory {
  static void drawSelectCircle(Graphics g, Bounds bds, Location loc) {
    if (Math.min(bds.getHeight(), bds.getWidth()) <= 20) return; // no dot for narrow mode
    int locDelta = Math.max(bds.getHeight(), bds.getWidth()) <= 50 ? 8 : 6;
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
    super("Multiplexer", S.getter("multiplexerComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          Plexers.ATTR_SIZE,
          Plexers.ATTR_SELECT_LOC,
          Plexers.ATTR_SELECT,
          StdAttr.WIDTH,
          Plexers.ATTR_DISABLED,
          Plexers.ATTR_ENABLE
        },
        new Object[] {
          Direction.EAST, Plexers.SIZE_WIDE, Plexers.SELECT_BOTTOM_LEFT,
          Plexers.DEFAULT_SELECT, BitWidth.ONE, Plexers.DISABLED_ZERO,
          Plexers.DEFAULT_ENABLE
        });
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(Plexers.ATTR_SELECT, 1, 5, 0),
            new BitWidthConfigurator(StdAttr.WIDTH)));
    setIcon(new PlexerIcon(false,false));
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    if (attr == Plexers.ATTR_ENABLE) {
      int newer = ver.compareTo(LogisimVersion.get(2, 6, 4));
      return Boolean.valueOf(newer >= 0);
    } else {
      return super.getDefaultAttributeValue(attr, ver);
    }
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    StringBuffer CompleteName = new StringBuffer();
    CompleteName.append(CorrectLabel.getCorrectLabel(this.getName()));
    if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1) CompleteName.append("_bus");
    CompleteName.append(
        "_" + Integer.toString(1 << attrs.getValue(Plexers.ATTR_SELECT).getWidth()));
    return CompleteName.toString();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Object size = attrs.getValue(Plexers.ATTR_SIZE);
    boolean wide = size == Plexers.SIZE_WIDE;
    Direction dir = attrs.getValue(StdAttr.FACING);
    BitWidth select = attrs.getValue(Plexers.ATTR_SELECT);
    int inputs = 1 << select.getWidth();
    if (inputs == 2) {
      int w = (wide ? 30 : 20);
      return Bounds.create(-w, -20, w, 40).rotate(Direction.EAST, dir, 0, 0);
    } else {
      int w = (wide ? 40 : 20);
      int lengthAdjust = (wide ? 0 : -5);
      int offs = -(inputs / 2) * 10 - 10;
      int length = inputs * 10 + 20 + lengthAdjust;
      // narrow isn't symmetrical when switchinng selector sides, rotating
      if (!wide && (dir == Direction.SOUTH || dir == Direction.WEST)) offs += 5;
      return Bounds.create(-w, offs, w, length).rotate(Direction.EAST, dir, 0, 0);
    }
  }

  @Override
  public boolean HasThreeStateDrivers(AttributeSet attrs) {
    return (attrs.getValue(Plexers.ATTR_DISABLED) == Plexers.DISABLED_FLOATING);
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new MultiplexerHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING
        || attr == Plexers.ATTR_SELECT_LOC
        || attr == Plexers.ATTR_SELECT
        || attr == Plexers.ATTR_SIZE) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == StdAttr.WIDTH || attr == Plexers.ATTR_ENABLE) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == Plexers.ATTR_DISABLED) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Object size = painter.getAttributeValue(Plexers.ATTR_SIZE);
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    BitWidth select = painter.getAttributeValue(Plexers.ATTR_SELECT);
    Bounds bds = painter.getBounds();
    int lean;
    if (select.getWidth() == 1) lean = (size == Plexers.SIZE_NARROW ? 7 : 10);
    else lean = (size == Plexers.SIZE_NARROW ? 10 : 20);
    Plexers.drawTrapezoid(painter.getGraphics(), bds, facing, lean);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    Object size = painter.getAttributeValue(Plexers.ATTR_SIZE);
    boolean wide = size == Plexers.SIZE_WIDE;
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    BitWidth select = painter.getAttributeValue(Plexers.ATTR_SELECT);
    boolean enable = painter.getAttributeValue(Plexers.ATTR_ENABLE).booleanValue();
    int inputs = 1 << select.getWidth();

    // draw stubs for select/enable inputs that aren't on instance boundary
    GraphicsUtil.switchToWidth(g, 3);
    boolean vertical = facing != Direction.NORTH && facing != Direction.SOUTH;
    Object selectLoc = painter.getAttributeValue(Plexers.ATTR_SELECT_LOC);
    int selMult = selectLoc == Plexers.SELECT_BOTTOM_LEFT ? 1 : -1;
    boolean oddside = (vertical == (selMult < 0));
    int dx, dy;
    if (wide) {
      dx = vertical ? 0 : -2 * selMult;
      dy = vertical ? 2 * selMult : 0;
    } else if (vertical) {
      dx = (facing == Direction.EAST ? 1 : -1);
      ;
      dy = 2 * selMult;
    } else {
      dx = -2 * selMult;
      dy = (facing == Direction.SOUTH ? 1 : -1);
      ;
    }
    if (inputs == 2 || (!wide && oddside)) { // draw select wire
      Location pt = painter.getInstance().getPortLocation(inputs);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(inputs).getColor());
      }
      int len = (wide ? 2 : 1);
      g.drawLine(pt.getX() - len * dx, pt.getY() - len * dy, pt.getX(), pt.getY());
    }
    if (enable) {
      Location en = painter.getInstance().getPortLocation(inputs + 1);
      if (painter.getShowState()) {
        g.setColor(painter.getPortValue(inputs + 1).getColor());
      }
      int len = (inputs == 2) ? 3 : wide ? 2 : oddside ? 4 : 2;
      g.drawLine(en.getX() - len * dx, en.getY() - len * dy, en.getX(), en.getY());
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
    g.setColor(Color.BLACK);
    int lean;
    if (inputs == 2) {
      lean = (size == Plexers.SIZE_NARROW ? 7 : 10);
      Plexers.drawTrapezoid(g, bds, facing, lean);
    } else {
      lean = (size == Plexers.SIZE_NARROW ? 10 : 20);
      Plexers.drawTrapezoid(g, bds, facing, lean);
    }
    if (size == Plexers.SIZE_WIDE)
      GraphicsUtil.drawCenteredText(
          g, "MUX", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth data = state.getAttributeValue(StdAttr.WIDTH);
    BitWidth select = state.getAttributeValue(Plexers.ATTR_SELECT);
    boolean enable = state.getAttributeValue(Plexers.ATTR_ENABLE).booleanValue();
    int inputs = 1 << select.getWidth();
    Value en = enable ? state.getPortValue(inputs + 1) : Value.TRUE;
    Value out;
    if (en == Value.FALSE) {
      Object opt = state.getAttributeValue(Plexers.ATTR_DISABLED);
      Value base = opt == Plexers.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
      out = Value.repeat(base, data.getWidth());
    } else if (en == Value.ERROR && state.isPortConnected(inputs + 1)) {
      out = Value.createError(data);
    } else {
      Value sel = state.getPortValue(inputs);
      if (sel.isFullyDefined()) {
        out = state.getPortValue((int)sel.toLongValue());
      } else if (sel.isErrorValue()) {
        out = Value.createError(data);
      } else {
        out = Value.createUnknown(data);
      }
    }
    state.setPort(inputs + (enable ? 2 : 1), out, Plexers.DELAY);
  }

  private void updatePorts(Instance instance) {
    Object size = instance.getAttributeValue(Plexers.ATTR_SIZE);
    boolean wide = size == Plexers.SIZE_WIDE;
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    boolean vertical = dir != Direction.NORTH && dir != Direction.SOUTH;
    Object selectLoc = instance.getAttributeValue(Plexers.ATTR_SELECT_LOC);
    boolean botLeft = selectLoc == Plexers.SELECT_BOTTOM_LEFT;
    int selMult = botLeft ? 1 : -1;
    BitWidth data = instance.getAttributeValue(StdAttr.WIDTH);
    BitWidth select = instance.getAttributeValue(Plexers.ATTR_SELECT);
    boolean enable = instance.getAttributeValue(Plexers.ATTR_ENABLE).booleanValue();

    int inputs = 1 << select.getWidth();
    Port[] ps = new Port[inputs + (enable ? 3 : 2)];
    Location sel;
    int w, s;
    if (inputs == 2) {
      w = (size == Plexers.SIZE_NARROW ? 20 : 30);
      s = (size == Plexers.SIZE_NARROW ? 10 : 20);
      Location end0;
      Location end1;
      if (dir == Direction.WEST) {
        end0 = Location.create(w, -10);
        end1 = Location.create(w, 10);
        sel = Location.create(s, selMult * 20);
      } else if (dir == Direction.NORTH) {
        end0 = Location.create(-10, w);
        end1 = Location.create(10, w);
        sel = Location.create(selMult * -20, s);
      } else if (dir == Direction.SOUTH) {
        end0 = Location.create(-10, -w);
        end1 = Location.create(10, -w);
        sel = Location.create(selMult * -20, -s);
      } else {
        end0 = Location.create(-w, -10);
        end1 = Location.create(-w, 10);
        sel = Location.create(-s, selMult * 20);
      }
      ps[0] = new Port(end0.getX(), end0.getY(), Port.INPUT, data.getWidth());
      ps[1] = new Port(end1.getX(), end1.getY(), Port.INPUT, data.getWidth());
    } else {
      w = (size == Plexers.SIZE_NARROW ? 20 : 40);
      s = (size == Plexers.SIZE_NARROW ? 10 : 20);
      int dx = -(inputs / 2) * 10;
      int ddx = 10;
      int dy = -(inputs / 2) * 10;
      int ddy = 10;
      if (dir == Direction.WEST) {
        dx = w;
        ddx = 0;
        sel = Location.create(s, selMult * (dy + 10 * inputs));
      } else if (dir == Direction.NORTH) {
        dy = w;
        ddy = 0;
        sel = Location.create(selMult * dx, s);
      } else if (dir == Direction.SOUTH) {
        dy = -w;
        ddy = 0;
        sel = Location.create(selMult * dx, -s);
      } else {
        dx = -w;
        ddx = 0;
        sel = Location.create(-s, selMult * (dy + 10 * inputs));
      }
      for (int i = 0; i < inputs; i++) {
        ps[i] = new Port(dx, dy, Port.INPUT, data.getWidth());
        dx += ddx;
        dy += ddy;
      }
    }
    if (!wide && !vertical && botLeft)
      sel = sel.translate(-10, 0); // left side, adjust selector left
    else if (!wide && vertical && !botLeft)
      sel = sel.translate(0, -10); // top side, adjust selector up
    Location en = sel.translate(dir, 10);
    ps[inputs] = new Port(sel.getX(), sel.getY(), Port.INPUT, select.getWidth());
    if (enable) {
      ps[inputs + 1] = new Port(en.getX(), en.getY(), Port.INPUT, BitWidth.ONE);
    }
    ps[ps.length - 1] = new Port(0, 0, Port.OUTPUT, data.getWidth());

    for (int i = 0; i < inputs; i++) {
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
