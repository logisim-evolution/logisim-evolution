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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.LedBarIcon;

import static com.cburch.logisim.std.Strings.S;

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

  /* ****************************************************************** */

  public LedBar() {
    super(_ID, S.getter("ioLedBarComponent"), 8, 1, new LedBarHdlGeneratorFactory());
    setIcon(new LedBarIcon());

    ATTR_DOT_SHAPE.setHidden(true);
    ATTR_MATRIX_ROWS.setHidden(true);

    setScaleY(3);
    setDrawBorder(false);
  }

  /* ****************************************************************** */
}
