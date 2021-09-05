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

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

import static com.cburch.logisim.std.Strings.S;

/** LED dot Matrix */
public class DotMatrix extends DotMatrixBase {

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "DotMatrix";

  public DotMatrix() {
    super(_ID, S.getter("dotMatrixComponent"), 5, 7);
  }

  public static final Attribute<BitWidth> ATTR_MATRIX_COLS =
      Attributes.forBitWidth("matrixcols", S.getter("ioMatrixCols"), 1, Value.MAX_WIDTH);
  public static final Attribute<BitWidth> ATTR_MATRIX_ROWS =
      Attributes.forBitWidth("matrixrows", S.getter("ioMatrixRows"), 1, Value.MAX_WIDTH);

  public Attribute<BitWidth> getAttributeRows() {
    return ATTR_MATRIX_ROWS;
  }

  public Attribute<BitWidth> getAttributeColumns() {
    return ATTR_MATRIX_COLS;
  }

  public Attribute<AttributeOption> getAttributeShape() {
    return ATTR_DOT_SHAPE;
  }

  public AttributeOption getDefaultShape() {
    return SHAPE_SQUARE;
  }

  @Override
  public Attribute<AttributeOption> getAttributeInputType() {
    return ATTR_INPUT_TYPE;
  }

  @Override
  public AttributeOption getAttributeItemColumn() {
    return INPUT_COLUMN;
  }

  @Override
  public AttributeOption getAttributeItemRow() {
    return INPUT_ROW;
  }

  @Override
  public AttributeOption getAttributeItemSelect() {
    return INPUT_SELECT;
  }
}
