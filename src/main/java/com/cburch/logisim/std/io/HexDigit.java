/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.data.Attribute;
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
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Hex Digit Display";

  protected static final int HEX = 0;
  protected static final int DP = 1;

  // Possible display configurations for "no valid input" mode.
  protected enum NoDataDisplayMode {
    BLANK,
    u,
    U,
    H
  }

  static final NoDataDisplayMode NO_DATA_DISPLAY = NoDataDisplayMode.BLANK;

  public HexDigit() {
    super(_ID, S.getter("hexDigitComponent"), new HexDigitHdlGeneratorFactory(), true);
    setAttributes(
        new Attribute[] {
          IoLibrary.ATTR_ON_COLOR,
          IoLibrary.ATTR_OFF_COLOR,
          IoLibrary.ATTR_BACKGROUND,
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
          IoLibrary.DEFAULT_BACKGROUND,
          Boolean.TRUE,
          "",
          Direction.EAST,
          StdAttr.DEFAULT_LABEL_FONT,
          false,
          new ComponentMapInformationContainer(0, 8, 0, null, SevenSegment.getLabels(), null)
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
    instance
        .getAttributeValue(StdAttr.MAPINFO)
        .setNrOfOutports(6 + nrPorts, SevenSegment.getLabels());
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance
        .getAttributeSet()
        .setValue(
            StdAttr.MAPINFO,
            new ComponentMapInformationContainer(0, 8, 0, null, SevenSegment.getLabels(), null));
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

  /**
   * Maps integer value to display segments. Each nibble is one segment, in top-down, left-to-right
   * order, middle three nibbles are the three horizontal segments:
   *
   * <code>
   *
   * ··A··
   * F···B
   * ··G··
   * E···C
   * ··D··
   *
   * bits: 0xFEAGDBC i.e.: 0x1110111 => "0" shape
   *
   * </code> *
   *
   * @param value value to map
   * @return
   */
  public static int getSegs(int value) {
    int segs;
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
        switch (NO_DATA_DISPLAY) {
          case H:
            segs = SEG_B_MASK | SEG_C_MASK | SEG_E_MASK | SEG_F_MASK | SEG_G_MASK; // a "H" for static icon
            break;
          case U:
            segs = SEG_B_MASK | SEG_C_MASK | SEG_E_MASK | SEG_F_MASK | SEG_D_MASK;  // a "U" for static icon
            break;
          case u:
            segs = SEG_C_MASK | SEG_E_MASK | SEG_D_MASK;  // a "u" for static icon
            break;
          case BLANK:
          default:
            segs = 0;
            break;
        }

        break;
      default:
        // This shape indicates error value (out of bounds)
        segs = SEG_A_MASK | SEG_D_MASK | SEG_G_MASK;
        break;
    }
    return segs;
  }

  //                                     FEAGDBC
  public static final int SEG_A_MASK = 0x0010000;
  public static final int SEG_B_MASK = 0x0000010;
  public static final int SEG_C_MASK = 0x0000001;
  public static final int SEG_D_MASK = 0x0000100;
  public static final int SEG_E_MASK = 0x0100000;
  public static final int SEG_F_MASK = 0x1000000;
  public static final int SEG_G_MASK = 0x0001000;

  @Override
  public void propagate(InstanceState state) {
    var summary = 0;
    var baseVal = state.getPortValue(HEX);
    if (baseVal == null) baseVal = Value.createUnknown(BitWidth.create(4));
    int segs = getSegs((int) baseVal.toLongValue());
    if ((segs & SEG_C_MASK) != 0) summary |= 4; // vertical seg in bottom right
    if ((segs & SEG_B_MASK) != 0) summary |= 2; // vertical seg in top right
    if ((segs & SEG_D_MASK) != 0) summary |= 8; // horizontal seg at bottom
    if ((segs & SEG_G_MASK) != 0) summary |= 64; // horizontal seg at middle
    if ((segs & SEG_A_MASK) != 0) summary |= 1; // horizontal seg at top
    if ((segs & SEG_E_MASK) != 0) summary |= 16; // vertical seg at bottom left
    if ((segs & SEG_F_MASK) != 0) summary |= 32; // vertical seg at top left

    if (state.getAttributeValue(SevenSegment.ATTR_DP)) {
      final var dpVal = state.getPortValue(DP);
      if (dpVal != null && (int) dpVal.toLongValue() == 1)
        summary |= 128; // decimal point
    }

    Object value = summary;
    final var data = (InstanceDataSingleton) state.getData();
    if (data == null) {
      state.setData(new InstanceDataSingleton(value));
    } else {
      data.setValue(value);
    }
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path path) {
    return new HexDigitShape(x, y, path);
  }
}
