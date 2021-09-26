/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedMap;
import java.util.TreeMap;

public class ComparatorHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "NrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String TWOS_COMPLEMENT_STRING = "TwosComplement";
  private static final int TWOS_COMPLEMENT_ID = -2;
  
  public static final Map<AttributeOption, Integer> SIGNED_MAP = new HashMap<>() {{ 
      put(Comparator.UNSIGNED_OPTION, 0); 
      put(Comparator.SIGNED_OPTION, 1); 
    }};
 
  
  public ComparatorHDLGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(TWOS_COMPLEMENT_STRING, TWOS_COMPLEMENT_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR, 
            SIGNED_MAP);
    getWiresduringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWires(Netlist theNetlist, AttributeSet attrs) {
    if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1)
      myWires
          .addWire("s_signed_less", 1)
          .addWire("s_unsigned_less", 1)
          .addWire("s_signed_greater", 1)
          .addWire("s_unsigned_greater", 1);
  }


  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var map = new TreeMap<String, Integer>();
    final var inputbits = (attrs.getValue(StdAttr.WIDTH).getWidth() == 1) ? 1 : NR_OF_BITS_ID;
    map.put("DataA", inputbits);
    map.put("DataB", inputbits);
    return map;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var Contents = LineBuffer.getBuffer();
    Contents.pair("twosComplement", TWOS_COMPLEMENT_STRING);

    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      if (nrOfBits == 1) {
        Contents.add("""
            A_EQ_B <= DataA XNOR DataB;
            A_LT_B <= DataA AND NOT(DataB) WHEN {{twosComplement}} = 1 ELSE NOT(DataA) AND DataB;
            A_GT_B <= NOT(DataA) AND DataB WHEN {{twosComplement}} = 1 ELSE DataA AND NOT(DataB);
            """);
      } else {
        Contents.add("""
            s_signed_less <= '1' WHEN signed(DataA) < signed(DataB) ELSE '0';
            s_unsigned_less <= '1' WHEN unsigned(DataA) < unsigned(DataB) ELSE '0';
            s_signed_greater <= '1' WHEN signed(DataA) > signed(DataB) ELSE '0';
            s_unsigned_greater <= '1' WHEN unsigned(DataA) > unsigned(DataB) ELSE '0';
            "
            A_EQ_B <= '1' WHEN DataA = DataB ELSE '0';
            A_GT_B <= s_signed_greater WHEN {{twosComplement}} = 1 ELSE s_unsigned_greater;
            A_LT_B <= s_signed_less    WHEN {{TwosComplement}} = 1 ELSE s_unsigned_less;
            """);
      }
    } else {
      if (nrOfBits == 1) {
        Contents.add("""
            assign A_EQ_B = (DataA == DataB);
            assign A_LT_B = (DataA < DataB);
            assign A_GT_B = (DataA > DataB); 
            """);
      } else {
        Contents.add("""
            assign s_signed_less = ($signed(DataA) < $signed(DataB));
            assign s_unsigned_less = (DataA < DataB);
            assign s_signed_greater = ($signed(DataA) > $signed(DataB));
            assign s_unsigned_greater = (DataA > DataB);
            
            assign A_EQ_B = (DataA == DataB);
            assign A_GT_B = ({{twosComplement}}==1) ? s_signed_greater : s_unsigned_greater;
            assign A_LT_B = ({{twosComplement}}==1) ? s_signed_less : s_unsigned_less;
            """);
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
}
