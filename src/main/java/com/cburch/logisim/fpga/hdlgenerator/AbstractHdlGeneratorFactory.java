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
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.wiring.ClockHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;
import com.cburch.logisim.util.StringUtil;
import java.io.File;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

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
  public List<String> getArchitecture(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    if (getWiresPortsDuringHDLWriting) {
      myWires.removeWires();
      myTypedWires.clear();
      myPorts.removePorts();
      getGenerationTimeWiresPorts(theNetlist, attrs);
    }
    contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()));
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("{{architecture}} platformIndependent {{of}} {{1}} {{is}} ", componentName).empty();
      if (myTypedWires.getNrOfTypes() > 0) {
        contents.addRemarkBlock("Here all private types are defined")
            .add(myTypedWires.getTypeDefinitions())
            .empty();
      }

      final var components = getComponentDeclarationSection(theNetlist, attrs);
      if (!components.isEmpty())
        contents.addRemarkBlock("Here all used components are defined", 3).add(components.getWithIndent()).empty();

      final var typedWires = myTypedWires.getTypedWires();
      final var mySignals = new HashMap<String, String>();
      // first we gather some info on the wire names
      var maxNameLength = 0;
      for (final var wire : myWires.wireKeySet()) {
        maxNameLength = Math.max(maxNameLength, wire.length());
        mySignals.put(wire, getTypeIdentifier(myWires.get(wire), attrs));
      }
      for (final var reg : myWires.registerKeySet()) {
        maxNameLength = Math.max(maxNameLength, reg.length());
        mySignals.put(reg, getTypeIdentifier(myWires.get(reg), attrs));
      }
      for (final var wire : typedWires.keySet()) {
        maxNameLength = Math.max(maxNameLength, wire.length());
        mySignals.put(wire, typedWires.get(wire));
      }
      // now we add them
      if (maxNameLength > 0) contents.addRemarkBlock("All used signals are defined here");
      final var sortedSignals = new TreeSet<>(mySignals.keySet());
      for (final var signal : sortedSignals)
        contents.add("   {{signal}} {{1}}{{2}} : {{3}};", signal, " ".repeat(maxNameLength - signal.length()),
            mySignals.get(signal));
      if (maxNameLength > 0) contents.empty();
      contents.add("{{begin}}")
          .add(getModuleFunctionality(theNetlist, attrs).getWithIndent())
          .add("{{end}} platformIndependent;");
    } else {
      final var preamble = String.format("module %s( ", componentName);
      final var indenting = " ".repeat(preamble.length());
      final var body = LineBuffer.getHdlBuffer();
      if (myPorts.isEmpty()) {
        contents.add(preamble + " );");
      } else {
        final var ports = new TreeSet<>(myPorts.keySet());
        for (final var port : myPorts.keySet())
          if (myPorts.isClock(port)) ports.add(myPorts.getTickName(port));
        var first = true;
        var maxNrOfPorts = ports.size();
        for (final var port : ports) {
          maxNrOfPorts--;
          final var end = maxNrOfPorts == 0 ? " );" : ",";
          contents.add("{{1}}{{2}}{{3}}", first ? preamble : indenting, port, end);
          first = false;
        }
      }
      if (!myParametersList.isEmpty(attrs)) {
        body.empty().addRemarkBlock("Here all module parameters are defined with a dummy value");
        final var parameters = new TreeSet<String>();
        for (final var paramId : myParametersList.keySet(attrs)) {
          // For verilog we specify a maximum vector, this seems the best way to do it
          final var paramName = myParametersList.isPresentedByInteger(paramId, attrs)
              ? myParametersList.get(paramId, attrs) : String.format("[64:0] %s", myParametersList.get(paramId, attrs));
          parameters.add(paramName);
        }
        for (final var param : parameters)
          body.add(String.format("parameter %s = 1;", param));
      }
      if (myTypedWires.getNrOfTypes() > 0) {
        body.empty()
            .addRemarkBlock("Here all private types are defined")
            .add(myTypedWires.getTypeDefinitions());
      }
      final var inputs = myPorts.keySet(Port.INPUT);
      for (final var input : myPorts.keySet(Port.INPUT)) {
        if (myPorts.isClock(input))
          inputs.add(myPorts.getTickName(input));
      }
      if (!inputs.isEmpty()) {
        body.empty().addRemarkBlock("The inputs are defined here");
        if (!getVerilogSignalSet("input", inputs, attrs, true, body)) return null;
      }
      final var outputs = myPorts.keySet(Port.OUTPUT);
      if (!outputs.isEmpty()) {
        body.empty().addRemarkBlock("The outputs are defined here");
        if (!getVerilogSignalSet("output", outputs, attrs, true, body)) return null;
      }
      final var inouts = myPorts.keySet(Port.INOUT);
      if (!inouts.isEmpty()) {
        body.empty().addRemarkBlock("The inouts are defined here");
        if (!getVerilogSignalSet("inout", inouts, attrs, true, body)) return null;
      }
      final var wires = myWires.wireKeySet();
      if (!wires.isEmpty()) {
        body.empty().addRemarkBlock("The wires are defined here");
        if (!getVerilogSignalSet("wire", wires, attrs, false, body)) return null;
      }
      final var regs = myWires.registerKeySet();
      if (!regs.isEmpty()) {
        body.empty().addRemarkBlock("The registers are defined here");
        if (!getVerilogSignalSet("reg", regs, attrs, false, body)) return null;
      }
      final var typedWires = myTypedWires.getTypedWires();
      if (!typedWires.isEmpty()) {
        body.empty().addRemarkBlock("The type defined signals are defined here");
        final var sortedWires = new TreeSet<>(typedWires.keySet());
        var maxNameLength = 0;
        for (final var wire : sortedWires)
          maxNameLength = Math.max(maxNameLength, typedWires.get(wire).length());
        for (final var wire : sortedWires) {
          final var typeName = typedWires.get(wire);
          body.add(LineBuffer.format("{{1}}{{2}} {{3}};", typeName, " ".repeat(maxNameLength - typeName.length()), wire));
        }
      }
      body.empty()
          .addRemarkBlock("The module functionality is described here")
          .add(getModuleFunctionality(theNetlist, attrs));
      contents.add(body.getWithIndent()).add("endmodule");
    }
    return contents.get();
  }

  public LineBuffer getComponentDeclarationSection(Netlist theNetlist, AttributeSet attrs) {
    /*
     * This method returns all the component definitions used as component
     * in the circuit. This method is only called in case of VHDL-code
     * generation.
     */
    return LineBuffer.getHdlBuffer();
  }

  @Override
  public LineBuffer getComponentInstantiation(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) contents.add(getVHDLBlackBox(theNetlist, attrs, componentName, false));
    return contents;
  }

  @Override
  public LineBuffer getComponentMap(Netlist nets, Long componentId, Object componentInfo, String name) {
    final var contents = LineBuffer.getHdlBuffer();
    final var parameterMap = new TreeMap<String, String>();
    final var portMap = getPortMap(nets, componentInfo);
    final var componentHdlName =
            (componentInfo instanceof netlistComponent comp)
              ? comp.getComponent().getFactory().getHDLName(((netlistComponent) componentInfo).getComponent().getAttributeSet())
              : name;
    final var compName = StringUtil.isNotEmpty(name) ? name : componentHdlName;
    final var thisInstanceIdentifier = getInstanceIdentifier(componentInfo, componentId);
    final var oneLine = new StringBuilder();
    if (componentInfo == null) parameterMap.putAll(myParametersList.getMaps(null));
    else if (componentInfo instanceof netlistComponent comp) {
      final var attrs = comp.getComponent().getAttributeSet();
      parameterMap.putAll(myParametersList.getMaps(attrs));
    }
    var tabLength = 0;
    var first = true;
    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("{{1}} : {{2}}", thisInstanceIdentifier, compName);
      if (!parameterMap.isEmpty()) {
        // first we gather information on the generic string lengths
        var maxNameLength = 0;
        for (final var generic : parameterMap.keySet())
          maxNameLength = Math.max(maxNameLength, generic.length());
        var currentGeneric = 0;
        final var genericNames = new TreeSet<>(parameterMap.keySet());
        final var nrOfGenerics = genericNames.size();
        // now we add them
        for (final var generic : genericNames) {
          final var preamble = currentGeneric == 0 ? "{{generic}} {{map}} (" : " ".repeat(13);
          contents.add("   {{1}} {{2}}{{3}} => {{4}}{{5}}", preamble, generic,
              " ".repeat(Math.max(0, maxNameLength - generic.length())),
              parameterMap.get(generic),
              currentGeneric == (nrOfGenerics - 1) ? " )" : ",");
          currentGeneric++;
        }
      }
      if (!portMap.isEmpty()) {
        // first we gather information on the port string lengths
        var maxNameLength = 0;
        for (final var port : portMap.keySet())
          maxNameLength = Math.max(maxNameLength, port.length());
        var currentPort = 0;
        final var portNames = new TreeSet<>(portMap.keySet());
        final var nrOfPorts = portNames.size();
        for (final var port : portNames) {
          final var preamble = currentPort == 0 ? "{{port}} {{map}} (" : " ".repeat(10);
          contents.add("   {{1}} {{2}}{{3}} => {{4}}{{5}}", preamble, port,
              " ".repeat(Math.max(0, maxNameLength - port.length())),
              portMap.get(port),
              currentPort == (nrOfPorts - 1) ? " );" : ",");
          currentPort++;
        }
      }
    } else {
      oneLine.append(compName);
      if (!parameterMap.isEmpty()) {
        oneLine.append(" #(");
        tabLength = oneLine.length();
        first = true;
        for (var parameter : parameterMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            oneLine.append(" ".repeat(tabLength));
          } else first = false;
          oneLine.append(".").append(parameter).append("(").append(parameterMap.get(parameter)).append(")");
        }
        oneLine.append(")");
        contents.add(oneLine.toString());
        oneLine.setLength(0);
      }
      oneLine.append("   ").append(thisInstanceIdentifier).append(" (");
      if (!portMap.isEmpty()) {
        tabLength = oneLine.length();
        first = true;
        for (var port : portMap.keySet()) {
          if (!first) {
            oneLine.append(",");
            contents.add(oneLine.toString());
            oneLine.setLength(0);
            oneLine.append(" ".repeat(tabLength));
          } else first = false;
          oneLine.append(".").append(port).append("(");
          final var MappedSignal = portMap.get(port);
          if (!MappedSignal.contains(",")) {
            oneLine.append(MappedSignal);
          } else {
            final var vectorList = MappedSignal.split(",");
            oneLine.append("{");
            var tabSize = oneLine.length();
            for (var vectorEntries = 0; vectorEntries < vectorList.length; vectorEntries++) {
              oneLine.append(vectorList[vectorEntries].replace("}", "").replace("{", ""));
              if (vectorEntries < vectorList.length - 1) {
                contents.add(oneLine + ",");
                oneLine.setLength(0);
                oneLine.append(" ".repeat(tabSize));
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
    return contents;
  }

  @Override
  public List<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()))
          .add(Hdl.getExtendedLibrary())
          .add(getVHDLBlackBox(theNetlist, attrs, componentName, true));
    }
    return contents.get();
  }

  private String getInstanceIdentifier(Object componentInfo, Long componentId) {
    if (componentInfo instanceof netlistComponent comp) {
      final var attrs = comp.getComponent().getAttributeSet();
      if (attrs.containsAttribute(StdAttr.LABEL)) {
        final var label = attrs.getValue(StdAttr.LABEL);
        if (StringUtil.isNotEmpty(label)) {
          return CorrectLabel.getCorrectLabel(label);
        }
      }
    }
    return LineBuffer.format("{{1}}_{{2}}", subDirectoryName.toUpperCase(), componentId.toString());
  }

  /* Here all public entries for HDL generation are defined */
  @Override
  public LineBuffer getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    throw new IllegalAccessError("BUG: Inline code not supported");
  }

  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    /*
     * In this method the functionality of the black-box is described. It is
     * used for both VHDL and VERILOG.
     */
    return LineBuffer.getHdlBuffer();
  }

  public Map<String, String> getPortMap(Netlist nets, Object mapInfo) {
    final var result = new TreeMap<String, String>();
    if ((mapInfo instanceof netlistComponent componentInfo) && !myPorts.isEmpty()) {
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
          if (StringUtil.isNullOrEmpty(clockNetName)) {
            // FIXME hard coded string
            Reporter.report.addSevereWarning(
                String.format("Component \"%s\" in circuit \"%s\" has a gated clock connection!", compName, nets.getCircuitName()));
            gatedClock = true;
          }
          if (hasClock && !gatedClock && Netlist.isFlipFlop(attrs)) {
            if (nets.requiresGlobalClockConnection()) {
              result.put(myPorts.getTickName(port), LineBuffer
                  .formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, ClockHdlGeneratorFactory.GLOBAL_CLOCK_INDEX));
            } else {
              final var clockIndex = activeLow ? ClockHdlGeneratorFactory.NEGATIVE_EDGE_TICK_INDEX : ClockHdlGeneratorFactory.POSITIVE_EDGE_TICK_INDEX;
              result.put(myPorts.getTickName(port), LineBuffer.formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, clockIndex));
            }
            result.put(HdlPorts.CLOCK, LineBuffer
                .formatHdl("{{1}}{{<}}{{2}}{{>}}", clockNetName, ClockHdlGeneratorFactory.GLOBAL_CLOCK_INDEX));
          } else if (!hasClock) {
            result.put(myPorts.getTickName(port), Hdl.zeroBit());
            result.put(HdlPorts.CLOCK, Hdl.zeroBit());
          } else {
            result.put(myPorts.getTickName(port), Hdl.oneBit());
            if (!gatedClock) {
              final var clockIndex = activeLow ? ClockHdlGeneratorFactory.INVERTED_DERIVED_CLOCK_INDEX : ClockHdlGeneratorFactory.DERIVED_CLOCK_INDEX;
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
          result.putAll(Hdl.getNetMap(port, myPorts.doPullDownOnFloat(port), componentInfo, myPorts.getComponentPortId(port), nets));
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

  protected List<String> getVHDLBlackBox(Netlist theNetlist, AttributeSet attrs,
      String componentName, Boolean isEntity) {
    final var contents = LineBuffer.getHdlBuffer().addVhdlKeywords();
    var maxNameLength = 0;
    if (getWiresPortsDuringHDLWriting) {
      myWires.removeWires();
      myTypedWires.clear();
      myPorts.removePorts();
      getGenerationTimeWiresPorts(theNetlist, attrs);
    }
    contents.add(isEntity ? "{{entity}} {{1}} {{is}}" : "{{component}} {{1}}", componentName);
    if (!myParametersList.isEmpty(attrs)) {
      // first we build a list with parameters to determine the max. string length
      final var myParameters = new HashMap<String, Boolean>();
      for (final var generic : myParametersList.keySet(attrs)) {
        final var parameterName = myParametersList.get(generic, attrs);
        maxNameLength = Math.max(maxNameLength, parameterName.length());
        myParameters.put(parameterName, myParametersList.isPresentedByInteger(generic, attrs));
      }
      maxNameLength += 1; // add one space after the longest one
      final var myGenerics = new TreeSet<>(myParameters.keySet());
      var currentGenericId = 0;
      for (final var thisGeneric : myGenerics) {
        if (currentGenericId == 0) {
          contents.add("   {{generic}} ( {{1}}{{2}}: {{3}}{{4}};", thisGeneric,
              " ".repeat(Math.max(0, maxNameLength - thisGeneric.length())),
              myParameters.get(thisGeneric) ? "{{integer}}" : "std_logic_vector",
              currentGenericId == (myGenerics.size() - 1) ? " )" : "");
        } else {
          contents.add("             {{1}}{{2}}: {{3}}{{4}};", thisGeneric,
              " ".repeat(Math.max(0, maxNameLength - thisGeneric.length())),
              myParameters.get(thisGeneric) ? "{{integer}}" : "std_logic_vector",
              currentGenericId == (myGenerics.size() - 1) ? " )" : "");
        }
        currentGenericId++;
      }
    }
    if (!myPorts.isEmpty()) {
      // now we gather information on the in/out and inout ports
      maxNameLength = 0;
      var nrOfEntries = myPorts.keySet().size();
      final var tickers = new TreeSet<String>();
      for (final var portName : myPorts.keySet()) {
        maxNameLength = Math.max(maxNameLength, portName.length());
        if (myPorts.isClock(portName)) {
          final var tickerName = myPorts.getTickName(portName);
          maxNameLength = Math.max(maxNameLength, tickerName.length());
          tickers.add(tickerName);
          nrOfEntries++;
        }
      }
      maxNameLength += 1; // add a space after the longest name
      var nrOfPortBits = 0;
      var firstEntry = true;
      var currentEntry = 0;
      // now we process in order
      var direction = (!myPorts.keySet(Port.INOUT).isEmpty()) ? Vhdl.getVhdlKeyword("IN   ") : Vhdl.getVhdlKeyword("IN ");
      final var myInputs = new TreeSet<>(myPorts.keySet(Port.INPUT));
      myInputs.addAll(tickers);
      for (final var input : myInputs) {
        nrOfPortBits = myPorts.contains(input) ? myPorts.get(input, attrs) : 1;
        final var type = getTypeIdentifier(nrOfPortBits, attrs);
        firstEntry = addPortEntry(contents, firstEntry, nrOfEntries, currentEntry, input, direction, type, maxNameLength);
        currentEntry++;
      }
      direction = Vhdl.getVhdlKeyword("INOUT");
      final var myInOuts = new TreeSet<>(myPorts.keySet(Port.INOUT));
      for (final var inout : myInOuts) {
        nrOfPortBits = myPorts.get(inout, attrs);
        final var type = getTypeIdentifier(nrOfPortBits, attrs);
        firstEntry = addPortEntry(contents, firstEntry, nrOfEntries, currentEntry, inout, direction, type, maxNameLength);
        currentEntry++;
      }
      direction = (!myPorts.keySet(Port.INOUT).isEmpty()) ? Vhdl.getVhdlKeyword("OUT  ") : Vhdl.getVhdlKeyword("OUT");
      final var myOutputs = new TreeSet<>(myPorts.keySet(Port.OUTPUT));
      for (final var output : myOutputs) {
        nrOfPortBits = myPorts.get(output, attrs);
        final var type = getTypeIdentifier(nrOfPortBits, attrs);
        firstEntry = addPortEntry(contents, firstEntry, nrOfEntries, currentEntry, output, direction, type, maxNameLength);
        currentEntry++;
      }
    }
    if (isEntity) {
      contents.add("{{end}} {{entity}} {{1}};", componentName);
    } else {
      contents.add("{{end}} {{component}};");
    }
    return contents.getWithIndent(isEntity ? 0 : 1);
  }

  private boolean addPortEntry(LineBuffer contents, boolean firstEntry, int nrOfEntries, int currentEntry,
                               String name, String direction, String type, int maxLength) {
    final var fmt = firstEntry
                    ? "   {{port}} ( {{1}}{{2}}: {{3}} {{4}}{{5}};"
                    : "          {{1}}{{2}}: {{3}} {{4}}{{5}};";
    contents.add(fmt, name, " ".repeat(maxLength - name.length()), direction, type, currentEntry == (nrOfEntries - 1) ? " )" : "");

    // FIXME: refactor code that uses this retval, because as it's a const, then the logic using it can probably be simplified.
    return false;
  }

  private String getTypeIdentifier(int nrOfBits, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer().addVhdlKeywords();
    if (nrOfBits < 0) {
      // we have generic based vector
      if (!myParametersList.containsKey(nrOfBits, attrs))
        throw new IllegalArgumentException("Generic parameter not specified in the parameters list");
      contents.add("std_logic_vector( ({{1}} - 1) {{downto}} 0 )", myParametersList.get(nrOfBits, attrs));
    } else if (nrOfBits == 0) {
      contents.add("std_logic_vector( 0 {{downto}} 0 )");
    } else if (nrOfBits > 1) {
      contents.add("std_logic_vector( {{1}} {{downto}} 0 )", nrOfBits - 1);
    } else {
      contents.add("std_logic");
    }
    return contents.get(0);
  }

  private boolean getVerilogSignalSet(String preamble, List<String> signals, AttributeSet attrs, boolean isPort, LineBuffer contents) {
    if (signals.isEmpty()) return true;
    final var signalSet = new HashMap<String, String>();
    for (final var input : signals) {
      // this we have to check for the tick
      final var nrOfBits = isPort ? myPorts.contains(input) ? myPorts.get(input, attrs) : 1 : myWires.get(input);
      if (nrOfBits < 0) {
        if (myParametersList.containsKey(nrOfBits, attrs)) {
          signalSet.put(input, String.format("%s [%s-1:0]", preamble, myParametersList.get(nrOfBits, attrs)));
        } else {
          // FIXME: hard coded String
          Reporter.report.addFatalError("Internal Error, Parameter not present in HDL generation, your HDL code will not work!");
          return false;
        }
      } else if (nrOfBits == 0) {
        signalSet.put(input, String.format("%s [0:0]", preamble));
      } else if (nrOfBits > 1) {
        signalSet.put(input, String.format("%s [%d:0]", preamble, nrOfBits - 1));
      } else {
        signalSet.put(input, preamble);
      }
    }
    final var sortedSignals = new TreeSet<>(signalSet.keySet());
    var maxNameLength = 0;
    for (final var signal : sortedSignals)
      maxNameLength = Math.max(maxNameLength, signalSet.get(signal).length());
    for (final var signal : sortedSignals) {
      final var type = signalSet.get(signal);
      contents.add(LineBuffer.format("{{1}}{{2}} {{3}};", type, " ".repeat(maxNameLength - type.length()), signal));
    }
    return true;
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return true;
  }

  @Override
  public boolean isOnlyInlined() {
    return false;
  }
}
