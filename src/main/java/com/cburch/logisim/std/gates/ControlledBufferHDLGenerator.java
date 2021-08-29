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

package com.cburch.logisim.std.gates;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;

public class ControlledBufferHDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public boolean IsOnlyInlined() {
    return true;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }

  @Override
  public ArrayList<String> GetInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    final var contents = new LineBuffer();
    final var triName = GetNetName(componentInfo, 2, true, nets);
    var inpName = "";
    var outpName = "";
    var triState = "";
    final var nrBits = componentInfo.getComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth();
    if (nrBits > 1) {
      inpName = GetBusName(componentInfo, 1, nets);
      outpName = GetBusName(componentInfo, 0, nets);
      triState = HDL.isVHDL() ? "(OTHERS => 'Z')" : nrBits + "'bZ";
    } else {
      inpName = GetNetName(componentInfo, 1, true, nets);
      outpName = GetNetName(componentInfo, 0, true, nets);
      triState = HDL.isVHDL() ? "'Z'" : "1'bZ";
    }
    if (componentInfo.isEndConnected(2) && componentInfo.isEndConnected(0)) {
      final var invert = ((ControlledBuffer) componentInfo.getComponent().getFactory()).isInverter()
              ? HDL.notOperator()
              : "";
      if (HDL.isVHDL()) {
        contents.add("   {{1}}<= {{2}}{{3}} WHEN {{4}} = '1' ELSE {{5}};", outpName, invert, inpName, triName, triState);
      } else {
        contents.add("   assign {{1}} = ({{2}}) ? {{3}}{{4}} : {{5}};", outpName, triName, invert, inpName, triState);
      }
    }
    return contents.get();
  }
}
