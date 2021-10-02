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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;

public class PriorityEncoder extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Priority Encoder";

  static final int OUT = 0;
  static final int EN_IN = 1;
  static final int EN_OUT = 2;
  static final int GS = 3;

  public PriorityEncoder() {
    super(_ID, S.getter("priorityEncoderComponent"), new PriorityEncoderHdlGeneratorFactory());
    setAttributes(
        new Attribute[] {StdAttr.FACING, PlexersLibrary.ATTR_SELECT, PlexersLibrary.ATTR_DISABLED},
        new Object[] {Direction.EAST, BitWidth.create(3), PlexersLibrary.DISABLED_ZERO});
    setKeyConfigurator(new BitWidthConfigurator(PlexersLibrary.ATTR_SELECT, 1, 5, 0));
    setIcon(new ArithmeticIcon("Pri"));
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var dir = attrs.getValue(StdAttr.FACING);
    final var select = attrs.getValue(PlexersLibrary.ATTR_SELECT);
    final var inputs = 1 << select.getWidth();
    final var offs = -5 * inputs;
    final var len = 10 * inputs + 10;
    if (dir == Direction.NORTH) {
      return Bounds.create(offs, 0, len, 40);
    } else if (dir == Direction.SOUTH) {
      return Bounds.create(offs, -40, len, 40);
    } else if (dir == Direction.WEST) {
      return Bounds.create(0, offs, 40, len);
    } else { // dir == Direction.EAST
      return Bounds.create(-40, offs, 40, len);
    }
  }

  @Override
  public boolean hasThreeStateDrivers(AttributeSet attrs) {
    return (attrs.getValue(PlexersLibrary.ATTR_DISABLED) == PlexersLibrary.DISABLED_FLOATING);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == PlexersLibrary.ATTR_SELECT) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == PlexersLibrary.ATTR_DISABLED) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var facing = painter.getAttributeValue(StdAttr.FACING);

    painter.drawBounds();
    final var bds = painter.getBounds();
    g.setColor(Color.GRAY);
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
    GraphicsUtil.drawText(g, "0", x0, y0, halign, GraphicsUtil.V_BASELINE);
    g.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(g, "Pri", bds.getX() + bds.getWidth() / 2, bds.getY() + bds.getHeight() / 2);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    final var select = state.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    var n = 1 << select.getWidth();
    final var enabled = state.getPortValue(n + EN_IN) != Value.FALSE;

    int out = -1;
    Value outDefault;
    if (enabled) {
      outDefault = Value.createUnknown(select);
      for (int i = n - 1; i >= 0; i--) {
        if (state.getPortValue(i) == Value.TRUE) {
          out = i;
          break;
        }
      }
    } else {
      Object opt = state.getAttributeValue(PlexersLibrary.ATTR_DISABLED);
      Value base = opt == PlexersLibrary.DISABLED_ZERO ? Value.FALSE : Value.UNKNOWN;
      outDefault = Value.repeat(base, select.getWidth());
    }
    if (out < 0) {
      state.setPort(n + OUT, outDefault, PlexersLibrary.DELAY);
      state.setPort(n + EN_OUT, enabled ? Value.TRUE : Value.FALSE, PlexersLibrary.DELAY);
      state.setPort(n + GS, Value.FALSE, PlexersLibrary.DELAY);
    } else {
      state.setPort(n + OUT, Value.createKnown(select, out), PlexersLibrary.DELAY);
      state.setPort(n + EN_OUT, Value.FALSE, PlexersLibrary.DELAY);
      state.setPort(n + GS, Value.TRUE, PlexersLibrary.DELAY);
    }
  }

  private void updatePorts(Instance instance) {
    Object dir = instance.getAttributeValue(StdAttr.FACING);
    final var select = instance.getAttributeValue(PlexersLibrary.ATTR_SELECT);
    var n = 1 << select.getWidth();
    final var ps = new Port[n + 4];
    if (dir == Direction.NORTH || dir == Direction.SOUTH) {
      int x = -5 * n + 10;
      int y = dir == Direction.NORTH ? 40 : -40;
      for (var i = 0; i < n; i++) {
        ps[i] = new Port(x + 10 * i, y, Port.INPUT, 1);
      }
      ps[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
      ps[n + EN_IN] = new Port(x + 10 * n, y / 2, Port.INPUT, 1);
      ps[n + EN_OUT] = new Port(x - 10, y / 2, Port.OUTPUT, 1);
      ps[n + GS] = new Port(10, 0, Port.OUTPUT, 1);
    } else {
      int x = dir == Direction.EAST ? -40 : 40;
      int y = -5 * n + 10;
      for (var i = 0; i < n; i++) {
        ps[i] = new Port(x, y + 10 * i, Port.INPUT, 1);
      }
      ps[n + OUT] = new Port(0, 0, Port.OUTPUT, select.getWidth());
      ps[n + EN_IN] = new Port(x / 2, y + 10 * n, Port.INPUT, 1);
      ps[n + EN_OUT] = new Port(x / 2, y - 10, Port.OUTPUT, 1);
      ps[n + GS] = new Port(0, 10, Port.OUTPUT, 1);
    }

    for (var i = 0; i < n; i++) {
      ps[i].setToolTip(S.getter("priorityEncoderInTip", "" + i));
    }
    ps[n + OUT].setToolTip(S.getter("priorityEncoderOutTip"));
    ps[n + EN_IN].setToolTip(S.getter("priorityEncoderEnableInTip"));
    ps[n + EN_OUT].setToolTip(S.getter("priorityEncoderEnableOutTip"));
    ps[n + GS].setToolTip(S.getter("priorityEncoderGroupSignalTip"));

    instance.setPorts(ps);
  }
}
