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
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.SevenSegmentIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceDataSingleton;
import com.cburch.logisim.instance.InstanceFactory;
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

public class SevenSegment extends InstanceFactory implements DynamicElementProvider {
  static void drawBase(InstancePainter painter, boolean DrawPoint) {
    ensureSegments();
    InstanceDataSingleton data = (InstanceDataSingleton) painter.getData();
    int summ = (data == null ? 0 : ((Integer) data.getValue()).intValue());
    Boolean active = painter.getAttributeValue(Io.ATTR_ACTIVE);
    int desired = active == null || active.booleanValue() ? 1 : 0;

    Bounds bds = painter.getBounds();
    int x = bds.getX() + 5;
    int y = bds.getY();

    Graphics g = painter.getGraphics();
    Color onColor = painter.getAttributeValue(Io.ATTR_ON_COLOR);
    Color offColor = painter.getAttributeValue(Io.ATTR_OFF_COLOR);
    Color bgColor = painter.getAttributeValue(Io.ATTR_BACKGROUND);
    if (painter.shouldDrawColor() && bgColor.getAlpha() != 0) {
      g.setColor(bgColor);
      g.fillRect(bds.getX(), bds.getY(), bds.getWidth(), bds.getHeight());
      g.setColor(Color.BLACK);
    }
    painter.drawBounds();
    g.setColor(Color.DARK_GRAY);
    for (int i = 0; i <= 7; i++) {
      if (painter.getShowState()) {
        g.setColor(((summ >> i) & 1) == desired ? onColor : offColor);
      }
      if (i < 7) {
        Bounds seg = SEGMENTS[i];
        g.fillRect(x + seg.getX(), y + seg.getY(), seg.getWidth(), seg.getHeight());
      } else {
        if (DrawPoint) g.fillOval(x + 28, y + 48, 5, 5); // draw decimal point
      }
    }
    g.setColor(Color.BLACK);
    painter.drawLabel();
    painter.drawPorts();
  }

  static void ensureSegments() {
    if (SEGMENTS == null) {
      SEGMENTS =
          new Bounds[] {
            Bounds.create(3, 8, 19, 4),
            Bounds.create(23, 10, 4, 19),
            Bounds.create(23, 30, 4, 19),
            Bounds.create(3, 47, 19, 4),
            Bounds.create(-2, 30, 4, 19),
            Bounds.create(-2, 10, 4, 19),
            Bounds.create(3, 28, 19, 4)
          };
    }
  }

  public static final ArrayList<String> GetLabels() {
    ArrayList<String> LabelNames = new ArrayList<String>();
    for (int i = 0; i < 8; i++) LabelNames.add("");
    LabelNames.set(Segment_A, "Segment_A");
    LabelNames.set(Segment_B, "Segment_B");
    LabelNames.set(Segment_C, "Segment_C");
    LabelNames.set(Segment_D, "Segment_D");
    LabelNames.set(Segment_E, "Segment_E");
    LabelNames.set(Segment_F, "Segment_F");
    LabelNames.set(Segment_G, "Segment_G");
    LabelNames.set(DP, "DecimalPoint");
    return LabelNames;
  }
  
  public static final String getOutputLabel(int id) {
    if (id < 0 || id > GetLabels().size()) return "Undefined";
    return GetLabels().get(id);
  }

  public static final int Segment_A = 0;
  public static final int Segment_B = 1;
  public static final int Segment_C = 2;
  public static final int Segment_D = 3;
  public static final int Segment_E = 4;
  public static final int Segment_F = 5;
  public static final int Segment_G = 6;

  public static final int DP = 7;

  static Bounds[] SEGMENTS = null;

  static Color DEFAULT_OFF = new Color(220, 220, 220);
  
  public static final Attribute<Boolean> ATTR_DP = 
    Attributes.forBoolean("decimalPoint", S.getter("SevenSegDP"));

  public SevenSegment() {
    super("7-Segment Display", S.getter("sevenSegmentComponent"));
    setAttributes(
        new Attribute[] {
          Io.ATTR_ON_COLOR,
          Io.ATTR_OFF_COLOR,
          Io.ATTR_BACKGROUND,
          Io.ATTR_ACTIVE,
          ATTR_DP,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          new Color(240, 0, 0),
          DEFAULT_OFF,
          Io.DEFAULT_BACKGROUND,
          Boolean.TRUE,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          false,
          new ComponentMapInformationContainer( 0, 8, 0, null, GetLabels(), null )
        });
    setOffsetBounds(Bounds.create(-5, 0, 40, 60));
    setIcon(new SevenSegmentIcon(false));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
  }
  
