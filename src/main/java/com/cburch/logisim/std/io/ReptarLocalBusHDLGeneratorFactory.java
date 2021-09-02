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

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.FileWriter;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ReptarLocalBusHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(Netlist nets, AttributeSet attrs, String componentName) {
    final var contents = new LineBuffer();
    if (HDL.isVHDL()) {
      contents
          .pair("compName", componentName)
          .add(FileWriter.getGenerateRemark(componentName, nets.projName()))
          .addLines(
              "",
              "ARCHITECTURE PlatformIndependent OF {{compName}} IS ",
              "",
              "BEGIN",
              "",
              "FPGA_out(0) <= NOT SP6_LB_WAIT3_i;",
              "FPGA_out(1) <= NOT IRQ_i;",
              "SP6_LB_nCS3_o       <= FPGA_in(0);",
              "SP6_LB_nADV_ALE_o   <= FPGA_in(1);",
              "SP6_LB_RE_nOE_o     <= FPGA_in(2);",
              "SP6_LB_nWE_o        <= FPGA_in(3);",
              "Addr_LB_o           <= FPGA_in(11 DOWNTO 4);",
              "",
              "IOBUF_Addresses_Datas : for i in 0 to Addr_Data_LB_io'length-1 generate",
              "  IOBUF_Addresse_Data : IOBUF",
              "  generic map (",
              "    DRIVE => 12,",
              "    IOSTANDARD => \"LVCMOS18\",",
              "    SLEW => \"FAST\"",
              "  )",
              "  port map (",
              "    O => Addr_Data_LB_o(i), -- Buffer output",
              "    IO => Addr_Data_LB_io(i), -- Buffer inout port (connect directly to top-level port)",
              "    I => Addr_Data_LB_i(i), -- Buffer input",
              "    T => Addr_Data_LB_tris_i -- 3-state enable input, high=input, low=output",
              "  );",
              "end generate;",
              "",
              "END PlatformIndependent;");
    }
    return contents.get();
  }

  @Override
  public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    return (new LineBuffer())
        .addLines(
            "COMPONENT LocalBus",
            "   PORT ( SP6_LB_WAIT3_i     : IN  std_logic;",
            "          IRQ_i              : IN  std_logic;",
            "          Addr_Data_LB_io    : INOUT  std_logic_vector( 15 DOWNTO 0 );",
            "          Addr_LB_o          : OUT std_logic_vector( 8 DOWNTO 0 );",
            "          SP6_LB_RE_nOE_o    : OUT std_logic;",
            "          SP6_LB_nADV_ALE_o  : OUT std_logic;",
            "          SP6_LB_nCS3_o      : OUT std_logic;",
            "          SP6_LB_nWE_o       : OUT std_logic;",
            "          FPGA_in            : IN std_logic_vector(12 downto 0);",
            "          FPGA_out           : OUT std_logic_vector(1 downto 0);",
            "         Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);",
            "         Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);",
            "         Addr_Data_LB_tris_i : IN std_logic);",
            "END COMPONENT;")
        .getWithIndent();
  }

  @Override
  public String getComponentStringIdentifier() {
    return "ReptarLB";
  }

  @Override
  public ArrayList<String> GetEntity(Netlist nets, AttributeSet attrs, String componentName) {
    return (new LineBuffer())
        .pair("compName", componentName)
        .add(FileWriter.getGenerateRemark(componentName, nets.projName()))
        .add(FileWriter.getExtendedLibrary())
        .addLines(
            "Library UNISIM;",
            "use UNISIM.vcomponents.all;",
            "",
            "ENTITY {{compName}} IS",
            "   PORT ( Addr_Data_LB_io     : INOUT std_logic_vector(15 downto 0);",
            "          SP6_LB_nCS3_o       : OUT std_logic;",
            "          SP6_LB_nADV_ALE_o   : OUT std_logic;",
            "          SP6_LB_RE_nOE_o     : OUT std_logic;",
            "          SP6_LB_nWE_o        : OUT std_logic;",
            "          SP6_LB_WAIT3_i      : IN std_logic;",
            "          IRQ_i               : IN std_logic;",
            "          FPGA_in             : IN std_logic_vector(12 downto 0);",
            "          FPGA_out            : OUT std_logic_vector(1 downto 0);",
            "          Addr_LB_o           : OUT std_logic_vector(8 downto 0);",
            "          Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);",
            "          Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);",
            "          Addr_Data_LB_tris_i : IN std_logic);",
            "END {{compName}};")
        .get();
  }

  @Override
  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put("Addr_Data_LB_io", 16);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("SP6_LB_WAIT3_i", 1);
    map.put("IRQ_i", 1);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.add(" ");
    } else {
      // FIXME: hardcoded string
      throw new UnsupportedOperationException("Reptar Local Bus doesn't support Verilog yet.");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist theNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("SP6_LB_nCS3_o", 1);
    map.put("SP6_LB_nADV_ALE_o", 1);
    map.put("SP6_LB_RE_nOE_o", 1);
    map.put("SP6_LB_nWE_o", 1);
    map.put("Addr_LB_o", 9);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist nets, Object mapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(mapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) mapInfo;

    map.put(
        "Addr_Data_LB_io",
        String.format(
            "%s(%d DOWNTO %d)",
            LocalInOutBubbleBusname,
            ComponentInfo.getLocalBubbleInOutEndId(),
            ComponentInfo.getLocalBubbleInOutStartId()));
    map.put(
        "FPGA_in",
        String.format(
            "%s(%d DOWNTO %d)",
            LocalInputBubbleBusname,
            ComponentInfo.getLocalBubbleInputEndId(),
            ComponentInfo.getLocalBubbleInputStartId()));
    map.put(
        "FPGA_out",
        String.format(
            "%s(%d DOWNTO %d)",
            LocalOutputBubbleBusname
                + ComponentInfo.getLocalBubbleOutputEndId()
                + ComponentInfo.getLocalBubbleOutputStartId()));
    map.putAll(
        GetNetMap(
            "SP6_LB_nCS3_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nCS3_o,
            nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_nADV_ALE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nADV_ALE_o,
            nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_RE_nOE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_RE_nOE_o,
            nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_nWE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nWE_o,
            nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_WAIT3_i",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_WAIT3_i,
            nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_o",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_o,
            nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_i",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_i,
            nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_tris_i",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_tris_i,
            nets));
    map.putAll(
        GetNetMap(
            "Addr_LB_o", true, ComponentInfo, ReptarLocalBus.Addr_LB_o, nets));
    map.putAll(
        GetNetMap("IRQ_i", true, ComponentInfo, ReptarLocalBus.IRQ_i, nets));
    return map;
  }

  @Override
  public String GetSubDir() {
    return "io";
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return HDL.isVHDL();
  }
}
