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
 * Original code by Marcin Orlowski (http://MarcinOrlowski.com), 2021
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.*;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.LedClusterIcon;
import com.cburch.logisim.instance.*;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;

import java.awt.*;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

import static com.cburch.logisim.std.Strings.S;

public class LedCluster extends InstanceFactory {
  public static final String name = "LED Cluster";

  private static class State implements InstanceData, Cloneable {
    private int value;
    private final int size;

    public State(int value, int size) {
      this.value = value;
      this.size = size;
    }

    public boolean bitSet(int bitIndex) {
      if (bitIndex >= size) {
        return false;
      }
      return (value & (1 << bitIndex)) != 0;
    }

    public void setValue(int value) {
      this.value = value;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }
  }

  public static ArrayList<String> getLabels(int size) {
    ArrayList<String> labelNames = new ArrayList<>();
    for (int i = 0; i < size; i++) {
      labelNames.add(getInputLabel(i));
    }
    return labelNames;
  }

  public static String getInputLabel(int id) {
    return "lc_" + (id + 1);
  }

  public static final int MAX_SEGMENTS = 32;
  public static final int MIN_SEGMENTS = 1;

  public static final Attribute<BitWidth> ATTR_SIZE =
      Attributes.forBitWidth("number", S.getter("ledCLusterNrOfSegments"),
              MIN_SEGMENTS, MAX_SEGMENTS);

  public LedCluster() {
    super(name, S.getter("ledClusterComponent"));
    int clusterSize = 8;
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          Io.ATTR_ON_COLOR,
          Io.ATTR_OFF_COLOR,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          ATTR_SIZE,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.SOUTH,
          new Color(0, 240, 0),
          Color.gray,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          BitWidth.create(clusterSize),
          new ComponentMapInformationContainer(0, clusterSize, 0, getLabels(clusterSize), null, null)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new LedClusterIcon());
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(ATTR_SIZE),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));

    System.out.print(LedCluster.class.getSimpleName()+"\n");
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
    int clusterSize = instance.getAttributeValue(ATTR_SIZE).getWidth();
    instance.getAttributeSet().setValue(StdAttr.MAPINFO,
      new ComponentMapInformationContainer(0, clusterSize, 0, getLabels(clusterSize), null, null));
  }

  private void updatePorts(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    int n = instance.getAttributeValue(ATTR_SIZE).getWidth();
    int cx = 0, cy = 0, dx = 0, dy = 0;
    if (facing == Direction.WEST) {
      dy = -10;
    } else if (facing == Direction.EAST) {
      dy = 10;
    } else if (facing == Direction.SOUTH) {
      cx = -10 * (n + 1);
      dx = 10;
    } else {
      dx = 10;
    }
    Port[] ps = new Port[n];
    for (int i = 0; i < ps.length; i++) {
      ps[i] = new Port(cx + (i + 1) * dx, cy + (i + 1) * dy, Port.INPUT, 1);
      ps[i].setToolTip(S.getter("Input: LED" + (i + 1)));
    }
    instance.setPorts(ps);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    int n = attrs.getValue(ATTR_SIZE).getWidth();
    return Bounds.create(0, 0, (n + 1) * 10, 40).rotate(Direction.NORTH, facing, 0, 0);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == ATTR_SIZE) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
      ComponentMapInformationContainer map = instance.getAttributeValue(StdAttr.MAPINFO);
      if (map != null) {
        map.setNrOfInports(instance.getAttributeValue(ATTR_SIZE).getWidth(),
            getLabels(instance.getAttributeValue(ATTR_SIZE).getWidth()));
      }
    } else if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    LedCluster.State state = (LedCluster.State) painter.getData();
    if (state == null || state.size != painter.getAttributeValue(ATTR_SIZE).getWidth()) {
      int val = (state == null) ? 0 : state.value;
      state = new LedCluster.State(val, painter.getAttributeValue(ATTR_SIZE).getWidth());
      painter.setData(state);
    }
    int n = painter.getAttributeValue(ATTR_SIZE).getWidth();

    Direction facing = painter.getAttributeValue(StdAttr.FACING);
    Location loc = painter.getLocation();
    int x = loc.getX();
    int y = loc.getY();
    if (facing == Direction.SOUTH) {
      x -= 10 * (n + 1);
      y -= 40;
    }
    Graphics g = painter.getGraphics();
    g.translate(x, y);
    double rotate = 0.0;
    if (facing != Direction.NORTH && facing != Direction.SOUTH && g instanceof Graphics2D) {
      rotate = -facing.getRight().toRadians();
      ((Graphics2D) g).rotate(rotate);
    }
    g.setColor(Color.DARK_GRAY);
    g.fillRect(1, 1, (n + 1) * 10 - 2, 40 - 2);

    for (int i = 0; i < n; i++) {
      g.setColor(painter.getAttributeValue(state.bitSet(i) ? Io.ATTR_ON_COLOR : Io.ATTR_OFF_COLOR));
      g.fillRect(7 + (i * 10), 7, 6, 27);
    }

    if (rotate != 0.0) {
      ((Graphics2D) g).rotate(-rotate);
    }
    g.translate(-x, -y);

    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    State pins = (State) state.getData();
    int value = 0;
    for (int i = 0; i < pins.size; i++) {
      Value val = state.getPortValue(i);
      if (val == Value.TRUE) value |= 1 << i;

    }
    State data = (State) state.getData();
    if (data == null) {
      state.setData(new State(value, state.getAttributeValue(ATTR_SIZE).getWidth()));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }
}
