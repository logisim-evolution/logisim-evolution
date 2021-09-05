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

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.DipswitchIcon;
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

public class DipSwitch extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "DipSwitch";

  public static class Poker extends InstancePoker {

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      final var val = (State) state.getData();
      final var loc = state.getInstance().getLocation();
      final var facing = state.getInstance().getAttributeValue(StdAttr.FACING);
      final var n = state.getInstance().getAttributeValue(ATTR_SIZE).getWidth();
      int i;
      if (facing == Direction.SOUTH) {
        i = n + (e.getX() - loc.getX() - 5) / 10;
      } else if (facing == Direction.EAST) {
        i = (e.getY() - loc.getY() - 5) / 10;
      } else if (facing == Direction.WEST) {
        i = (loc.getY() - e.getY() - 5) / 10;
      } else {
        i = (e.getX() - loc.getX() - 5) / 10;
      }
      val.ToggleBit(i);
      state.getInstance().fireInvalidated();
    }
  }

  private static class State implements InstanceData, Cloneable {

    private int Value;
    private final int size;

    public State(int value, int size) {
      Value = value;
      this.size = size;
    }

    public boolean BitSet(int bitindex) {
      if (bitindex >= size) {
        return false;
      }
      int mask = 1 << bitindex;
      return (Value & mask) != 0;
    }

    @Override
    public Object clone() {
      try {
        return super.clone();
      } catch (CloneNotSupportedException e) {
        return null;
      }
    }

    public void ToggleBit(int bitindex) {
      if ((bitindex < 0) || (bitindex >= size)) {
        return;
      }
      int mask = 1 << bitindex;
      Value ^= mask;
    }
  }

  public static ArrayList<String> GetLabels(int size) {
    final var labelNames = new ArrayList<String>();
    for (var i = 0; i < size; i++) {
      labelNames.add(getInputLabel(i));
    }
    return labelNames;
  }

  public static String getInputLabel(int id) {
    return "sw_" + (id + 1);
  }

  public static final int MAX_SWITCH = 32;
  public static final int MIN_SWITCH = 2;

  public static final Attribute<BitWidth> ATTR_SIZE =
      Attributes.forBitWidth("number", S.getter("nrOfSwitch"), MIN_SWITCH, MAX_SWITCH);

  public DipSwitch() {
    super(_ID, S.getter("DipSwitchComponent"));
    var dipSize = 8;
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          ATTR_SIZE,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.NORTH,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          BitWidth.create(dipSize),
          new ComponentMapInformationContainer(dipSize, 0, 0, GetLabels(dipSize), null, null)
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new DipswitchIcon());
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(ATTR_SIZE),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));
    setInstancePoker(Poker.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
    int dipSize = instance.getAttributeValue(ATTR_SIZE).getWidth();
    instance.getAttributeSet().setValue(StdAttr.MAPINFO, new ComponentMapInformationContainer(dipSize, 0, 0, GetLabels(dipSize), null, null));
  }

  private void updatePorts(Instance instance) {
    final var facing = instance.getAttributeValue(StdAttr.FACING);
    final var n = instance.getAttributeValue(ATTR_SIZE).getWidth();
    int cx = 0, cy = 0, dx = 0, dy = 0;
    if (facing == Direction.WEST) {
      // cy = -10*(n+1); dy = 10;
      dy = -10;
    } else if (facing == Direction.EAST) {
      // cy = 10*(n+1); dy = -10;
      dy = 10;
    } else if (facing == Direction.SOUTH) {
      cx = -10 * (n + 1);
      dx = 10;
    } else {
      dx = 10;
    }
    final var ps = new Port[n];
    for (var i = 0; i < ps.length; i++) {
      ps[i] = new Port(cx + (i + 1) * dx, cy + (i + 1) * dy, Port.OUTPUT, 1);
      ps[i].setToolTip(S.getter("DIP" + (i + 1)));
    }
    instance.setPorts(ps);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var facing = attrs.getValue(StdAttr.FACING);
    final var n = attrs.getValue(ATTR_SIZE).getWidth();
    return Bounds.create(0, 0, (n + 1) * 10, 40).rotate(Direction.NORTH, facing, 0, 0);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) {
      MyHDLGenerator = new ButtonHDLGeneratorFactory();
    }
    return MyHDLGenerator.HDLTargetSupported(attrs);
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
            GetLabels(instance.getAttributeValue(ATTR_SIZE).getWidth()));
      }
    } else if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var segmentWidth = 10;

    var state = (State) painter.getData();
    if (state == null || state.size != painter.getAttributeValue(ATTR_SIZE).getWidth()) {
      final var val = (state == null) ? 0 : state.Value;
      state = new State(val, painter.getAttributeValue(ATTR_SIZE).getWidth());
      painter.setData(state);
    }
    int n = painter.getAttributeValue(ATTR_SIZE).getWidth();

    final var facing = painter.getAttributeValue(StdAttr.FACING);
    final var loc = painter.getLocation();
    var x = loc.getX();
    var y = loc.getY();
    if (facing == Direction.SOUTH) {
      x -= segmentWidth * (n + 1);
      y -= 40;
    }
    final var g = painter.getGraphics();
    g.translate(x, y);
    var rotate = 0.0;
    if (facing != Direction.NORTH && facing != Direction.SOUTH && g instanceof Graphics2D) {
      rotate = -facing.getRight().toRadians();
      ((Graphics2D) g).rotate(rotate);
    }

    // draw switch background
    g.setColor(Color.DARK_GRAY);
    g.fillRect(1, 1, (n + 1) * segmentWidth - 2, 40 - 2);

    // switch bg and labels
    g.setFont(DrawAttr.DEFAULT_FONT);
    if (n > 9) {
      g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 0.6f));
    }
    for (var i = 0; i < n; i++) {
      g.setColor(state.BitSet(i) ? Value.TRUE_COLOR : Color.white);
      g.fillRect(7 + (i * segmentWidth), 16, 6, 20);

      g.setColor(Color.white);
      final var s = Integer.toString(i + 1);
      GraphicsUtil.drawCenteredText(g, s, 9 + (i * segmentWidth), 8);
    }

    // draw each switch state
    for (var i = 0; i < n; i++) {
      g.setColor(state.BitSet(i) ? Color.DARK_GRAY : Color.GRAY);
      int ypos = state.BitSet(i) ? 17 : 26;
      g.fillRect(8 + (i * segmentWidth), ypos, 4, 9);
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
    var pins = (State) state.getData();
    if (pins == null || pins.size != state.getAttributeValue(ATTR_SIZE).getWidth()) {
      int val = (pins == null) ? 0 : pins.Value;
      pins = new State(val, state.getAttributeValue(ATTR_SIZE).getWidth());
      state.setData(pins);
    }
    for (var i = 0; i < pins.size; i++) {
      Value pinstate = (pins.BitSet(i)) ? Value.TRUE : Value.FALSE;
      state.setPort(i, pinstate, 1);
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }
}
