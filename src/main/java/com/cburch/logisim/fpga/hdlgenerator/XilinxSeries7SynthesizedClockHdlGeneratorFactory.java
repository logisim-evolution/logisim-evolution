/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.hdlgenerator;

import java.util.List;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.file.FileWriter;
import com.cburch.logisim.util.LineBuffer;

/**
 * Use Xilinx MMCM clock tile to accelerate hardware clock.
 */
public class XilinxSeries7SynthesizedClockHdlGeneratorFactory extends SynthesizedClockHdlGeneratorFactory {

  private final long fpgaClockFrequency;
  private double preMultiplier;
  private double preDivider;
  private final double mmcmPeriodNs;

  /**
   * Instantiates an HDL clock generator for the Xilinx 7-series FPGA.
   * A pre-multiplier and a pre-divider can be specified to set the output
   * frequency. There are limitations for the settings of each of these parameters.
   * See referenced docs. Base frequency must be 10Mhz or greater, which is edited here.

   * @param fpga_clock_frequency  Clock frequency in hertz.
   * @param preMultiplier         A double to three decimals that will multiply the
   *                              clock frequency.
   * @param preDivider            A double to three decimals that will divide the
   *                              clock frequency.
   * @throws Exception
   * @see                         <a href="https://docs.xilinx.com/v/u/en-US/ug472_7Series_Clocking" target="_top">Xilinx 7-Series Clocking</a>
   */
  public XilinxSeries7SynthesizedClockHdlGeneratorFactory(long fpga_clock_frequency, double preMultiplier, double preDivider) throws Exception {
    super();
    fpgaClockFrequency = fpga_clock_frequency;
    this.preDivider = preDivider;
    this.preMultiplier = preMultiplier;
    // Compute the period of the system clock in nanoseconds to three decimals
    // (must be 10mhz or greater for mmcm)
    if (fpga_clock_frequency < 10000000) {
      throw new Exception("MMCM requires external FPGA clock of 10MHz or greater.");
    }
    // Get precision to the ps
    mmcmPeriodNs = Math.round(1000000000000.0 / (double) fpgaClockFrequency) / 1000.0;
    myWires
        .addWire(SYNTHESIZED_CLOCK, 1)
        .addWire("s_unbufferedSynthClk", 1)
        .addWire("s_clkfbout", 1)
        .addWire("s_bufferedFPGAClock", 1);
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist TheNetlist, AttributeSet attrs) {
    final var contents =
        LineBuffer.getHdlBuffer()
          .pair("synthesizedClock", SYNTHESIZED_CLOCK)
          .pair("preDivider", String.valueOf(preDivider))
          .pair("preMultiplier", String.valueOf(preMultiplier))
          .pair("clkInPeriodNs", String.valueOf(mmcmPeriodNs))
          .add("")
          .addRemarkBlock("Here the output is defined.")
          .add("{{assign}} SynthesizedClock {{=}} {{synthesizedClock}};")
          .add("")
          .addRemarkBlock("Here the update logic is defined.");

    if (Hdl.isVhdl()) {
      contents.addVhdlKeywords().add("""
        clkbuf: BUFG port map (I=>s_unbufferedSynthClk, O=>{{synthesizedClock}});
        iclkbuf: BUFG port map (I=>FPGAClock, O=>s_bufferedFPGAClock);

        clock: MMCM_BASE generic map (
          clkin1_period  => {{clkInPeriodNs}},
          clkfbout_mult_f => {{preMultiplier}},
          clkout0_divide_f => {{preDivider}}
        )
        port map(
            rst      => '0',
            pwrdwn   => '0',
            clkin1   => s_bufferedFPGAClock,
            clkfbin  => s_clkfbout,
            clkfbout => s_clkfbout,
            clkout0  => s_unbufferedSynthClk
        );
      """).empty();
    } else {
      contents.add("""
        BUFG clkbuf (.I(s_unbufferedSynthClk), .O({{synthesizedClock}}));
        BUFG iclkbuf (.I(FPGAClock), .O(s_bufferedFPGAClock));

        MMCME2_BASE #(
          .CLKIN1_PERIOD({{clkInPeriodNs}}),
          .CLKFBOUT_MULT_F({{preMultiplier}}),
          .CLKOUT0_DIVIDE_F({{preDivider}})
        ) clock (
          .RST(1'b0),
          .PWRDWN(1'b0),
          .CLKIN1(s_bufferedFPGAClock),
          .CLKFBIN(s_clkfbout),
          .CLKFBOUT(s_clkfbout),
          .CLKOUT0(s_unbufferedSynthClk),
          .CLKOUT0B(),
          .CLKOUT1(),
          .CLKOUT1B(),
          .CLKOUT2(),
          .CLKOUT2B(),
          .CLKOUT3(),
          .CLKOUT3B(),
          .CLKOUT4(),
          .CLKOUT5(),
          .CLKOUT6(),
          .CLKFBOUTB(),
          .LOCKED()
        );
      """).empty();
    }
    return contents;
  }

  public static List<String> getUnisimLibrary() {
    final var lines = LineBuffer.getBuffer();
    lines.addVhdlKeywords().add("""

          {{library}} unisim;
          {{use}} unisim.vcomponents.all;

        """);
    return lines.get();
  }

  // Override entity creator to add unisim library. Only works for Xilinx.
  @Override
  public List<String> getEntity(Netlist theNetlist, AttributeSet attrs, String componentName) {
    final var contents = LineBuffer.getHdlBuffer();
    if (Hdl.isVhdl()) {
      contents.add(FileWriter.getGenerateRemark(componentName, theNetlist.projName()))
          .add(Hdl.getExtendedLibrary())
          .add(getUnisimLibrary())
          .add(getVHDLBlackBox(theNetlist, attrs, componentName, true));
    }
    return contents.get();
  }
}
