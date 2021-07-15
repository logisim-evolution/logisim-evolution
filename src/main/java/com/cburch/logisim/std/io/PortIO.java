/*
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

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.KeyEvent;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.Arrays;

public class PortIO extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "PortIO";

  public static ArrayList<String> GetLabels(int size) {
    ArrayList<String> LabelNames = new ArrayList<>();
    for (var i = 0; i < size; i++) {
      LabelNames.add("pin_" + (i + 1));
    }
    return LabelNames;
  }

  private static class PortState implements InstanceData, Cloneable {

    Value[] pin; // pindata = usrdata + indata
    Value[] usr; // usrdata
    int size;

    public PortState(int size) {
      this.size = size;
      final var nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);
      pin = new Value[nBus];
      usr = new Value[nBus];
      for (var i = 0; i < nBus; i++) {
        final var n = (Math.min(size, BitWidth.MAXWIDTH));
        pin[i] = Value.createUnknown(BitWidth.create(n));
        usr[i] = Value.createUnknown(BitWidth.create(n));
        size -= n;
      }
    }

    public void resize(int sz) {
      final var nBus = (((sz - 1) / BitWidth.MAXWIDTH) + 1);
      if (nBus != (((size - 1) / BitWidth.MAXWIDTH) + 1)) {
        pin = Arrays.copyOf(pin, nBus);
        usr = Arrays.copyOf(usr, nBus);
      }
      for (var i = 0; i < nBus; i++) {
        final var n = (Math.min(sz, BitWidth.MAXWIDTH));
        if (pin[i] == null)
          pin[i] = Value.createUnknown(BitWidth.create(n));
        else
          pin[i] = pin[i].extendWidth(n, Value.UNKNOWN);
        if (usr[i] == null)
          usr[i] = Value.createUnknown(BitWidth.create(n));
        else
          usr[i] = usr[i].extendWidth(n, Value.UNKNOWN);
      }
      size = sz;
    }

    public void toggle(int i) {
      final var n = i / BitWidth.MAXWIDTH;
      i = i % BitWidth.MAXWIDTH;
      var v = usr[n].get(i);
      if (v == Value.UNKNOWN)
        v = Value.FALSE;
      else if (v == Value.FALSE)
        v = Value.TRUE;
      else
        v = Value.UNKNOWN;
      usr[n] = usr[n].set(i, v);
    }

    public Value get(int i) {
      return pin[i / BitWidth.MAXWIDTH].get(i % BitWidth.MAXWIDTH);
    }

    public Color getColor(int i) {
      final var v = get(i);
      return v == Value.UNKNOWN ? Color.LIGHT_GRAY : v.getColor();
    }

    @Override
    public Object clone() {
      try {
        final var other = (PortState) super.clone();
        other.pin = Arrays.copyOf(pin, pin.length);
        other.usr = Arrays.copyOf(usr, usr.length);
        return other;
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  public static class PortPoker extends InstancePoker {
    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      final var loc = state.getInstance().getLocation();
      final var cx = e.getX() - loc.getX() - 7 + 2;
      final var cy = e.getY() - loc.getY() - 25 + 2;
      if (cx < 0 || cy < 0) return;
      final var i = cx / 10;
      final var j = cy / 10;
      if (j > 1) return;
      final var n = 2 * i + j;
      final var data = getState(state);
      if (n < 0 || n >= data.size) return;
      data.toggle(n);
      state.getInstance().fireInvalidated();
    }
  }

  public static final int MAX_IO = 128;
  public static final int MIN_IO = 2;
  private static final int INITPORTSIZE = 8;
  public static final Attribute<BitWidth> ATTR_SIZE =
      Attributes.forBitWidth("number", S.getter("pioNumber"), MIN_IO, MAX_IO);

  public static final AttributeOption INPUT = new AttributeOption("onlyinput", S.getter("pioInput"));
  public static final AttributeOption OUTPUT = new AttributeOption("onlyOutput", S.getter("pioOutput"));
  public static final AttributeOption INOUTSE = new AttributeOption("IOSingleEnable", S.getter("pioIOSingle"));
  public static final AttributeOption INOUTME = new AttributeOption("IOMultiEnable", S.getter("pioIOMultiple"));

  public static final Attribute<AttributeOption> ATTR_DIR =
      Attributes.forOption("direction", S.getter("pioDirection"), new AttributeOption[] {INPUT, OUTPUT, INOUTSE, INOUTME});

  protected static final int DELAY = 1;

  public PortIO() {
    super(_ID, S.getter("pioComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          ATTR_SIZE,
          ATTR_DIR,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.EAST,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          false,
          BitWidth.create(INITPORTSIZE),
          INOUTSE,
          new ComponentMapInformationContainer(0, 0, INITPORTSIZE, null, null, GetLabels(INITPORTSIZE))
        });
    setFacingAttribute(StdAttr.FACING);
    setIconName("pio.gif");
    setKeyConfigurator(JoinedConfigurator.create(
            new BitWidthConfigurator(ATTR_SIZE, MIN_IO, MAX_IO, KeyEvent.ALT_DOWN_MASK),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
    setInstancePoker(PortPoker.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_BOTTOM);
    ComponentMapInformationContainer map = instance.getAttributeSet().getValue(StdAttr.MAPINFO);
    if (map == null) {
      map = new ComponentMapInformationContainer(0, 0, INITPORTSIZE, null, null, GetLabels(INITPORTSIZE));
      instance.getAttributeSet().setValue(ATTR_SIZE, BitWidth.create(INITPORTSIZE));
      instance.getAttributeSet().setValue(ATTR_DIR, INOUTSE);
    }
    instance.getAttributeSet().setValue(StdAttr.MAPINFO, map.clone());
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var dir = instance.getAttributeValue(ATTR_DIR);
    final var size = instance.getAttributeValue(ATTR_SIZE).getWidth();
    // logisim max bus size is BitWidth.MAXWIDTH, so use multiple buses if needed
    final var nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);
    var nPorts = -1;
    if (dir == INPUT || dir == OUTPUT)
      nPorts = nBus;
    else if (dir == INOUTME)
      nPorts = 3 * nBus;
    else if (dir == INOUTSE)
      nPorts = 2 * nBus + 1;
    Port[] ps = new Port[nPorts];
    var p = 0;

    var x = 0;
    var y = 0;
    var dx = 0;
    var dy = 0;
    if (facing == Direction.NORTH)
      dy = -10;
    else if (facing == Direction.SOUTH)
      dy = 10;
    else if (facing == Direction.WEST)
      dx = -10;
    else
      dx = 10;
    if (dir == INPUT || dir == OUTPUT) {
      x += dx;
      y += dy;
    }
    if (dir == INOUTSE) {
      ps[p] = new Port(x - dy, y + dx, Port.INPUT, 1);
      ps[p].setToolTip(S.getter("pioOutEnable"));
      p++;
      x += dx;
      y += dy;
    }
    var n = size;
    var i = 0;
    while (n > 0) {
      final var e = Math.min(n, BitWidth.MAXWIDTH);
      final var range = "[" + i + "..." + (i + e - 1) + "]";
      if (dir == INOUTME) {
        ps[p] = new Port(x - dy, y + dx, Port.INPUT, e);
        ps[p].setToolTip(S.getter("pioOutEnables", range));
        p++;
        x += dx;
        y += dy;
      }
      if (dir == OUTPUT || dir == INOUTSE || dir == INOUTME) {
        ps[p] = new Port(x, y, Port.INPUT, e);
        ps[p].setToolTip(S.getter("pioOutputs", range));
        p++;
        x += dx;
        y += dy;
      }
      i += BitWidth.MAXWIDTH;
      n -= e;
    }
    n = size;
    i = 0;
    while (n > 0) {
      final var e = Math.min(n, BitWidth.MAXWIDTH);
      String range = "[" + i + "..." + (i + e - 1) + "]";
      if (dir == INPUT || dir == INOUTSE || dir == INOUTME) {
        ps[p] = new Port(x, y, Port.OUTPUT, e);
        ps[p].setToolTip(S.getter("pioInputs", range));
        p++;
        x += dx;
        y += dy;
      }
      i += BitWidth.MAXWIDTH;
      n -= e;
    }
    instance.setPorts(ps);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    var n = attrs.getValue(ATTR_SIZE).getWidth();
    if (n < 8)
      n = 8;
    return Bounds.create(0, 0, 10 + n / 2 * 10, 50).rotate(Direction.EAST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_BOTTOM);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_BOTTOM);
    } else if (attr == ATTR_SIZE || attr == ATTR_DIR) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_BOTTOM);
      ComponentMapInformationContainer map = instance.getAttributeValue(StdAttr.MAPINFO);
      if (map != null) {
        final var nrPins = instance.getAttributeValue(ATTR_SIZE).getWidth();
        var inputs = 0;
        var outputs = 0;
        var ios = 0;
        final var labels = GetLabels(nrPins);
        if (instance.getAttributeValue(ATTR_DIR) == INPUT) {
          inputs = nrPins;
        } else if (instance.getAttributeValue(ATTR_DIR) == OUTPUT) {
          outputs = nrPins;
        } else {
          ios = nrPins;
        }
        map.setNrOfInports(inputs, labels);
        map.setNrOfOutports(outputs, labels);
        map.setNrOfInOutports(ios, labels);
      }
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var facing = painter.getAttributeValue(StdAttr.FACING);

    final var bds = painter.getBounds().rotate(Direction.EAST, facing, 0, 0);
    final var w = bds.getWidth();
    final var h = bds.getHeight();
    final var x = painter.getLocation().getX();
    final var y = painter.getLocation().getY();
    final var g = painter.getGraphics();
    g.translate(x, y);
    var rotate = 0.0;
    if (facing != Direction.EAST) {
      rotate = -facing.toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.DARK_GRAY);
    int[] bx = {1, 1, 5, w - 6, w - 2, w - 2, 1};
    int[] by = {20, h - 8, h - 4, h - 4, h - 8, 20, 20};
    g.fillPolygon(bx, by, 6);
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 1);
    g.drawPolyline(bx, by, 7);

    final var size = painter.getAttributeValue(ATTR_SIZE).getWidth();
    final var nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);
    if (!painter.getShowState()) {
      g.setColor(Color.LIGHT_GRAY);
      for (var i = 0; i < size; i++)
        g.fillRect(7 + ((i / 2) * 10), 25 + (i % 2) * 10, 6, 6);
    }  else {
      PortState data = getState(painter);
      for (var i = 0; i < size; i++) {
        g.setColor(data.getColor(i));
        g.fillRect(7 + ((i / 2) * 10), 25 + (i % 2) * 10, 6, 6);
      }
    }
    g.setColor(Color.BLACK);
    AttributeOption dir = painter.getAttributeValue(ATTR_DIR);
    var px = ((dir == INOUTSE || dir == INOUTME) ? 0 : 10);
    final var py = 0;
    for (var p = 0; p < nBus; p++) {
      if (dir == INOUTSE) {
        GraphicsUtil.switchToWidth(g, 3);
        if (p == 0) {
          g.drawLine(px, py + 10, px + 6, py + 10);
          px += 10;
        } else {
          g.drawLine(px - 6, py + 10, px - 4, py + 10);
        }
      }
      if (dir == INOUTME) {
        GraphicsUtil.switchToWidth(g, 3);
        g.drawLine(px, py + 10, px + 6, py + 10);
        px += 10;
      }
      if (dir == OUTPUT || dir == INOUTSE || dir == INOUTME) {
        GraphicsUtil.switchToWidth(g, 3);
        g.drawLine(px, py, px, py + 4);
        g.drawLine(px, py + 15, px, py + 20);
        GraphicsUtil.switchToWidth(g, 2);
        int[] xp = {px, px - 4, px + 4, px};
        int[] yp = {py + 15, py + 5, py + 5, py + 15};
        g.drawPolyline(xp, yp, 4);
        px += 10;
      }
    }

    for (var p = 0; p < nBus; p++) {
      if (dir == INPUT || dir == INOUTSE || dir == INOUTME) {
        GraphicsUtil.switchToWidth(g, 3);
        g.drawLine(px, py, px, py + 5);
        g.drawLine(px, py + 16, px, py + 20);
        GraphicsUtil.switchToWidth(g, 2);
        int[] xp = {px, px - 4, px + 4, px};
        int[] yp = {py + 6, py + 16, py + 16, py + 6};
        g.drawPolyline(xp, yp, 4);
        px += 10;
      }
    }

    GraphicsUtil.switchToWidth(g, 1);
    ((Graphics2D) g).rotate(-rotate);
    g.translate(-x, -y);

    painter.drawPorts();
    g.setColor(painter.getAttributeValue(StdAttr.LABEL_COLOR));
    painter.drawLabel();
  }

  private static PortState getState(InstanceState state) {
    final var size = state.getAttributeValue(ATTR_SIZE).getWidth();
    var data = (PortState) state.getData();
    if (data == null) {
      data = new PortState(size);
      state.setData(data);
      return data;
    }
    if (data.size != size)
      data.resize(size);
    return data;
  }

  @Override
  public void propagate(InstanceState state) {
    final var dir = state.getAttributeValue(ATTR_DIR);
    final var size = state.getAttributeValue(ATTR_SIZE).getWidth();
    final var nBus = (((size - 1) / BitWidth.MAXWIDTH) + 1);

    final var data = getState(state);

    if (dir == OUTPUT) {
      for (var i = 0; i < nBus; i++) {
        data.pin[i] = state.getPortValue(i);
      }
    } else if (dir == INPUT) {
      for (var i = 0; i < nBus; i++) {
        data.pin[i] = data.usr[i];
        state.setPort(i, data.pin[i], DELAY);
      }
    } else if (dir == INOUTSE) {
      final var en = state.getPortValue(0);
      // pindata = usrdata + en.controls(indata)
      // where "+" resolves like:
      //     Z 0 1 E
      //     -------
      // Z | Z 0 1 E
      // 0 | 0 0 E E
      // 1 | 1 E 1 E
      for (var i = 0; i < nBus; i++) {
        final var in = state.getPortValue(i + 1);
        data.pin[i] = data.usr[i].combine(en.controls(in));
        state.setPort(1 + nBus + i, data.pin[i], DELAY);
      }
    } else if (dir == INOUTME) {
      for (var i = 0; i < nBus; i++) {
        final var en = state.getPortValue(i * 2);
        final var in = state.getPortValue(i * 2 + 1);
        data.pin[i] = data.usr[i].combine(en.controls(in));
        state.setPort(2 * nBus + i, data.pin[i], DELAY);
      }
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new PortHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
