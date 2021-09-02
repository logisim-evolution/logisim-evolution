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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class Ttl7447HDLGenerator extends AbstractHDLGeneratorFactory {

  @Override
  public String getComponentStringIdentifier() {
    return "TTL7447";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("BCD0", 1);
    map.put("BCD1", 1);
    map.put("BCD2", 1);
    map.put("BCD3", 1);
    map.put("LT", 1);
    map.put("BI", 1);
    map.put("RBI", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("Sega", 1);
    map.put("Segb", 1);
    map.put("Segc", 1);
    map.put("Segd", 1);
    map.put("Sege", 1);
    map.put("Segf", 1);
    map.put("Segg", 1);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var map = new TreeMap<String, Integer>();
    map.put("segments", 7);
    map.put("bcd", 4);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new LineBuffer();
    return contents
        .addLines(
            "Sega  <= segments(0,",
            "Segb  <= segments(1,",
            "Segc  <= segments(2,",
            "Segd  <= segments(3,",
            "Sege  <= segments(4,",
            "Segf  <= segments(5,",
            "Segg  <= segments(6,",
            "",
            "bcd   <= BCD3&BCD2&BCD1&BCD0;",
            "",
            "Decode : PROCESS ( bcd , LT , BI , RBI ) IS",
            "   BEGIN",
            "      CASE bcd IS",
            "         WHEN \"0000\" => segments <= \"0111111\";",
            "         WHEN \"0001\" => segments <= \"0000110\";",
            "         WHEN \"0010\" => segments <= \"1011011\";",
            "         WHEN \"0011\" => segments <= \"1001111\";",
            "         WHEN \"0100\" => segments <= \"1100110\";",
            "         WHEN \"0101\" => segments <= \"1101101\";",
            "         WHEN \"0110\" => segments <= \"1111101\";",
            "         WHEN \"0111\" => segments <= \"0000111\";",
            "         WHEN \"1000\" => segments <= \"1111111\";",
            "         WHEN \"1001\" => segments <= \"1100111\";",
            "         WHEN \"1010\" => segments <= \"1110111\";",
            "         WHEN \"1011\" => segments <= \"1111100\";",
            "         WHEN \"1100\" => segments <= \"0111001\";",
            "         WHEN \"1101\" => segments <= \"1011110\";",
            "         WHEN \"1110\" => segments <= \"1111001\";",
            "         WHEN OTHERS => segments <= \"1110001\";",
            "      END CASE;",
            "      IF (BI = '0') THEN segments <= \"0000000\";",
            "      ELSIF (LT = '0') THEN segments <= \"1111111\";",
            "      ELSIF ((RBI='0') AND (bcd=\"0000\")) THEN segments <= \"0000000\";",
            "      END IF;",
            "   END PROCESS Decode;")
        .getWithIndent();
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var comp = (NetlistComponent) mapInfo;
    map.putAll(GetNetMap("BCD0", true, comp, 6, nets));
    map.putAll(GetNetMap("BCD1", true, comp, 0, nets));
    map.putAll(GetNetMap("BCD2", true, comp, 1, nets));
    map.putAll(GetNetMap("BCD3", true, comp, 5, nets));
    map.putAll(GetNetMap("LT", false, comp, 2, nets));
    map.putAll(GetNetMap("BI", false, comp, 3, nets));
    map.putAll(GetNetMap("RBI", false, comp, 4, nets));
    map.putAll(GetNetMap("Sega", true, comp, 11, nets));
    map.putAll(GetNetMap("Segb", true, comp, 10, nets));
    map.putAll(GetNetMap("Segc", true, comp, 9, nets));
    map.putAll(GetNetMap("Segd", true, comp, 8, nets));
    map.putAll(GetNetMap("Sege", true, comp, 7, nets));
    map.putAll(GetNetMap("Segf", true, comp, 13, nets));
    map.putAll(GetNetMap("Segg", true, comp, 12, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    /*
     * this method returns the module directory where the HDL code needs to
     * be placed
     */
    return "ttl";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    /* TODO: Add support for the ones with VCC and Ground Pin */
    if (attrs == null) return false;
    return (!attrs.getValue(TtlLibrary.VCC_GND) && HDL.isVHDL());
  }
}
