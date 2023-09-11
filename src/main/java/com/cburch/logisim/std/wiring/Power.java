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
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Graphics2D;

public class Power extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Power";

  private static class PowerHdlGeneratorFactory extends AbstractConstantHdlGeneratorFactory {
    @Override
    public long getConstant(AttributeSet attrs) {
      long ConstantValue = 0;
      for (int bit = 0; bit < attrs.getValue(StdAttr.WIDTH).getWidth(); bit++) {
        ConstantValue <<= 1;
        ConstantValue |= 1;
      }
      return ConstantValue;
    }
  }

  public Power() {
    super(_ID, S.getter("powerComponent"), new PowerHdlGeneratorFactory());
    setIconName("power.gif");
    setAttributes(
        new Attribute[] {StdAttr.FACING, StdAttr.WIDTH},
        new Object[] {Direction.NORTH, BitWidth.ONE});
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
      g.setColor(Value.repeat(Value.TRUE, width.getWidth()).getColor());
    }
    g.drawPolygon(new int[] {6, 14, 6}, new int[] {-8, 0, 8}, 3);

    g.dispose();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    return Bounds.create(0, -8, 15, 16)
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
    state.setPort(0, Value.repeat(Value.TRUE, width.getWidth()), 1);
  }
}
