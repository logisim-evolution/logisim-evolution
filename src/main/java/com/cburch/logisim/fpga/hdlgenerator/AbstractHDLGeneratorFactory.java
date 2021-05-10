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
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.prefs.AppPreferences;

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

  protected static String IntToBin(int value, int nr_of_bits) {
    int mask = 1 << (nr_of_bits - 1);
    StringBuilder result = new StringBuilder();
    int align = (7 - nr_of_bits) >> 1;
    while ((result.length() < align) && HDL.isVHDL()) {
      result.append(" ");
    }
    String VhdlQuotes = (nr_of_bits == 1) ? "'" : "\"";
    result.append((HDL.isVHDL()) ? VhdlQuotes : nr_of_bits + "'b");
    while (mask != 0) {
      if ((value & mask) != 0) {
        result.append("1");
      } else {
        result.append("0");
      }
      mask >>= 1;
    }
    if (HDL.isVHDL()) result.append(VhdlQuotes);
    while ((result.length() < 7) && HDL.isVHDL()) {
      result.append(" ");
    }
    return result.toString();
  }

  public static boolean WriteArchitecture(
      String TargetDirectory,
      ArrayList<String> Contents,
      String ComponentName) {
    if (Contents == null || Contents.isEmpty()) {
      Reporter.Report.AddFatalError(
          "INTERNAL ERROR: Empty behavior description for Component '"
              + ComponentName
              + "' received!");
      return false;
    }
    File OutFile = FileWriter.GetFilePointer(TargetDirectory, ComponentName, false);
    if (OutFile == null) {
      return false;
    }
    return FileWriter.WriteContents(OutFile, Contents);
  }

  public static boolean WriteEntity(
      String TargetDirectory,
      ArrayList<String> Contents,
      String ComponentName) {
    if (!HDL.isVHDL()) return true;
    if (Contents.isEmpty()) {
      Reporter.Report.AddFatalError("INTERNAL ERROR: Empty entity description received!");
      return false;
    }
    File OutFile = FileWriter.GetFilePointer(TargetDirectory, ComponentName, true);
    if (OutFile == null) return false;
    return FileWriter.WriteContents(OutFile, Contents);
  }

  public static final int MaxLineLength = 80;

  /* Here the common predefined methods are defined */
  public boolean GenerateAllHDLDescriptions(
      Set<String> HandledComponents,
      String WorkingDir,
      ArrayList<String> Hierarchy) {
    return true;
  }

  public ArrayList<String> GetArchitecture(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName) {
    ArrayList<String> Contents = new ArrayList<>();
    Map<String, Integer> InputsList = GetInputList(TheNetlist, attrs); 
    Map<String, Integer> InOutsList = GetInOutList(TheNetlist, attrs);
    Map<String, Integer> OutputsList = GetOutputList(TheNetlist, attrs);
    Map<Integer, String> ParameterList = GetParameterList(attrs);
    Map<String, Integer> WireList = GetWireList(attrs, TheNetlist);
    Map<String, Integer> RegList = GetRegList(attrs);
    Map<String, Integer> MemList = GetMemList(attrs);
    StringBuffer OneLine = new StringBuffer();
    Contents.addAll(FileWriter.getGenerateRemark(ComponentName, TheNetlist.projName()));
    if (HDL.isVHDL()) {
      ArrayList<String> libs = GetExtraLibraries();
      if (!libs.isEmpty()) {
        Contents.addAll(libs);
        Contents.add("");
      }
      Contents.add("ARCHITECTURE PlatformIndependent OF " + ComponentName + " IS ");
      Contents.add("");
      int NrOfTypes = GetNrOfTypes(TheNetlist, attrs);
      if (NrOfTypes > 0) {
        Contents.addAll(MakeRemarkBlock("Here all private types are defined", 3));
        for (String ThisType : GetTypeDefinitions(TheNetlist, attrs)) {
          Contents.add("   " + ThisType + ";");
        }
        Contents.add("");
      }
      ArrayList<String> Comps = GetComponentDeclarationSection(TheNetlist, attrs);
      if (!Comps.isEmpty()) {
        Contents.addAll(MakeRemarkBlock("Here all used components are defined", 3));
        Contents.addAll(Comps);
        Contents.add("");
      }
      Contents.addAll(MakeRemarkBlock("Here all used signals are defined", 3));
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
              Reporter.Report.AddFatalError(
                  "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              Contents.clear();
              return Contents;
            }
            OneLine.append("(").append(ParameterList.get(WireList.get(Wire))).append("-1)");
          } else {
            if (WireList.get(Wire) == 0) {
              OneLine.append("0");
            } else {
              OneLine.append((WireList.get(Wire) - 1));
            }
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL " + OneLine);
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
              Reporter.Report.AddFatalError(
                  "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              Contents.clear();
              return Contents;
            }
            OneLine.append("(").append(ParameterList.get(RegList.get(Reg))).append("-1)");
          } else {
            if (RegList.get(Reg) == 0) {
              OneLine.append("0");
            } else {
              OneLine.append((RegList.get(Reg) - 1));
            }
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL " + OneLine);
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
        Contents.add("   SIGNAL " + OneLine);
        OneLine.setLength(0);
      }
      Contents.add("");
      Contents.add("BEGIN");
      Contents.addAll(GetModuleFunctionality(TheNetlist, attrs));
      Contents.add("END PlatformIndependent;");
    } else {
      String Preamble = "module " + ComponentName + "( ";
      StringBuilder Indenting = new StringBuilder();
      while (Indenting.length() < Preamble.length()) {
        Indenting.append(" ");
      }
      if (InputsList.isEmpty() && OutputsList.isEmpty() && InOutsList.isEmpty()) {
        Contents.add(Preamble + " );");
      } else {
        StringBuilder ThisLine = new StringBuilder();
        for (String inp : InputsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(inp);
          } else {
            Contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(inp);
          }
        }
        for (String outp : OutputsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(outp);
          } else {
            Contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(outp);
          }
        }
        for (String io : InOutsList.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(io);
          } else {
            Contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(io);
           }
        }
        if (ThisLine.length() != 0) {
          Contents.add(ThisLine + ");");
        } else {
          Reporter.Report.AddError("Internale Error in Verilog Architecture generation!");
        }
      }
      if (!ParameterList.isEmpty()) {
        Contents.add("");
        Contents.addAll(MakeRemarkBlock("Here all module parameters are defined with a dummy value", 3));
        for (int param : ParameterList.keySet()) {
          Contents.add("   parameter " + ParameterList.get(param) + " = 1;");
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
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[").append(ParameterList.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append("  ").append(inp).append(";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the inputs are defined", 3));
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
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[").append(ParameterList.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" ").append(outp).append(";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the outputs are defined", 3));
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
            Reporter.Report.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[").append(ParameterList.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" ").append(io).append(";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the ios are defined", 3));
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
            Reporter.Report.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[").append(ParameterList.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" ").append(wire).append(";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the internal wires are defined", 3));
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
            Reporter.Report.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents;
          }
          OneLine.append("[").append(ParameterList.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) {
              OneLine.append("[0:0]");
            }
          }
        }
        OneLine.append(" ").append(reg).append(";");
        if (firstline) {
          firstline = false;
          Contents.add("");
          Contents.addAll(MakeRemarkBlock("Here the internal registers are defined", 3));
        }
        Contents.add(OneLine.toString());
      }
      /* TODO: Add memlist */
      if (!firstline) {
        Contents.add("");
      }
      Contents.addAll(GetModuleFunctionality(TheNetlist, attrs));
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
      Netlist TheNets) {
    StringBuffer Contents = new StringBuffer();
    if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
      ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
      boolean IsOutput = ThisEnd.IsOutputEnd();
      int NrOfBits = ThisEnd.NrOfBits();
      if ((NrOfBits > 1) && (bitindex >= 0) && (bitindex < NrOfBits)) {
        if (ThisEnd.GetConnection((byte) bitindex).GetParrentNet() == null) {
          /* The net is not connected */
          if (IsOutput) {
            Contents.append(HDL.unconnected(false));
          } else {
            Contents.append(HDL.GetZeroVector(1,FloatingNetTiedToGround));
          }
        } else {
          Net ConnectedNet = ThisEnd.GetConnection((byte) bitindex).GetParrentNet();
          int ConnectedNetBitIndex = ThisEnd.GetConnection((byte) bitindex).GetParrentNetBitIndex();
          if (!ConnectedNet.isBus()) {
            Contents.append(NetName).append(TheNets.GetNetId(ConnectedNet));
          } else {
            Contents.append(
                BusName
                    + TheNets.GetNetId(ConnectedNet)
                    + HDL.BracketOpen()
                    + ConnectedNetBitIndex
                    + HDL.BracketClose());
          }
        }
      }
    }
    return Contents.toString();
  }

  public static String GetBusNameContinues(NetlistComponent comp, int EndIndex, Netlist TheNets) {
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
    Result =
        BusName
            + TheNets.GetNetId(ConnectedNet)
            + HDL.BracketOpen()
            + ConnectionInformation.GetConnection((byte) (ConnectionInformation.NrOfBits() - 1)).GetParrentNetBitIndex()
            + HDL.vectorLoopId()
            + ConnectionInformation.GetConnection((byte) (0)).GetParrentNetBitIndex()
            + HDL.BracketClose();
    return Result;
  }

  public static String GetBusName( NetlistComponent comp, int EndIndex, Netlist TheNets) {
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
    if (ConnectedNet.BitWidth() != NrOfBits)
      return GetBusNameContinues(comp,EndIndex,TheNets);
    return BusName + TheNets.GetNetId(ConnectedNet);
  }

  public static String GetClockNetName(NetlistComponent comp, int EndIndex, Netlist TheNets) {
    StringBuilder Contents = new StringBuilder();
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
          Contents.append(ClockTreeName).append(clocksourceid);
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
    return new ArrayList<>();
  }

  public ArrayList<String> GetComponentInstantiation(Netlist TheNetlist, AttributeSet attrs, String ComponentName) {
    ArrayList<String> Contents = new ArrayList<>();
    if (HDL.isVHDL()) Contents.addAll(GetVHDLBlackBox(TheNetlist, attrs, ComponentName, false));
    return Contents;
  }

  public ArrayList<String> GetComponentMap(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      MappableResourcesContainer MapInfo,
      String Name) {
    ArrayList<String> Contents = new ArrayList<>();
    Map<String, Integer> ParameterMap = GetParameterMap(Nets, ComponentInfo);
    Map<String, String> PortMap = GetPortMap(Nets, ComponentInfo == null ? MapInfo : ComponentInfo);
    String CompName = (Name != null && !Name.isEmpty()) ? Name :
        (ComponentInfo == null)
            ? this.getComponentStringIdentifier()
            : ComponentInfo.GetComponent()
                .getFactory()
                .getHDLName(ComponentInfo.GetComponent().getAttributeSet());
    String ThisInstanceIdentifier = GetInstanceIdentifier(ComponentInfo, ComponentId);
    StringBuilder OneLine = new StringBuilder();
    int TabLength;
    boolean first;
    if (HDL.isVHDL()) {
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
          OneLine.append(" ".repeat(Math.max(0, SallignmentSize - generic.length())));
          OneLine.append("=> ").append(ParameterMap.get(generic));
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
          OneLine.append(" ".repeat(Math.max(0, SallignmentSize - port.length())));
          OneLine.append("=> ").append(PortMap.get(port));
        }
        OneLine.append(");");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
    } else {
      OneLine.append("   ").append(CompName);
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
          OneLine.append(".").append(parameter).append("(").append(ParameterMap.get(parameter))
              .append(")");
        }
        OneLine.append(")");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      OneLine.append("      ").append(ThisInstanceIdentifier).append(" (");
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
          OneLine.append(".").append(port).append("(");
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
                Entry = Entry.replace("{", "");
              }
              if (Entry.contains("}")) {
                Entry = Entry.replace("}", "");
              }
              OneLine.append(Entry);
              if (vectorentries < VectorList.length - 1) {
                Contents.add(OneLine + ",");
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
    }
    Contents.add("");
    return Contents;
  }

  public String getComponentStringIdentifier() {
    return "AComponent";
  }

  @Override
  public ArrayList<String> GetEntity(
      Netlist TheNetlist,
      AttributeSet attrs,
      String ComponentName) {
    ArrayList<String> Contents = new ArrayList<>();
    if (HDL.isVHDL()) {
      Contents.addAll(FileWriter.getGenerateRemark(ComponentName, TheNetlist.projName()));
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
    return new ArrayList<>();
  }

  public ArrayList<String> GetInlinedCode(
      Netlist Nets,
      Long ComponentId,
      NetlistComponent ComponentInfo,
      String CircuitName) {
    return new ArrayList<>();
  }

  public SortedMap<String, Integer> GetInOutList(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns a map list of all the INOUT of a black-box. The
     * String Parameter represents the Name, and the Integer parameter
     * represents: >0 The number of bits of the signal <0 A parameterized
     * vector of bits where the value is the "key" of the parameter map 0 Is
     * an invalid value and must not be used
     */
    return new TreeMap<>();
  }

  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns a map list of all the inputs of a black-box. The
     * String Parameter represents the Name, and the Integer parameter
     * represents: >0 The number of bits of the signal <0 A parameterized
     * vector of bits where the value is the "key" of the parameter map 0 Is
     * an invalid value and must not be used
     */
    return new TreeMap<>();
  }

  public String GetInstanceIdentifier(NetlistComponent ComponentInfo, Long ComponentId) {
    /*
     * this method returns the Name of this instance of an used component,
     * e.g. "GATE_1"
     */
    return getComponentStringIdentifier() + "_" + ComponentId.toString();
  }

  public SortedMap<String, Integer> GetMemList(AttributeSet attrs) {
    /*
     * This method returns a map list of all the memory contents signals
     * used in the black-box. The String Parameter represents the Name, and
     * the Integer parameter represents the type definition.
     */
    return new TreeMap<>();
  }

  public ArrayList<String> GetModuleFunctionality( Netlist TheNetlist, AttributeSet attrs) {
    /*
     * In this method the functionality of the black-box is described. It is
     * used for both VHDL and VERILOG.
     */
    return new ArrayList<>();
  }

  public Map<String, String> GetNetMap(
      String SourceName,
      boolean FloatingPinTiedToGround,
      NetlistComponent comp,
      int EndIndex,
      Netlist TheNets) {
    Map<String, String> NetMap = new HashMap<>();
    if ((EndIndex < 0) || (EndIndex >= comp.NrOfEnds())) {
      Reporter.Report.AddFatalError("INTERNAL ERROR: Component tried to index non-existing SolderPoint");
      return NetMap;
    }
    ConnectionEnd ConnectionInformation = comp.getEnd(EndIndex);
    boolean IsOutput = ConnectionInformation.IsOutputEnd();
    int NrOfBits = ConnectionInformation.NrOfBits();
    if (NrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      NetMap.put(SourceName, GetNetName(comp, EndIndex, FloatingPinTiedToGround, TheNets));
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
          NetMap.put(SourceName, HDL.unconnected(true));
        } else {
          NetMap.put(SourceName, HDL.GetZeroVector(NrOfBits, FloatingPinTiedToGround));
        }
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (TheNets.IsContinuesBus(comp, EndIndex)) {
          /* Another easy case, the continues bus connection */
          NetMap.put(SourceName, GetBusNameContinues(comp, EndIndex, TheNets));
        } else {
          /* The last case, we have to enumerate through each bit */
          if (HDL.isVHDL()) {
            StringBuffer SourceNetName = new StringBuffer();
            for (int i = 0; i < NrOfBits; i++) {
              /* First we build the Line information */
              SourceNetName.setLength(0);
              SourceNetName.append(SourceName).append("(").append(i).append(") ");
              ConnectionPoint SolderPoint = ConnectionInformation.GetConnection((byte) i);
              if (SolderPoint.GetParrentNet() == null) {
                /* The net is not connected */
                if (IsOutput) {
                  NetMap.put(SourceNetName.toString(), HDL.unconnected(false));
                } else {
                  NetMap.put(SourceNetName.toString(), HDL.GetZeroVector(1, FloatingPinTiedToGround));
                }
              } else {
                /*
                 * The net is connected, we have to find out if
                 * the connection is to a bus or to a normal net
                 */
                if (SolderPoint.GetParrentNet().BitWidth() == 1) {
                  /* The connection is to a Net */
                  NetMap.put(SourceNetName.toString(),NetName + TheNets.GetNetId(SolderPoint.GetParrentNet()));
                } else {
                  /* The connection is to an entry of a bus */
                  NetMap.put(
                      SourceNetName.toString(),
                      BusName
                          + TheNets.GetNetId(SolderPoint.GetParrentNet())
                          + "("
                          + SolderPoint.GetParrentNetBitIndex()
                          + ")");
                }
              }
            }
          } else {
            ArrayList<String> SeperateSignals = new ArrayList<>();
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
                  SeperateSignals.add(HDL.GetZeroVector(1, FloatingPinTiedToGround));
                }
              } else {
                /*
                 * The net is connected, we have to find out if
                 * the connection is to a bus or to a normal net
                 */
                if (SolderPoint.GetParrentNet().BitWidth() == 1) {
                  /* The connection is to a Net */
                  SeperateSignals.add(
                      NetName + TheNets.GetNetId(SolderPoint.GetParrentNet()));
                } else {
                  /* The connection is to an entry of a bus */
                  SeperateSignals.add(
                      BusName
                          + TheNets.GetNetId(SolderPoint.GetParrentNet())
                          + "["
                          + SolderPoint.GetParrentNetBitIndex()
                          + "]");
                }
              }
            }
            /* Finally we can put all together */
            StringBuilder Vector = new StringBuilder();
            Vector.append("{");
            for (int i = NrOfBits; i > 0; i--) {
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
      Netlist MyNetlist) {
    StringBuffer Contents = new StringBuffer();
    String FloatingValue = (FloatingNetTiedToGround) ? HDL.zeroBit() : HDL.oneBit();
    if ((EndIndex >= 0) && (EndIndex < comp.NrOfEnds())) {
      ConnectionEnd ThisEnd = comp.getEnd(EndIndex);
      boolean IsOutput = ThisEnd.IsOutputEnd();
      if (ThisEnd.NrOfBits() == 1) {
        ConnectionPoint SolderPoint = ThisEnd.GetConnection((byte) 0);
        if (SolderPoint.GetParrentNet() == null) {
          /* The net is not connected */
          if (IsOutput) {
            Contents.append(HDL.unconnected(true));
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
            Contents.append(NetName).append(MyNetlist.GetNetId(SolderPoint.GetParrentNet()));
          } else {
            /* The connection is to an entry of a bus */
            Contents.append(
                BusName
                    + MyNetlist.GetNetId(SolderPoint.GetParrentNet())
                    + HDL.BracketOpen()
                    + SolderPoint.GetParrentNetBitIndex()
                    + HDL.BracketClose());
          }
        }
      }
    }
    return Contents.toString();
  }

  public int GetNrOfTypes(Netlist TheNetlist, AttributeSet attrs) {
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
    return new TreeMap<>();
  }

  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    /*
     * This method returns a map list of all parameters/generic. The integer
     * parameter represents the key that can be used for the parameterized
     * input and/or output vectors. The String is the name of the parameter.
     * In VHDL all parameters are assumed to be INTEGER.
     */
    return new TreeMap<>();
  }

  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    /*
     * This method returns the assigned parameter/generic values used for
     * the given component, the key is the name of the parameter/generic,
     * and the Integer its assigned value
     */
    return new TreeMap<>();
  }

  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    /*
     * This method returns the assigned input/outputs of the component, the
     * key is the name of the input/output (bit), and the value represent
     * the connected net.
     */
    return new TreeMap<>();
  }

  public SortedMap<String, Integer> GetRegList(AttributeSet attrs) {
    /*
     * This method returns a map list of all the registers/flipflops used in
     * the black-box. The String Parameter represents the Name, and the
     * Integer parameter represents: >0 The number of bits of the signal <0
     * A parameterized vector of bits where the value is the "key" of the
     * parameter map 0 Is an invalid value and must not be used In VHDL
     * there is no distinction between wire and reg. You can put them in
     * both GetRegList or GetWireList
     */
    return new TreeMap<>();
  }

  public String GetRelativeDirectory() {
    String Subdir = GetSubDir();
    if (!Subdir.endsWith(File.separator) & !Subdir.isEmpty()) {
      Subdir += File.separatorChar;
    }
    return AppPreferences.HDL_Type.get().toLowerCase() + File.separatorChar + Subdir;
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

  public SortedSet<String> GetTypeDefinitions(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns all the type definitions used without the ending
     * ;
     */
    return new TreeSet<>();
  }

  private ArrayList<String> GetVHDLBlackBox(
      Netlist TheNetlist, AttributeSet attrs, String ComponentName, Boolean IsEntity ) {
    ArrayList<String> Contents = new ArrayList<>();
    Map<String, Integer> InputsList = GetInputList(TheNetlist, attrs);
    Map<String, Integer> InOutsList = GetInOutList(TheNetlist, attrs);
    Map<String, Integer> OutputsList = GetOutputList(TheNetlist, attrs);
    Map<Integer, String> ParameterList = GetParameterList(attrs);
    StringBuilder OneLine = new StringBuilder();
    int IdentSize;
    String CompTab = (IsEntity) ? "" : "   ";
    boolean first;
    if (IsEntity) {
      Contents.add("ENTITY " + ComponentName + " IS");
    } else {
      Contents.add("   COMPONENT " + ComponentName);
    }
    if (!ParameterList.isEmpty()) {
      OneLine.append(CompTab).append("   GENERIC ( ");
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
        OneLine.append(
            " ".repeat(Math.max(0, PallignmentSize - ParameterList.get(generic).length())));
        OneLine.append(": INTEGER");
      }
      OneLine.append(");");
      Contents.add(OneLine.toString());
      OneLine.setLength(0);
    }
    if (!InputsList.isEmpty() || !OutputsList.isEmpty() || !InOutsList.isEmpty()) {
      int nr_of_bits;
      OneLine.append(CompTab).append("   PORT ( ");
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
        OneLine.append(" ".repeat(Math.max(0, PallignmentSize - input.length())));
        OneLine.append(": IN  std_logic");
        nr_of_bits = InputsList.get(input);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (").append(ParameterList.get(nr_of_bits))
              .append("-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( ").append(nr_of_bits - 1).append(" DOWNTO 0 )");
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
        OneLine.append(" ".repeat(Math.max(0, PallignmentSize - inout.length())));
        OneLine.append(": INOUT  std_logic");
        nr_of_bits = InOutsList.get(inout);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (").append(ParameterList.get(nr_of_bits))
              .append("-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( ").append(nr_of_bits - 1).append(" DOWNTO 0 )");
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
        OneLine.append(" ".repeat(Math.max(0, PallignmentSize - output.length())));
        OneLine.append(": OUT std_logic");
        nr_of_bits = OutputsList.get(output);
        if (nr_of_bits < 0) {
          /* we have a parameterized output */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (").append(ParameterList.get(nr_of_bits))
              .append("-1) DOWNTO 0 )");
        } else {
          if (nr_of_bits > 1) {
            /* we have a bus */
            OneLine.append("_vector( ").append(nr_of_bits - 1).append(" DOWNTO 0 )");
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
      Contents.add("END " + ComponentName + ";");
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
    return new TreeMap<>();
  }

  public boolean HDLTargetSupported(AttributeSet attrs) {
    return false;
  }

  public boolean IsOnlyInlined() {
    return false;
  }

  public boolean IsOnlyInlined(IOComponentTypes map) {
    return true;
  }

  /* Here all global helper methods are defined */
  protected ArrayList<String> MakeRemarkBlock( String RemarkText, Integer NrOfIndentSpaces) {
    int MaxRemarkLength = MaxLineLength - 2 * HDL.remarkOverhead() - NrOfIndentSpaces;
    String[] RemarkWords = RemarkText.split(" ");
    StringBuilder OneLine = new StringBuilder();
    ArrayList<String> Contents = new ArrayList<>();
    int maxWordLength = 0;
    for (String word : RemarkWords) {
      if (word.length() > maxWordLength) {
        maxWordLength = word.length();
      }
    }
    if (MaxRemarkLength < maxWordLength) {
      return Contents;
    }
    /* we start with generating the first remark line */
    while (OneLine.length() < NrOfIndentSpaces) {
      OneLine.append(" ");
    }
    for (int i = 0; i < MaxLineLength - NrOfIndentSpaces; i++)
      OneLine.append(HDL.getRemakrChar(i==0,i==MaxLineLength - NrOfIndentSpaces-1));
    Contents.add(OneLine.toString());
    OneLine.setLength(0);
    /* Next we put the remark text block in 1 or multiple lines */
    for (String remarkWord : RemarkWords) {
      if ((OneLine.length() + remarkWord.length() + HDL.remarkOverhead()) > (MaxLineLength - 1)) {
        /* Next word does not fit, we end this line and create a new one */
        while (OneLine.length() < (MaxLineLength - HDL.remarkOverhead())) {
          OneLine.append(" ");
        }
        OneLine.append(" "+HDL.getRemakrChar(false,false)+HDL.getRemakrChar(false,false));
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      while (OneLine.length() < NrOfIndentSpaces) {
        OneLine.append(" ");
      }
      if (OneLine.length() == NrOfIndentSpaces) {
        /* we put the preamble */
        OneLine.append(HDL.getRemarkStart());
      }
      if (remarkWord.endsWith("\\")) {
        /* Forced new line */
        OneLine.append(remarkWord, 0, remarkWord.length() - 1);
        while (OneLine.length() < (MaxLineLength - HDL.remarkOverhead())) {
          OneLine.append(" ");
        }
      } else {
        OneLine.append(remarkWord).append(" ");
      }
    }
    if (OneLine.length() > (NrOfIndentSpaces + HDL.remarkOverhead())) {
      /* we have an unfinished remark line */
      while (OneLine.length() < (MaxLineLength - HDL.remarkOverhead())) {
        OneLine.append(" ");
      }
      OneLine.append(" "+HDL.getRemakrChar(false,false)+HDL.getRemakrChar(false,false));
      Contents.add(OneLine.toString());
      OneLine.setLength(0);
    }
    /* we end with generating the last remark line */
    while (OneLine.length() < NrOfIndentSpaces) {
      OneLine.append(" ");
    }
    for (int i = 0; i < MaxLineLength - NrOfIndentSpaces; i++)
      OneLine.append(HDL.getRemakrChar(i==MaxLineLength - NrOfIndentSpaces-1,i==0));
    Contents.add(OneLine.toString());
    return Contents;
  }

  public static ArrayList<String> GetToplevelCode(MapComponent Component) {
    StringBuffer Temp = new StringBuffer();
    ArrayList<String> contents = new ArrayList<>();
    if (Component.getNrOfPins() <= 0) {
      Reporter.Report.AddError("BUG: Found a component with not pins");
      return contents;
    }
    for (int i = 0 ; i < Component.getNrOfPins() ; i++) {
      Temp.setLength(0);
      Temp.append("   ").append(HDL.assignPreamble());
      /* IO-pins need to be mapped directly to the top-level component and cannot be
       * passed by signals, so we skip them.
       */
      if (Component.isIO(i)) continue;
      if (!Component.isMapped(i)) {
        /* unmapped output pins we leave unconnected */
        if (Component.isOutput(i)) continue;
        Temp.append(Component.getHdlSignalName(i));
        allign(Temp);
        Temp.append(HDL.assignOperator());
        Temp.append(HDL.zeroBit()+";");
        contents.add(Temp.toString());
        continue;
      }
      if (Component.isInput(i)) {
        Temp.append(Component.getHdlSignalName(i));
        allign(Temp);
        Temp.append(HDL.assignOperator());
        if (Component.IsConstantMapped(i)) {
          Temp.append(Component.isZeroConstantMap(i) ? HDL.zeroBit() : HDL.oneBit());
        } else {
          if (Component.isExternalInverted(i)) Temp.append(HDL.notOperator()+"n_");
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
      Temp.append(HDL.assignOperator());
      if (Component.isExternalInverted(i)) Temp.append(HDL.notOperator());
      Temp.append(Component.getHdlSignalName(i)+";");
      contents.add(Temp.toString());
    }
    contents.add(" ");
    return contents;
  }
  
  private static void allign(StringBuffer s) {
    while (s.length() < 40) s.append(" ");
  }
}
