package com.cburch.logisim.fpga.hdlgenerator;

import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;

public class RGBArrayColumnScanningHDLGeneratorFactory extends LedArrayColumnScanningHDLGeneratorFactory {

  public static String RGBArrayName = "RGBArrayColumnScanning";

  public static ArrayList<String> getPortMap(int id) {
    ArrayList<String> map = new ArrayList<>();
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
    SortedMap<String, Integer> Outputs = new TreeMap<>();
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayColumnAddress, nrOfColumnAddressBitsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs, nrOfRowsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs, nrOfRowsGeneric);
    Outputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs, nrOfRowsGeneric);
    return Outputs;
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    SortedMap<String, Integer> Inputs = new TreeMap<>();
    Inputs.put(TickComponentHDLGeneratorFactory.FPGAClock, 1);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs, nrOfLedsGeneric);
    Inputs.put(LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs, nrOfLedsGeneric);
    return Inputs;
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    SortedMap<String, Integer> Wires = new TreeMap<>();
    Wires.putAll(super.GetWireList(attrs, Nets));
    Wires.put("s_maxRedLedInputs", maxNrLedsGeneric);
    Wires.put("s_maxBlueLedInputs", maxNrLedsGeneric);
    Wires.put("s_maxGreenLedInputs", maxNrLedsGeneric);
    return Wires;
  }
  
  
  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    ArrayList<String> Contents = new ArrayList<>();
    Contents.addAll(getColumnCounterCode());
    if (HDL.isVHDL()) {
      Contents.add("   makeVirtualInputs : PROCESS ( internalRedLeds, internalGreenLeds, internalBlueLeds ) IS");
      Contents.add("   BEGIN");
      Contents.add("      s_maxRedLedInputs <= (OTHERS => '0');");
      Contents.add("      s_maxGreenLedInputs <= (OTHERS => '0');");
      Contents.add("      s_maxBlueLedInputs <= (OTHERS => '0');");
      Contents.add("      IF (" + activeLowString + " = 1) THEN");
      Contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      Contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      Contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= NOT " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      Contents.add("                                       ELSE");
      Contents.add("         s_maxRedLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)   <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + ";");
      Contents.add("         s_maxGreenLedInputs( " + nrOfLedsString + "-1 DOWNTO 0) <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + ";");
      Contents.add("         s_maxBlueLedInputs( " + nrOfLedsString + "-1 DOWNTO 0)  <= " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + ";");
      Contents.add("      END IF;");
      Contents.add("   END PROCESS makeVirtualInputs;");
      Contents.add("");
      Contents.add("   GenOutputs : FOR n IN " + nrOfRowsString + "-1 DOWNTO 0 GENERATE");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs 
          + "(n)   <= s_maxRedLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + "(n) <= s_maxGreenLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      Contents.add("      " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + "(n)  <= s_maxBlueLedInputs(to_integer(unsigned(s_columnCounterReg)) + n*nrOfColumns);");
      Contents.add("   END GENERATE GenOutputs;");
    } else {
      Contents.add("");
      Contents.add("   genvar i;");
      Contents.add("   generate");
      Contents.add("      for (i = 0; i < " + nrOfRowsString + "; i = i + 1) begin");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowRedOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRedInputs
          + "[i*nrOfColumns+s_columnCounterReg];");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowGreenOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayGreenInputs
          + "[i*nrOfColumns+s_columnCounterReg];");
      Contents.add("         assign " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayRowBlueOutputs
          + "[i]  = (activeLow == 1) ? ~" 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[i*nrOfColumns+s_columnCounterReg] : ");
      Contents.add("                                                       " 
          + LedArrayGenericHDLGeneratorFactory.LedArrayBlueInputs
          + "[i*nrOfColumns+s_columnCounterReg];");

      Contents.add("      end");
      Contents.add("   endgenerate");
    }
    return Contents;
  }

  @Override
  public String getComponentStringIdentifier() {
    return RGBArrayName;
  }
  
}
