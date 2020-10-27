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
import com.cburch.logisim.data.BitWidth;
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
import java.awt.Color;
import java.awt.event.KeyEvent;

public class HexDigit extends InstanceFactory implements DynamicElementProvider {

  protected static final int HEX = 0;
  protected static final int DP = 1;

  public HexDigit() {
    super("Hex Digit Display", S.getter("hexDigitComponent"));
    setAttributes(
        new Attribute[] {
          Io.ATTR_ON_COLOR,
          Io.ATTR_OFF_COLOR,
          Io.ATTR_BACKGROUND,
          SevenSegment.ATTR_DP,
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          new Color(240, 0, 0),
          SevenSegment.DEFAULT_OFF,
          Io.DEFAULT_BACKGROUND,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          false,
          new ComponentMapInformationContainer(0, 8, 0, null, SevenSegment.GetLabels(), null) 
        });
    setOffsetBounds(Bounds.create(-15, -60, 40, 60));
    setIcon(new SevenSegmentIcon(true));
    setKeyConfigurator(new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK));
  }
  
  private void updatePorts(Instance instance) {
    int nrPorts = instance.getAttributeValue(SevenSegment.ATTR_DP) ? 2 : 1;
    Port[] ps = new Port[nrPorts];
    ps[HEX] = new Port(0, 0, Port.INPUT, 4);
    ps[HEX].setToolTip(S.getter("hexDigitDataTip"));
    if (nrPorts > 1) {
      ps[DP] = new Port(20, 0, Port.INPUT, 1);
      ps[DP].setToolTip(S.getter("hexDigitDPTip"));
    }
    instance.setPorts(ps);
    instance.getAttributeValue(StdAttr.MAPINFO).setNrOfOutports(6+nrPorts, SevenSegment.GetLabels());
  }

  @Override
  protected void configureNewInstance(Instance instance) {
	instance.getAttributeSet().setValue(StdAttr.MAPINFO, new ComponentMapInformationContainer( 0, 8, 0, null, SevenSegment.GetLabels(), null ));
    instance.addAttributeListener();
    updatePorts(instance);
    SevenSegment.computeTextField(instance);
  }
  
  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      SevenSegment.computeTextField(instance);
    } else if (attr == SevenSegment.ATTR_DP) {
      updatePorts(instance);
    }
  }
  
  @Override
  public void paintInstance(InstancePainter painter) {
    SevenSegment.drawBase(painter, painter.getAttributeValue(SevenSegment.ATTR_DP));
  }
  
  public static int getSegs(int value) {
    int segs; // each nibble is one segment, in top-down, left-to-right
    // order: middle three nibbles are the three horizontal segments
    switch (value) {
      case 0:
        segs = 0x1110111;
        break;
      case 1:
        segs = 0x0000011;
        break;
      case 2:
        segs = 0x0111110;
        break;
      case 3:
        segs = 0x0011111;
        break;
      case 4:
        segs = 0x1001011;
        break;
      case 5:
        segs = 0x1011101;
        break;
      case 6:
        segs = 0x1111101;
        break;
      case 7:
        segs = 0x0010011;
        break;
      case 8:
        segs = 0x1111111;
        break;
      case 9:
        segs = 0x1011011;
        break;
      case 10:
        segs = 0x1111011;
        break;
      case 11:
        segs = 0x1101101;
        break;
      case 12:
        segs = 0x1110100;
        break;
      case 13:
        segs = 0x0101111;
        break;
      case 14:
        segs = 0x1111100;
        break;
      case 15:
        segs = 0x1111000;
        break;
      case -1:
        segs = SEG_B_MASK|SEG_C_MASK|SEG_E_MASK|SEG_F_MASK|SEG_G_MASK;
        break; // a H for static icon
      default:
        segs = 0x0001000;
        break; // a dash '-'
    }
    return segs;
  }
  
  public static int SEG_A_MASK = 0x10000;
  public static int SEG_B_MASK = 0x10;
  public static int SEG_C_MASK = 0x1;
  public static int SEG_D_MASK = 0x100;
  public static int SEG_E_MASK = 0x100000;
  public static int SEG_F_MASK = 0x1000000;
  public static int SEG_G_MASK = 0x1000;

  @Override
  public void propagate(InstanceState state) {
    int summary = 0;
    Value baseVal = state.getPortValue(HEX);
    if (baseVal == null) baseVal = Value.createUnknown(BitWidth.create(4));
    int segs = getSegs((int)baseVal.toLongValue());
    if ((segs & SEG_C_MASK) != 0) summary |= 4; // vertical seg in bottom right
    if ((segs & SEG_B_MASK) != 0) summary |= 2; // vertical seg in top right
    if ((segs & SEG_D_MASK) != 0) summary |= 8; // horizontal seg at bottom
    if ((segs & SEG_G_MASK) != 0) summary |= 64; // horizontal seg at middle
    if ((segs & SEG_A_MASK) != 0) summary |= 1; // horizontal seg at top
    if ((segs & SEG_E_MASK) != 0) summary |= 16; // vertical seg at bottom left
    if ((segs & SEG_F_MASK) != 0) summary |= 32; // vertical seg at top left

    if (state.getAttributeValue(SevenSegment.ATTR_DP)) {
      Value dpVal = state.getPortValue(DP);
      if (dpVal != null && (int)dpVal.toLongValue() == 1) summary |= 128; // decimal point
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

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new HexDigitHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new HexDigitShape(x, y, path);
  }
}
