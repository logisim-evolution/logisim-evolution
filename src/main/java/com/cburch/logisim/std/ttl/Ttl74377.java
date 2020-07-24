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

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;

import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl74377 extends AbstractOctalFlops {

  public class Ttl74377HDLGenerator extends AbstractOctalFlopsHDLGenerator {

    @Override
    public String getComponentStringIdentifier() {
      return "TTL74377";
    }

    @Override
    public SortedMap<String, String> GetPortMap(
          Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
      SortedMap<String, String> PortMap = new TreeMap<String, String>();
      if (!(MapInfo instanceof NetlistComponent)) return PortMap;
      NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
      PortMap.putAll(super.GetPortMap(Nets, ComponentInfo, Reporter, HDLType));
      PortMap.put("nCLR", "'1'");
      PortMap.putAll(GetNetMap("nCLKEN", false, ComponentInfo, 0, Reporter, HDLType, Nets));
      return PortMap;
    }
  }

  public Ttl74377() {
    super(
        "74377",
        (byte) 20,
        new byte[] {2, 5, 6, 9, 12, 15, 16, 19},
        new String[] {
          "nCLKen", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4", "Q4", "CLK", "Q5", "D5", "D6", "Q6",
          "Q7", "D7", "D8", "Q8"
        });
    super.SetWe(true);
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl74377HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }
}