  private void updatePorts(Instance instance) {
	boolean hasDp = instance.getAttributeValue(ATTR_DP);
    Port[] ps = new Port[hasDp ? 8 : 7];
    ps[Segment_A] = new Port(20, 0, Port.INPUT, 1);
    ps[Segment_B] = new Port(30, 0, Port.INPUT, 1);
    ps[Segment_C] = new Port(20, 60, Port.INPUT, 1);
    ps[Segment_D] = new Port(10, 60, Port.INPUT, 1);
    ps[Segment_E] = new Port(0, 60, Port.INPUT, 1);
    ps[Segment_F] = new Port(10, 0, Port.INPUT, 1);
    ps[Segment_G] = new Port(0, 0, Port.INPUT, 1);
    ps[Segment_A].setToolTip(S.getter("Segment_A"));
    ps[Segment_B].setToolTip(S.getter("Segment_B"));
    ps[Segment_C].setToolTip(S.getter("Segment_C"));
    ps[Segment_D].setToolTip(S.getter("Segment_D"));
    ps[Segment_E].setToolTip(S.getter("Segment_E"));
    ps[Segment_F].setToolTip(S.getter("Segment_F"));
    ps[Segment_G].setToolTip(S.getter("Segment_G"));
    if (hasDp) {
      ps[DP] = new Port(30, 60, Port.INPUT, 1);
      ps[DP].setToolTip(S.getter("DecimalPoint"));
    }
    instance.setPorts(ps);
    instance.getAttributeValue(StdAttr.MAPINFO).setNrOfOutports(hasDp ? 8 : 7, GetLabels());
  }

  @Override
  public boolean ActiveOnHigh(AttributeSet attrs) {
    return attrs.getValue(Io.ATTR_ACTIVE);
  }

  public static final void computeTextField(Instance instance) {
    Direction facing = instance.getAttributeValue(StdAttr.FACING);
    Object labelLoc = instance.getAttributeValue(StdAttr.LABEL_LOC);

    Bounds bds = instance.getBounds();
    int x = bds.getX() + bds.getWidth() / 2;
    int y = bds.getY() + bds.getHeight() / 2;
    int halign = GraphicsUtil.H_CENTER;
    int valign = GraphicsUtil.V_CENTER;
    if (labelLoc == Direction.NORTH) {
      y = bds.getY() - 2;
      valign = GraphicsUtil.V_BOTTOM;
    } else if (labelLoc == Direction.SOUTH) {
      y = bds.getY() + bds.getHeight() + 2;
      valign = GraphicsUtil.V_TOP;
    } else if (labelLoc == Direction.EAST) {
      x = bds.getX() + bds.getWidth() + 2;
      halign = GraphicsUtil.H_LEFT;
    } else if (labelLoc == Direction.WEST) {
      x = bds.getX() - 2;
      halign = GraphicsUtil.H_RIGHT;
    }
    if (labelLoc == facing) {
      if (labelLoc == Direction.NORTH || labelLoc == Direction.SOUTH) {
        x += 2;
        halign = GraphicsUtil.H_LEFT;
      } else {
        y -= 2;
        valign = GraphicsUtil.V_BOTTOM;
      }
    }

    instance.setTextField(StdAttr.LABEL, StdAttr.LABEL_FONT, x, y, halign, valign);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
	instance.getAttributeSet().setValue(StdAttr.MAPINFO, new ComponentMapInformationContainer( 0, 8, 0, null, GetLabels(), null ));
    instance.addAttributeListener();
    updatePorts(instance);
    computeTextField(instance);
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
      computeTextField(instance);
    } else if (attr == StdAttr.LABEL_LOC) {
      computeTextField(instance);
    } else if (attr == ATTR_DP) {
      updatePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    drawBase(painter, painter.getAttributeValue(ATTR_DP).booleanValue());
  }

  @Override
  public void propagate(InstanceState state) {
    int summary = 0;
    int max = state.getAttributeValue(ATTR_DP) ? 8 : 7;
    for (int i = 0; i < max; i++) {
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
    return new SevenSegmentShape(x, y, path);
  }
}
