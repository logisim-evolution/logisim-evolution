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
import com.cburch.logisim.fpga.designrulecheck.ConnectionPoint;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.ClockHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

public class AbstractHdlGeneratorFactory implements HdlGeneratorFactory {

  private final String subDirectoryName;
  protected final HdlParameters myParametersList = new HdlParameters();
  protected final HdlWires myWires = new HdlWires();
  protected final HdlPorts myPorts = new HdlPorts();
  protected final HdlTypes myTypedWires = new HdlTypes();
  protected boolean getWiresPortsDuringHDLWriting = false;

  public AbstractHdlGeneratorFactory() {
    final var className = getClass().toString().replace('.', ':').replace(' ', ':');
    final var parts = className.split(":");
    if (parts.length < 2) throw new ExceptionInInitializerError("Cannot read class path!");
    subDirectoryName = parts[parts.length - 2];
  }

  public AbstractHdlGeneratorFactory(String subDirectory) {
    subDirectoryName = subDirectory;
  }

  // Handle to get the wires and ports during generation time
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {}

  /* Here the common predefined methods are defined */
  @Override
  public boolean generateAllHDLDescriptions(
      Set<String> handledComponents,
      String workingDirectory,
      List<String> hierarchy) {
    return true;
  }

  @Override
  public ArrayList<String> getArchitecture(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var oneLine = new StringBuilder();
    if (getWiresPortsDuringHDLWriting) {
      myWires.removeWires();
      myTypedWires.clear();
      myPorts.removePorts();
      getGenerationTimeWiresPorts(theNetlist, attrs);
    }
    contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()));
    if (Hdl.isVhdl()) {
      final var libs = GetExtraLibraries();
      if (!libs.isEmpty()) {
        contents.add(libs);
        contents.empty();
      }
      contents.add("ARCHITECTURE PlatformIndependent OF {{1}} IS ", componentName);
      contents.add("");
      if (myTypedWires.getNrOfTypes() > 0) {
        contents.addRemarkBlock("Here all private types are defined")
            .add(myTypedWires.getTypeDefinitions())
            .empty();
      }
      final var components = GetComponentDeclarationSection(theNetlist, attrs);
      if (!components.isEmpty()) {
        contents.addRemarkBlock("Here all used components are defined").add(components).add("");
      }

      contents.addRemarkBlock("Here all used signals are defined");
      for (final var wire : myWires.wireKeySet()) {
        oneLine.append(wire);
        while (oneLine.length() < SIGNAL_ALLIGNMENT_SIZE) oneLine.append(" ");
        oneLine.append(": std_logic");
        if (myWires.get(wire) == 1) {
          oneLine.append(";");
        } else {
          oneLine.append("_vector( ");
          if (myWires.get(wire) < 0) {
            if (!myParametersList.containsKey(myWires.get(wire), attrs)) {
              Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              return contents.clear().get();
            }
            oneLine.append("(").append(myParametersList.get(myWires.get(wire), attrs)).append("-1)");
          } else {
            oneLine.append((myWires.get(wire) == 0) ? "0" : (myWires.get(wire) - 1));
          }
          oneLine.append(" DOWNTO 0 );");
        }
        contents.add("   SIGNAL {{1}}", oneLine);
        oneLine.setLength(0);
      }

      for (final var reg : myWires.registerKeySet()) {
        oneLine.append(reg);
        while (oneLine.length() < SIGNAL_ALLIGNMENT_SIZE) oneLine.append(" ");
        oneLine.append(": std_logic");
        if (myWires.get(reg) == 1) {
          oneLine.append(";");
        } else {
          oneLine.append("_vector( ");
          if (myWires.get(reg) < 0) {
            if (!myParametersList.containsKey(myWires.get(reg), attrs)) {
              Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
              contents.clear();
              return contents.get();
            }
            oneLine.append("(").append(myParametersList.get(myWires.get(reg), attrs)).append("-1)");
          } else {
            if (myWires.get(reg) == 0) {
              oneLine.append("0");
            } else {
              oneLine.append((myWires.get(reg) - 1));
            }
          }
          oneLine.append(" DOWNTO 0 );");
        }
        contents.add("   SIGNAL {{1}}", oneLine.toString());
        oneLine.setLength(0);
      }

      final var typedWires = myTypedWires.getTypedWires();
      for (final var wire : typedWires.keySet()) {
        oneLine.append(wire);
        while (oneLine.length() < SIGNAL_ALLIGNMENT_SIZE) oneLine.append(" ");
        oneLine.append(": ").append(typedWires.get(wire)).append(";");
        contents.add("   SIGNAL {{1}}", oneLine.toString());
        oneLine.setLength(0);
      }
      contents.add("")
          .add("BEGIN")
          .add(getModuleFunctionality(theNetlist, attrs))
          .add("END PlatformIndependent;");
    } else {
      final var Preamble = String.format("module %s( ", componentName);
      final var Indenting = new StringBuilder();
      while (Indenting.length() < Preamble.length()) Indenting.append(" ");
      if (myPorts.isEmpty()) {
        contents.add(Preamble + " );");
      } else {
        final var ThisLine = new StringBuilder();
        for (final var inp : myPorts.keySet(Port.INPUT)) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(inp);
          } else {
            contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(inp);
          }
          // Special case for the clocks we have to add the tick
          if (myPorts.isClock(inp)) {
            contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(myPorts.getTickName(inp));
          }
        }
        for (final var outp : myPorts.keySet(Port.OUTPUT)) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(outp);
          } else {
            contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(outp);
          }
        }
        for (final var io : myPorts.keySet(Port.INOUT)) {
          if (ThisLine.length() == 0) {
            ThisLine.append(Preamble).append(io);
          } else {
            contents.add(ThisLine + ",");
            ThisLine.setLength(0);
            ThisLine.append(Indenting).append(io);
          }
        }
        if (ThisLine.length() != 0) {
          contents.add(ThisLine + ");");
        } else {
          Reporter.report.addError("Internale Error in Verilog Architecture generation!");
        }
      }
      if (!myParametersList.isEmpty(attrs)) {
        contents.empty();
        contents.addRemarkBlock("Here all module parameters are defined with a dummy value");
        for (final var param : myParametersList.keySet(attrs)) {
          // For verilog we specify a maximum vector, this seems the best way to do it
          final var vectorString = (myParametersList.isPresentedByInteger(param, attrs)) ? "" : "[64:0]";
          contents.add("   parameter {{1}} {{2}} = 1;", vectorString, myParametersList.get(param, attrs));
        }
        contents.empty();
      }
      if (myTypedWires.getNrOfTypes() > 0) {
        contents.addRemarkBlock("Here all private types are defined")
            .add(myTypedWires.getTypeDefinitions())
            .empty();
      }
      var firstline = true;
      var nrOfPortBits = 0;
      for (final var inp : myPorts.keySet(Port.INPUT)) {
        oneLine.setLength(0);
        oneLine.append("   input");
        nrOfPortBits = myPorts.get(inp, attrs);
        if (nrOfPortBits < 0) {
          /* we have a parameterized array */
          if (!myParametersList.containsKey(nrOfPortBits, attrs)) {
            Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return contents.clear().get();
          }
          oneLine.append("[").append(myParametersList.get(nrOfPortBits, attrs)).append("-1:0]");
        } else {
          if (nrOfPortBits > 1) {
            oneLine.append("[").append(nrOfPortBits - 1).append(":0]");
          } else {
            if (nrOfPortBits == 0) {
              oneLine.append("[0:0]");
            }
          }
        }
        oneLine.append("  ").append(inp).append(";");
        if (firstline) {
          firstline = false;
          contents.add("");
          contents.addRemarkBlock("Here the inputs are defined");
        }
        contents.add(oneLine.toString());
        // special case for the clock, we have to add the tick
        if (myPorts.isClock(inp)) {
          oneLine.setLength(0);
          oneLine.append("   input  ").append(myPorts.getTickName(inp)).append(";");
          contents.add(oneLine.toString());
        }
      }
      firstline = true;
      for (final var outp : myPorts.keySet(Port.OUTPUT)) {
        oneLine.setLength(0);
        oneLine.append("   output");
        nrOfPortBits = myPorts.get(outp, attrs);
        if (nrOfPortBits < 0) {
          /* we have a parameterized array */
          if (!myParametersList.containsKey(nrOfPortBits, attrs)) {
            Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            contents.clear();
            return contents.get();
          }
          oneLine.append("[").append(myParametersList.get(nrOfPortBits, attrs)).append("-1:0]");
        } else {
          if (nrOfPortBits > 1) {
            oneLine.append("[").append(nrOfPortBits - 1).append(":0]");
          } else {
            if (nrOfPortBits == 0) {
              oneLine.append("[0:0]");
            }
          }
        }
        oneLine.append(" ").append(outp).append(";");
        if (firstline) {
          firstline = false;
          contents.empty().addRemarkBlock("Here the outputs are defined");
        }
        contents.add(oneLine.toString());
      }
      firstline = true;
      for (final var io : myPorts.keySet(Port.INOUT)) {
        oneLine.setLength(0);
        oneLine.append("   inout");
        nrOfPortBits = myPorts.get(io, attrs);
        if (nrOfPortBits < 0) {
          /* we have a parameterized array */
          if (!myParametersList.containsKey(nrOfPortBits, attrs)) {
            Reporter.report.addFatalError(
                "Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return contents.clear().get();
          }
          oneLine.append("[").append(myParametersList.get(nrOfPortBits, attrs)).append("-1:0]");
        } else {
          if (nrOfPortBits > 1) {
            oneLine.append("[").append(nrOfPortBits - 1).append(":0]");
          } else {
            if (nrOfPortBits == 0) {
              oneLine.append("[0:0]");
            }
          }
        }
        oneLine.append(" ").append(io).append(";");
        if (firstline) {
          firstline = false;
          contents.empty().addRemarkBlock("Here the ios are defined");
        }
        contents.add(oneLine.toString());
      }
      firstline = true;
      for (final var wire : myWires.wireKeySet()) {
        oneLine.setLength(0);
        oneLine.append("   wire");
        nrOfPortBits = myWires.get(wire);
        if (nrOfPortBits < 0) {
          /* we have a parameterized array */
          if (!myParametersList.containsKey(nrOfPortBits, attrs)) {
            Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return contents.clear().get();
          }
          oneLine.append("[").append(myParametersList.get(nrOfPortBits, attrs)).append("-1:0]");
        } else {
          if (nrOfPortBits > 1) {
            oneLine.append("[").append(nrOfPortBits - 1).append(":0]");
          } else {
            if (nrOfPortBits == 0) oneLine.append("[0:0]");
          }
        }
        oneLine.append(" ").append(wire).append(";");
        if (firstline) {
          firstline = false;
          contents.empty();
          contents.addRemarkBlock("Here the internal wires are defined");
        }
        contents.add(oneLine.toString());
      }
      firstline = true;
      for (final var reg : myWires.registerKeySet()) {
        oneLine.setLength(0);
        oneLine.append("   reg");
        nrOfPortBits = myWires.get(reg);
        if (nrOfPortBits < 0) {
          /* we have a parameterized array */
          if (!myParametersList.containsKey(nrOfPortBits, attrs)) {
            Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
            return contents.clear().get();
          }
          oneLine.append("[").append(myParametersList.get(nrOfPortBits, attrs)).append("-1:0]");
        } else {
          if (nrOfPortBits > 1) {
            oneLine.append("[").append(nrOfPortBits - 1).append(":0]");
          } else {
            if (nrOfPortBits == 0) oneLine.append("[0:0]");
          }
        }
        oneLine.append(" ").append(reg).append(";");
        if (firstline) {
          firstline = false;
          contents
              .empty()
              .addRemarkBlock("Here the internal registers are defined");
        }
        contents.add(oneLine.toString());
      }
      firstline = true;
      final var typedWires = myTypedWires.getTypedWires();
      for (final var wire : typedWires.keySet()) {
        oneLine.setLength(0);
        oneLine.append("   ")
            .append(typedWires.get(wire))
            .append(" ")
            .append(wire)
            .append(";");
        if (firstline) {
          firstline = false;
          contents
              .empty()
              .addRemarkBlock("Here the type defined signals are defined");
        }
        contents.add("   {{1}}", oneLine.toString());
      }
      if (!firstline) {
        contents.empty();
      }
      contents.add(getModuleFunctionality(theNetlist, attrs)).empty().add("endmodule");
    }
    return contents.get();
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
    var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) contents.add(GetVHDLBlackBox(theNetlist, attrs, componentName, false));
    return contents.get();
  }

  @Override
  public ArrayList<String> getComponentMap(
      Netlist nets,
      Long componentId,
      Object componentInfo,
      String name) {
    final var contents = new ArrayList<String>();
    final var parameterMap = new TreeMap<String, String>();
    final var portMap = getPortMap(nets, componentInfo);
    final var componentHdlName = (componentInfo instanceof netlistComponent nc)
                   ? nc.getComponent().getFactory().getHDLName(nc.getComponent().getAttributeSet())
                   : name;
    final var CompName = (name != null && !name.isEmpty()) ? name : componentHdlName;
    final var ThisInstanceIdentifier = getInstanceIdentifier(componentInfo, componentId);
    final var oneLine = new StringBuilder();
    if (componentInfo == null) parameterMap.putAll(myParametersList.getMaps(null));
    if (componentInfo instanceof netlistComponent nc) {
      final var attrs = nc.getComponent().getAttributeSet();
      parameterMap.putAll(myParametersList.getMaps(attrs));
    }
    var TabLength = 0;
    var first = true;
    if (Hdl.isVhdl()) {
      contents.add("   " + ThisInstanceIdentifier + " : " + CompName);
      if (!parameterMap.isEmpty()) {
        oneLine.append("      GENERIC MAP ( ");
        TabLength = oneLine.length();
        first = true;
        for (var generic : parameterMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            while (oneLine.length() < TabLength) {
              oneLine.append(" ");
            }
          } else {
            first = false;
          }
          oneLine.append(generic);
          oneLine.append(" ".repeat(Math.max(0, SIGNAL_ALLIGNMENT_SIZE - generic.length())));
          oneLine.append("=> ").append(parameterMap.get(generic));
        }
        oneLine.append(")");
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
      if (!portMap.isEmpty()) {
        oneLine.append("      PORT MAP ( ");
        TabLength = oneLine.length();
        first = true;
        for (var port : portMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            while (oneLine.length() < TabLength) {
              oneLine.append(" ");
            }
          } else {
            first = false;
          }
          oneLine.append(port);
          oneLine.append(" ".repeat(Math.max(0, SIGNAL_ALLIGNMENT_SIZE - port.length())));
          oneLine.append("=> ").append(portMap.get(port));
        }
        oneLine.append(");");
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
    } else {
      oneLine.append("   ").append(CompName);
      if (!parameterMap.isEmpty()) {
        oneLine.append(" #(");
        TabLength = oneLine.length();
        first = true;
        for (var parameter : parameterMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            while (oneLine.length() < TabLength) {
              oneLine.append(" ");
            }
          } else {
            first = false;
          }
          oneLine.append(".").append(parameter).append("(").append(parameterMap.get(parameter)).append(")");
        }
        oneLine.append(")");
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
      oneLine.append("      ").append(ThisInstanceIdentifier).append(" (");
      if (!portMap.isEmpty()) {
        TabLength = oneLine.length();
        first = true;
        for (var port : portMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            while (oneLine.length() < TabLength) {
              oneLine.append(" ");
            }
          } else {
            first = false;
          }
          oneLine.append(".").append(port).append("(");
          final var MappedSignal = portMap.get(port);
          if (!MappedSignal.contains(",")) {
            oneLine.append(MappedSignal);
          } else {
            String[] VectorList = MappedSignal.split(",");
            oneLine.append("{");
            var TabSize = oneLine.length();
            for (var vectorentries = 0; vectorentries < VectorList.length; vectorentries++) {
              var Entry = VectorList[vectorentries];
              if (Entry.contains("{")) {
                Entry = Entry.replace("{", "");
              }
              if (Entry.contains("}")) {
                Entry = Entry.replace("}", "");
              }
              oneLine.append(Entry);
              if (vectorentries < VectorList.length - 1) {
                contents.add(oneLine + ",");
                oneLine.setLength(0);
                while (oneLine.length() < TabSize) {
                  oneLine.append(" ");
                }
              } else {
                oneLine.append("}");
              }
            }
          }
          oneLine.append(")");
        }
      }
      oneLine.append(");");
      contents.add(oneLine.toString());
    }
    contents.add("");
    return contents;
  }

  @Override
  public ArrayList<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName) {
    var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()))
          .add(FileWriter.getExtendedLibrary())
          .add(GetVHDLBlackBox(theNetlist, attrs, componentName, true));
    }
    return contents.get();
  }

  private String getInstanceIdentifier(Object componentInfo, Long componentId) {
    if (componentInfo instanceof netlistComponent nc) {
      final var attrs = nc.getComponent().getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var label = attrs.getValue(StdAttr.LABEL);
        if ((label != null) && !label.isEmpty())
          return CorrectLabel.getCorrectLabel(label);
      }
    }
    return LineBuffer.format("{{1}}_{{2}}", subDirectoryName.toUpperCase(), componentId.toString());
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
      netlistComponent componentInfo,
      String circuitName) {
    throw new IllegalAccessError("BUG: Inline code not supported");
  }

  public ArrayList<String> getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    /*
     * In this method the functionality of the black-box is described. It is
     * used for both VHDL and VERILOG.
     */
    return new ArrayList<>();
  }

  public Map<String, String> GetNetMap(
      String SourceName,
      boolean FloatingPinTiedToGround,
      netlistComponent comp,
      int EndIndex,
      Netlist TheNets) {
    var NetMap = new HashMap<String, String>();
    if ((EndIndex < 0) || (EndIndex >= comp.nrOfEnds())) {
      Reporter.report.addFatalError("INTERNAL ERROR: Component tried to index non-existing SolderPoint");
      return NetMap;
    }
    final var ConnectionInformation = comp.getEnd(EndIndex);
    final var IsOutput = ConnectionInformation.isOutputEnd();
    final var NrOfBits = ConnectionInformation.getNrOfBits();
    if (NrOfBits == 1) {
      /* Here we have the easy case, just a single bit net */
      NetMap.put(SourceName, Hdl.getNetName(comp, EndIndex, FloatingPinTiedToGround, TheNets));
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
          NetMap.put(SourceName, Hdl.unconnected(true));
        } else {
          NetMap.put(SourceName, Hdl.getZeroVector(NrOfBits, FloatingPinTiedToGround));
        }
      } else {
        /*
         * There are connections, we detect if it is a continues bus
         * connection
         */
        if (TheNets.isContinuesBus(comp, EndIndex)) {
          /* Another easy case, the continues bus connection */
          NetMap.put(SourceName, Hdl.getBusNameContinues(comp, EndIndex, TheNets));
        } else {
          /* The last case, we have to enumerate through each bit */
          if (Hdl.isVhdl()) {
            var SourceNetName = new StringBuilder();
            for (var i = 0; i < NrOfBits; i++) {
              /* First we build the Line information */
              SourceNetName.setLength(0);
              SourceNetName.append(SourceName).append("(").append(i).append(") ");
              ConnectionPoint SolderPoint = ConnectionInformation.get((byte) i);
              if (SolderPoint.getParentNet() == null) {
                /* The net is not connected */
                if (IsOutput) {
                  NetMap.put(SourceNetName.toString(), Hdl.unconnected(false));
                } else {
                  NetMap.put(SourceNetName.toString(), Hdl.getZeroVector(1, FloatingPinTiedToGround));
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
                  SeperateSignals.add(Hdl.getZeroVector(1, FloatingPinTiedToGround));
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

  public int getNrOfTypes(Netlist TheNetlist, AttributeSet attrs) {
    /* In this method you can specify the number of own defined Types */
    return 0;
  }

  public SortedMap<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var result = new TreeMap<String, String>();
    if (mapInfo instanceof netlistComponent componentInfo && !myPorts.isEmpty()) {
      final var compName = componentInfo.getComponent().getFactory().getDisplayName();
      final var attrs = componentInfo.getComponent().getAttributeSet();
      if (getWiresPortsDuringHDLWriting) {
        myWires.removeWires();
        myTypedWires.clear();
        myPorts.removePorts();
        getGenerationTimeWiresPorts(nets, componentInfo.getComponent().getAttributeSet());
      }
      for (var port : myPorts.keySet()) {
        if (myPorts.isClock(port)) {
          var gatedClock = false;
          var hasClock = true;
          var clockAttr = attrs.containsAttribute(StdAttr.EDGE_TRIGGER)
              ? attrs.getValue(StdAttr.EDGE_TRIGGER) : attrs.getValue(StdAttr.TRIGGER);
          if (clockAttr == null) clockAttr = StdAttr.TRIG_RISING; // default case if no other specified (for TTL library)
          final var activeLow = StdAttr.TRIG_LOW.equals(clockAttr) || StdAttr.TRIG_FALLING.equals(clockAttr);
          final var compPinId = myPorts.getComponentPortId(port);
          if (!componentInfo.isEndConnected(compPinId)) {
            // FIXME hard coded string
            Reporter.report.addSevereWarning(
                String.format("Component \"%s\" in circuit \"%s\" has no clock connection!", compName, nets.getCircuitName()));
            hasClock = false;
          }
          final var clockNetName = Hdl.getClockNetName(componentInfo, compPinId, nets);
          if (clockNetName == null || clockNetName.isEmpty()) {
            // FIXME hard coded string
            Reporter.report.addSevereWarning(
                String.format("Component \"%s\" in circuit \"%s\" has a gated clock connection!", compName, nets.getCircuitName()));
            gatedClock = true;
          }
          if (hasClock && !gatedClock && Netlist.isFlipFlop(attrs)) {
            if (nets.requiresGlobalClockConnection()) {
              result.put(myPorts.getTickName(port), LineBuffer
                  .formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX));
            } else {
              final var clockIndex = activeLow ? ClockHDLGeneratorFactory.NEGATIVE_EDGE_TICK_INDEX : ClockHDLGeneratorFactory.POSITIVE_EDGE_TICK_INDEX;
              result.put(myPorts.getTickName(port), LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, clockIndex));
            }
            result.put(HdlPorts.CLOCK, LineBuffer
                .formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, ClockHDLGeneratorFactory.GLOBAL_CLOCK_INDEX));
          } else if (!hasClock) {
            result.put(myPorts.getTickName(port), Hdl.zeroBit());
            result.put(HdlPorts.CLOCK, Hdl.zeroBit());
          } else {
            result.put(myPorts.getTickName(port), Hdl.oneBit());
            if (!gatedClock) {
              final var clockIndex = activeLow ? ClockHDLGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX : ClockHDLGeneratorFactory.DERIVED_CLOCK_INDEX;
              result.put(HdlPorts.CLOCK, LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, clockIndex));
            } else {
              result.put(HdlPorts.CLOCK, Hdl.getNetName(componentInfo, compPinId, true, nets));
            }
          }
        } else if (myPorts.isFixedMapped(port)) {
          final var fixedMap = myPorts.getFixedMap(port);
          if (HdlPorts.PULL_DOWN.equals(fixedMap))
            result.put(port, Hdl.getConstantVector(0, myPorts.get(port, attrs)));
          else if (HdlPorts.PULL_UP.equals(fixedMap))
            result.put(port, Hdl.getConstantVector(0xFFFFFFFFFFFFFFFFL, myPorts.get(port, attrs)));
          else
            result.put(port, fixedMap);
        } else {
          result.putAll(GetNetMap(port, myPorts.doPullDownOnFloat(port), componentInfo, myPorts.getComponentPortId(port), nets));
        }
      }
    }
    return result;
  }

  @Override
  public String getRelativeDirectory() {
    final var mainDirectory = AppPreferences.HdlType.get().toLowerCase();
    final var directoryName = new StringBuilder();
    directoryName.append(mainDirectory);
    if (!mainDirectory.endsWith(File.separator)) directoryName.append(File.separator);
    if (!subDirectoryName.isEmpty()) {
      directoryName.append(subDirectoryName);
      if (!subDirectoryName.endsWith(File.separator)) directoryName.append(File.separator);
    }
    return directoryName.toString();
  }

  private ArrayList<String> GetVHDLBlackBox(Netlist TheNetlist, AttributeSet attrs,
      String ComponentName, Boolean IsEntity) {
    var contents = new ArrayList<String>();
    var oneLine = new StringBuilder();
    var IdentSize = 0;
    var CompTab = (IsEntity) ? "" : "   ";
    var first = true;
    if (getWiresPortsDuringHDLWriting) {
      myWires.removeWires();
      myTypedWires.clear();
      myPorts.removePorts();
      getGenerationTimeWiresPorts(TheNetlist, attrs);
    }
    if (IsEntity) {
      contents.add("ENTITY " + ComponentName + " IS");
    } else {
      contents.add("   COMPONENT " + ComponentName);
    }
    if (!myParametersList.isEmpty(attrs)) {
      oneLine.append(CompTab).append("   GENERIC ( ");
      IdentSize = oneLine.length();
      first = true;
      for (var generic : myParametersList.keySet(attrs)) {
        if (!first) {
          oneLine.append(";");
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          while (oneLine.length() < IdentSize) {
            oneLine.append(" ");
          }
        } else {
          first = false;
        }
        final var parameterName = myParametersList.get(generic, attrs);
        oneLine.append(parameterName);
        oneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - parameterName.length())));
        oneLine.append(myParametersList.isPresentedByInteger(generic, attrs) ? ": INTEGER" : ": std_logic_vector");
      }
      oneLine.append(");");
      contents.add(oneLine.toString());
      oneLine.setLength(0);
    }
    if (!myPorts.isEmpty()) {
      var NrOfPortBits = 0;
      oneLine.append(CompTab).append("   PORT ( ");
      IdentSize = oneLine.length();
      first = true;
      for (var input : myPorts.keySet(Port.INPUT)) {
        if (!first) {
          oneLine.append(";");
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          while (oneLine.length() < IdentSize) {
            oneLine.append(" ");
          }
        } else {
          first = false;
        }
        oneLine.append(input);
        oneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - input.length())));
        oneLine.append(": IN  std_logic");
        NrOfPortBits = myPorts.get(input, attrs);
        if (NrOfPortBits < 0) {
          /* we have a parameterized input */
          if (!myParametersList.containsKey(NrOfPortBits, attrs)) {
            contents.clear();
            return contents;
          }
          oneLine.append("_vector( (")
              .append(myParametersList.get(NrOfPortBits, attrs))
              .append("-1) DOWNTO 0 )");
        } else {
          if (NrOfPortBits > 1) {
            /* we have a bus */
            oneLine.append("_vector( ").append(NrOfPortBits - 1).append(" DOWNTO 0 )");
          } else {
            if (NrOfPortBits == 0) {
              oneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
        // special case of the clock, we have to add the tick
        if (myPorts.isClock(input)) {
          oneLine.append(";");
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          oneLine.append(" ".repeat(IdentSize));
          oneLine.append(myPorts.getTickName(input));
          oneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - myPorts.getTickName(input).length())));
          oneLine.append(": IN  std_logic");
        }
      }
      for (var inout : myPorts.keySet(Port.INOUT)) {
        if (!first) {
          oneLine.append(";");
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          while (oneLine.length() < IdentSize) {
            oneLine.append(" ");
          }
        } else {
          first = false;
        }
        oneLine.append(inout);
        oneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - inout.length())));
        oneLine.append(": INOUT  std_logic");
        NrOfPortBits = myPorts.get(inout, attrs);
        if (NrOfPortBits < 0) {
          /* we have a parameterized input */
          if (!myParametersList.containsKey(NrOfPortBits, attrs)) {
            contents.clear();
            return contents;
          }
          oneLine.append("_vector( (")
              .append(myParametersList.get(NrOfPortBits, attrs))
              .append("-1) DOWNTO 0 )");
        } else {
          if (NrOfPortBits > 1) {
            /* we have a bus */
            oneLine.append("_vector( ").append(NrOfPortBits - 1).append(" DOWNTO 0 )");
          } else {
            if (NrOfPortBits == 0) {
              oneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
      }
      for (var output : myPorts.keySet(Port.OUTPUT)) {
        if (!first) {
          oneLine.append(";");
          contents.add(oneLine.toString());
          oneLine.setLength(0);
          while (oneLine.length() < IdentSize) {
            oneLine.append(" ");
          }
        } else {
          first = false;
        }
        oneLine.append(output);
        oneLine.append(" ".repeat(Math.max(0, PORT_ALLIGNMENT_SIZE - output.length())));
        oneLine.append(": OUT std_logic");
        NrOfPortBits = myPorts.get(output, attrs);
        if (NrOfPortBits < 0) {
          /* we have a parameterized output */
          if (!myParametersList.containsKey(NrOfPortBits, attrs)) {
            contents.clear();
            return contents;
          }
          oneLine.append("_vector( (")
              .append(myParametersList.get(NrOfPortBits, attrs))
              .append("-1) DOWNTO 0 )");
        } else {
          if (NrOfPortBits > 1) {
            /* we have a bus */
            oneLine.append("_vector( ").append(NrOfPortBits - 1).append(" DOWNTO 0 )");
          } else {
            if (NrOfPortBits == 0) {
              oneLine.append("_vector( 0 DOWNTO 0 )");
            }
          }
        }
      }
      oneLine.append(");");
      contents.add(oneLine.toString());
    }
    if (IsEntity) {
      contents.add("END " + ComponentName + ";");
    } else {
      contents.add("   END COMPONENT;");
    }
    contents.add("");
    return contents;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
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
      Reporter.report.addError("BUG: Found a component with no pins");
      return contents;
    }
    for (var i = 0; i < Component.getNrOfPins(); i++) {
      temp.setLength(0);
      temp.append("   ").append(Hdl.assignPreamble());
      /* the internal mapped signals are handled in the top-level HDL generator */
      if (Component.isInternalMapped(i)) continue;
      /* IO-pins need to be mapped directly to the top-level component and cannot be
       * passed by signals, so we skip them.
       */
      if (Component.isIo(i)) continue;
      if (!Component.isMapped(i)) {
        /* unmapped output pins we leave unconnected */
        if (Component.isOutput(i)) continue;
        temp.append(Component.getHdlSignalName(i));
        allign(temp);
        temp.append(Hdl.assignOperator());
        temp.append(Hdl.zeroBit()).append(";");
        contents.add(temp.toString());
        continue;
      }
      if (Component.isInput(i)) {
        temp.append(Component.getHdlSignalName(i));
        allign(temp);
        temp.append(Hdl.assignOperator());
        if (Component.IsConstantMapped(i)) {
          temp.append(Component.isZeroConstantMap(i) ? Hdl.zeroBit() : Hdl.oneBit());
        } else {
          if (Component.isExternalInverted(i)) temp.append(Hdl.notOperator()).append("n_");
          temp.append(Component.getHdlString(i));
        }
        temp.append(";");
        contents.add(temp.toString());
        continue;
      }
      if (Component.isOpenMapped(i)) continue;
      if (Component.isExternalInverted(i)) temp.append("n_");
      temp.append(Component.getHdlString(i));
      allign(temp);
      temp.append(Hdl.assignOperator());
      if (Component.isExternalInverted(i)) temp.append(Hdl.notOperator());
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
