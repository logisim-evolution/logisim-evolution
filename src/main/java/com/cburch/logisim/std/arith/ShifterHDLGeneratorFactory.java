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

public class ShifterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String shiftModeStr = "ShifterMode";
  private static final int ShiftModeId = -1;

  @Override
  public String getComponentStringIdentifier() {
    return "Shifter";
  }

  @Override
  public SortedMap<String, Integer> GetInputList(Netlist TheNetlist, AttributeSet attrs) {
    final var inputs = new TreeMap<String, Integer>();
    inputs.put("DataA", attrs.getValue(StdAttr.WIDTH).getWidth());
    inputs.put("ShiftAmount", getNrofShiftBits(attrs));
    return inputs;
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = (new LineBuffer())
            .pair("shiftMode", shiftModeStr);
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (HDL.isVHDL()) {
      contents.add("""
            -----------------------------------------------------------------------------
            --- ShifterMode represents when:                                          ---
            --- 0 : Logical Shift Left                                                ---
            --- 1 : Rotate Left                                                       ---
            -----------------------------------------------------------------------------
            --- 2 : Logical Shift Right                                               ---
            --- 3 : Arithmetic Shift Right                                            ---
            --- 4 : Rotate Right                                                      ---
            -----------------------------------------------------------------------------
            """)
          .empty(2);

      if (nrOfBits == 1) {
        contents.add("""
            Result <= DataA WHEN {{shiftMode}} = 1 OR
                                 {{shiftMode}} = 3 OR
                                 {{shiftMode}} = 4 ELSE DataA AND NOT(ShiftAmount);
            """);
      } else {
        for (var stage = 0; stage < getNrofShiftBits(attrs); stage++)
          contents.add(GetStageFunctionalityVHDL(stage, nrOfBits));
        contents
            .add("""
                -----------------------------------------------------------------------------
                --- Here we assign the result                                             ---
                -----------------------------------------------------------------------------
                """)
            .add("Result <= s_stage_{{1}}_result;", (getNrofShiftBits(attrs) - 1))
            .add("");
      }
    } else {
      contents.add("""
            /***************************************************************************
             ** ShifterMode represents when:                                          **
             ** 0 : Logical Shift Left                                                **
             ** 1 : Rotate Left                                                       **
             ** 2 : Logical Shift Right                                               **
             ** 3 : Arithmetic Shift Right                                            **
             ** 4 : Rotate Right                                                      **
             ***************************************************************************/
             
             
            """);

      if (nrOfBits == 1) {
        contents.add("""
            assign Result = ( ({{shiftMode}} == 1) ||
                              ({{shiftMode}} == 3) ||
                              ({{shiftMode}} == 4) ) ? DataA : DataA&(~ShiftAmount);
            """);
      } else {
        for (var stage = 0; stage < getNrofShiftBits(attrs); stage++) {
          contents.add(GetStageFunctionalityVerilog(stage, nrOfBits));
        }
        contents.add("""
            /***************************************************************************
             ** Here we assign the result                                             **
             ***************************************************************************/
             
            assign Result = s_stage_{{1}}_result;
            
            """, getNrofShiftBits(attrs) - 1);
      }
    }
    return contents.getWithIndent();
  }

  private int getNrofShiftBits(AttributeSet attrs) {
    final var inputBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    var shift = 1;
    while ((1 << shift) < inputBits) shift++;
    return shift;
  }

  @Override
  public SortedMap<String, Integer> GetOutputList(Netlist TheNetlist, AttributeSet attrs) {
    final var outputs = new TreeMap<String, Integer>();
    final var inputbits = attrs.getValue(StdAttr.WIDTH).getWidth();
    outputs.put("Result", inputbits);
    return outputs;
  }

  @Override
  public SortedMap<Integer, String> GetParameterList(AttributeSet attrs) {
    final var parameters = new TreeMap<Integer, String>();
    parameters.put(ShiftModeId, shiftModeStr);
    return parameters;
  }

  @Override
  public SortedMap<String, Integer> GetParameterMap(Netlist Nets, NetlistComponent ComponentInfo) {
    final var parameterMap = new TreeMap<String, Integer>();
    Object shift = ComponentInfo.getComponent().getAttributeSet().getValue(Shifter.ATTR_SHIFT);
    if (shift == Shifter.SHIFT_LOGICAL_LEFT) parameterMap.put(shiftModeStr, 0);
    else if (shift == Shifter.SHIFT_ROLL_LEFT) parameterMap.put(shiftModeStr, 1);
    else if (shift == Shifter.SHIFT_LOGICAL_RIGHT) parameterMap.put(shiftModeStr, 2);
    else if (shift == Shifter.SHIFT_ARITHMETIC_RIGHT) parameterMap.put(shiftModeStr, 3);
    else parameterMap.put(shiftModeStr, 4);
    return parameterMap;
  }

  @Override
  public SortedMap<String, String> GetPortMap(Netlist Nets, Object MapInfo) {
    final var portMap = new TreeMap<String, String>();
    if (!(MapInfo instanceof NetlistComponent)) return portMap;
    final var componentInfo = (NetlistComponent) MapInfo;
    portMap.putAll(GetNetMap("DataA", true, componentInfo, Shifter.IN0, Nets));
    portMap.putAll(GetNetMap("ShiftAmount", true, componentInfo, Shifter.IN1, Nets));
    portMap.putAll(GetNetMap("Result", true, componentInfo, Shifter.OUT, Nets));
    return portMap;
  }

  private ArrayList<String> GetStageFunctionalityVerilog(int stageNumber, int nrOfBits) {
    final var contents = (new LineBuffer())
            .pair("shiftMode", shiftModeStr)
            .pair("stageNumber", stageNumber)
            .pair("nrOfBits1", nrOfBits - 1)
            .pair("nrOfBits2", nrOfBits - 2);
    final var nrOfBitsToShift = (1 << stageNumber);
    contents.add("""
          "/***************************************************************************
          ** Here stage {{stageNumber}} of the binary shift tree is defined
          ***************************************************************************/
          
          """);
    if (stageNumber == 0) {
      contents.add("""
          assign s_stage_0_shiftin = (({{shiftMode}} == 1) || ({{shiftMode}} == 3))
               ? DataA[{{shiftMode}}] : ({{nrOfBits1}} == 4) ? DataA[0] : 0;
          
          assign s_stage_0_result  = (ShiftAmount == 0)
               ? DataA
               : (({{shiftMode}} == 0) || ({{shiftMode}} == 1))
                  ? {DataA[{{nrOfBits2}}:0],s_stage_0_shiftin}
                  : {s_stage_0_shiftin,DataA[{{nrOfBits1}}:1]};
          
          """);
    } else {
      final var pairs =
          (new LineBuffer.Pairs())
              .pair("stageNumber1", stageNumber - 1)
              .pair("nrOfBitsToShift", nrOfBitsToShift)
              .pair("bitsShiftDiff", (nrOfBits - nrOfBitsToShift))
              .pair("bitsShiftDiff1", (nrOfBits - nrOfBitsToShift - 1));

      contents.add("""
          assign s_stage_{{stageNumber}}_shiftin = ({{shiftMode}} == 1) ?
                                     s_stage_{{stageNumber1}}_result[{{nrOfBits1}}:{{bitsShiftDiff}}] :
                                     ({{shiftMode}} == 3) ?
                                     { {{nrOfBitsToShift}}{s_stage_{{stageNumber1}}_result[{{nrOfBits1}}]} } :
                                     ({{shiftMode}} == 4) ?
                                     s_stage_{{stageNumber1}}_result[{{nrOfBitsToShift1}}:0] : 0;

          assign s_stage_{{stageNumber1}}_result  = (ShiftAmount[{{stageNumber}}]==0) ?
                                     s_stage_{{stageNumber1}}_result :
                                     (({{shiftMode}} == 0)||({{shiftMode}} == 1)) ?
                                     {s_stage_{{stageNumber1}}_result[{{bitsShiftDiff1}}:0],s_stage_{{stageNumber}}_shiftin} :
                                     {s_stage_{{stageNumber}}_shiftin,s_stage_{{stageNumber1}}_result[{{nrOfBits1}}:{{nrOfBitsToShift}}]};
          
          """, pairs);
    }
    return contents.getWithIndent();
  }

  private ArrayList<String> GetStageFunctionalityVHDL(int stageNumber, int nrOfBits) {
    final var nrOfBitsToShift = (1 << stageNumber);
    final var contents =
        (new LineBuffer())
          .pair("shiftMode", shiftModeStr)
          .pair("stageNumber", stageNumber)
          .pair("stageNumber1", stageNumber - 1)
          .pair("nrOfBits1", nrOfBits - 1)
          .pair("nrOfBits2", nrOfBits - 2)
          .pair("bitsShiftDiff", (nrOfBits - nrOfBitsToShift))
          .pair("bitsShiftDiff1", (nrOfBits - nrOfBitsToShift - 1))
          .pair("nrOfBitsToShift", nrOfBitsToShift)
          .pair("nrOfBitsToShift1", nrOfBitsToShift - 1);

    contents.add("""
        -----------------------------------------------------------------------------
        --- Here stage {{stageNumber}} of the binary shift tree is defined
        -----------------------------------------------------------------------------
          
        """);

    if (stageNumber == 0) {
      contents
          .add("""
            s_stage_0_shiftin <= DataA({{nrOfBits1}}) WHEN {{shiftMode}} = 1 OR {{shiftMode}} = 3 ELSE
                                 DataA(0) WHEN {{shiftMode}} = 4 ELSE '0';")
 
            s_stage_0_result  <= DataA
            """)
          .add(
              (nrOfBits == 2)
                  ? "                        WHEN ShiftAmount = '0' ELSE"
                  : "                        WHEN ShiftAmount(0) = '0' ELSE")
          .add("""
                               DataA({{nrOfBits2}} DOWNTO 0)&s_stage_0_shiftin
                                  WHEN {{shiftMode}} = 0 OR {{shiftMode}} = 1 ELSE
                               s_stage_0_shiftin&DataA( {{nrOfBits2}} DOWNTO 1 );
            """);
    } else {
      contents
          .add("""
            s_stage_{{stageNumber}}_shiftin <= s_stage_{{stageNumber1}}_result( {{nrOfBits1}} DOWNTO {{bitsShiftDiff}} ) WHEN {{shiftMode}} = 1 ELSE
                                 (OTHERS => s_stage_{{stageNumber1}}_result({{stageNumber1}})) WHEN {{shiftMode}} = 3 ELSE
                                 s_stage_{{stageNumber1}}_result( {{nrOfBitsToShift1}} DOWNTO 0 ) WHEN {{shiftMode}} = 4 ELSE
                                 (OTHERS => '0');
            
            s_stage_{{stageNumber}}_result  <= s_stage_{{stageNumber1}}_result
                                    WHEN ShiftAmount({{stageNumber}}) = '0' ELSE
                                 s_stage_{{stageNumber1}}_result( {{bitsShiftDiff1}} DOWNTO 0 )&s_stage_{{stageNumber}}_shiftin
                                    WHEN {{shiftMode}} = 0 OR {{shiftMode}} = 1 ELSE
                                 s_stage_{{stageNumber}}_shiftin&s_stage_{{stageNumber1}}_result( {{nrOfBits1}} DOWNTO {{nrOfBitsToShift}} );
            """);
    }
    contents.empty();
    return contents.getWithIndent();
  }

  @Override
  public String GetSubDir() {
    return "arithmetic";
  }

  @Override
  public SortedMap<String, Integer> GetWireList(AttributeSet attrs, Netlist Nets) {
    final var wires = new TreeMap<String, Integer>();
    int shift = getNrofShiftBits(attrs);
    int loop;
    for (loop = 0; loop < shift; loop++) {
      wires.put("s_stage_" + loop + "_result", attrs.getValue(StdAttr.WIDTH).getWidth());
      wires.put("s_stage_" + loop + "_shiftin", 1 << loop);
    }
    return wires;
  }

  @Override
  public boolean HDLTargetSupported(AttributeSet attrs) {
    return true;
  }
}
