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
import com.cburch.logisim.gui.icons.TransmissionGateIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;

public class TransmissionGate extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Transmission Gate";

  static final int OUTPUT = 0;
  static final int INPUT = 1;
  static final int GATE0 = 2;
  static final int GATE1 = 3;

  public TransmissionGate() {
    super(_ID, S.getter("transmissionGateComponent"));
    setIcon(new TransmissionGateIcon());
    setAttributes(
        new Attribute[] {StdAttr.FACING, StdAttr.SELECT_LOC, StdAttr.WIDTH},
        new Object[] {Direction.EAST, StdAttr.SELECT_TOP_RIGHT, BitWidth.ONE});
    setFacingAttribute(StdAttr.FACING);
    setKeyConfigurator(new BitWidthConfigurator(StdAttr.WIDTH));
  }

  private Value computeOutput(InstanceState state) {
    BitWidth width = state.getAttributeValue(StdAttr.WIDTH);
    Value input = state.getPortValue(INPUT);
    Value gate0 = state.getPortValue(GATE0);
    Value gate1 = state.getPortValue(GATE1);

    if (gate0.isFullyDefined() && gate1.isFullyDefined() && gate0 != gate1) {
      if (gate0 == Value.TRUE) {
        return Value.createUnknown(width);
      } else {
        return input;
      }
    } else {
      if (input.isFullyDefined()) {
        return Value.createError(width);
      } else {
        Value[] v = input.getAll();
        for (int i = 0; i < v.length; i++) {
          if (v[i] != Value.UNKNOWN) {
            v[i] = Value.ERROR;
          }
        }
        return Value.create(v);
      }
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public boolean contains(Location loc, AttributeSet attrs) {
    if (super.contains(loc, attrs)) {
      Direction facing = attrs.getValue(StdAttr.FACING);
      Location center = Location.create(0, 0, true).translate(facing, -20);
      return center.manhattanDistanceTo(loc) < 24;
    } else {
      return false;
    }
  }

  private void drawInstance(InstancePainter painter, boolean isGhost) {
    Bounds bds = painter.getBounds();
    Object powerLoc = painter.getAttributeValue(StdAttr.SELECT_LOC);
    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    boolean flip =
        (facing == Direction.NORTH || facing == Direction.WEST)
            == (powerLoc == StdAttr.SELECT_TOP_RIGHT);

    int degrees = Direction.WEST.toDegrees() - facing.toDegrees();
    if (flip) degrees += 180;
    double radians = Math.toRadians((degrees + 360) % 360);

    Graphics2D g = (Graphics2D) painter.getGraphics().create();
    g.rotate(radians, bds.getX() + 20, bds.getY() + 20);
    g.translate(bds.getX(), bds.getY());
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);

    Color gate0 = g.getColor();
    Color gate1 = gate0;
    Color input = gate0;
    Color output = gate0;
    Color platform = gate0;
    if (!isGhost && painter.getShowState()) {
      gate0 = painter.getPortValue(GATE0).getColor();
      gate1 = painter.getPortValue(GATE0).getColor();
      input = painter.getPortValue(INPUT).getColor();
      output = painter.getPortValue(OUTPUT).getColor();
      platform = computeOutput(painter).getColor();
    }

    g.setColor(flip ? input : output);
    g.drawLine(0, 20, 13, 20);
    g.drawLine(13, 14, 13, 26);

    g.setColor(flip ? output : input);
    g.drawLine(27, 20, 40, 20);
    g.drawLine(27, 14, 27, 26);

    g.setColor(gate0);
    g.drawLine(20, 38, 20, 40);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(17, 32, 6, 6);
    g.drawLine(11, 31, 29, 31);
    GraphicsUtil.switchToWidth(g, Wire.WIDTH);

    g.setColor(gate1);
    g.drawLine(20, 7, 20, 0);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawLine(11, 9, 29, 9);

    g.setColor(platform);
    g.drawLine(9, 13, 31, 13);
    g.drawLine(9, 27, 31, 27);
    GraphicsUtil.switchToWidth(g, 1);
    if (flip) { // arrow
      g.drawLine(19, 18, 21, 20);
      g.drawLine(19, 22, 21, 20);
    } else {
      g.drawLine(21, 18, 19, 20);
      g.drawLine(21, 22, 19, 20);
    }

    g.dispose();
  }

  @Override
  public Object getInstanceFeature(final Instance instance, Object key) {
    if (key == WireRepair.class) {
      return (WireRepair) data -> true;
    }
    return super.getInstanceFeature(instance, key);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(0, -20, 40, 40).rotate(Direction.WEST, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING || attr == StdAttr.SELECT_LOC) {
      instance.recomputeBounds();
      updatePorts(instance);
    } else if (attr == StdAttr.WIDTH) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    drawInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawInstance(painter, false);
  }

  @Override
  public void propagate(InstanceState state) {
    state.setPort(OUTPUT, computeOutput(state), 1);
  }

  private void updatePorts(Instance instance) {
    int dx = 0;
    int dy = 0;
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    if (facing == Direction.NORTH) {
      dy = 1;
    } else if (facing == Direction.EAST) {
      dx = -1;
    } else if (facing == Direction.SOUTH) {
      dy = -1;
    } else if (facing == Direction.WEST) {
      dx = 1;
    }

    Object powerLoc = instance.getAttributeValue(StdAttr.SELECT_LOC);
    boolean flip =
        (facing == Direction.NORTH || facing == Direction.WEST)
            == (powerLoc == StdAttr.SELECT_TOP_RIGHT);

    Port[] ports = new Port[4];
    ports[OUTPUT] = new Port(0, 0, Port.OUTPUT, StdAttr.WIDTH);
    ports[INPUT] = new Port(40 * dx, 40 * dy, Port.INPUT, StdAttr.WIDTH);
    if (flip) {
      ports[GATE1] = new Port(20 * (dx - dy), 20 * (dx + dy), Port.INPUT, 1);
      ports[GATE0] = new Port(20 * (dx + dy), 20 * (-dx + dy), Port.INPUT, 1);
    } else {
      ports[GATE0] = new Port(20 * (dx - dy), 20 * (dx + dy), Port.INPUT, 1);
      ports[GATE1] = new Port(20 * (dx + dy), 20 * (-dx + dy), Port.INPUT, 1);
    }
    ports[INPUT].setToolTip(S.getter("transmissionGateSource"));
    ports[OUTPUT].setToolTip(S.getter("transmissionGateDrain"));
    ports[GATE0].setToolTip(S.getter("transmissionGatePGate"));
    ports[GATE1].setToolTip(S.getter("transmissionGateNGate"));
    instance.setPorts(ports);
  }
}
