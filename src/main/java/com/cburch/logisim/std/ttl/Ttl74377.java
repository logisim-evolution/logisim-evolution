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

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl74377 extends AbstractOctalFlops {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74377";

  public static class Ttl74377HDLGenerator extends AbstractOctalFlopsHDLGenerator {

    @Override
    public String getComponentStringIdentifier() {
      return "TTL" + _ID;
    }

    @Override
    public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
      final var map = new TreeMap<String, String>();
      if (!(mapInfo instanceof NetlistComponent)) return map;
      final var comp = (NetlistComponent) mapInfo;
      map.putAll(super.GetPortMap(nets, comp));
      map.put("nCLR", "'1'");
      map.putAll(GetNetMap("nCLKEN", false, comp, 0, nets));
      return map;
    }
  }

  public Ttl74377() {
    super(
        _ID,
        (byte) 20,
        new byte[] {2, 5, 6, 9, 12, 15, 16, 19},
        new String[] {
          "nCLKen", "Q1", "D1", "D2", "Q2", "Q3", "D3", "D4", "Q4", "CLK", "Q5", "D5", "D6", "Q6",
          "Q7", "D7", "D8", "Q8"
        });
    super.SetWe(true);
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new Ttl74377HDLGenerator();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }
}
