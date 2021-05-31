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
  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    ArrayList<String> Contents = new ArrayList<>();
    String TriName = GetNetName(ComponentInfo,2,true, Nets);
    String InpName = "";
    String OutpName = "";
    String TriState = ""; 
    int nrBits = ComponentInfo.GetComponent().getAttributeSet().getValue(StdAttr.WIDTH).getWidth(); 
    if (nrBits > 1) {
      InpName = GetBusName(ComponentInfo,1, Nets);
      OutpName = GetBusName(ComponentInfo,0, Nets);
      TriState = HDL.isVHDL() ? "(OTHERS => 'Z')" : nrBits+"'bZ";
    } else {
      InpName = GetNetName(ComponentInfo,1,true, Nets);
      OutpName = GetNetName(ComponentInfo,0,true, Nets);
      TriState = HDL.isVHDL() ? "'Z'" : "1'bZ";
    }
    if (ComponentInfo.EndIsConnected(2) && ComponentInfo.EndIsConnected(0)) {
      String invert = ((ControlledBuffer)ComponentInfo.GetComponent().getFactory()).isInverter() ?
        HDL.notOperator() : "";
      if (HDL.isVHDL()) {
        Contents.add("   "+OutpName+ "<= "+invert+InpName+" WHEN "+TriName+
                     " = '1' ELSE "+TriState+";");
      } else {
        Contents.add("   assign "+OutpName+" = ("+TriName+") ? "+invert+InpName+
                     " : "+TriState+";");
      }
    }
    return Contents;
  }
}
