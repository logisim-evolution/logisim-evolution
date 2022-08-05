/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.bfh;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.gui.Reporter;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlParameters;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.util.LineBuffer;

public class BinToBcdHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STR = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;

  public BinToBcdHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_BITS_STR, NR_OF_BITS_ID, HdlParameters.MAP_INT_ATTRIBUTE, BinToBcd.ATTR_BinBits);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    final var nrOfSignalBits = switch (nrOfPorts) {
      case 2 -> 7;
      case 3 -> 11;
      default -> 16;
    };
    final var nrOfSignals = switch (nrOfPorts) {
      case 2 -> 4;
      case 3 -> 7;
      default -> 11;
    };
    for (var signal = 0; signal < nrOfSignals; signal++)
      myWires.addWire(String.format("s_level%d", signal), nrOfSignalBits);
    myPorts.add(Port.INPUT, "binValue", NR_OF_BITS_ID, 0);
    for (var i = 1; i <= nrOfPorts; i++)
      myPorts.add(Port.OUTPUT, String.format("bcd%d", (int) (Math.pow(10, i - 1))), 4, i);
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    return Hdl.isVhdl();
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist netlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer()
            .pair("nrOfBits", NR_OF_BITS_STR);
    final var nrOfBits = attrs.getValue(BinToBcd.ATTR_BinBits);
    final var nrOfPorts = (int) (Math.log10(1 << nrOfBits.getWidth()) + 1.0);
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords();
      switch (nrOfPorts) {
        case 2 -> contents
            .add("""
                s_level0(6 {{downto}} {{nrOfBits}}) <= ({{others}} => '0');
                s_level0({{nrOfBits}}-1 {{downto}} 0) <= binValue;
                s_level1(2 {{downto}} 0) <= s_level0(2 {{downto}} 0);
                s_level2(1 {{downto}} 0) <= s_level1(1 {{downto}} 0);
                s_level2(6)          <= s_level1(6);
                s_level3(6 {{downto}} 5) <= s_level2(6 {{downto}} 5);
                s_level3(0)          <= s_level2(0);

                bcd1  <= s_level3( 3 {{downto}} 0);
                bcd10 <= \"0\"&s_level3(6 {{downto}} 4);
                """)
            .add(getAdd3Block("s_level0", 6, "s_level1", 6, "C1"))
            .add(getAdd3Block("s_level1", 5, "s_level2", 5, "C2"))
            .add(getAdd3Block("s_level2", 4, "s_level3", 4, "C3"));
        case 3 -> contents
            .add("""
                s_level0(10 {{downto}} {{nrOfBits}}) <= ({{others}} => '0');
                s_level0({{nrOfBits}}-1 {{downto}} 0) <= binValue;
                s_level1(10)          <= s_level0(10);
                s_level1( 5 {{downto}} 0) <= s_level0( 5 {{downto}} 0);
                s_level2(10 {{downto}} 9) <= s_level1(10 {{downto}} 9);
                s_level2( 4 {{downto}} 0) <= s_level1( 4 {{downto}} 0);
                s_level3(10 {{downto}} 8) <= s_level2(10 {{downto}} 8);
                s_level3( 3 {{downto}} 0) <= s_level2( 3 {{downto}} 0);
                s_level4( 2 {{downto}} 0) <= s_level3( 2 {{downto}} 0);
                s_level5(10)          <= s_level4(10);
                s_level5( 1 {{downto}} 0) <= s_level4( 1 {{downto}} 0);
                s_level6(10 {{downto}} 9) <= s_level5(10 {{downto}} 9);
                s_level6(0)           <= s_level5(0);

                bcd1   <= s_level6( 3 {{downto}} 0 );
                bcd10  <= s_level6( 7 {{downto}} 4 );
                bcd100 <= "0"&s_level6(10 {{downto}} 8);
                """)
            .add(getAdd3Block("s_level0", 9, "s_level1", 9, "C0"))
            .add(getAdd3Block("s_level1", 8, "s_level2", 8, "C1"))
            .add(getAdd3Block("s_level2", 7, "s_level3", 7, "C2"))
            .add(getAdd3Block("s_level3", 6, "s_level4", 6, "C3"))
            .add(getAdd3Block("s_level4", 5, "s_level5", 5, "C4"))
            .add(getAdd3Block("s_level5", 4, "s_level6", 4, "C5"))
            .add(getAdd3Block("s_level3", 10, "s_level4", 10, "C6"))
            .add(getAdd3Block("s_level4", 9, "s_level5", 9, "C7"))
            .add(getAdd3Block("s_level5", 8, "s_level6", 8, "C8"));
        case 4 -> contents
            .add("""
                s_level0(15 {{downto}} {{nrOfBits}}) <= ({{others}} => '0');
                s_level0({{nrOfBits}}-1 {{downto}} 0) <= binValue;
                s_level1(15 {{downto}} 14)  <= s_level0(15 {{downto}} 14);
                s_level1( 9 {{downto}}  0)  <= s_level0( 9 {{downto}}  0);
                s_level2(15 {{downto}} 13)  <= s_level1(15 {{downto}} 13);
                s_level2( 8 {{downto}}  0)  <= s_level1( 8 {{downto}}  0);
                s_level3(15 {{downto}} 12)  <= s_level2(15 {{downto}} 12);
                s_level3( 7 {{downto}}  0)  <= s_level2( 7 {{downto}}  0);
                s_level4(15)            <= s_level3(15);
                s_level4( 6 {{downto}}  0)  <= s_level3( 6 {{downto}}  0);
                s_level5(15 {{downto}} 14)  <= s_level4(15 {{downto}} 14);
                s_level5( 5 {{downto}}  0)  <= s_level4( 5 {{downto}}  0);
                s_level6(15 {{downto}} 13)  <= s_level5(15 {{downto}} 13);
                s_level6( 4 {{downto}}  0)  <= s_level5( 4 {{downto}}  0);
                s_level7( 3 {{downto}}  0)  <= s_level6( 3 {{downto}}  0);
                s_level8(15)            <= s_level7(15);
                s_level8( 2 {{downto}}  0)  <= s_level7( 2 {{downto}}  0);
                s_level9(15 {{downto}} 14)  <= s_level8(15 {{downto}} 14);
                s_level9( 1 {{downto}}  0)  <= s_level8( 1 {{downto}}  0);
                s_level10(15 {{downto}} 13) <= s_level9(15 {{downto}} 13);
                s_level10(0)            <= s_level9(0);

                bcd1    <= s_level10( 3 {{downto}}  0);
                bcd10   <= s_level10( 7 {{downto}}  4);
                bcd100  <= s_level10(11 {{downto}}  8);
                bcd1000 <= s_level10(15 {{downto}} 12);
                """)
            .add(getAdd3Block("s_level0", 13, "s_level1", 13, "C0"))
            .add(getAdd3Block("s_level1", 12, "s_level2", 12, "C1"))
            .add(getAdd3Block("s_level2", 11, "s_level3", 11, "C2"))
            .add(getAdd3Block("s_level3", 10, "s_level4", 10, "C3"))
            .add(getAdd3Block("s_level4", 9, "s_level5", 9, "C4"))
            .add(getAdd3Block("s_level5", 8, "s_level6", 8, "C5"))
            .add(getAdd3Block("s_level6", 7, "s_level7", 7, "C6"))
            .add(getAdd3Block("s_level7", 6, "s_level8", 6, "C7"))
            .add(getAdd3Block("s_level8", 5, "s_level9", 5, "C8"))
            .add(getAdd3Block("s_level9", 4, "s_level10", 4, "C9"))
            .add(getAdd3Block("s_level3", 14, "s_level4", 14, "C10"))
            .add(getAdd3Block("s_level4", 13, "s_level5", 13, "C11"))
            .add(getAdd3Block("s_level5", 12, "s_level6", 12, "C12"))
            .add(getAdd3Block("s_level6", 11, "s_level7", 11, "C13"))
            .add(getAdd3Block("s_level7", 10, "s_level8", 10, "C14"))
            .add(getAdd3Block("s_level8", 9, "s_level9", 9, "C15"))
            .add(getAdd3Block("s_level9", 8, "s_level10", 8, "C16"))
            .add(getAdd3Block("s_level6", 15, "s_level7", 15, "C17"))
            .add(getAdd3Block("s_level7", 14, "s_level8", 14, "C18"))
            .add(getAdd3Block("s_level8", 13, "s_level9", 13, "C19"))
            .add(getAdd3Block("s_level9", 12, "s_level10", 12, "C20"));
      }
    } else {
      // FIXME: hardcoded String
      Reporter.report.addFatalError("Strange, this should not happen as Verilog is not yet supported!\n");
    }
    return contents.empty();
  }

  private LineBuffer getAdd3Block(String srcName, int srcStartId, String destName, int destStartId, String processName) {
    return LineBuffer.getBuffer()
        .addVhdlKeywords()
        .pair("srcName", srcName)
        .pair("srcStartId", srcStartId)
        .pair("srcDownTo", (srcStartId - 3))
        .pair("destName", destName)
        .pair("destStartId", destStartId)
        .pair("destDownTo", (destStartId - 3))
        .pair("proc", processName)
        .empty()
        .add("""
            add3{{proc}} : {{process}} ({{srcName}}) {{is}}
            {{begin}}
               {{case}} ( {{srcName}}( {{srcStartId}} {{downto}} {{srcDownTo}}) ) {{is}}
                  {{when}} "0000" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0000";
                  {{when}} "0001" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0001";
                  {{when}} "0010" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0010";
                  {{when}} "0011" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0011";
                  {{when}} "0100" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0100";
                  {{when}} "0101" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "1000";
                  {{when}} "0110" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "1001";
                  {{when}} "0111" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "1010";
                  {{when}} "1000" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "1011";
                  {{when}} "1001" => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "1100";
                  {{when}} {{others}} => {{destName}}( {{destStartId}} {{downto}} {{destDownTo}} ) <= "0000";
               {{end}} {{case}};
            {{end}} {{process}} add3{{proc}};
            """);
  }
}
