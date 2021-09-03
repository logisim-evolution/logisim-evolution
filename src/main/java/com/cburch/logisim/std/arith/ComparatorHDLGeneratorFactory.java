/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.SortedMap;
import java.util.TreeMap;

public class ComparatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NrOfBitsStr = "NrOfBits";
  private static final int NrOfBitsId = -1;
  private static final String TwosComplementStr = "TwosComplement";
  private static final int TwosComplementId = -2;

  @Override
  public String getComponentStringIdentifier() {
    return "Comparator";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NrOfBitsId;
    map.put("DataA", inputbits);
    map.put("DataB", inputbits);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = new LineBuffer();
    Contents.pair("twosComplement", TwosComplementStr);

    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      if (nrOfBits == 1) {
        Contents.addLines(
            "A_EQ_B <= DataA XNOR DataB;",
            "A_LT_B <= DataA AND NOT(DataB) WHEN {{twosComplement}} = 1 ELSE NOT(DataA) AND DataB;",
            "A_GT_B <= NOT(DataA) AND DataB WHEN {{twosComplement}} = 1 ELSE DataA AND NOT(DataB);");
      } else {
        Contents.addLines(
            "s_signed_less <= '1' WHEN signed(DataA) < signed(DataB) ELSE '0';",
            "s_unsigned_less <= '1' WHEN unsigned(DataA) < unsigned(DataB) ELSE '0';",
            "s_signed_greater <= '1' WHEN signed(DataA) > signed(DataB) ELSE '0';",
            "s_unsigned_greater <= '1' WHEN unsigned(DataA) > unsigned(DataB) ELSE '0';",
            "",
            "A_EQ_B <= '1' WHEN DataA = DataB ELSE '0';",
            "A_GT_B <= s_signed_greater WHEN {{twosComplement}} = 1 ELSE s_unsigned_greater;",
            "A_LT_B <= s_signed_less    WHEN {{TwosComplement}} = 1 ELSE s_unsigned_less;");
      }
    } else {
      if (nrOfBits == 1) {
        Contents.addLines(
            "assign A_EQ_B = (DataA == DataB);",
            "assign A_LT_B = (DataA < DataB);",
            "assign A_GT_B = (DataA > DataB);");
      } else {
        Contents.addLines(
            "assign s_signed_less = ($signed(DataA) < $signed(DataB));",
            "assign s_unsigned_less = (DataA < DataB);",
            "assign s_signed_greater = ($signed(DataA) > $signed(DataB));",
            "assign s_unsigned_greater = (DataA > DataB);",
            "",
            "assign A_EQ_B = (DataA == DataB);",
            "assign A_GT_B = ({{twosComplement}}==1) ? s_signed_greater : s_unsigned_greater;",
            "assign A_LT_B = ({{twosComplement}}==1) ? s_signed_less : s_unsigned_less;");
      }
    }
    return Contents.getWithIndent();
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    map.put("A_GT_B", 1);
    map.put("A_EQ_B", 1);
    map.put("A_LT_B", 1);
    return map;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var map = new TreeMap<Integer, String>();
    final var inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (inputbits > 1) {
      map.put(NrOfBitsId, NrOfBitsStr);
    }
    map.put(TwosComplementId, TwosComplementStr);
    return map;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var map = new TreeMap<String, Integer>();
    final var nrOfBits = ComponentInfo.getComponent().getEnd(0).getWidth().getWidth();
    var isSigned = 0;
    AttributeSet attrs = ComponentInfo.getComponent().getAttributeSet();
    if (attrs.containsAttribute(Comparator.MODE_ATTRIBUTE)) {
      if (attrs.getValue(Comparator.MODE_ATTRIBUTE).equals(Comparator.SIGNED_OPTION))
        isSigned = 1;
    }
    if (nrOfBits > 1) {
      map.put(NrOfBitsStr, nrOfBits);
    }
    map.put(TwosComplementStr, isSigned);
    return map;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    NetlistComponent ComponentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("DataA", true, ComponentInfo, 0, Nets));
    portMap.putAll(GetNetMap("DataB", true, ComponentInfo, 1, Nets));
    portMap.putAll(GetNetMap("A_GT_B", true, ComponentInfo, 2, Nets));
    portMap.putAll(GetNetMap("A_EQ_B", true, ComponentInfo, 3, Nets));
    portMap.putAll(GetNetMap("A_LT_B", true, ComponentInfo, 4, Nets));
    return portMap;
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    int inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (inputbits > 1) {
      wires.put("s_signed_less", 1);
      wires.put("s_unsigned_less", 1);
      wires.put("s_signed_greater", 1);
      wires.put("s_unsigned_greater", 1);
    }
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
