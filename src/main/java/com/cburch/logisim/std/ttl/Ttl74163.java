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

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

public class Ttl74163 extends Ttl74161 {

  public static final String _ID = "74163";

  @Override
  public void ttlpropagate(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(4));
      state.setData(data);
    }

    final var triggered = data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING);
    if (triggered) {
      final var nClear = state.getPortValue(PORT_INDEX_nCLR).toLongValue();
      final var nLoad = state.getPortValue(PORT_INDEX_nLOAD).toLongValue();
      var counter = data.getValue().toLongValue();;

      if (nClear == 0) {
        counter = 0;
      } else if (nLoad == 0) {
        counter = state.getPortValue(PORT_INDEX_A).toLongValue();
        counter += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
        counter += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
        counter += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      } else {
        final var enpAndEnt = state.getPortValue(PORT_INDEX_EnP).and(state.getPortValue(PORT_INDEX_EnT)).toLongValue();
        if (enpAndEnt == 1) {
          counter++;
          if (counter > 15) {
            counter = 0;
          }
        } else {
          return; // Nothing changed so return
        }
      }
      
      data.setValue(Value.createKnown(BitWidth.create(4), counter));
      final var vA = data.getValue().get(0);
      final var vB = data.getValue().get(1);
      final var vC = data.getValue().get(2);
      final var vD = data.getValue().get(3);

      state.setPort(PORT_INDEX_QA, vA, 1);
      state.setPort(PORT_INDEX_QB, vB, 1);
      state.setPort(PORT_INDEX_QC, vC, 1);
      state.setPort(PORT_INDEX_QD, vD, 1);

      // RC0 = QA AND QB AND QC AND QD AND ENT
      state.setPort(PORT_INDEX_RC0, state.getPortValue(PORT_INDEX_EnT).and(vA).and(vB).and(vC).and(vD), 1);
    } 
  }

}
