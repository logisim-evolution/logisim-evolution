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

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ReptarLocalBusHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  @Override
  public ArrayList<String> GetArchitecture(
      Netlist nets,
      AttributeSet attrs,
      String componentName) {
    final var contents = new ArrayList<String>();
    if (HDL.isVHDL()) {
      contents.addAll(FileWriter.getGenerateRemark(componentName, nets.projName()));
      contents.add("");
      contents.add("ARCHITECTURE PlatformIndependent OF " + componentName + " IS ");
      contents.add("");
      contents.add("BEGIN");
      contents.add("");
      contents.add("FPGA_out(0) <= NOT SP6_LB_WAIT3_i;");
      contents.add("FPGA_out(1) <= NOT IRQ_i;");
      contents.add("SP6_LB_nCS3_o       <= FPGA_in(0);");
      contents.add("SP6_LB_nADV_ALE_o   <= FPGA_in(1);");
      contents.add("SP6_LB_RE_nOE_o     <= FPGA_in(2);");
      contents.add("SP6_LB_nWE_o        <= FPGA_in(3);");
      contents.add("Addr_LB_o           <= FPGA_in(11 DOWNTO 4);");
      contents.add("");
      contents.add("IOBUF_Addresses_Datas : for i in 0 to Addr_Data_LB_io'length-1 generate");
      contents.add("  IOBUF_Addresse_Data : IOBUF");
      contents.add("  generic map (");
      contents.add("    DRIVE => 12,");
      contents.add(" IOSTANDARD => \"LVCMOS18\",");
      contents.add("    SLEW => \"FAST\"");
      contents.add("  )");
      contents.add("  port map (");
      contents.add("    O => Addr_Data_LB_o(i), -- Buffer output");
      contents.add(
          "    IO => Addr_Data_LB_io(i), -- Buffer inout port (connect directly to top-level port)");
      contents.add("    I => Addr_Data_LB_i(i), -- Buffer input");
      contents.add("    T => Addr_Data_LB_tris_i -- 3-state enable input, high=input, low=output");
      contents.add("  );");
      contents.add("end generate;");
      contents.add("");
      contents.add("END PlatformIndependent;");
    }
    return contents;
  }

  @Override
  public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    final var contents = new ArrayList<String>();
    contents.add("   COMPONENT LocalBus");
    contents.add("      PORT ( SP6_LB_WAIT3_i     : IN  std_logic;");
    contents.add("             IRQ_i              : IN  std_logic;");
    contents.add("             Addr_Data_LB_io    : INOUT  std_logic_vector( 15 DOWNTO 0 );");
    contents.add("             Addr_LB_o          : OUT std_logic_vector( 8 DOWNTO 0 );");
    contents.add("             SP6_LB_RE_nOE_o    : OUT std_logic;");
    contents.add("             SP6_LB_nADV_ALE_o  : OUT std_logic;");
    contents.add("             SP6_LB_nCS3_o      : OUT std_logic;");
    contents.add("             SP6_LB_nWE_o       : OUT std_logic;");
    contents.add("             FPGA_in            : IN std_logic_vector(12 downto 0);");
    contents.add("             FPGA_out           : OUT std_logic_vector(1 downto 0);");
    contents.add("            Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);");
    contents.add("            Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);");
    contents.add("            Addr_Data_LB_tris_i : IN std_logic);");
    contents.add("   END COMPONENT;");
    return contents;
  }

  @Override
  public String getComponentStringIdentifier() {
    return "ReptarLB";
  }

  @Override
  public ArrayList<String> GetEntity(
      Netlist nets,
      AttributeSet attrs,
      String componentName) {
    final var contents = new ArrayList<String>();
    contents.addAll(FileWriter.getGenerateRemark(componentName, nets.projName()));
    contents.addAll(FileWriter.getExtendedLibrary());
    contents.add("Library UNISIM;");
    contents.add("use UNISIM.vcomponents.all;");
    contents.add("");
    contents.add("ENTITY " + componentName + " IS");
    contents.add("   PORT ( Addr_Data_LB_io     : INOUT std_logic_vector(15 downto 0);");
    contents.add("          SP6_LB_nCS3_o       : OUT std_logic;");
    contents.add("          SP6_LB_nADV_ALE_o   : OUT std_logic;");
    contents.add("          SP6_LB_RE_nOE_o     : OUT std_logic;");
    contents.add("          SP6_LB_nWE_o        : OUT std_logic;");
    contents.add("          SP6_LB_WAIT3_i      : IN std_logic;");
    contents.add("          IRQ_i               : IN std_logic;");
    contents.add("          FPGA_in             : IN std_logic_vector(12 downto 0);");
    contents.add("          FPGA_out            : OUT std_logic_vector(1 downto 0);");
    contents.add("          Addr_LB_o           : OUT std_logic_vector(8 downto 0);");
    contents.add("          Addr_Data_LB_o      : OUT std_logic_vector(15 downto 0);");
    contents.add("          Addr_Data_LB_i      : IN std_logic_vector(15 downto 0);");
    contents.add("          Addr_Data_LB_tris_i : IN std_logic);");
    contents.add("END " + componentName + ";");

    return contents;
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
      throw new UnsupportedOperationException("Reptar Local Bus doesn't support verilog yet.");
    }
    return contents;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("SP6_LB_nCS3_o", 1);
    map.put("SP6_LB_nADV_ALE_o", 1);
    map.put("SP6_LB_RE_nOE_o", 1);
    map.put("SP6_LB_nWE_o", 1);
    map.put("Addr_LB_o", 9);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var map = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return map;
    final var ComponentInfo = (NetlistComponent) MapInfo;

    map.put(
        "Addr_Data_LB_io",
        LocalInOutBubbleBusname
            + "("
            + ComponentInfo.GetLocalBubbleInOutEndId()
            + " DOWNTO "
            + ComponentInfo.GetLocalBubbleInOutStartId()
            + ")");
    map.put(
        "FPGA_in",
        LocalInputBubbleBusname
          + "("
          + ComponentInfo.GetLocalBubbleInputEndId()
          + " DOWNTO "
          + ComponentInfo.GetLocalBubbleInputStartId()
          + ")");
    map.put(
        "FPGA_out",
        LocalOutputBubbleBusname
          + "("
          + ComponentInfo.GetLocalBubbleOutputEndId()
          + " DOWNTO "
          + ComponentInfo.GetLocalBubbleOutputStartId()
          + ")");
    map.putAll(
        GetNetMap(
            "SP6_LB_nCS3_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nCS3_o,
            Nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_nADV_ALE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nADV_ALE_o,
            Nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_RE_nOE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_RE_nOE_o,
            Nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_nWE_o",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_nWE_o,
            Nets));
    map.putAll(
        GetNetMap(
            "SP6_LB_WAIT3_i",
            true,
            ComponentInfo,
            ReptarLocalBus.SP6_LB_WAIT3_i,
            Nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_o",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_o,
            Nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_i",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_i,
            Nets));
    map.putAll(
        GetNetMap(
            "Addr_Data_LB_tris_i",
            true,
            ComponentInfo,
            ReptarLocalBus.Addr_Data_LB_tris_i,
            Nets));
    map.putAll(
        GetNetMap(
            "Addr_LB_o", true, ComponentInfo, ReptarLocalBus.Addr_LB_o, Nets));
    map.putAll(
        GetNetMap("IRQ_i", true, ComponentInfo, ReptarLocalBus.IRQ_i, Nets));
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
