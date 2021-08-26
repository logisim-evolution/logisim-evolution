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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class Ttl74163 extends Ttl74161 {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74163";

  public Ttl74163() {
    super(_ID);
  }

  @Override
  public void ttlpropagate(InstanceState state) {
    var data = getStateData(state);

    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    if (triggered) {
      final var nClear = state.getPortValue(PORT_INDEX_nCLR).toLongValue();
      final var nLoad = state.getPortValue(PORT_INDEX_nLOAD).toLongValue();
      var counter = data.getValue().toLongValue();

      if (nClear == 0) {
        counter = 0;
      } else if (nLoad == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue()
            + state.getPortValue(PORT_INDEX_B).toLongValue() << 1
            + state.getPortValue(PORT_INDEX_C).toLongValue() << 2
            + state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      } else {
        if (state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT)).toLongValue() != 1) return; // Nothing changed so return 
        counter = (counter + 1) & 15;
      }
      updateState(state, counter);
    } 
  }

}
