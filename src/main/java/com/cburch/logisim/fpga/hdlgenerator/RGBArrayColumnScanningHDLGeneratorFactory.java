package com.cburch.logisim.fpga.hdlgenerator;

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
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = new ArrayList<String>();
    contents.addAll(getColumnCounterCode());
    if (HDL.isVHDL()) {
      contents.add("   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS");
      contents.add("   BEGIN");
      contents.add("      s_maxRedLedInputs <= (OTHERS => '0');");
      contents.add("      s_maxGreenLedInputs <= (OTHERS => '0');");
      contents.add("      s_maxBlueLedInputs <= (OTHERS => '0');");
      contents.add("      IF (" + activeLowString + " = 1) THEN");
      contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      contents.add("                                       ELSE");
      contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      contents.add("      END IF;");
      contents.add("   END PROCESS makeVirtualInputs;");
      contents.add("");
      contents.add("   GenOutputs : FOR n IN " + nrOfRowsString + "-1 DOWNTO 0 GENERATE");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs 
          + "(n)   <= s_maxRedLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + "(n) <= s_maxGreenLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + "(n)  <= s_maxBlueLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      contents.add("   END GENERATE GenOutputs;");
    } else {
      contents.add("");
      contents.add("   genvar i;");
      contents.add("   generate");
      contents.add("      for (i = 0; i < " + nrOfRowsString + "; i = i + 1) begin");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[i*nrOfColumns+s_columnCounterReg];");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[i*nrOfColumns+s_columnCounterReg];");
      contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[i*nrOfColumns+s_columnCounterReg];");

      contents.add("      end");
      contents.add("   endgenerate");
    }
    return contents;
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }
  
}
