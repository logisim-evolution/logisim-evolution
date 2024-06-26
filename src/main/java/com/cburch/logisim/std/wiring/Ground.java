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

import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.GroundIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;

public class Ground extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Ground";

  public Ground() {
    super(_ID, S.getter("groundComponent"), new AbstractConstantHdlGeneratorFactory());
    setIcon(new GroundIcon());
    setAttributes(
        new Attribute[] {StdAttr.FACING, StdAttr.WIDTH},
        new Object[] {Direction.SOUTH, BitWidth.ONE});
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
    setPorts(new Port[] {new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH)});
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
  }

  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    Location loc = painter.getLocation();
    g.translate(loc.getX(), loc.getY());

    Direction from = painter.getAttributeValue(StdAttr.FACING);
    int degrees = Direction.EAST.toDegrees() - from.toDegrees();
    double radians = Math.toRadians((degrees + 360) % 360);
    g.rotate(radians);

    GraphicsUtil.switchToWidth(g, Wire.WIDTH);
    if (!isGhost && painter.getShowState()) {
      g.setColor(painter.getPortValue(0).getColor());
    }
    g.drawLine(0, 0, 5, 0);

    GraphicsUtil.switchToWidth(g, 1);
    if (!isGhost && painter.shouldDrawColor()) {
      BitWidth width = painter.getAttributeValue(StdAttr.WIDTH);
      g.setColor(Value.repeat(Value.FALSE, width.getWidth()).getColor());
    }
    g.drawLine(6, -8, 6, 8);
    g.drawLine(9, -5, 9, 5);
    g.drawLine(12, -2, 12, 2);

    g.dispose();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(0, -8, 14, 16)
        .rotate(Direction.EAST, attrs.getValue(StdAttr.FACING), 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    state.setPort(0, Value.repeat(Value.FALSE, width.getWidth()), 1);
  }
}
