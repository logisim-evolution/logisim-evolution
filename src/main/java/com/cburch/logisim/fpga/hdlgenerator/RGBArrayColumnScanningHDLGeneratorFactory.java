package com.cburch.logisim.fpga.hdlgenerator;

import com.cburch.logisim.util.ContentBuilder;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;

public class RGBArrayColumnScanningHDLGeneratorFactory extends LedArrayColumnScanningHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayColumnScanning";

  public static ArrayList<String> getPortMap(int id) {
    final var map = new ArrayList<String>();
    if (HDL.isVHDL()) {
      map.add("      PORT MAP ( "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + id
          + ",");
      map.add("                 "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + " => "
          + TickComponentHDLGeneratorFactory.FPGAClock
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + " => "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + id
          + ",");
      map.add("                 "
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + " => s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + id
          + ");");
    } else {
      map.add("      (."
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress
          + id
          + "),");
      map.add("       ."
          + TickComponentHDLGeneratorFactory.FPGAClock
          + "("
          + TickComponentHDLGeneratorFactory.FPGAClock
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + "("
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + id
          + "),");
      map.add("       ."
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "(s_"
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + id
          + "));");
    }
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress, nrOfColumnAddressBitsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs, nrOfRowsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs, nrOfRowsGeneric);
    outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs, nrOfRowsGeneric);
    return outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return inputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    wires.putAll(super.GetWireList(attrs, Nets));
    wires.put("s_maxRedLedInputs", maxNrLedsGeneric);
    wires.put("s_maxBlueLedInputs", maxNrLedsGeneric);
    wires.put("s_maxGreenLedInputs", maxNrLedsGeneric);
    return wires;
  }


  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var redIn = LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs;
    final var greenIn = LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs;
    final var blueIn = LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs;

    final var redOut = LedArrayGenericHDLGeneratorFactory.LedArrayRedOutputs;
    final var greenOut = LedArrayGenericHDLGeneratorFactory.LedArrayGreenOutputs;
    final var blueOut = LedArrayGenericHDLGeneratorFactory.LedArrayBlueOutputs;

    final var contents = new ContentBuilder();
    contents.add(getColumnCounterCode());
    if (HDL.isVHDL()) {
      contents
          .add(
              "   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS")
          .add("   BEGIN")
          .add("      s_maxRedLedInputs <= (OTHERS => '0');")
          .add("      s_maxGreenLedInputs <= (OTHERS => '0');")
          .add("      s_maxBlueLedInputs <= (OTHERS => '0');")
          .add("      IF (%s = 1) THEN", activeLowString)
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0)   <= NOT %s;", nrOfLedsString, redIn)
          .add("         s_maxGreenLedInputs(%s-1 DOWNTO 0) <= NOT %s;", nrOfLedsString, greenIn)
          .add("         s_maxBlueLedInputs(%s-1 DOWNTO 0)  <= NOT %s;", nrOfLedsString, blueIn)
          .add("      ELSE")
          .add("         s_maxRedLedInputs(%s-1 DOWNTO 0)   <= %s;", nrOfLedsString, redIn)
          .add("         s_maxGreenLedInputs(%s-1 DOWNTO 0) <= %s;", nrOfLedsString, greenIn)
          .add("         s_maxBlueLedInputs(%s-1 DOWNTO 0)  <= %s;", nrOfLedsString, blueIn)
          .add("      END IF;")
          .add("   END PROCESS makeVirtualInputs;")
          .add("")
          .add("   GenOutputs : FOR n IN %s-1 DOWNTO 0 GENERATE", nrOfRowsString)
          .add("      %s(n) <= s_maxRedLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);", redIn)
          .add("      %s(n) <= s_maxGreenLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);", greenIn)
          .add("      %s(n) <= s_maxBlueLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);", blueIn)
          .add("   END GENERATE GenOutputs;");
    } else {
      contents
          .add("")
          .add("   genvar i;")
          .add("   generate")
          .add("      for (i = 0; i < %s; i = i + 1) begin", nrOfRowsString)
          .add("         assign %s[i] = (activeLow == 1)", redOut)
          .add("            ? ~%s[i*nrOfColumns+s_columnCounterReg]", redIn)
          .add("            : %s[i*nrOfColumns+s_columnCounterReg];", redIn)
          .add("         assign %s[i] = (activeLow == 1)", greenOut)
          .add("            ? ~%s[i*nrOfColumns+s_columnCounterReg]", greenIn)
          .add("            : %s[i*nrOfColumns+s_columnCounterReg];", greenIn)
          .add("         assign %s[i] = (activeLow == 1)", blueOut)
          .add("            ? ~%s[i*nrOfColumns+s_columnCounterReg]", blueIn)
          .add("            : %s[i*nrOfColumns+s_columnCounterReg];", blueIn)
          .add("      end")
          .add("   endgenerate");
    }
    return contents.get();
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }

}
