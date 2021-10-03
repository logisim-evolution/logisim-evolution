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
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;
import java.util.HashMap;

public class ShifterHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String SHIFT_MODE_STRING = "shifterMode";
  private static final int SHIFT_MODE_ID = -1;

  public ShifterHdlGeneratorFactory() {
    super();
    myParametersList.add(SHIFT_MODE_STRING, SHIFT_MODE_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Shifter.ATTR_SHIFT,
        new HashMap<AttributeOption, Integer>() {{
          put(Shifter.SHIFT_LOGICAL_LEFT, 0);
          put(Shifter.SHIFT_ROLL_LEFT, 1);
          put(Shifter.SHIFT_LOGICAL_RIGHT, 2);
          put(Shifter.SHIFT_ARITHMETIC_RIGHT, 3);
          put(Shifter.SHIFT_ROLL_RIGHT, 4);
        }}
    );
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    myPorts
        .add(Port.INPUT, "dataA", 0, Shifter.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "shiftAmount", 0, Shifter.IN1, Shifter.SHIFT_BITS_ATTR)
        .add(Port.OUTPUT, "result", 0, Shifter.OUT, StdAttr.WIDTH);
    for (var stage = 0; stage < attrs.getValue(Shifter.SHIFT_BITS_ATTR); stage++)
      myWires
          .addWire(String.format("s_stage%dResult", stage), attrs.getValue(StdAttr.WIDTH).getWidth())
          .addWire(String.format("s_stage%dShiftIn", stage), 1 << stage);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("shiftMode", SHIFT_MODE_STRING);
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var nrOfShiftBits = attrs.getValue(Shifter.SHIFT_BITS_ATTR);
    contents.addRemarkBlock("""
          ShifterMode represents when:
          0 : Logical Shift Left
          1 : Rotate Left
          2 : Logical Shift Right
          3 : Arithmetic Shift Right
          4 : Rotate Right
          """);
    if (Hdl.isVhdl()) {
      if (nrOfBits == 1) {
        contents.addVhdlKeywords().add("""
            result <= dataA {{when}} {{shiftMode}} = 1 {{or}}
                                 {{shiftMode}} = 3 {{or}}
                                 {{shiftMode}} = 4 {{else}} dataA {{and}} {{not}}(shiftAmount);
            """);
      } else {
        for (var stage = 0; stage < nrOfShiftBits; stage++)
          contents.add(getStageFunctionalityVhdl(stage, nrOfBits));
        contents
            .empty()
            .addRemarkBlock("The result is assigned here")
            .add("result <= s_stage{{1}}Result;", (nrOfShiftBits - 1));
      }
    } else {
      if (nrOfBits == 1) {
        contents.add("""
            assign result = ( ({{shiftMode}} == 1) ||
                              ({{shiftMode}} == 3) ||
                              ({{shiftMode}} == 4) ) ? dataA : dataA&(~shiftAmount);
            """);
      } else {
        for (var stage = 0; stage < nrOfShiftBits; stage++) {
          contents.add(getStageFunctionalityVerilog(stage, nrOfBits));
        }
        contents
            .empty()
            .addRemarkBlock("The result is assigned here")
            .add("assign result = s_stage{{1}}Result", nrOfShiftBits - 1);
      }
    }
    return contents.empty();
  }

  private LineBuffer getStageFunctionalityVerilog(int stageNumber, int nrOfBits) {
    final var contents = LineBuffer.getBuffer()
            .pair("shiftMode", SHIFT_MODE_STRING)
            .pair("stageNumber", stageNumber)
            .pair("nrOfBits1", nrOfBits - 1)
            .pair("nrOfBits2", nrOfBits - 2);
    final var nrOfBitsToShift = (1 << stageNumber);
    contents.empty().addRemarkBlock(String.format("Stage %d of the binary shift tree is defined here", stageNumber));
    if (stageNumber == 0) {
      contents.add("""
          assign s_stage0ShiftIn = (({{shiftMode}} == 1) || ({{shiftMode}} == 3))
               ? dataA[{{shiftMode}}] : ({{nrOfBits1}} == 4) ? dataA[0] : 0;

          assign s_stage0Result  = (shiftAmount == 0)
               ? dataA
               : (({{shiftMode}} == 0) || ({{shiftMode}} == 1))
                  ? {dataA[{{nrOfBits2}}:0],s_stage0ShiftIn}
                  : {s_stage0ShiftIn,dataA[{{nrOfBits1}}:1]};

          """);
    } else {
      contents
          .pair("stageNumber1", stageNumber - 1)
          .pair("nrOfBitsToShift", nrOfBitsToShift)
          .pair("nrOfBitsToShift1", nrOfBitsToShift - 1)
          .pair("bitsShiftDiff", (nrOfBits - nrOfBitsToShift))
          .pair("bitsShiftDiff1", (nrOfBits - nrOfBitsToShift - 1))
          .add("""
          assign s_stage{{stageNumber}}ShiftIn = ({{shiftMode}} == 1) ?
                                     s_stage{{stageNumber1}}Result[{{nrOfBits1}}:{{bitsShiftDiff}}] :
                                     ({{shiftMode}} == 3) ?
                                     { {{nrOfBitsToShift}}{s_stage{{stageNumber1}}Result[{{nrOfBits1}}]} } :
                                     ({{shiftMode}} == 4) ?
                                     s_stage{{stageNumber1}}Result[{{nrOfBitsToShift1}}:0] : 0;

          assign s_stage{{stageNumber1}}Result  = (shiftAmount[{{stageNumber}}]==0) ?
                                     s_stage{{stageNumber1}}Result :
                                     (({{shiftMode}} == 0)||({{shiftMode}} == 1)) ?
                                     {s_stage{{stageNumber1}}Result[{{bitsShiftDiff1}}:0],s_stage{{stageNumber}}ShiftIn} :
                                     {s_stage{{stageNumber}}ShiftIn,s_stage{{stageNumber1}}Result[{{nrOfBits1}}:{{nrOfBitsToShift}}]};

            """);
    }
    return contents;
  }

  private LineBuffer getStageFunctionalityVhdl(int stageNumber, int nrOfBits) {
    final var nrOfBitsToShift = (1 << stageNumber);
    final var contents =
        LineBuffer.getBuffer()
          .addVhdlKeywords()
          .pair("shiftMode", SHIFT_MODE_STRING)
          .pair("stageNumber", stageNumber)
          .pair("stageNumber1", stageNumber - 1)
          .pair("nrOfBits1", nrOfBits - 1)
          .pair("nrOfBits2", nrOfBits - 2)
          .pair("bitsShiftDiff", (nrOfBits - nrOfBitsToShift))
          .pair("bitsShiftDiff1", (nrOfBits - nrOfBitsToShift - 1))
          .pair("nrOfBitsToShift", nrOfBitsToShift)
          .pair("nrOfBitsToShift1", nrOfBitsToShift - 1)
          .empty()
          .addRemarkBlock(String.format("Stage %d of the binary shift tree is defined here", stageNumber));
    if (stageNumber == 0) {
      contents
          .add("""
            s_stage0ShiftIn <= dataA({{nrOfBits1}}) {{when}} {{shiftMode}} = 1 {{or}} {{shiftMode}} = 3 {{else}}
                               dataA(0) {{when}} {{shiftMode}} = 4 {{else}} '0';

            s_stage0Result  <= dataA
            """)
          .add(
              (nrOfBits == 2)
                  ? "                      {{when}} shiftAmount = '0' {{else}}"
                  : "                      {{when}} shiftAmount(0) = '0' {{else}}")
          .add("""
                               dataA({{nrOfBits2}} {{downto}} 0)&s_stage0ShiftIn
                                  {{when}} {{shiftMode}} = 0 {{or}} {{shiftMode}} = 1 {{else}}
                               s_stage0ShiftIn&dataA( {{nrOfBits2}} {{downto}} 1 );
            """);
    } else {
      contents
          .add("""
            s_stage{{stageNumber}}ShiftIn <= s_stage{{stageNumber1}}Result( {{nrOfBits1}} {{downto}} {{bitsShiftDiff}} ) {{when}} {{shiftMode}} = 1 {{else}}
                               ({{others}} => s_stage{{stageNumber1}}Result({{stageNumber1}})) {{when}} {{shiftMode}} = 3 {{else}}
                               s_stage{{stageNumber1}}Result( {{nrOfBitsToShift1}} {{downto}} 0 ) {{when}} {{shiftMode}} = 4 {{else}}
                               ({{others}} => '0');

            s_stage{{stageNumber}}Result  <= s_stage{{stageNumber1}}Result
                                  {{when}} shiftAmount({{stageNumber}}) = '0' {{else}}
                               s_stage{{stageNumber1}}Result( {{bitsShiftDiff1}} {{downto}} 0 )&s_stage{{stageNumber}}ShiftIn
                                  {{when}} {{shiftMode}} = 0 {{or}} {{shiftMode}} = 1 {{else}}
                               s_stage{{stageNumber}}ShiftIn&s_stage{{stageNumber1}}Result( {{nrOfBits1}} {{downto}} {{nrOfBitsToShift}} );
            """);
    }
    return contents;
  }
}
