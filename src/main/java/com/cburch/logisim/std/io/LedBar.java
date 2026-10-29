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

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.data.ComponentMapInformationContainer;
import com.cburch.logisim.gui.icons.LedBarIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import java.awt.Color;

/** LED cluster */
public class LedBar extends DotMatrixBase {

  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "LedBar";

  protected static final Attribute<BitWidth> ATTR_MATRIX_ROWS =
      Attributes.forBitWidth("matrixrows", S.getter("ioMatrixRows"), 1, Value.MAX_WIDTH);
  public static final Attribute<BitWidth> ATTR_MATRIX_COLS =
      Attributes.forBitWidth("matrixcols", S.getter("ioLedBarSegments"), 1, Value.MAX_WIDTH);

  /** Height of the unrotated bar, in grid cells. 3 matches circuits saved before this attribute. */
  public static final int DEFAULT_HEIGHT = 3;

  public static final Attribute<Integer> ATTR_HEIGHT =
      Attributes.forIntegerRange("height", S.getter("ioLedBarHeight"), 1, 8);

  protected static final Attribute<AttributeOption> ATTR_DOT_SHAPE =
      Attributes.forOption(
          "dotshape",
          S.getter("ioMatrixShape"),
          new AttributeOption[] {
            SHAPE_PADDED_SQUARE,
          });

  protected static final AttributeOption INPUT_ONE_WIRE =
      new AttributeOption("row", S.getter("ioLedBarInputOneWire"));
  protected static final AttributeOption INPUT_SEPARATED =
      new AttributeOption("column", S.getter("ioLedBarInputSeparated"));

  protected static final Attribute<AttributeOption> ATTR_INPUT_TYPE =
      Attributes.forOption(
          "inputtype",
          S.getter("ioLedBarInput"),
          new AttributeOption[] {INPUT_SEPARATED, INPUT_ONE_WIRE});

  @Override
  public Attribute<BitWidth> getAttributeRows() {
    return ATTR_MATRIX_ROWS;
  }

  @Override
  public Attribute<BitWidth> getAttributeColumns() {
    return ATTR_MATRIX_COLS;
  }

  @Override
  public Attribute<AttributeOption> getAttributeShape() {
    return ATTR_DOT_SHAPE;
  }

  @Override
  public AttributeOption getDefaultShape() {
    return SHAPE_PADDED_SQUARE;
  }

  @Override
  public Attribute<AttributeOption> getAttributeInputType() {
    return ATTR_INPUT_TYPE;
  }

  @Override
  public AttributeOption getAttributeItemColumn() {
    return INPUT_SEPARATED;
  }

  @Override
  public AttributeOption getAttributeItemRow() {
    return INPUT_ONE_WIRE;
  }

  @Override
  public AttributeOption getAttributeItemSelect() {
    return INPUT_SELECT;
  }

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path p) {
    return new LedBarShape(x, y, p);
  }

  /**
   * East keeps the historical horizontal bar: segments run to the right, and in One Wire mode bit 0
   * is the rightmost segment. Other facings rotate that layout around the anchor.
   */
  @Override
  protected Direction facingOf(AttributeSet attrs) {
    if (attrs == null || !attrs.containsAttribute(StdAttr.FACING)) {
      return Direction.EAST;
    }
    final var facing = attrs.getValue(StdAttr.FACING);
    return facing == null ? Direction.EAST : facing;
  }

  @Override
  protected int scaleY(AttributeSet attrs) {
    if (attrs != null && attrs.containsAttribute(ATTR_HEIGHT)) {
      final var height = attrs.getValue(ATTR_HEIGHT);
      if (height != null) {
        return height;
      }
    }
    return super.scaleY(attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    super.instanceAttributeChanged(instance, attr);
    if (attr == StdAttr.FACING || attr == ATTR_HEIGHT) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_LEFT);
      updatePorts(instance);
    }
  }

  public LedBar() {
    super(_ID, S.getter("ioLedBarComponent"), 8, 1, new LedBarHdlGeneratorFactory());
    setIcon(new LedBarIcon());

    ATTR_DOT_SHAPE.setHidden(true);
    ATTR_MATRIX_ROWS.setHidden(true);

    // Factory scale remains the historical height for anything that still reads the field.
    // Each instance uses ATTR_HEIGHT instead.
    setScaleY(DEFAULT_HEIGHT);
    setDrawBorder(false);
    setFacingAttribute(StdAttr.FACING);
    setAttributes(
        new Attribute<?>[] {
          getAttributeInputType(),
          StdAttr.FACING,
          ATTR_HEIGHT,
          getAttributeColumns(),
          getAttributeRows(),
          StdAttr.SELECT_LOC,
          IoLibrary.ATTR_ON_COLOR,
          IoLibrary.ATTR_OFF_COLOR,
          ATTR_PERSIST,
          getAttributeShape(),
          StdAttr.LABEL,
          StdAttr.LABEL_LOC,
          StdAttr.LABEL_FONT,
          StdAttr.LABEL_COLOR,
          StdAttr.LABEL_VISIBILITY,
          StdAttr.MAPINFO
        },
        new Object[] {
          getAttributeItemColumn(),
          Direction.EAST,
          DEFAULT_HEIGHT,
          BitWidth.create(8),
          BitWidth.create(1),
          StdAttr.SELECT_BOTTOM_LEFT,
          Color.GREEN,
          Color.gray,
          0,
          getDefaultShape(),
          "",
          Direction.NORTH,
          StdAttr.DEFAULT_LABEL_FONT,
          StdAttr.DEFAULT_LABEL_COLOR,
          true,
          new ComponentMapInformationContainer(
              0, 8, 0, null, getLabels(1, 8), null)
        });
  }
}
