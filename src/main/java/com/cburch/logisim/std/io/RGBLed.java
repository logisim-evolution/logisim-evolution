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

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.LEDIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstanceLogger;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.event.KeyEvent;
import java.util.ArrayList;

public class RGBLed extends InstanceFactory implements DynamicElementProvider {

  public static class Logger extends InstanceLogger {
    @Override
    public String getLogName(InstanceState state, Object option) {
      return state.getAttributeValue(StdAttr.LABEL);
    }

    @Override
    public Value getLogValue(InstanceState state, Object option) {
      InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
      if (data == null) return Value.FALSE;
      return data.getValue() == Value.TRUE ? Value.TRUE : Value.FALSE;
    }
  }

  public static final ArrayList<String> GetLabels() {
    ArrayList<String> LabelNames = new ArrayList<String>();
    for (int i = 0; i < 3; i++) LabelNames.add("");
    LabelNames.set(RED, "RED");
    LabelNames.set(GREEN, "GREEN");
    LabelNames.set(BLUE, "BLUE");
    return LabelNames;
  }
  
  public static final String getLabel(int id) {
    if (id < 0 || id > GetLabels().size()) return "Undefined";
    return GetLabels().get(id);
  }

  public static final int RED = 0;
  public static final int GREEN = 1;
  public static final int BLUE = 2;

  public RGBLed() {
    super("RGBLED", S.getter("RGBledComponent"));
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          Io.ATTR_ACTIVE,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          Direction.WEST,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer( 0, 3, 0, null, GetLabels(), null ) 
        });
    setFacingAttribute(StdAttr.FACING);
    setIcon(new LEDIcon(true));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
    setInstanceLogger(Logger.class);
  }

  private void updatePorts(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    Port[] ps = new Port[3];
    int cx = 0, cy = 0, dx = 0, dy = 0;
    if (facing == Direction.NORTH) {
      cy = 10;
      dx = 10;
    } else if (facing == Direction.EAST) {
      cx = -10;
      dy = 10;
    } else if (facing == Direction.SOUTH) {
      cy = -10;
      dx = -10;
    } else {
      cx = 10;
      dy = -10;
    }
    ps[RED] = new Port(0, 0, Port.INPUT, 1);
    ps[GREEN] = new Port(cx + dx, cy + dy, Port.INPUT, 1);
    ps[BLUE] = new Port(cx - dx, cy - dy, Port.INPUT, 1);
    ps[RED].setToolTip(S.getter("RED"));
    ps[GREEN].setToolTip(S.getter("GREEN"));
    ps[BLUE].setToolTip(S.getter("BLUE"));
    instance.setPorts(ps);
  }

  @Override
  public boolean ActiveOnHigh(AttributeSet attrs) {
    return attrs.getValue(Io.ATTR_ACTIVE);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction facing = attrs.getValue(StdAttr.FACING);
    return Bounds.create(0, -10, 20, 20).rotate(Direction.WEST, facing, 0, 0);
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new AbstractLedHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING) {
      instance.recomputeBounds();
      updatePorts(instance);
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    } else if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_LEFT);
    }
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bds.getX() + 1, bds.getY() + 1, bds.getWidth() - 2, bds.getHeight() - 2);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
    int summ = (data == null ? 0 : ((Integer) data.getValue()).intValue());
    Bounds bds = painter.getBounds().expand(-1);

    Graphics g = painter.getGraphics();
    if (painter.getShowState()) {
      Boolean activ = painter.getAttributeValue(Io.ATTR_ACTIVE);
      int mask = activ.booleanValue() ? 0 : 7;
      summ ^= mask;
      int red = ((summ >> RED) & 1) * 0xFF;
      int green = ((summ >> GREEN) & 1) * 0xFF;
      int blue = ((summ >> BLUE) & 1) * 0xFF;
      Color LedColor = new Color(red, green, blue);
      g.setColor(LedColor);
      g.fillOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    }
    g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawOval(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
    GraphicsUtil.switchToWidth(g, 1);
    painter.drawLabel();
    painter.drawPorts();
  }

  @Override
  public void propagate(InstanceState state) {
    int summary = 0;
    for (int i = 0; i < 3; i++) {
      Value val = state.getPortValue(i);
      if (val == Value.TRUE) summary |= 1 << i;
    }
    Object value = Integer.valueOf(summary);
    InstanceDataSingleton data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(value));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }

  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new RGBLedShape(x, y, path);
  }
}
