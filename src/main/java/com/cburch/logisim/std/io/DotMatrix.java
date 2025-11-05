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
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;

/** LED dot Matrix */
public class DotMatrix extends DotMatrixBase {

  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "DotMatrix";

  public DotMatrix() {
    super(_ID, S.getter("dotMatrixComponent"), 5, 7, new DotMatrixHdlGeneratorFactory());
  }

  public static final Attribute<BitWidth> ATTR_MATRIX_COLS =
      Attributes.forBitWidth("matrixcols", S.getter("ioMatrixCols"), 1, Value.MAX_WIDTH);
  public static final Attribute<BitWidth> ATTR_MATRIX_ROWS =
      Attributes.forBitWidth("matrixrows", S.getter("ioMatrixRows"), 1, Value.MAX_WIDTH);

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

  @Override
  public DynamicElement createDynamicElement(int x, int y, DynamicElement.Path p) {
    return new DotMatrixShape(x, y, p);
  }
}
