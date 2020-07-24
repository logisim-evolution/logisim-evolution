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

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.IOComponentTypes;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.ConnectionEnd;
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.Net;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.gui.FPGAReport;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.SortedSet;
import java.util.TreeMap;
import java.util.TreeSet;

public class AbstractHDLGeneratorFactory implements HDLGeneratorFactory {

  protected static String IntToBin(int value, int nr_of_bits, String HDLType) {
    int mask = 1 << (nr_of_bits - 1);
    StringBuffer result = new StringBuffer();
    int align = (7 - nr_of_bits) >> 1;
    while ((result.length() < align) && HDLType.equals(HDLGeneratorFactory.VHDL)) {
      result.append(" ");
    }
    String VhdlQuotes = (nr_of_bits == 1) ? "'" : "\"";
    result.append(
        (HDLType.equals(HDLGeneratorFactory.VHDL))
            ? VhdlQuotes
            : Integer.toString(nr_of_bits) + "'b");
    while (mask != 0) {
      if ((value & mask) != 0) {
        result.append("1");
      } else {
        result.append("0");
      }
      mask >>= 1;
    }
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      result.append(VhdlQuotes);
    }
    while ((result.length() < 7) && HDLType.equals(HDLGeneratorFactory.VHDL)) {
      result.append(" ");
    }
    return result.toString();
  }

  public static boolean WriteArchitecture(
      String TargetDirectory,
      ArrayList<String> Contents,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    if (Contents == null || Contents.isEmpty()) {
      Reporter.AddFatalError(
          "INTERNAL ERROR: Empty behavior description for Component '"
              + ComponentName
              + "' received!");
      return false;
    }
    File OutFile =
        FileWriter.GetFilePointer(TargetDirectory, ComponentName, false, Reporter, HDLType);
    if (OutFile == null) {
      return false;
    }
    return FileWriter.WriteContents(OutFile, Contents, Reporter);
  }

  public static boolean WriteEntity(
      String TargetDirectory,
      ArrayList<String> Contents,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    if (HDLType.endsWith(HDLGeneratorFactory.VERILOG)) {
      return true;
    }
    if (Contents.isEmpty()) {
      Reporter.AddFatalError("INTERNAL ERROR: Empty entity description received!");
      return false;
    }
    File OutFile =
        FileWriter.GetFilePointer(TargetDirectory, ComponentName, true, Reporter, HDLType);
    if (OutFile == null) {
      return false;
    }
    return FileWriter.WriteContents(OutFile, Contents, Reporter);
  }

  public static final int MaxLineLength = 80;

  /* Here the common predefined methods are defined */
  public boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents,
      String WorkingDir,
      ArrayList<String> Hierarchy,
      FPGAReport Reporter,
      String HDLType) {
    return true;
  }

  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Map<String, Integer> InputsList = GetInputList(TheNetlist, attrs); 
    Map<String, Integer> InOutsList = GetInOutList(TheNetlist, attrs);
    Map<String, Integer> OutputsList = GetOutputList(TheNetlist, attrs);
    Map<Integer, String> ParameterList = GetParameterList(attrs);
    Map<String, Integer> WireList = GetWireList(attrs, TheNetlist);
    Map<String, Integer> RegList = GetRegList(attrs, HDLType);
    Map<String, Integer> MemList = GetMemList(attrs, HDLType);
    StringBuffer OneLine = new StringBuffer();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName, HDLType, TheNetlist.projName()));
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      ArrayList<String> libs = GetExtraLibraries();
      if (!libs.isEmpty()) {
        Contents.addAll(libs);
        Contents.add("");
      }
      Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName.toString() + " IS ");
      Contents.add("");
      int NrOfTypes = GetNrOfTypes(TheNetlist, attrs, HDLType);
      if (NrOfTypes > 0) {
        Contents.addAll(MakeRemarkBlock("Here all private types are defined", 3, HDLType));
        for (String ThisType : GetTypeDefinitions(TheNetlist, attrs, HDLType)) {
          Contents.add("   " + ThisType + ";");
        }
        Contents.add("");
      }
      ArrayList<String> Comps = GetComponentDeclarationSection(TheNetlist, attrs);
      if (!Comps.isEmpty()) {
        Contents.addAll(MakeRemarkBlock("Here all used components are defined", 3, HDLType));
        Contents.addAll(Comps);
        Contents.add("");
      }
      Contents.addAll(MakeRemarkBlock("Here all used signals are defined", 3, HDLType));
      for (String Wire : WireList.keySet()) {
        OneLine.append(Wire);
        while (OneLine.length() < SallignmentSize) {
          OneLine.append(" ");
        }
        OneLine.append(": std_logic");
        if (WireList.get(Wire) == 1) {
          OneLine.append(";");
        } else {
          OneLine.append("_vector( ");
          if (WireList.get(Wire) < 0) {
            if (!ParameterList.containsKey(WireList.get(Wire))) {
              Reporter.AddFatalError(
                  "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              Contents.clear();
              return Contents;
            }
            OneLine.append("(" + ParameterList.get(WireList.get(Wire)) + "-1)");
          } else {
            if (WireList.get(Wire) == 0) {
              OneLine.append("0");
            } else {
              OneLine.append(Integer.toString(WireList.get(Wire) - 1));
            }
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL " + OneLine.toString());
        OneLine.setLength(0);
      }
      for (String Reg : RegList.keySet()) {
        OneLine.append(Reg);
        while (OneLine.length() < SallignmentSize) {
          OneLine.append(" ");
        }
        OneLine.append(": std_logic");
        if (RegList.get(Reg) == 1) {
          OneLine.append(";");
        } else {
          OneLine.append("_vector( ");
          if (RegList.get(Reg) < 0) {
            if (!ParameterList.containsKey(RegList.get(Reg))) {
              Reporter.AddFatalError(
                  "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              Contents.clear();
              return Contents;
            }
            OneLine.append("(" + ParameterList.get(RegList.get(Reg)) + "-1)");
          } else {
            if (RegList.get(Reg) == 0) {
              OneLine.append("0");
            } else {
              OneLine.append(Integer.toString(RegList.get(Reg) - 1));
            }
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL " + OneLine.toString());
        OneLine.setLength(0);
      }
      for (String Mem : MemList.keySet()) {
        OneLine.append(Mem);
        while (OneLine.length() < SallignmentSize) {
          OneLine.append(" ");
        }
        OneLine.append(": ");
        OneLine.append(GetType(MemList.get(Mem)));
        OneLine.append(";");
        Contents.add("   SIGNAL " + OneLine.toString());
        OneLine.setLength(0);
      }
      Contents.add("");
      Contents.add("BEGIN");
      Contents.addAll(GetModuleFunctionality(TheNetlist, attrs, Reporter, HDLType));
      Contents.add("END PlatformIndependent;");
    } else {
      String Preamble = "module " + ComponentName + "( ";
      StringBuffer Indenting = new StringBuffer();
      while (Indenting.length() < Preamble.length()) {
        Indenting.append(" ");
      }
      if (InputsList.isEmpty() && OutputsList.isEmpty() && InOutsList.isEmpty()) {
        Contents.add(Preamble + " );");
      } else {
        StringBuffer ThisLine = new StringBuffer();
        for (String inp : InputsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble.toString() + inp);
          } else {
            Contents.add(ThisLine.toString() + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting.toString() + inp);
          }
        }
        for (String outp : OutputsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble.toString() + outp);
          } else {
            Contents.add(ThisLine.toString() + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting.toString() + outp);
          }
        }
        for (String io : InOutsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble.toString() + io);
          } else {
            Contents.add(ThisLine.toString() + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting.toString() + io);
           }
        }
        if (ThisLine.length() != 0) {
          Contents.add(ThisLine.toString() + ");");
        } else {
          Reporter.AddError("Internale Error in Verilog Architecture generation!");
        }
      }
      if (!ParameterList.isEmpty()) {
        Contents.add("");
        Contents.addAll(
            MakeRemarkBlock(
                "Here all module parameters are defined with a dummy value", 3, HDLType));
        for (int param : ParameterList.keySet()) {
          Contents.add("   parameter " + ParameterList.get(param).toString() + " = 1;");
        }
        Contents.add("");
      }
      boolean firstline = true;
      int nr_of_bits;
      for (String inp : InputsList.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   input");
        nr_of_bits = InputsList.get(inp);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Reporter.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[" + ParameterList.get(nr_of_bits).toString() + "-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[" + Integer.toString(nr_of_bits - 1) + ":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append("  " + inp + ";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the inputs are defined", 3, HDLType));
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (String outp : OutputsList.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   output");
        nr_of_bits = OutputsList.get(outp);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Reporter.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[" + ParameterList.get(nr_of_bits).toString() + "-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[" + Integer.toString(nr_of_bits - 1) + ":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" " + outp + ";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the outputs are defined", 3, HDLType));
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (String io : InOutsList.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   inout");
        nr_of_bits = InOutsList.get(io);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Reporter.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[" + ParameterList.get(nr_of_bits).toString() + "-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[" + Integer.toString(nr_of_bits - 1) + ":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" " + io + ";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the ios are defined", 3, HDLType));
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (String wire : WireList.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   wire");
        nr_of_bits = WireList.get(wire);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Reporter.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[" + ParameterList.get(nr_of_bits).toString() + "-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[" + Integer.toString(nr_of_bits - 1) + ":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" " + wire + ";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the internal wires are defined", 3, HDLType));
        }
        Contents.add(OneLine.toString());
      }
      for (String reg : RegList.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   reg");
        nr_of_bits = RegList.get(reg);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Reporter.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[" + ParameterList.get(nr_of_bits).toString() + "-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[" + Integer.toString(nr_of_bits - 1) + ":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" " + reg + ";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the internal registers are defined", 3, HDLType));
        }
        Contents.add(OneLine.toString());
      }
      /* TODO: Add memlist */
      if (!firstline) {
        Contents.add("");
      }
      Contents.addAll(GetModuleFunctionality(TheNetlist, attrs, Reporter, HDLType));
      Contents.add("");
      Contents.add("endmodule");
    }
    return Contents;
  }

  public String GetBusEntryName(
      NetlistComponent comp,
      int EndIndex,
      boolean FloatingNetTiedToGround,
      int bitindex,
      String HDLType,
      Netlist TheNets) {
    StringBuffer Contents = new StringBuffer();
    String BracketOpen = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? ")" : "]";
    if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
      ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
      boolean IsOutput = ThisEnd.IsOutputEnd();
      int NrOfBits = ThisEnd.NrOfBits();
      if ((NrOfBits > 1) && (bitindex >= 0) && (bitindex < NrOfBits)) {
        if (ThisEnd.GetConnection((byte) bitindex).GetParrentNet() == null) {
          /* The net is not connected */
          if (IsOutput) {
            if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
              Contents.append("OPEN");
            } else {
              Contents.append("'bz");
            }
          } else {
            Contents.append(
                GetZeroVector(
                    1, // kwalsh: was ThisEnd.NrOfBits(),
                    FloatingNetTiedToGround,
                    HDLType));
          }
        } else {
          Net ConnectedNet = ThisEnd.GetConnection((byte) bitindex).GetParrentNet();
          int ConnectedNetBitIndex = ThisEnd.GetConnection((byte) bitindex).GetParrentNetBitIndex();
          if (!ConnectedNet.isBus()) {
            Contents.append(NetName + Integer.toString(TheNets.GetNetId(ConnectedNet)));
          } else {
            Contents.append(
                BusName
                    + Integer.toString(TheNets.GetNetId(ConnectedNet))
                    + BracketOpen
                    + Integer.toString(ConnectedNetBitIndex)
                    + BracketClose);
          }
        }
      }
    }
    return Contents.toString();
  }

  public static String GetBusNameContinues(
      NetlistComponent comp, int EndIndex, String HDLType, Netlist TheNets) {
    String Result;
    String BracketOpen = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? ")" : "]";
    String VectorLoopId = (HDLType.equals(HDLGeneratorFactory.VHDL)) ? " DOWNTO " : ":";
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      return "";
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      return "";
    }
    if (!TheNets.IsContinuesBus(comp, EndIndex)) {
      return "";
    }
    Net ConnectedNet = ConnectionInformation.GetConnection((byte) 0).GetParrentNet();
    Result =
        BusName
            + Integer.toString(TheNets.GetNetId(ConnectedNet))
            + BracketOpen
            + Integer.toString(
                ConnectionInformation.GetConnection((byte) (ConnectionInformation.NrOfBits() - 1))
                    .GetParrentNetBitIndex())
            + VectorLoopId
            + Integer.toString(
                ConnectionInformation.GetConnection((byte) (0)).GetParrentNetBitIndex())
            + BracketClose;
    return Result;
  }

  public static String GetBusName(
      NetlistComponent comp, int EndIndex, String HDLType, Netlist TheNets) {
    String Result;
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      return "";
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      return "";
    }
    if (!TheNets.IsContinuesBus(comp, EndIndex)) {
      return "";
    }
    Net ConnectedNet = ConnectionInformation.GetConnection((byte) 0).GetParrentNet();
    Result = BusName + Integer.toString(TheNets.GetNetId(ConnectedNet));
    return Result;
  }

  public static String GetClockNetName(NetlistComponent comp, int EndIndex, Netlist TheNets) {
    StringBuffer Contents = new StringBuffer();
    if ((TheNets.GetCurrentHierarchyLevel() != null)
        && (EndIndex >= 0)
        && (EndIndex < comp.NrOfEnds())) {
      ConnectionEnd EndData = comp.getEnd(EndIndex);
      if (EndData.NrOfBits() == 1) {
        Net ConnectedNet = EndData.GetConnection((byte) 0).GetParrentNet();
        byte ConnectedNetBitIndex = EndData.GetConnection((byte) 0).GetParrentNetBitIndex();
        /* Here we search for a clock net Match */
        int clocksourceid =
            TheNets.GetClockSourceId(
                TheNets.GetCurrentHierarchyLevel(), ConnectedNet, ConnectedNetBitIndex);
        if (clocksourceid >= 0) {
          Contents.append(ClockTreeName + Integer.toString(clocksourceid));
        }
      }
    }
    return Contents.toString();
  }

  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns all the component definitions used as component
     * in the circuit. This method is only called in case of VHDL-code
     * generation.
     */
    ArrayList<String> Components = new ArrayList<String>();
    return Components;
  }

  public ArrayList<String> GetComponentInstantiation(
      Netlist TheNetlist, AttributeSet attrs, String ComponentName, String HDLType ) {
    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      Contents.addAll(GetVHDLBlackBox(TheNetlist, attrs, ComponentName, false));
    }
    return Contents;
  }

  public ArrayList<String> GetComponentMap(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      MappableResourcesContainer MapInfo,
      FPGAReport Reporter,
      String Name,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    Map<String, Integer> ParameterMap = GetParameterMap(Nets, ComponentInfo, Reporter);
    Map<String, String> PortMap = GetPortMap(Nets, ComponentInfo == null ? MapInfo : ComponentInfo, Reporter, HDLType);
    String CompName = (Name != null && !Name.isEmpty()) ? Name :
        (ComponentInfo == null)
            ? this.getComponentStringIdentifier()
            : ComponentInfo.GetComponent()
                .getFactory()
                .getHDLName(ComponentInfo.GetComponent().getAttributeSet());
    String ThisInstanceIdentifier = GetInstanceIdentifier(ComponentInfo, ComponentId);
    StringBuffer OneLine = new StringBuffer();
    int TabLength;
    boolean first;
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      Contents.add("   " + ThisInstanceIdentifier + " : " + CompName);
      if (!ParameterMap.isEmpty()) {
        OneLine.append("      GENERIC MAP ( ");
        TabLength = OneLine.length();
        first = true;
        for (String generic : ParameterMap.keySet()) {
          if (!first) {
            OneLine.append(",");
            Contents.add(OneLine.toString());
            OneLine.setLength(0);
            while (OneLine.length() < TabLength) {
              OneLine.append(" ");
            }
          } else {
            first = false;
          }
          OneLine.append(generic);
          for (int i = generic.length(); i < SallignmentSize; i++) {
            OneLine.append(" ");
          }
          OneLine.append("=> " + Integer.toString(ParameterMap.get(generic)));
        }
        OneLine.append(")");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      if (!PortMap.isEmpty()) {
        OneLine.append("      PORT MAP ( ");
        TabLength = OneLine.length();
        first = true;
        for (String port : PortMap.keySet()) {
          if (!first) {
            OneLine.append(",");
            Contents.add(OneLine.toString());
            OneLine.setLength(0);
            while (OneLine.length() < TabLength) {
              OneLine.append(" ");
            }
          } else {
            first = false;
          }
          OneLine.append(port);
          for (int i = port.length(); i < SallignmentSize; i++) {
            OneLine.append(" ");
          }
          OneLine.append("=> " + PortMap.get(port));
        }
        OneLine.append(");");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      Contents.add("");
    } else {
      OneLine.append("   " + CompName);
      if (!ParameterMap.isEmpty()) {
        OneLine.append(" #(");
        TabLength = OneLine.length();
        first = true;
        for (String parameter : ParameterMap.keySet()) {
          if (!first) {
            OneLine.append(",");
            Contents.add(OneLine.toString());
            OneLine.setLength(0);
            while (OneLine.length() < TabLength) {
              OneLine.append(" ");
            }
          } else {
            first = false;
          }
          OneLine.append(
              "." + parameter + "(" + Integer.toString(ParameterMap.get(parameter)) + ")");
        }
        OneLine.append(")");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      OneLine.append("      " + ThisInstanceIdentifier + " (");
      if (!PortMap.isEmpty()) {
        TabLength = OneLine.length();
        first = true;
        for (String port : PortMap.keySet()) {
          if (!first) {
            OneLine.append(",");
            Contents.add(OneLine.toString());
            OneLine.setLength(0);
            while (OneLine.length() < TabLength) {
              OneLine.append(" ");
            }
          } else {
            first = false;
          }
          OneLine.append("." + port + "(");
          String MappedSignal = PortMap.get(port);
          if (!MappedSignal.contains(",")) {
            OneLine.append(MappedSignal);
          } else {
            String[] VectorList = MappedSignal.split(",");
            OneLine.append("{");
            int TabSize = OneLine.length();
            for (int vectorentries = 0; vectorentries < VectorList.length; vectorentries++) {
              String Entry = VectorList[vectorentries];
              if (Entry.contains("{")) {
                Entry.replaceAll("{", "");
              }
              if (Entry.contains("}")) {
                Entry.replaceAll("}", "");
              }
              OneLine.append(Entry);
              if (vectorentries < VectorList.length - 1) {
                Contents.add(OneLine.toString() + ",");
                OneLine.setLength(0);
                while (OneLine.length() < TabSize) {
                  OneLine.append(" ");
                }
              } else {
                OneLine.append("}");
              }
            }
          }
          OneLine.append(")");
        }
      }
      OneLine.append(");");
      Contents.add(OneLine.toString());
      Contents.add("");
    }
    return Contents;
  }

  public String getComponentStringIdentifier() {
    return "AComponent";
  }

  @Override
  public ArrayList<String> GetEntity(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName,
      FPGAReport Reporter,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    if (HDLType.equals(HDLGeneratorFactory.VHDL)) {
      Contents.addAll(
          FileWriter.getGenerateRemark(
              ComponentName, HDLGeneratorFactory.VHDL, TheNetlist.projName()));
      Contents.addAll(FileWriter.getExtendedLibrary());
      Contents.addAll(GetVHDLBlackBox(TheNetlist, attrs, ComponentName, true /* , false */));
    }
    return Contents;
  }

  /* Here all public entries for HDL generation are defined */
  public ArrayList<String> GetExtraLibraries() {
    /*
     * this method returns extra VHDL libraries required for simulation
     * and/or synthesis
     */
    return new ArrayList<String>();
  }

  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      FPGAReport Reporter,
      String CircuitName,
      String HDLType) {
    ArrayList<String> Contents = new ArrayList<String>();
    return Contents;
  }

  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns a map list of all the INOUT of a black-box. The
     * String Parameter represents the Name, and the Integer parameter
     * represents: >0 The number of bits of the signal <0 A parameterized
     * vector of bits where the value is the "key" of the parameter map 0 Is
     * an invalid value and must not be used
     */
    SortedMap<String, Integer> InOuts = new TreeMap<String, Integer>();
    return InOuts;
  }

  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns a map list of all the inputs of a black-box. The
     * String Parameter represents the Name, and the Integer parameter
     * represents: >0 The number of bits of the signal <0 A parameterized
     * vector of bits where the value is the "key" of the parameter map 0 Is
     * an invalid value and must not be used
     */
    SortedMap<String, Integer> Inputs = new TreeMap<String, Integer>();
    return Inputs;
  }

  public String GetInstanceIdentifier(NetlistComponent ComponentInfo, Long ComponentId) {
    /*
     * this method returns the Name of this instance of an used component,
     * e.g. "GATE_1"
     */
    return getComponentStringIdentifier() + "_" + ComponentId.toString();
  }

  public SortedMap<String, Integer> GetMemList(AttributeSet attrs, String HDLType) {
    /*
     * This method returns a map list of all the memory contents signals
     * used in the black-box. The String Parameter represents the Name, and
     * the Integer parameter represents the type definition.
     */
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    return Regs;
  }

  public ArrayList<String> GetModuleFunctionality(
      Netlist TheNetlist, AttributeSet attrs, FPGAReport Reporter, String HDLType) {
    /*
     * In this method the functionality of the black-box is described. It is
     * used for both VHDL and VERILOG.
     */
    ArrayList<String> Contents = new ArrayList<String>();
    return Contents;
  }

  public Map<String, String> GetNetMap(
      String SourceName,
      boolean FloatingPinTiedToGround,
      NetlistComponent comp,
      int EndIndex,
      FPGAReport Reporter,
      String HDLType,
      Netlist TheNets) {
    Map<String, String> NetMap = new HashMap<String, String>();
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      Reporter.AddFatalError("INTERNAL ERROR: Component tried to index non-existing SolderPoint");
      return NetMap;
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    boolean IsOutput = ConnectionInformation.IsOutputEnd();
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      NetMap.put(SourceName, GetNetName(comp, EndIndex, FloatingPinTiedToGround, HDLType, TheNets));
    } else {
      /*
       * Here we have the more difficult case, it is a bus that needs to
       * be mapped
       */
      /* First we check if the bus has a connection */
      boolean Connected = false;
      for (int i = 0; i < NrOfBits; i++) {
        if (ConnectionInformation.GetConnection((byte) i).GetParrentNet() != null) {
          Connected = true;
        }
      }
      if (!Connected) {
        /* Here is the easy case, the bus is unconnected */
        if (IsOutput) {
          if (HDLType.equals(VHDL)) {
            NetMap.put(SourceName, "OPEN");
          } else {
            NetMap.put(SourceName, "");
          }
        } else {
          NetMap.put(SourceName, GetZeroVector(NrOfBits, FloatingPinTiedToGround, HDLType));
        }
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (TheNets.IsContinuesBus(comp, EndIndex)) {
          /* Another easy case, the continues bus connection */
          NetMap.put(SourceName, GetBusNameContinues(comp, EndIndex, HDLType, TheNets));
        } else {
          /* The last case, we have to enumerate through each bit */
          if (HDLType.equals(VHDL)) {
            StringBuffer SourceNetName = new StringBuffer();
            for (int i = 0; i < NrOfBits; i++) {
              /* First we build the Line information */
              SourceNetName.setLength(0);
              SourceNetName.append(SourceName + "(" + Integer.toString(i) + ") ");
              ConnectionPoint SolderPoint = ConnectionInformation.GetConnection((byte) i);
              if (SolderPoint.GetParrentNet() == null) {
                /* The net is not connected */
                if (IsOutput) {
                  NetMap.put(SourceNetName.toString(), "OPEN");
                } else {
                  NetMap.put(
                      SourceNetName.toString(), GetZeroVector(1, FloatingPinTiedToGround, HDLType));
                }
              } else {
                /*
                 * The net is connected, we have to find out if
                 * the connection is to a bus or to a normal net
                 */
                if (SolderPoint.GetParrentNet().BitWidth() == 1) {
                  /* The connection is to a Net */
                  NetMap.put(
                      SourceNetName.toString(),
                      NetName + Integer.toString(TheNets.GetNetId(SolderPoint.GetParrentNet())));
                } else {
                  /* The connection is to an entry of a bus */
                  NetMap.put(
                      SourceNetName.toString(),
                      BusName
                          + Integer.toString(TheNets.GetNetId(SolderPoint.GetParrentNet()))
                          + "("
                          + Integer.toString(SolderPoint.GetParrentNetBitIndex())
                          + ")");
                }
              }
            }
          } else {
            ArrayList<String> SeperateSignals = new ArrayList<String>();
            /*
             * First we build an array with all the signals that
             * need to be concatenated
             */
            for (int i = 0; i < NrOfBits; i++) {
              ConnectionPoint SolderPoint = ConnectionInformation.GetConnection((byte) i);
              if (SolderPoint.GetParrentNet() == null) {
                /* this entry is not connected */
                if (IsOutput) {
                  SeperateSignals.add("1'bZ");
                } else {
                  SeperateSignals.add(GetZeroVector(1, FloatingPinTiedToGround, HDLType));
                }
              } else {
                /*
                 * The net is connected, we have to find out if
                 * the connection is to a bus or to a normal net
                 */
                if (SolderPoint.GetParrentNet().BitWidth() == 1) {
                  /* The connection is to a Net */
                  SeperateSignals.add(
                      NetName + Integer.toString(TheNets.GetNetId(SolderPoint.GetParrentNet())));
                } else {
                  /* The connection is to an entry of a bus */
                  SeperateSignals.add(
                      BusName
                          + Integer.toString(TheNets.GetNetId(SolderPoint.GetParrentNet()))
                          + "["
                          + Integer.toString(SolderPoint.GetParrentNetBitIndex())
                          + "]");
                }
              }
            }
            /* Finally we can put all together */
            StringBuffer Vector = new StringBuffer();
            Vector.append("{");
            for (int i = NrOfBits; i > 0; i++) {
              Vector.append(SeperateSignals.get(i - 1));
              if (i != 1) {
                Vector.append(",");
              }
            }
            Vector.append("}");
            NetMap.put(SourceName, Vector.toString());
          }
        }
      }
    }
    return NetMap;
  }

  public String GetNetName(
      NetlistComponent comp,
      int EndIndex,
      boolean FloatingNetTiedToGround,
      String HDLType,
      Netlist MyNetlist) {
    StringBuffer Contents = new StringBuffer();
    String ZeroValue = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String OneValue = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    String BracketOpen = (HDLType.equals(VHDL)) ? "(" : "[";
    String BracketClose = (HDLType.equals(VHDL)) ? ")" : "]";
    String Unconnected = (HDLType.equals(VHDL)) ? "OPEN" : "open";
    String FloatingValue = (FloatingNetTiedToGround) ? ZeroValue : OneValue;
    if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
      ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
      boolean IsOutput = ThisEnd.IsOutputEnd();
      if (ThisEnd.NrOfBits() == 1) {
        ConnectionPoint SolderPoint = ThisEnd.GetConnection((byte) 0);
        if (SolderPoint.GetParrentNet() == null) {
          /* The net is not connected */
          if (IsOutput) {
            Contents.append(Unconnected);
          } else {
            Contents.append(FloatingValue);
          }
        } else {
          /*
           * The net is connected, we have to find out if the
           * connection is to a bus or to a normal net
           */
          if (SolderPoint.GetParrentNet().BitWidth() == 1) {
            /* The connection is to a Net */
            Contents.append(
                NetName + Integer.toString(MyNetlist.GetNetId(SolderPoint.GetParrentNet())));
          } else {
            /* The connection is to an entry of a bus */
            Contents.append(
                BusName
                    + Integer.toString(MyNetlist.GetNetId(SolderPoint.GetParrentNet()))
                    + BracketOpen
                    + Integer.toString(SolderPoint.GetParrentNetBitIndex())
                    + BracketClose);
          }
        }
      }
    }
    return Contents.toString();
  }

  public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs, String HDLType) {
    /* In this method you can specify the number of own defined Types */
    return 0;
  }

  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns a map list of all the outputs of a black-box. The
     * String Parameter represents the Name, and the Integer parameter
     * represents: >0 The number of bits of the signal <0 A parameterized
     * vector of bits where the value is the "key" of the parameter map 0 Is
     * an invalid value and must not be used
     */
    SortedMap<String, Integer> Outputs = new TreeMap<String, Integer>();
    return Outputs;
  }

  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    /*
     * This method returns a map list of all parameters/generic. The integer
     * parameter represents the key that can be used for the parameterized
     * input and/or output vectors. The String is the name of the parameter.
     * In VHDL all parameters are assumed to be INTEGER.
     */
    SortedMap<Integer, String> Parameters = new TreeMap<Integer, String>();
    return Parameters;
  }

  public SortedMap<String, Integer> GetParameterMap(
      Netlist Nets, NetlistComponent ComponentInfo, FPGAReport Reporter) {
    /*
     * This method returns the assigned parameter/generic values used for
     * the given component, the key is the name of the parameter/generic,
     * and the Integer its assigned value
     */
    SortedMap<String, Integer> ParameterMap = new TreeMap<String, Integer>();
    return ParameterMap;
  }

  public SortedMap<String, String> GetPortMap(
      Netlist Nets, Object MapInfo, FPGAReport Reporter, String HDLType) {
    /*
     * This method returns the assigned input/outputs of the component, the
     * key is the name of the input/output (bit), and the value represent
     * the connected net.
     */
    SortedMap<String, String> PortMap = new TreeMap<String, String>();
    return PortMap;
  }

  public SortedMap<String, Integer> GetRegList(AttributeSet attrs, String HDLType) {
    /*
     * This method returns a map list of all the registers/flipflops used in
     * the black-box. The String Parameter represents the Name, and the
     * Integer parameter represents: >0 The number of bits of the signal <0
     * A parameterized vector of bits where the value is the "key" of the
     * parameter map 0 Is an invalid value and must not be used In VHDL
     * there is no distinction between wire and reg. You can put them in
     * both GetRegList or GetWireList
     */
    SortedMap<String, Integer> Regs = new TreeMap<String, Integer>();
    return Regs;
  }

  public String GetRelativeDirectory(String HDLType) {
    String Subdir = GetSubDir();
    if (!Subdir.endsWith(File.separator) & !Subdir.isEmpty()) {
      Subdir += File.separatorChar;
    }
    return HDLType.toLowerCase() + File.separatorChar + Subdir;
  }

  public String GetSubDir() {
    /*
     * this method returns the module sub-directory where the HDL code is
     * placed
     */
    return "";
  }

  public String GetType(int TypeNr) {
    /* This method returns the type name indicated by TypeNr */
    return "";
  }

  public SortedSet<String> GetTypeDefinitions(
      Netlist TheNetlist, AttributeSet attrs, String HDLType) {
    /*
     * This method returns all the type definitions used without the ending
     * ;
     */
    return new TreeSet<String>();
  }

  private ArrayList<String> GetVHDLBlackBox(
      Netlist TheNetlist, AttributeSet attrs, String ComponentName, Boolean IsEntity ) {
    ArrayList<String> Contents = new ArrayList<String>();
    Map<String, Integer> InputsList = GetInputList(TheNetlist, attrs);
    Map<String, Integer> InOutsList = GetInOutList(TheNetlist, attrs);
    Map<String, Integer> OutputsList = GetOutputList(TheNetlist, attrs);
    Map<Integer, String> ParameterList = GetParameterList(attrs);
    StringBuffer OneLine = new StringBuffer();
    int IdentSize;
    String CompTab = (IsEntity) ? "" : "   ";
    boolean first;
    if (IsEntity) {
      Contents.add("ENTITY " + ComponentName.toString() + " IS");
    } else {
      Contents.add("   COMPONENT " + ComponentName.toString());
    }
    if (!ParameterList.isEmpty()) {
      OneLine.append(CompTab + "   GENERIC ( ");
      IdentSize = OneLine.length();
      first = true;
      for (int generic : ParameterList.keySet()) {
        if (!first) {
          OneLine.append(";");
          Contents.add(OneLine.toString());
          OneLine.setLength(0);
          while (OneLine.length() < IdentSize) {
            OneLine.append(" ");
          }
        } else {
          first = false;
        }
        OneLine.append(ParameterList.get(generic));
        for (int i = ParameterList.get(generic).length(); i < PallignmentSize; i++) {
          OneLine.append(" ");
        }
        OneLine.append(": INTEGER");
      }
      OneLine.append(");");
      Contents.add(OneLine.toString());
      OneLine.setLength(0);
    }
    if (!InputsList.isEmpty() || !OutputsList.isEmpty() || !InOutsList.isEmpty()) {
      int nr_of_bits;
      OneLine.append(CompTab + "   PORT ( ");
      IdentSize = OneLine.length();
      first = true;
      for (String input : InputsList.keySet()) {
        if (!first) {
          OneLine.append(";");
          Contents.add(OneLine.toString());
          OneLine.setLength(0);
          while (OneLine.length() < IdentSize) {
            OneLine.append(" ");
          }
        } else {
          first = false;
        }
        OneLine.append(input);
        for (int i = input.length(); i < PallignmentSize; i++) {
          OneLine.append(" ");
        }
        OneLine.append(": IN  std_logic");
        nr_of_bits = InputsList.get(input);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (" + ParameterList.get(nr_of_bits) + "-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( " + Integer.toString(nr_of_bits - 1) + " DOWNTO 0 )");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
      }
      for (String inout : InOutsList.keySet()) {
        if (!first) {
          OneLine.append(";");
          Contents.add(OneLine.toString());
          OneLine.setLength(0);
          while (OneLine.length() < IdentSize) {
            OneLine.append(" ");
          }
        } else {
          first = false;
        }
        OneLine.append(inout);
        for (int i = inout.length(); i < PallignmentSize; i++) {
          OneLine.append(" ");
        }
        OneLine.append(": INOUT  std_logic");
        nr_of_bits = InOutsList.get(inout);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (" + ParameterList.get(nr_of_bits) + "-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( " + Integer.toString(nr_of_bits - 1) + " DOWNTO 0 )");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
      }
      for (String output : OutputsList.keySet()) {
        if (!first) {
          OneLine.append(";");
          Contents.add(OneLine.toString());
          OneLine.setLength(0);
          while (OneLine.length() < IdentSize) {
            OneLine.append(" ");
          }
        } else {
          first = false;
        }
        OneLine.append(output);
        for (int i = output.length(); i < PallignmentSize; i++) {
          OneLine.append(" ");
        }
        OneLine.append(": OUT std_logic");
        nr_of_bits = OutputsList.get(output);
        if (nr_of_bits < 0) {
          /* we have a parameterized output */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (" + ParameterList.get(nr_of_bits) + "-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( " + Integer.toString(nr_of_bits - 1) + " DOWNTO 0 )");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
      }
      OneLine.append(");");
      Contents.add(OneLine.toString());
    }
    if (IsEntity) {
      Contents.add("END " + ComponentName.toString() + ";");
    } else {
      Contents.add("   END COMPONENT;");
    }
    Contents.add("");
    return Contents;
  }

  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    /*
     * This method returns a map list of all the wires/signals used in the
     * black-box. The String Parameter represents the Name, and the Integer
     * parameter represents: >0 The number of bits of the signal <0 A
     * parameterized vector of bits where the value is the "key" of the
     * parameter map 0 Is an invalid value and must not be used In VHDL a
     * single bit "wire" is transformed to std_logic, all the others are
     * std_logic_vectors
     */
    SortedMap<String, Integer> Wires = new TreeMap<String, Integer>();
    return Wires;
  }

  public String GetZeroVector(int NrOfBits, boolean FloatingPinTiedToGround, String HDLType) {
    StringBuffer Contents = new StringBuffer();
    if (HDLType.equals(VHDL)) {
      String FillValue = (FloatingPinTiedToGround) ? "0" : "1";
      String HexFillValue = (FloatingPinTiedToGround) ? "0" : "F";
      if (NrOfBits == 1) {
        Contents.append("'" + FillValue + "'");
      } else {
        if ((NrOfBits % 4) > 0) {
          Contents.append("\"");
          for (int i = 0; i < (NrOfBits % 4); i++) {
            Contents.append(FillValue);
          }
          Contents.append("\"");
          if (NrOfBits > 3) {
            Contents.append("&");
          }
        }
        if ((NrOfBits / 4) > 0) {
          Contents.append("X\"");
          for (int i = 0; i < (NrOfBits / 4); i++) {
            Contents.append(HexFillValue);
          }
          Contents.append("\"");
        }
      }
    } else {
      Contents.append(Integer.toString(NrOfBits) + "'d");
      if (FloatingPinTiedToGround) {
        Contents.append("0");
      } else {
        Contents.append("-1");
      }
    }
    return Contents.toString();
  }

  public boolean HDLTargetSupported(String HDLType, AttributeSet attrs) {
    return false;
  }

  public boolean IsOnlyInlined(String HDLType) {
    return false;
  }

  public boolean IsOnlyInlined(String HDLType, IOComponentTypes map) {
    return true;
  }

  /* Here all global helper methods are defined */
  protected ArrayList<String> MakeRemarkBlock(
      String RemarkText, Integer NrOfIndentSpaces, String HDLType) {
    int RemarkOverhead = (HDLType.equals(VHDL)) ? 3 : 4;
    int MaxRemarkLength = MaxLineLength - 2 * RemarkOverhead - NrOfIndentSpaces;
    String[] RemarkWords = RemarkText.split(" ");
    StringBuffer OneLine = new StringBuffer();
    ArrayList<String> Contents = new ArrayList<String>();
    int maxWordLength = 0;
    for (int i = 0; i < RemarkWords.length; i++) {
      if (RemarkWords[i].length() > maxWordLength) {
        maxWordLength = RemarkWords[i].length();
      }
    }
    if (MaxRemarkLength < maxWordLength) {
      return Contents;
    }
    /* we start with generating the first remark line */
    while (OneLine.length() < NrOfIndentSpaces) {
      OneLine.append(" ");
    }
    for (int i = 0; i < MaxLineLength - NrOfIndentSpaces; i++) {
      if (HDLType.equals(VHDL)) {
        OneLine.append("-");
      } else {
        if (i == 0) {
          OneLine.append("/");
        } else {
          if (i < (MaxLineLength - NrOfIndentSpaces - 1)) {
            OneLine.append("*");
          }
        }
      }
    }
    Contents.add(OneLine.toString());
    OneLine.setLength(0);
    /* Next we put the remark text block in 1 or multiple lines */
    for (int word = 0; word < RemarkWords.length; word++) {
      if ((OneLine.length() + RemarkWords[word].length() + RemarkOverhead) > (MaxLineLength - 1)) {
        /* Next word does not fit, we end this line and create a new one */
        while (OneLine.length() < (MaxLineLength - RemarkOverhead)) {
          OneLine.append(" ");
        }
        if (HDLType.equals(VHDL)) {
          OneLine.append(" --");
        } else {
          OneLine.append(" **");
        }
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      while (OneLine.length() < NrOfIndentSpaces) {
        OneLine.append(" ");
      }
      if (OneLine.length() == NrOfIndentSpaces) {
        /* we put the preamble */
        if (HDLType.equals(VHDL)) {
          OneLine.append("-- ");
        } else {
          OneLine.append(" ** ");
        }
      }
      if (RemarkWords[word].endsWith("\\")) {
        /* Forced new line */
        OneLine.append(RemarkWords[word].substring(0, RemarkWords[word].length() - 1));
        while (OneLine.length() < (MaxLineLength - RemarkOverhead)) {
          OneLine.append(" ");
        }
      } else {
        OneLine.append(RemarkWords[word] + " ");
      }
    }
    if (OneLine.length() > (NrOfIndentSpaces + RemarkOverhead)) {
      /* we have an unfinished remark line */
      while (OneLine.length() < (MaxLineLength - RemarkOverhead)) {
        OneLine.append(" ");
      }
      if (HDLType.equals(VHDL)) {
        OneLine.append(" --");
      } else {
        OneLine.append(" **");
      }
      Contents.add(OneLine.toString());
      OneLine.setLength(0);
    }
    /* we end with generating the last remark line */
    while (OneLine.length() < NrOfIndentSpaces) {
      OneLine.append(" ");
    }
    for (int i = 0; i < MaxLineLength - NrOfIndentSpaces; i++) {
      if (HDLType.equals(VHDL)) {
        OneLine.append("-");
      } else {
        if (i == 0) {
          OneLine.append(" ");
        } else {
          if (i == (MaxLineLength - NrOfIndentSpaces - 1)) {
            OneLine.append("/");
          } else {
            OneLine.append("*");
          }
        }
      }
    }
    Contents.add(OneLine.toString());
    return Contents;
  }

  public static ArrayList<String> GetToplevelCode(String HDLType, FPGAReport Reporter, MapComponent Component) {
    String Preamble = (HDLType.equals(VHDL)) ? "" : "assign ";
    String AssignOperator = (HDLType.equals(VHDL)) ? " <= " : " = ";
    String NotOperator = (HDLType.equals(VHDL)) ? "NOT " : "~";
    String ZeroValue = (HDLType.equals(VHDL)) ? "'0'" : "1'b0";
    String OneValue = (HDLType.equals(VHDL)) ? "'1'" : "1'b1";
    StringBuffer Temp = new StringBuffer();
    ArrayList<String> contents = new ArrayList<String>();
    if (Component.getNrOfPins() <= 0) {
      Reporter.AddError("BUG: Found a component with not pins");
      return contents;
    }
    for (int i = 0 ; i < Component.getNrOfPins() ; i++) {
      Temp.setLength(0);
      Temp.append("   "+Preamble);
      /* IO-pins need to be mapped directly to the top-level component and cannot be
       * passed by signals, so we skip them.
       */
      if (Component.isIO(i)) continue;
      if (!Component.isMapped(i)) {
        /* unmapped output pins we leave unconnected */
        if (Component.isOutput(i)) continue;
        Temp.append(Component.getHdlSignalName(i,HDLType));
        allign(Temp);
        Temp.append(AssignOperator);
        Temp.append(ZeroValue+";");
        contents.add(Temp.toString());
        continue;
      }
      if (Component.isInput(i)) {
        Temp.append(Component.getHdlSignalName(i, HDLType));
        allign(Temp);
        Temp.append(AssignOperator);
        if (Component.IsConstantMapped(i)) {
          Temp.append(Component.isZeroConstantMap(i) ? ZeroValue : OneValue);
        } else {
          if (Component.isExternalInverted(i)) Temp.append(NotOperator+"n_");
          Temp.append(Component.getHdlString(i));
        }
        Temp.append(";");
        contents.add(Temp.toString());
        continue;
      }
      if (Component.IsOpenMapped(i)) continue;
      if (Component.isExternalInverted(i)) Temp.append("n_");
      Temp.append(Component.getHdlString(i));
      allign(Temp);
      Temp.append(AssignOperator);
      if (Component.isExternalInverted(i)) Temp.append(NotOperator);
      Temp.append(Component.getHdlSignalName(i, HDLType)+";");
      contents.add(Temp.toString());
    }
    contents.add(" ");
    return contents;
  }
  
  private static void allign(StringBuffer s) {
    while (s.length() < 40) s.append(" ");
  }
}
