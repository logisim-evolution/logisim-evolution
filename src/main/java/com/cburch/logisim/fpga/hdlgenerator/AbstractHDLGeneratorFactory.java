/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.data.MapComponent;
import com.cburch.logisim.fpga.data.MappableResourcesContainer;
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.LineBuffer;
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

  /* Here the common predefined methods are defined */
  @Override
  public boolean generateAllHDLDescriptions(
      Set<String> handledComponents,
      String workingDirectory,
      ArrayList<String> hierarchy) {
    return true;
  }

  @Override
  public ArrayList<String> getArchitecture(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var Contents = new LineBuffer();
    final var inputs = GetInputList(theNetlist, attrs);
    final var inOuts = GetInOutList(theNetlist, attrs);
    final var outputs = GetOutputList(theNetlist, attrs);
    final var params = GetParameterList(attrs);
    final var wires = GetWireList(attrs, theNetlist);
    final var regs = GetRegList(attrs);
    final var mems = GetMemList(attrs);
    final var OneLine = new StringBuilder();
    Contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()));
    if (HDL.isVHDL()) {
      final var libs = GetExtraLibraries();
      if (!libs.isEmpty()) {
        Contents.add(libs);
        Contents.empty();
      }
      Contents.add("ARCHITECTURE PlatformIndependent OF {{1}} IS ", componentName);
      Contents.add("");
      final var nrOfTypes = GetNrOfTypes(theNetlist, attrs);
      if (nrOfTypes > 0) {
        Contents.addRemarkBlock("Here all private types are defined");
        for (final var thisType : GetTypeDefinitions(theNetlist, attrs)) {
          Contents.add("   {{1}};", thisType);
        }
        Contents.empty();
      }
      final var components = GetComponentDeclarationSection(theNetlist, attrs);
      if (!components.isEmpty()) {
        Contents.addRemarkBlock("Here all used components are defined").add(components).add("");
      }

      Contents.addRemarkBlock("Here all used signals are defined");
      for (final var wire : wires.keySet()) {
        OneLine.append(wire);
        while (OneLine.length() < SIGNAL_ALLIGNMENT_SIZE) OneLine.append(" ");
        OneLine.append(": std_logic");
        if (wires.get(wire) == 1) {
          OneLine.append(";");
        } else {
          OneLine.append("_vector( ");
          if (wires.get(wire) < 0) {
            if (!params.containsKey(wires.get(wire))) {
              Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              return Contents.clear().get();
            }
            OneLine.append("(").append(params.get(wires.get(wire))).append("-1)");
          } else {
            OneLine.append((wires.get(wire) == 0) ? "0" : (wires.get(wire) - 1));
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL {{1}}", OneLine);
        OneLine.setLength(0);
      }

      for (final var reg : regs.keySet()) {
        OneLine.append(reg);
        while (OneLine.length() < SIGNAL_ALLIGNMENT_SIZE) OneLine.append(" ");
        OneLine.append(": std_logic");
        if (regs.get(reg) == 1) {
          OneLine.append(";");
        } else {
          OneLine.append("_vector( ");
          if (regs.get(reg) < 0) {
            if (!params.containsKey(regs.get(reg))) {
              Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              Contents.clear();
              return Contents.get();
            }
            OneLine.append("(").append(params.get(regs.get(reg))).append("-1)");
          } else {
            if (regs.get(reg) == 0) {
              OneLine.append("0");
            } else {
              OneLine.append((regs.get(reg) - 1));
            }
          }
          OneLine.append(" DOWNTO 0 );");
        }
        Contents.add("   SIGNAL {{1}}", OneLine);
        OneLine.setLength(0);
      }

      for (final var Mem : mems.keySet()) {
        OneLine.append(Mem);
        while (OneLine.length() < SIGNAL_ALLIGNMENT_SIZE) OneLine.append(" ");
        OneLine.append(": ").append(GetType(mems.get(Mem))).append(";");
        Contents.add("   SIGNAL " + OneLine);
        OneLine.setLength(0);
      }
      Contents.add("")
          .add("BEGIN")
          .add(GetModuleFunctionality(theNetlist, attrs))
          .add("END PlatformIndependent;");
    } else {
      final var Preamble = String.format("module %s( ", componentName);
      final var Indenting = new StringBuilder();
      while (Indenting.length() < Preamble.length()) Indenting.append(" ");
      if (inputs.isEmpty() && outputs.isEmpty() && inOuts.isEmpty()) {
        Contents.add(Preamble + " );");
      } else {
        final var ThisLine = new StringBuilder();
        for (final var inp : inputs.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(inp);
          } else {
            Contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(inp);
          }
        }
        for (final var outp : outputs.keySet()) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(outp);
          } else {
            Contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(outp);
          }
        }
        for (final var io : inOuts.keySet()) {
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
      if (!params.isEmpty()) {
        Contents.empty();
        Contents.addRemarkBlock("Here all module parameters are defined with a dummy value");
        for (final var param : params.keySet()) {
          Contents.add("   parameter {{1}} = 1;", params.get(param));
        }
        Contents.empty();
      }
      var firstline = true;
      var nr_of_bits = 0;
      for (final var inp : inputs.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   input");
        nr_of_bits = inputs.get(inp);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!params.containsKey(nr_of_bits)) {
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return Contents.clear().get();
          }
          OneLine.append("[").append(params.get(nr_of_bits)).append("-1:0]");
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
          Contents.addRemarkBlock("Here the inputs are defined");
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (final var outp : outputs.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   output");
        nr_of_bits = outputs.get(outp);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!params.containsKey(nr_of_bits)) {
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            Contents.clear();
            return Contents.get();
          }
          OneLine.append("[").append(params.get(nr_of_bits)).append("-1:0]");
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
          Contents.empty().addRemarkBlock("Here the outputs are defined");
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (final var io : inOuts.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   inout");
        nr_of_bits = inOuts.get(io);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!params.containsKey(nr_of_bits)) {
            Reporter.Report.AddFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return Contents.clear().get();
          }
          OneLine.append("[").append(params.get(nr_of_bits)).append("-1:0]");
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
          Contents.empty().addRemarkBlock("Here the ios are defined");
        }
        Contents.add(OneLine.toString());
      }
      firstline = true;
      for (final var wire : wires.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   wire");
        nr_of_bits = wires.get(wire);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!params.containsKey(nr_of_bits)) {
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return Contents.clear().get();
          }
          OneLine.append("[").append(params.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) OneLine.append("[0:0]");
          }
        }
        OneLine.append(" ").append(wire).append(";");
        if (firstline) {
          firstline = false;
          Contents.empty();
          Contents.addRemarkBlock("Here the internal wires are defined");
        }
        Contents.add(OneLine.toString());
      }
      for (final var reg : regs.keySet()) {
        OneLine.setLength(0);
        OneLine.append("   reg");
        nr_of_bits = regs.get(reg);
        if (nr_of_bits < 0) {
          /* we have a parameterized array */
          if (!params.containsKey(nr_of_bits)) {
            Reporter.Report.AddFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return Contents.clear().get();
          }
          OneLine.append("[").append(params.get(nr_of_bits)).append("-1:0]");
        } else {
          if (nr_of_bits > 1) {
            OneLine.append("[").append(nr_of_bits - 1).append(":0]");
          } else {
            if (nr_of_bits == 0) OneLine.append("[0:0]");
          }
        }
        OneLine.append(" ").append(reg).append(";");
        if (firstline) {
          firstline = false;
          Contents
              .empty()
              .addRemarkBlock("Here the internal registers are defined");
        }
        Contents.add(OneLine.toString());
      }
      /* TODO: Add memlist */
      if (!firstline) {
        Contents.empty();
      }
      Contents.add(GetModuleFunctionality(theNetlist, attrs)).empty().add("endmodule");
    }
    return Contents.get();
  }

  public ArrayList<String> GetComponentDeclarationSection(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * This method returns all the component definitions used as component
     * in the circuit. This method is only called in case of VHDL-code
     * generation.
     */
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> getComponentInstantiation(Netlist theNetlist, AttributeSet attrs, String componentName) {
    var Contents = new LineBuffer();
    if (HDL.isVHDL()) Contents.add(GetVHDLBlackBox(theNetlist, attrs, componentName, false));
    return Contents.get();
  }

  @Override
  public ArrayList<String> getComponentMap(
      Netlist nets,
      Long componentId,
      NetlistComponent componentInfo,
      MappableResourcesContainer mapInfo,
      String name) {
    final var Contents = new ArrayList<String>();
    final var ParameterMap = GetParameterMap(nets, componentInfo);
    final var PortMap = GetPortMap(nets, componentInfo == null ? mapInfo : componentInfo);
    final var CompName = (name != null && !name.isEmpty()) ? name :
        (componentInfo == null)
            ? this.getComponentStringIdentifier()
            : componentInfo.getComponent()
                .getFactory()
                .getHDLName(componentInfo.getComponent().getAttributeSet());
    final var ThisInstanceIdentifier = GetInstanceIdentifier(componentInfo, componentId);
    final var OneLine = new StringBuilder();
    var TabLength = 0;
    var first = true;
    if (HDL.isVHDL()) {
      Contents.add("   " + ThisInstanceIdentifier + " : " + CompName);
      if (!ParameterMap.isEmpty()) {
        OneLine.append("      GENERIC MAP ( ");
        TabLength = OneLine.length();
        first = true;
        for (var generic : ParameterMap.keySet()) {
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
          OneLine.append(" ".repeat(Math.max(0, SIGNAL_ALLIGNMENT_SIZE - generic.length())));
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
        for (var port : PortMap.keySet()) {
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
          OneLine.append(" ".repeat(Math.max(0, SIGNAL_ALLIGNMENT_SIZE - port.length())));
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
        for (var parameter : ParameterMap.keySet()) {
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
          OneLine.append(".").append(parameter).append("(").append(ParameterMap.get(parameter)).append(")");
        }
        OneLine.append(")");
        Contents.add(OneLine.toString());
        OneLine.setLength(0);
      }
      OneLine.append("      ").append(ThisInstanceIdentifier).append(" (");
      if (!PortMap.isEmpty()) {
        TabLength = OneLine.length();
        first = true;
        for (var port : PortMap.keySet()) {
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
          final var MappedSignal = PortMap.get(port);
          if (!MappedSignal.contains(",")) {
            OneLine.append(MappedSignal);
          } else {
            String[] VectorList = MappedSignal.split(",");
            OneLine.append("{");
            var TabSize = OneLine.length();
            for (var vectorentries = 0; vectorentries < VectorList.length; vectorentries++) {
              var Entry = VectorList[vectorentries];
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

  @Override
  public String getComponentStringIdentifier() {
    return "AComponent";
  }

  @Override
  public ArrayList<String> getEntity(
      Netlist theNetlist,
      AttributeSet attrs,
      String componentName) {
    var Contents = new LineBuffer();
    if (HDL.isVHDL()) {
      Contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()))
          .add(FileWriter.getExtendedLibrary())
          .add(GetVHDLBlackBox(theNetlist, attrs, componentName, true /* , false */));
    }
    return Contents.get();
  }

  /* Here all public entries for HDL generation are defined */
  public ArrayList<String> GetExtraLibraries() {
    /*
     * this method returns extra VHDL libraries required for simulation
     * and/or synthesis
     */
    return new ArrayList<>();
  }

  @Override
  public ArrayList<String> getInlinedCode(
      Netlist nets,
      Long componentId,
      NetlistComponent componentInfo,
      String circuitName) {
    throw new IllegalAccessError("BUG: Inline code not supported");
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

  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
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
    var NetMap = new HashMap<String, String>();
    if ((EndIndex < 0) || (EndIndex >= comp.nrOfEnds())) {
      Reporter.Report.AddFatalError("INTERNAL ERROR: Component tried to index non-existing SolderPoint");
      return NetMap;
    }
    final var ConnectionInformation = comp.getEnd(EndIndex);
    final var IsOutput = ConnectionInformation.isOutputEnd();
    final var NrOfBits = ConnectionInformation.getNrOfBits();
    if (NrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      NetMap.put(SourceName, HDL.getNetName(comp, EndIndex, FloatingPinTiedToGround, TheNets));
    } else {
      /*
       * Here we have the more difficult case, it is a bus that needs to
       * be mapped
       */
      /* First we check if the bus has a connection */
      var Connected = false;
      for (var i = 0; i < NrOfBits; i++) {
        if (ConnectionInformation.get((byte) i).getParentNet() != null) {
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
        if (TheNets.isContinuesBus(comp, EndIndex)) {
          /* Another easy case, the continues bus connection */
          NetMap.put(SourceName, HDL.getBusNameContinues(comp, EndIndex, TheNets));
        } else {
          /* The last case, we have to enumerate through each bit */
          if (HDL.isVHDL()) {
            var SourceNetName = new StringBuilder();
            for (var i = 0; i < NrOfBits; i++) {
              /* First we build the Line information */
              SourceNetName.setLength(0);
              SourceNetName.append(SourceName).append("(").append(i).append(") ");
              ConnectionPoint SolderPoint = ConnectionInformation.get((byte) i);
              if (SolderPoint.getParentNet() == null) {
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
                if (SolderPoint.getParentNet().getBitWidth() == 1) {
                  /* The connection is to a Net */
                  NetMap.put(
                      SourceNetName.toString(),
                      NET_NAME + TheNets.getNetId(SolderPoint.getParentNet()));
                } else {
                  /* The connection is to an entry of a bus */
                  NetMap.put(
                      SourceNetName.toString(),
                      BUS_NAME
                          + TheNets.getNetId(SolderPoint.getParentNet())
                          + "("
                          + SolderPoint.getParentNetBitIndex()
                          + ")");
                }
              }
            }
          } else {
            var SeperateSignals = new ArrayList<String>();
            /*
             * First we build an array with all the signals that
             * need to be concatenated
             */
            for (var i = 0; i < NrOfBits; i++) {
              final var SolderPoint = ConnectionInformation.get((byte) i);
              if (SolderPoint.getParentNet() == null) {
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
                if (SolderPoint.getParentNet().getBitWidth() == 1) {
                  /* The connection is to a Net */
                  SeperateSignals.add(NET_NAME + TheNets.getNetId(SolderPoint.getParentNet()));
                } else {
                  /* The connection is to an entry of a bus */
                  SeperateSignals.add(
                      BUS_NAME
                          + TheNets.getNetId(SolderPoint.getParentNet())
                          + "["
                          + SolderPoint.getParentNetBitIndex()
                          + "]");
                }
              }
            }
            /* Finally we can put all together */
            var Vector = new StringBuilder();
            Vector.append("{");
            for (var i = NrOfBits; i > 0; i--) {
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

  @Override
  public String getRelativeDirectory() {
    var Subdir = GetSubDir();
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

  private ArrayList<String> GetVHDLBlackBox(Netlist TheNetlist, AttributeSet attrs,
      String ComponentName, Boolean IsEntity) {
    var Contents = new ArrayList<String>();
    final var InputsList = GetInputList(TheNetlist, attrs);
    final var InOutsList = GetInOutList(TheNetlist, attrs);
    final var OutputsList = GetOutputList(TheNetlist, attrs);
    final var ParameterList = GetParameterList(attrs);
    var OneLine = new StringBuilder();
    var IdentSize = 0;
    var CompTab = (IsEntity) ? "" : "   ";
    var first = true;
    if (IsEntity) {
      Contents.add("ENTITY " + ComponentName + " IS");
    } else {
      Contents.add("   COMPONENT " + ComponentName);
    }
    if (!ParameterList.isEmpty()) {
      OneLine.append(CompTab).append("   GENERIC ( ");
      IdentSize = OneLine.length();
      first = true;
      for (var generic : ParameterList.keySet()) {
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
        OneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - ParameterList.get(generic).length())));
        OneLine.append(": INTEGER");
      }
      OneLine.append(");");
      Contents.add(OneLine.toString());
      OneLine.setLength(0);
    }
    if (!InputsList.isEmpty() || !OutputsList.isEmpty() || !InOutsList.isEmpty()) {
      var nr_of_bits = 0;
      OneLine.append(CompTab).append("   PORT ( ");
      IdentSize = OneLine.length();
      first = true;
      for (var input : InputsList.keySet()) {
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
        OneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - input.length())));
        OneLine.append(": IN  std_logic");
        nr_of_bits = InputsList.get(input);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (")
              .append(ParameterList.get(nr_of_bits))
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
      for (var inout : InOutsList.keySet()) {
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
        OneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - inout.length())));
        OneLine.append(": INOUT  std_logic");
        nr_of_bits = InOutsList.get(inout);
        if (nr_of_bits < 0) {
          /* we have a parameterized input */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (")
              .append(ParameterList.get(nr_of_bits))
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
      for (var output : OutputsList.keySet()) {
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
        OneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - output.length())));
        OneLine.append(": OUT std_logic");
        nr_of_bits = OutputsList.get(output);
        if (nr_of_bits < 0) {
          /* we have a parameterized output */
          if (!ParameterList.containsKey(nr_of_bits)) {
            Contents.clear();
            return Contents;
          }
          OneLine.append("_vector( (")
              .append(ParameterList.get(nr_of_bits))
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

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean isOnlyInlined() {
    return false;
  }

  public static ArrayList<String> GetToplevelCode(MapComponent Component) {
    var temp = new StringBuffer();
    var contents = new ArrayList<String>();
    if (Component.getNrOfPins() <= 0) {
      Reporter.Report.AddError("BUG: Found a component with no pins");
      return contents;
    }
    for (var i = 0; i < Component.getNrOfPins(); i++) {
      temp.setLength(0);
      temp.append("   ").append(HDL.assignPreamble());
      /* the internal mapped signals are handled in the top-level HDL generator */
      if (Component.isInternalMapped(i)) continue;
      /* IO-pins need to be mapped directly to the top-level component and cannot be
       * passed by signals, so we skip them.
       */
      if (Component.isIO(i)) continue;
      if (!Component.isMapped(i)) {
        /* unmapped output pins we leave unconnected */
        if (Component.isOutput(i)) continue;
        temp.append(Component.getHdlSignalName(i));
        allign(temp);
        temp.append(HDL.assignOperator());
        temp.append(HDL.zeroBit()).append(";");
        contents.add(temp.toString());
        continue;
      }
      if (Component.isInput(i)) {
        temp.append(Component.getHdlSignalName(i));
        allign(temp);
        temp.append(HDL.assignOperator());
        if (Component.IsConstantMapped(i)) {
          temp.append(Component.isZeroConstantMap(i) ? HDL.zeroBit() : HDL.oneBit());
        } else {
          if (Component.isExternalInverted(i)) temp.append(HDL.notOperator()).append("n_");
          temp.append(Component.getHdlString(i));
        }
        temp.append(";");
        contents.add(temp.toString());
        continue;
      }
      if (Component.IsOpenMapped(i)) continue;
      if (Component.isExternalInverted(i)) temp.append("n_");
      temp.append(Component.getHdlString(i));
      allign(temp);
      temp.append(HDL.assignOperator());
      if (Component.isExternalInverted(i)) temp.append(HDL.notOperator());
      temp.append(Component.getHdlSignalName(i)).append(";");
      contents.add(temp.toString());
    }
    contents.add(" ");
    return contents;
  }

  private static void allign(StringBuffer s) {
    while (s.length() < 40) s.append(" ");
  }
}
