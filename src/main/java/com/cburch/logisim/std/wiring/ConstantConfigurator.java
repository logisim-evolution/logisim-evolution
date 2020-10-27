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

package com.cburch.logisim.std.wiring;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.tools.key.LongConfigurator;

class ConstantConfigurator extends LongConfigurator {
  public ConstantConfigurator() {
    super(Constant.ATTR_VALUE, 0, 0, 0, 16);
  }

  @Override
  public long getMaximumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    long ret = width.getMask();
    if (ret >= 0) {
      return ret;
    } else {
      return Long.MAX_VALUE;
    }
  }

  @Override
  public long getMinimumValue(AttributeSet attrs) {
    BitWidth width = attrs.getValue(StdAttr.WIDTH);
    if (width.getWidth() < 64) {
      return 0;
    } else {
      return Long.MIN_VALUE;
    }
  }
}
