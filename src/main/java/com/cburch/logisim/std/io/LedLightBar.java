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
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.LedLightBarIcon;

import static com.cburch.logisim.std.Strings.S;

/**
 * LED cluster
 */
public class LedLightBar extends DotMatrixBase {

  protected static final Attribute<BitWidth> ATTR_MATRIX_ROWS
          = Attributes.forBitWidth("matrixrows", S.getter("ioMatrixRows"), 1, Value.MAX_WIDTH);
  protected static final Attribute<BitWidth> ATTR_MATRIX_COLS
          = Attributes.forBitWidth("matrixcols", S.getter("ioLightBarSegments"), 1, Value.MAX_WIDTH);

  protected static final Attribute<AttributeOption> ATTR_DOT_SHAPE = Attributes.forOption(
          "dotshape", S.getter("ioMatrixShape"), new AttributeOption[]{SHAPE_PADDED_SQUARE,});

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

  /* ****************************************************************** */

  public LedLightBar() {
    super("LedLightBar", S.getter("ioLightBarComponent"), 8, 1);
    setIcon(new LedLightBarIcon());

    ATTR_DOT_SHAPE.setHidden(true);
    ATTR_MATRIX_ROWS.setHidden(true);

    setScaleY(3);
    setDrawBorder(false);
  }

  /* ****************************************************************** */

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new LedLightBarHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

}
