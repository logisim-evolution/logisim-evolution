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

package com.cburch.logisim.circuit.appear;

import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.CircuitAttributes;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.Instance;
import java.util.Collection;
import java.util.List;

class DefaultAppearance {

  public static List<CanvasObject> build(
      Collection<Instance> pins, AttributeOption style, boolean Fixed, String CircuitName) {
    if (style == CircuitAttributes.APPEAR_CLASSIC) {
      return DefaultClassicAppearance.build(pins);
    } else if (style == CircuitAttributes.APPEAR_FPGA) {
      return DefaultHolyCrossAppearance.build(pins, CircuitName);
    } else {
      return DefaultEvolutionAppearance.build(pins, CircuitName, Fixed);
    }
  }

  static void sortPinList(List<Instance> pins, Direction facing) {
    if (facing == Direction.NORTH || facing == Direction.SOUTH)
      Location.sortHorizontal(pins);
    else
      Location.sortVertical(pins);
  }

  private DefaultAppearance() {}
}
