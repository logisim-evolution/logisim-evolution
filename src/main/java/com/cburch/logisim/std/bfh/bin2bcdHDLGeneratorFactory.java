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

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class bin2bcdHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STR = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "BIN2BCD";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put("BinValue", NR_OF_BITS_ID);
    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    final var nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    for (var i = 1; i <= nrOfPorts; i++) {
      outputs.put("BCD" + (int) (Math.pow(10, i - 1)), 4);
    }
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var parameterList = new TreeMap<Integer, String>();
    parameterList.put(NR_OF_BITS_ID, NR_OF_BITS_STR);
    return parameterList;
  }

  @Override
  public String GetSubDir() {
    return "bfh";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    final var binBits = ComponentInfo.getComponent().getEnd(0).getWidth().getWidth();
    parameterMap.put(NR_OF_BITS_STR, binBits);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) mapInfo;
    final var binBits = componentInfo.getComponent().getEnd(0).getWidth().getWidth();
    final var nrOfPorts = (int) (Math.log10(1 << binBits) + 1.0);
    portMap.putAll(GetNetMap("BinValue", true, componentInfo, 0, nets));
    for (var i = 1; i <= nrOfPorts; i++)
      portMap.putAll(GetNetMap("BCD" + (int) (Math.pow(10, i - 1)), true, componentInfo, i, nets));
    return portMap;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    final var nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    switch (nrOfPorts) {
      case 2:
        wires.put("s_level_0", 7);
        wires.put("s_level_1", 7);
        wires.put("s_level_2", 7);
        wires.put("s_level_3", 7);
        break;
      case 3:
        wires.put("s_level_0", 11);
        wires.put("s_level_1", 11);
        wires.put("s_level_2", 11);
        wires.put("s_level_3", 11);
        wires.put("s_level_4", 11);
        wires.put("s_level_5", 11);
        wires.put("s_level_6", 11);
        break;
      case 4:
        wires.put("s_level_0", 16);
        wires.put("s_level_1", 16);
        wires.put("s_level_2", 16);
        wires.put("s_level_3", 16);
        wires.put("s_level_4", 16);
        wires.put("s_level_5", 16);
        wires.put("s_level_6", 16);
        wires.put("s_level_7", 16);
        wires.put("s_level_8", 16);
        wires.put("s_level_9", 16);
        wires.put("s_level_10", 16);
        break;
    }
    return wires;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = (new LineBuffer())
            .pair("nrOfBits", NR_OF_BITS_STR);
    final var nrOfBits = attrs.getValue(bin2bcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    if (HDL.isVHDL()) {
      switch (nrOfPorts) {
        case 2:
          contents
              .addLines(
                  "s_level_0(6 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');",
                  "s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;",
                  "s_level_1(2 DOWNTO 0) <= s_level_0(2 DOWNTO 0);",
                  "s_level_2(1 DOWNTO 0) <= s_level_1(1 DOWNTO 0);",
                  "s_level_2(6)          <= s_level_1(6);",
                  "s_level_3(6 DOWNTO 5) <= s_level_2(6 DOWNTO 5);",
                  "s_level_3(0)          <= s_level_2(0);",
                  "",
                  "BCD1  <= s_level_3( 3 DOWNTO 0);",
                  "BCD10 <= \"0\"&s_level_3(6 DOWNTO 4);")
              .add(getAdd3Block("s_level_0", 6, "s_level_1", 6, "C1"))
              .add(getAdd3Block("s_level_1", 5, "s_level_2", 5, "C2"))
              .add(getAdd3Block("s_level_2", 4, "s_level_3", 4, "C3"));
          break;
        case 3:
          contents
              .addLines(
                  "s_level_0(10 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');",
                  "s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;",
                  "s_level_1(10)          <= s_level_0(10);",
                  "s_level_1( 5 DOWNTO 0) <= s_level_0( 5 DOWNTO 0);",
                  "s_level_2(10 DOWNTO 9) <= s_level_1(10 DOWNTO 9);",
                  "s_level_2( 4 DOWNTO 0) <= s_level_1( 4 DOWNTO 0);",
                  "s_level_3(10 DOWNTO 8) <= s_level_2(10 DOWNTO 8);",
                  "s_level_3( 3 DOWNTO 0) <= s_level_2( 3 DOWNTO 0);",
                  "s_level_4( 2 DOWNTO 0) <= s_level_3( 2 DOWNTO 0);",
                  "s_level_5(10)          <= s_level_4(10);",
                  "s_level_5( 1 DOWNTO 0) <= s_level_4( 1 DOWNTO 0);",
                  "s_level_6(10 DOWNTO 9) <= s_level_5(10 DOWNTO 9);",
                  "s_level_6(0)           <= s_level_5(0);",
                  "",
                  "BCD1   <= s_level_6( 3 DOWNTO 0 );",
                  "BCD10  <= s_level_6( 7 DOWNTO 4 );",
                  "BCD100 <= \"0\"&s_level_6(10 DOWNTO 8);")
              .add(getAdd3Block("s_level_0", 9, "s_level_1", 9, "C0"))
              .add(getAdd3Block("s_level_1", 8, "s_level_2", 8, "C1"))
              .add(getAdd3Block("s_level_2", 7, "s_level_3", 7, "C2"))
              .add(getAdd3Block("s_level_3", 6, "s_level_4", 6, "C3"))
              .add(getAdd3Block("s_level_4", 5, "s_level_5", 5, "C4"))
              .add(getAdd3Block("s_level_5", 4, "s_level_6", 4, "C5"))
              .add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C6"))
              .add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C7"))
              .add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C8"));
          break;
        case 4:
          contents
              .addLines(
                  "s_level_0(15 DOWNTO {{nrOfBits}}) <= (OTHERS => '0');",
                  "s_level_0({{nrOfBits}}-1 DOWNTO 0) <= BinValue;",
                  "s_level_1(15 DOWNTO 14)  <= s_level_0(15 DOWNTO 14);",
                  "s_level_1( 9 DOWNTO  0)  <= s_level_0( 9 DOWNTO  0);",
                  "s_level_2(15 DOWNTO 13)  <= s_level_1(15 DOWNTO 13);",
                  "s_level_2( 8 DOWNTO  0)  <= s_level_1( 8 DOWNTO  0);",
                  "s_level_3(15 DOWNTO 12)  <= s_level_2(15 DOWNTO 12);",
                  "s_level_3( 7 DOWNTO  0)  <= s_level_2( 7 DOWNTO  0);",
                  "s_level_4(15)            <= s_level_3(15);",
                  "s_level_4( 6 DOWNTO  0)  <= s_level_3( 6 DOWNTO  0);",
                  "s_level_5(15 DOWNTO 14)  <= s_level_4(15 DOWNTO 14);",
                  "s_level_5( 5 DOWNTO  0)  <= s_level_4( 5 DOWNTO  0);",
                  "s_level_6(15 DOWNTO 13)  <= s_level_5(15 DOWNTO 13);",
                  "s_level_6( 4 DOWNTO  0)  <= s_level_5( 4 DOWNTO  0);",
                  "s_level_7( 3 DOWNTO  0)  <= s_level_6( 3 DOWNTO  0);",
                  "s_level_8(15)            <= s_level_7(15);",
                  "s_level_8( 2 DOWNTO  0)  <= s_level_7( 2 DOWNTO  0);",
                  "s_level_9(15 DOWNTO 14)  <= s_level_8(15 DOWNTO 14);",
                  "s_level_9( 1 DOWNTO  0)  <= s_level_8( 1 DOWNTO  0);",
                  "s_level_10(15 DOWNTO 13) <= s_level_9(15 DOWNTO 13);",
                  "s_level_10(0)            <= s_level_9(0);",
                  "",
                  "BCD1    <= s_level_10( 3 DOWNTO  0 );",
                  "BCD10   <= s_level_10( 7 DOWNTO  4 );",
                  "BCD100  <= s_level_10(11 DOWNTO  8);",
                  "BCD1000 <= s_level_10(15 DOWNTO 12);")
              .add(getAdd3Block("s_level_0", 13, "s_level_1", 13, "C0"))
              .add(getAdd3Block("s_level_1", 12, "s_level_2", 12, "C1"))
              .add(getAdd3Block("s_level_2", 11, "s_level_3", 11, "C2"))
              .add(getAdd3Block("s_level_3", 10, "s_level_4", 10, "C3"))
              .add(getAdd3Block("s_level_4", 9, "s_level_5", 9, "C4"))
              .add(getAdd3Block("s_level_5", 8, "s_level_6", 8, "C5"))
              .add(getAdd3Block("s_level_6", 7, "s_level_7", 7, "C6"))
              .add(getAdd3Block("s_level_7", 6, "s_level_8", 6, "C7"))
              .add(getAdd3Block("s_level_8", 5, "s_level_9", 5, "C8"))
              .add(getAdd3Block("s_level_9", 4, "s_level_10", 4, "C9"))
              .add(getAdd3Block("s_level_3", 14, "s_level_4", 14, "C10"))
              .add(getAdd3Block("s_level_4", 13, "s_level_5", 13, "C11"))
              .add(getAdd3Block("s_level_5", 12, "s_level_6", 12, "C12"))
              .add(getAdd3Block("s_level_6", 11, "s_level_7", 11, "C13"))
              .add(getAdd3Block("s_level_7", 10, "s_level_8", 10, "C14"))
              .add(getAdd3Block("s_level_8", 9, "s_level_9", 9, "C15"))
              .add(getAdd3Block("s_level_9", 8, "s_level_10", 8, "C16"))
              .add(getAdd3Block("s_level_6", 15, "s_level_7", 15, "C17"))
              .add(getAdd3Block("s_level_7", 14, "s_level_8", 14, "C18"))
              .add(getAdd3Block("s_level_8", 13, "s_level_9", 13, "C19"))
              .add(getAdd3Block("s_level_9", 12, "s_level_10", 12, "C20"));
          break;
      }
    } else {
      Reporter.Report.AddFatalError("Strange, this should not happen as Verilog is not yet supported!\n");
    }
    return contents.getWithIndent();
  }

  private ArrayList<String> getAdd3Block(String srcName, int srcStartId, String destName, int destStartId, String processName) {
    return (new LineBuffer())
        .pair("srcName", srcName)
        .pair("srcStartId", srcStartId)
        .pair("srcDownTo", (srcStartId - 3))
        .pair("destName", destName)
        .pair("destStartId", destStartId)
        .pair("destDownTo", (destStartId - 3))
        .pair("proc", processName)
        .addLines(
            "",
            "ADD3_{{proc}} : PROCESS({{srcName}})",
            "BEGIN",
            "   CASE ( {{srcName}}( {{srcStartId}} DOWNTO {{srcDownTo}}) ) IS",
            "      WHEN \"0000\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0000\";",
            "      WHEN \"0001\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0001\";",
            "      WHEN \"0010\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0010\";",
            "      WHEN \"0011\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0011\";",
            "      WHEN \"0100\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0100\";",
            "      WHEN \"0101\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"1000\";",
            "      WHEN \"0110\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"1001\";",
            "      WHEN \"0111\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"1010\";",
            "      WHEN \"1000\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"1011\";",
            "      WHEN \"1001\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"1100\";",
            "      WHEN \"0000\" => {{destName}}( {{destStartId}} DOWNTO {{destDownTo}} ) <= \"0000\";",
            "   END CASE;",
            "END PROCESS ADD3_{{proc}};")
        .get();
  }
}
