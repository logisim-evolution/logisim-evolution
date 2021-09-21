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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.HDLParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.ArrayList;
import java.util.HashMap;

public class ShifterHDLGeneratorFactory extends AbstractHDLGeneratorFactory {

  private static final String SHIFT_MODE_STRING = "ShifterMode";
  private static final int SHIFT_MODE_ID = -1;

  public ShifterHDLGeneratorFactory() {
    super();
    myParametersList.add(SHIFT_MODE_STRING, SHIFT_MODE_ID, HDLParameters.MAP_ATTRIBUTE_OPTION, Shifter.ATTR_SHIFT, 
        new HashMap<AttributeOption, Integer>() {{
          put(Shifter.SHIFT_LOGICAL_LEFT, 0);
          put(Shifter.SHIFT_ROLL_LEFT, 1);
          put(Shifter.SHIFT_LOGICAL_RIGHT, 2);
          put(Shifter.SHIFT_ARITHMETIC_RIGHT, 3);
          put(Shifter.SHIFT_ROLL_RIGHT, 4);
        }}
    );
    getWiresPortsduringHDLWriting = true;
    myPorts
        .add(Port.INPUT, "DataA", 0, Shifter.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "ShiftAmount", 0, Shifter.IN1, Shifter.SHIFT_BITS_ATTR)
        .add(Port.OUTPUT, "Result", 0, Shifter.OUT, StdAttr.WIDTH);
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    for (var stage = 0; stage < attrs.getValue(Shifter.SHIFT_BITS_ATTR); stage++)
      myWires
          .addWire(String.format("s_stage_%d_result", stage), attrs.getValue(StdAttr.WIDTH).getWidth())
          .addWire(String.format("s_stage_%d_shiftin", stage), 1 << stage);
  }

  @Override
  public ArrayList<String> GetModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = (new LineBuffer())
            .pair("shiftMode", SHIFT_MODE_STRING);
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfShiftBits = attrs.getValue(Shifter.SHIFT_BITS_ATTR);
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
        for (var stage = 0; stage < nrOfShiftBits; stage++)
          contents.add(GetStageFunctionalityVHDL(stage, nrOfBits));
        contents
            .add("""
                -----------------------------------------------------------------------------
                --- Here we assign the result                                             ---
                -----------------------------------------------------------------------------
                """)
            .add("Result <= s_stage_{{1}}_result;", (nrOfShiftBits - 1))
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
        for (var stage = 0; stage < nrOfShiftBits; stage++) {
          contents.add(GetStageFunctionalityVerilog(stage, nrOfBits));
        }
        contents.add("""
            /***************************************************************************
             ** Here we assign the result                                             **
             ***************************************************************************/
             
            assign Result = s_stage_{{1}}_result;
            
            """, nrOfShiftBits - 1);
      }
    }
    return contents.getWithIndent();
  }

  private ArrayList<String> GetStageFunctionalityVerilog(int stageNumber, int nrOfBits) {
    final var contents = (new LineBuffer())
            .pair("shiftMode", SHIFT_MODE_STRING)
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
          .pair("shiftMode", SHIFT_MODE_STRING)
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
                                 DataA(0) WHEN {{shiftMode}} = 4 ELSE '0';
 
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
}
