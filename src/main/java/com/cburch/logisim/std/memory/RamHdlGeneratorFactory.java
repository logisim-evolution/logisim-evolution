/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.hdlgenerator.AbstractHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.HdlPorts;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class RamHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String ByteArrayStr = "byteArray";
  private static final int ByteArrayId = -1;
  private static final String RestArrayStr = "restArray";
  private static final int RestArrayId = -2;
  private static final String MemArrayStr = "memoryArray";
  private static final int MemArrayId = -3;

  public RamHdlGeneratorFactory() {
    super();
    getWiresPortsDuringHDLWriting = true;
  }
  
  private void getGenerationTimeWiresPortsLineEnables(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var nrOfaddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var ramEntries = (1 << nrOfaddressLines);
    final var dataLines = Math.max(1, RamAppearance.getNrLEPorts(attrs));
    myWires
        .addRegister("s_writeAddressReg", nrOfaddressLines)
        .addRegister("s_readAddressReg", nrOfaddressLines)
        .addWire("s_ramWriteAddress", nrOfaddressLines)
        .addWire("s_ramReadAddress", nrOfaddressLines)
        .addRegister("s_ramDataOut", nrOfBits)
        .addWire("s_ramWe", 1)
        .addRegister("s_weReg", 1)
        .addRegister("s_tickDelayReg", dataLines + 1)
        .addRegister("s_addressOffsetReg", nrOfaddressLines + 1);
    if (dataLines == 1) {
      myWires.addWire("s_ramDataIn", nrOfBits);
    } else {
      myWires.addRegister("s_ramDataIn", nrOfBits);
    }
    if (dataLines > 1) {
      for (var idx = 0; idx < dataLines; idx++) {
        myWires
            .addRegister(String.format("s_dataIn%dReg", idx), nrOfBits)
            .addRegister(String.format("s_dataOut%dReg", idx), nrOfBits)
            .addRegister(String.format("s_lineEnable%dReg", idx), 1);
        myPorts
            .add(Port.INPUT, String.format("data%dIn", idx), nrOfBits, RamAppearance.getDataInIndex(idx, attrs))
            .add(Port.OUTPUT, String.format("data%dOut", idx), nrOfBits, RamAppearance.getDataOutIndex(idx, attrs))
            .add(Port.INPUT, String.format("lineEnable%dIn", idx), 1, RamAppearance.getLEIndex(idx, attrs));
      }
    } else {
      myWires
          .addRegister("s_dataInReg", nrOfBits)
          .addRegister("s_dataOutReg", nrOfBits);
      myPorts
          .add(Port.INPUT, "dataIn", nrOfBits, RamAppearance.getDataInIndex(0, attrs))
          .add(Port.OUTPUT, "dataOut", nrOfBits, RamAppearance.getDataOutIndex(0, attrs));
    }
    myTypedWires
        .addArray(MemArrayId, MemArrayStr, nrOfBits, ramEntries)
        .addWire("s_memContents", MemArrayId);
    myPorts
        .add(Port.INPUT, "address", nrOfaddressLines, RamAppearance.getAddrIndex(0, attrs))
        .add(Port.INPUT, "we", 1, RamAppearance.getWEIndex(0, attrs))
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, RamAppearance.getClkIndex(0, attrs));
  }
  
  private void getGenerationTimeWiresPortsByteEnables(Netlist theNetlist, AttributeSet attrs) {
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    final var byteEnableOffset = RamAppearance.getBEIndex(0, attrs);
    final var nrBePorts = RamAppearance.getNrBEPorts(attrs);
    final var nrOfaddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var ramEntries = (1 << nrOfaddressLines);
    final var truncated = (nrOfBits % 8) != 0;
    myWires
        .addRegister("s_ramDataOut", nrOfBits)
        .addRegister("s_tickDelayLine", 3)
        .addRegister("s_dataInReg", nrOfBits)
        .addRegister("s_writeAddressReg", nrOfaddressLines)
        .addRegister("s_readAddressReg", nrOfaddressLines)
        .addRegister("s_weReg", 1)
        .addRegister("s_oeReg", 1)
        .addRegister("s_dataOutReg", nrOfBits)
        .addWire("s_ramAddress", nrOfaddressLines);
    if (byteEnables) {
      myWires
          .addRegister("s_byteEnableReg", nrBePorts);
      for (var idx = 0; idx < nrBePorts; idx++) {
        myWires
            .addWire(String.format("s_byteEnable%d", idx), 1)
            .addWire(String.format("s_we%d", idx), 1);
        myPorts
            .add(Port.INPUT, String.format("byteEnable%d", idx), 1, byteEnableOffset + nrBePorts - idx - 1);
      }
      myPorts.add(Port.INPUT, "oe", 1, RamAppearance.getOEIndex(0, attrs));
      var nrOfMems = nrBePorts;
      if (truncated) {
        myTypedWires
            .addArray(RestArrayId, RestArrayStr, nrOfBits % 8, ramEntries)
            .addWire("s_truncMemContents", RestArrayId);
        nrOfMems--;
      }
      myTypedWires
          .addArray(ByteArrayId, ByteArrayStr, 8, ramEntries);
      for (var mem = 0; mem < nrOfMems; mem++)
        myTypedWires
            .addWire(String.format("s_byteMem%dContents", mem), ByteArrayId);
    } else {
      myPorts.add(Port.INPUT, "oe", 1, Hdl.oneBit());
      myTypedWires
          .addArray(MemArrayId, MemArrayStr, nrOfBits, ramEntries)
          .addWire("s_memContents", MemArrayId);
      myWires
          .addWire("s_we", 1)
          .addWire("s_oe", 1);
    }
    myPorts
        .add(Port.INPUT, "address", nrOfaddressLines, RamAppearance.getAddrIndex(0, attrs))
        .add(Port.INPUT, "dataIn", nrOfBits, RamAppearance.getDataInIndex(0, attrs))
        .add(Port.INPUT, "we", 1, RamAppearance.getWEIndex(0, attrs))
        .add(Port.OUTPUT, "dataOut", nrOfBits, RamAppearance.getDataOutIndex(0, attrs))
        .add(Port.CLOCK, HdlPorts.getClockName(1), 1, RamAppearance.getClkIndex(0, attrs));
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      getGenerationTimeWiresPortsLineEnables(theNetlist, attrs);
    } else {
      getGenerationTimeWiresPortsByteEnables(theNetlist, attrs);
    }
  }
  
  private LineBuffer getModuleFunctionalityByteEnables(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1));
    final var be = attrs.getValue(RamAttributes.ATTR_ByteEnables);
    final var byteEnables = be != null && be.equals(RamAttributes.BUS_WITH_BYTEENABLES);
    final var syncRead = !attrs.getValue(Mem.ASYNC_READ);
    final var readAfterWrite = attrs.containsAttribute(Mem.READ_ATTR) & attrs.getValue(Mem.READ_ATTR).equals(Mem.READAFTERWRITE);
    final var writeTick = (readAfterWrite) ? 0 : 2;
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().addRemarkBlock("The control signals are defined here");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("s_byteEnable{{1}} <= s_byteEnableReg({{1}}) {{and}} s_tickDelayLine(2) {{and}} s_oeReg;", i)
              .add("s_we{{1}}         <= s_byteEnableReg({{1}}) {{and}} s_tickDelayLine({{2}}) {{and}} s_weReg;", i, writeTick);
        }
      } else {
        contents
            .add("s_oe <= s_tickDelayLine(2) {{and}} s_oeReg;")
            .add("s_we <= s_tickDelayLine({{1}}) {{and}} s_weReg;", writeTick);
      }
      contents
          .empty()
          .addRemarkBlock("The input registers are defined here")
          .add("""
              inputRegs : {{process}}({{clock}}, {{tick}}, address, dataIn, we, oe) {{is}}
              {{begin}}
                 {{if}} (rising_edge({{clock}})) {{then}}
              """);
      if (!syncRead) {
        contents.add("""
                          {{if}} (s_tickDelayLine(0) = '1') {{then}}
                             s_readAddressReg  <= address;
                          {{end}} {{if}};
                    """);
      }
      contents.add("""
                          {{if}} ({{tick}} = '1') {{then}}
                            s_dataInReg       <= dataIn;
                            s_writeAddressReg <= address;
                  """);
      if (syncRead) {
        contents.add("          s_readAddressReg  <= address;");
      }
      contents.add("""
                             s_weReg           <= we;
                             s_oeReg           <= oe;
                   """);
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++)
          contents.add("         s_byteEnableReg({{1}}) <= byteEnable{{1}};", i);
      }
      contents
          .add("""
                    {{end}} {{if}};
                 {{end}} {{if}};
              {{end}} {{process}} inputRegs;
              """)
          .empty()
          .add("""
              tickPipeReg : {{process}}({{clock}}) {{is}}
              {{begin}}
                 {{if}} (rising_edge({{clock}})) {{then}}
                     s_tickDelayLine(0)          <= {{tick}};
                     s_tickDelayLine(2 {{downto}} 1) <= s_tickDelayLine(1 {{downto}} 0);
                 {{end}} {{if}};
              {{end}} {{process}} tickPipeReg;
              """)
          .empty()
          .addRemarkBlock("The actual memorie(s) is(are) defined here");
      contents
          .add("s_ramAddress <= s_writeAddressReg {{when}} s_tickDelayLine({{1}}) = '1' {{else}} s_readAddressReg;", writeTick)
          .empty();
      if (byteEnables) {
        final var truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("mem{{1}} : {{process}}({{clock}}, s_we{{1}}, s_dataInReg, s_ramAddress) {{is}}", i)
              .add("{{begin}}")
              .add("   {{if}} (rising_edge({{clock}})) {{then}}")
              .add("      {{if}} (s_we{{1}} = '1') {{then}}", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          final var memName =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_truncMemContents"
                  : String.format("s_byteMem%dContents", i);
          contents
              .add("         {{1}}(to_integer(unsigned(s_ramAddress))) <= s_dataInReg({{2}} {{downto}} {{3}});", memName, endIndex, startIndex)
              .add("      {{end}} {{if}};")
              .add("      s_ramDataOut({{1}} {{downto}} {{2}}) <= {{3}}(to_integer(unsigned(s_ramAddress)));", endIndex, startIndex, memName)
              .add("   {{end}} {{if}};")
              .add("{{end}} {{process}} mem{{1}};", i)
              .add("");
        }
      } else {
        contents
            .add("""
                mem : {{process}}({{clock}} , s_we, s_dataInReg, s_ramAddress) {{is}}
                {{begin}}
                   {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_we = '1') {{then}}
                         s_memContents(to_integer(unsigned(s_ramAddress))) <= s_dataInReg;
                      {{end}} {{if}};
                      s_ramDataOut <= s_memContents(to_integer(unsigned(s_ramAddress)));
                   {{end}} {{if}};
                {{end}} {{process}} mem;
                """);
      }
      contents.empty().addRemarkBlock("The output register is defined here");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("res{{1}} : {{process}}({{clock}}, s_byteEnable{{1}}, s_ramDataOut) {{is}}", i)
              .add("{{begin}}")
              .add("   {{if}} (rising_edge({{clock}}) {{then}}")
              .add("      {{if}} (s_byteEnable{{1}} = '1') {{then}}", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          contents
              .add("         dataOut({{1}} {{downto}} {{2}}) <= s_ramDataOut({{1}} {{downto}} {{2}});", endIndex, startIndex)
              .add("      {{end}} {{if}};")
              .add("   {{end}} {{if}};")
              .add("{{end}} {{process}} res{{1}};", i);
        }
      } else {
        contents
            .add("""
                res : {{process}}({{clock}}, s_oe, s_ramDataOut) {{is}}
                {{begin}}
                   {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_oe = '1') {{then}}
                        dataOut <= s_ramDataOut;
                      {{end}} {{if}};
                   {{end}} {{if}};
                {{end}} {{process}} res;
                """);
      }
    } else {
      contents.empty().addVhdlKeywords().addRemarkBlock("The control signals are defined here");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("assign s_byteEnable{{1}} = s_byteEnableReg[{{1}}] & s_tickDelayLine[2] & s_oeReg;", i)
              .add("assign s_we{{1}}         = s_byteEnableReg[{{1}}] & s_tickDelayLine[{{2}}] & s_weReg;", i, writeTick);
        }
      } else {
        contents
            .add("assign s_oe = s_tickDelayLine[2] & s_oeReg;")
            .add("assign s_we = s_tickDelayLine[{{1}}] & s_weReg;", writeTick);
      }
      contents
          .empty()
          .addRemarkBlock("The input registers are defined here")
          .add("""
              always @(posedge {{clock}})
              begin
              """);
      if (!syncRead) {
        contents.add("  s_readAddressReg <= (s_tickDelayLine[0] == 1'b1) ? address : s_readAddressReg;");
      }
      contents.add("""
                     if ({{tick}} == 1'b1)
                       begin
                         s_dataInReg       <= dataIn;
                         s_writeAddressReg <= address;
                  """);
      if (syncRead) {
        contents.add("       s_readAddressReg  <= address;");
      }
      contents.add("""
                          s_weReg           <= we;
                          s_oeReg           <= oe;
                   """);
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++)
          contents.add("       s_byteEnableReg[{{1}}] <= byteEnable{{1}};", i);
      }
      contents
          .add("""
                end
              end
              """)
          .empty()
          .add("""
              always @(posedge {{clock}})
                s_tickDelayLine <= {s_tickDelayLine[1:0], tick};
              """)
          .empty()
          .addRemarkBlock("The actual memorie(s) is(are) defined here");
      contents
          .add("assign s_ramAddress = (s_tickDelayLine[{{1}}] == 1'b1) ? s_writeAddressReg : s_readAddressReg;", writeTick)
          .empty();
      if (byteEnables) {
        final var truncated = (attrs.getValue(Mem.DATA_ATTR).getWidth() % 8) != 0;
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("""
                  always @(posedge {{clock}})
                    begin    
                  """);
          contents.add("    if (s_we{{1}} == 1'b1)", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          final var memName =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1) && truncated)
                  ? "s_truncMemContents"
                  : String.format("s_byteMem%dContents", i);
          contents
              .add("      {{1}}[s_ramAddress] <= s_dataInReg[{{2}}:{{3}}];", memName, endIndex, startIndex)
              .add("    s_ramDataOut[{{1}}:{{2}}] <= {{3}}[s_ramAddress];", endIndex, startIndex, memName)
              .add("  end")
              .empty();
        }
      } else {
        contents
            .add("""
                always @(posedge clock)
                  begin
                    if (s_we == 1'b1)
                      s_memContents[s_ramAddress] <= s_dataInReg;
                    s_ramDataOut <= s_memContents[s_ramAddress];
                  end
                """);
      }
      contents
            .empty()
            .addRemarkBlock("The output register is defined here")
            .add("assign dataOut = s_dataOutReg;");
      if (byteEnables) {
        for (var i = 0; i < RamAppearance.getNrBEPorts(attrs); i++) {
          contents
              .add("always @(posedge {{clock}})")
              .add("  if (s_byteEnable{{1}} == 1'b1)", i);
          final var startIndex = i * 8;
          final var endIndex =
              (i == (RamAppearance.getNrBEPorts(attrs) - 1))
                  ? attrs.getValue(Mem.DATA_ATTR).getWidth() - 1
                  : (i + 1) * 8 - 1;
          contents
              .add("    s_dataOutReg[{{1}}:{{2}}] <= s_ramDataOut[{{1}}:{{2}}];", endIndex, startIndex)
              .empty();
        }
      } else {
        contents
            .add("""
                always @(posedge {{clock}})
                  if (s_oe == 1'b1)
                    s_dataOutReg <= s_ramDataOut;
                """);
      }
    }
    return contents.empty();
  }

  private LineBuffer getModuleFunctionalityLineEnables(Netlist theNetlist, AttributeSet attrs) {
    /*
     * In the logisim simulation, the RAM's with line enables have following behavior:
     * - Asynchronous read
     * - Write after Read
     * This is implemented using semi-dual-ported synchronous memories in FPGA by using multiple cycles.
     * Note that in the worst case this "simulated" behavior takes up to 9 FPGA-clock cycles,
     * hence the tick frequency should be 5 times slower than the FPGA clock to have proper behavior
     * on the FPGA.
     * 
     * IMPORTANT: 
     *  1) in case of a gated clock (hence the RAM is not connected to a clock component) this 
     * HDL-description will NOT work on the FPGA and the simulation in logisim and on an FPGA are for
     * sure not identical!
     *  2) This module uses system Verilog features.
     */
    final var contents = LineBuffer.getHdlBuffer()
        .pair("clock", HdlPorts.getClockName(1))
        .pair("tick", HdlPorts.getTickName(1));
    final var dataLines = Math.max(1, RamAppearance.getNrLEPorts(attrs));
    final var nrOfaddressLines = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    if (Hdl.isVhdl()) {
      contents.empty().addVhdlKeywords().addRemarkBlock("The synchronous semi-dual-ported memory is defined here");
      contents.add(String.format("s_ramWriteAddress <= std_logic_vector(unsigned(s_writeAddressReg) + unsigned(s_addressOffsetReg(%d {{downto}} 0)));",
              nrOfaddressLines - 1));
      contents.add(String.format("s_ramReadAddress <= std_logic_vector(unsigned(s_readAddressReg) + unsigned(s_addressOffsetReg(%d {{downto}} 0)));",
              nrOfaddressLines - 1));
      contents.add("""

                  blockramwrite : {{process}}({{clock}}) {{is}}
                  {{begin}}
                    {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_ramWe = '1') {{then}}
                        s_memContents(to_integer(unsigned(s_ramWriteAddress))) <= s_ramDataIn;
                      {{end}} {{if}};
                    {{end}} {{if}};
                  {{end}} {{process}} blockramwrite;

                  blockramread : {{process}}({{clock}}) {{is}}
                  {{begin}}
                    {{if}} (falling_edge({{clock}})) {{then}}
                      s_ramDataOut <= s_memContents(to_integer(unsigned(s_ramReadAddress)));
                    {{end}} {{if}};
                  {{end}} {{process}} blockramread;
                  """);
      contents.empty().addRemarkBlock("The input registers are defined here");
      contents.add("""
                  inputRegs : {{process}}({{clock}}) {{is}}
                  {{begin}}
                    {{if}} (rising_edge({{clock}})) {{then}}
                      {{if}} (s_tickDelayReg(0) = '1') {{then}}
                        s_readAddressReg <= address;
                      {{end}} {{if}};
                      {{if}} ({{tick}} = '1') {{then}}
                        s_writeAddressReg <= address;
                        s_weReg           <= we;
                  """);
      if (dataLines == 1) {
        contents.add("      s_dataInReg <= dataIn;");
      } else {
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("      s_dataIn%dReg <= data%dIn;", idx, idx));
          contents.add(String.format("      s_lineEnable%dReg <= lineEnable%dIn;", idx, idx));
        }
      }
      contents.add("""
                      {{end}} {{if}};
                    {{end}} {{if}};
                  {{end}} {{process}} inputRegs;
                  """);
      contents.empty().addRemarkBlock("The FSM's are defined here");
      contents.add("""
                  fsms : {{process}}({{clock}}) {{is}}
                  {{begin}}
                     {{if}} (rising_edge({{clock}})) {{then}}
                        s_tickDelayReg(0)  <= {{tick}};
                  """);
      if (dataLines == 1) {
        contents.add("      s_tickDelayReg(1)  <= s_tickDelayReg(0);");
      } else {
        contents.add(String.format("      s_tickDelayReg(%d {{downto}} 1) <= s_tickDelayReg(%d {{downto}} 0);", 
            dataLines, dataLines - 1));
      }
      contents.add("""
                        {{if}} (s_tickDelayReg(0) = '1') {{then}}
                          s_addressOffsetReg <= (OTHERS => '0');
                  """);
      contents.add(String.format("      {{elsif}} (unsigned(s_addressOffsetReg) < to_unsigned(%d,%d)) {{then}}",
          dataLines, nrOfaddressLines + 1));
      contents.add(String.format("       s_addressOffsetReg <= std_logic_vector(unsigned(s_addressOffsetReg) + to_unsigned(1,%d));",
          nrOfaddressLines + 1));             
      contents.add("""
                        {{end}} {{if}};
                     {{end}} {{if}};
                  {{end}} {{process}} fsms;
                  """);
      contents.empty().addRemarkBlock("Here the RamDatIn is defined");
      if (dataLines == 1) {
        contents.add("s_ramDataIn <= s_dataInReg;");
      } else {
        contents.add("{{with}} (s_addressOffsetReg) {{select}} s_ramDataIn <=");
        for (var idx = dataLines - 1; idx > 0; idx--) {
          final var binValue = Integer.toBinaryString(idx);
          final var extendedBinValue = new StringBuffer();
          while (extendedBinValue.length() < (nrOfaddressLines + 1 - binValue.length())) {
            extendedBinValue.append("0");
          }
          extendedBinValue.append(binValue);
          contents.add(String.format("  s_dataIn%dReg {{when}} \"%s\",", idx, extendedBinValue));
        }
        contents.add("               s_dataIn0Reg {{when}} {{others}};");
      }
      contents.empty().addRemarkBlock("Here the RamDataOut is defined");
      if (dataLines == 1) {
        contents.add("""
                    dataOut <= s_dataOutReg;

                    dataOutReg : {{process}} ({{clock}}) {{is}}
                    {{begin}}
                      {{if}} (rising_edge({{clock}})) {{then}}
                        {{if}} (s_tickDelayReg(1) = '1') {{then}}
                          s_dataOutReg <= s_ramDataOut;
                        {{end}} {{if}};
                      {{end}} {{if}};
                    {{end}} {{process}} dataOutReg;
                    """);
      } else {
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("data%dOut <= s_dataOut%dReg;", idx, idx));
        }
        contents.add("""

                    dataOutRegs : {{process}} ({{clock}}) {{is}}
                    {{begin}}
                      {{if}} (rising_edge({{clock}})) {{then}}
                    """);
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("    {{if}} (s_tickDelayReg(%d) = '1') {{then}}", idx + 1));
          contents.add(String.format("      s_dataOut%dReg <= s_ramDataOut;", idx));
          contents.add("    {{end}} {{if}};");
        }
        contents.add("""
                      {{end}} {{if}};
                    {{end}} {{process}} dataOutRegs;
                    """);
      }
      contents.empty().addRemarkBlock("Here the Ram write enable is defined");
      if (dataLines == 1) {
        contents.add("s_ramWe <= s_weReg {{and}} s_tickDelayReg(1);");
      } else {
        contents.add("s_ramWe <= s_weReg {{and}} (");
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("          (s_lineEnable%dReg {{and}} s_tickDelayReg(%d))%s", idx, idx + 1,
                  (idx == dataLines - 1) ? ");" : " {{or}}"));
        }
      }
    } else {
      contents.empty().addRemarkBlock("The synchronous semi-dual-ported memory is defined here");
      contents.add(String.format("assign s_ramWriteAddress = s_writeAddressReg + s_addressOffsetReg[%d:0];", nrOfaddressLines - 1));
      contents.add(String.format("assign s_ramReadAddress = s_readAddressReg + s_addressOffsetReg[%d:0];", nrOfaddressLines - 1));
      contents.empty();
      contents.add("""
                  always @(posedge clock)
                    if (s_ramWe == 1'b1) s_memContents[s_ramWriteAddress] <= s_ramDataIn;
                  
                  always @(negedge clock)
                    s_ramDataOut <= s_memContents[s_ramReadAddress];
                  """);
      contents.empty().addRemarkBlock("The input registers are defined here");
      contents.add("""
                  always @(posedge clock)
                    begin
                      if (s_tickDelayReg[0] == 1'b1)
                        s_readAddressReg <= address;
                      if ({{tick}} == 1'b1)
                        begin
                          s_writeAddressReg     <= address;
                          s_weReg               <= we;
                   """);
      if (dataLines == 1) {
        contents.add("        s_dataInReg      <= dataIn;");
      } else {
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("        s_dataIn%dReg     <= data%dIn;", idx, idx));
          contents.add(String.format("        s_lineEnable%dReg <= lineEnable%dIn;", idx, idx));
        }
      }
      contents.add("""
                        end
                      end
                  """);
      contents.empty().addRemarkBlock("The FSM's are defined here");
      contents.add("""
                  always @(posedge clock)
                    begin
                      s_tickDelayReg[0] <= {{tick}};
                  """);
      if (dataLines == 1) {
        contents.add("    s_tickDelayReg[1] <= s_tickDelayReg[0];");
      } else {
        contents.add(String.format("    s_tickDelayReg[%d:1] <= s_tickDelayReg[%d:0];", dataLines, dataLines - 1));
      }
      contents.add(String.format("    s_addressOffsetReg <= (s_tickDelayReg[0] == 1'b1) ? %d'd0 :", nrOfaddressLines + 1));
      contents.add(String.format("                          s_addressOffsetReg != %d'd%d ? s_addressOffsetReg + %d'd1 :", 
          nrOfaddressLines + 1, dataLines, nrOfaddressLines + 1));
      contents.add("""
                                            s_addressOffsetReg;
                    end
                  """);
      contents.empty().addRemarkBlock("Here the RamDatIn is defined");
      if (dataLines == 1) {
        contents.add("assign s_ramDataIn = s_dataInReg;");
      } else {
        contents.add("""
                    always @*
                      case (s_addressOffsetReg)
                    """);
        for (var idx = dataLines - 1; idx > 0; idx--) {
          contents.add(String.format("    %d'd%d    : s_ramDataIn <= s_dataIn%dReg;", nrOfaddressLines + 1, idx, idx));
        }
        contents.add("""
                        default : s_ramDataIn <= s_dataIn0Reg;
                      endcase;
                    """);
      }
      contents.empty().addRemarkBlock("Here the RamDatout is defined");
      if (dataLines == 1) {
        contents.add("""
                    assign dataOut = s_dataOutReg;
                    
                    always @(posedge clock)
                      s_dataOutReg <= (s_tickDelayReg[1] == 1'b1) ? s_ramDataOut : s_dataOutReg;
                    """);
      } else {
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("assign data%dOut = s_dataOut%dReg;", idx, idx));
        }
        contents.add("""
                    
                    always @(posedge clock)
                      begin
                    """);
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("    s_dataOut%dReg <= (s_tickDelayReg[%d] == 1'b1) ? s_ramDataOut : s_dataOut%dReg;",
                  idx, idx + 1, idx));
        }
        contents.add("""
                      end
                    """);
      }
      contents.empty().addRemarkBlock("Here the Ram write enable is defined");
      if (dataLines == 1) {
        contents.add("assign s_ramWe = s_weReg & s_tickDelayReg[1];");
      } else {
        contents.add("assign s_ramWe = s_weReg & (");
        for (var idx = 0; idx < dataLines; idx++) {
          contents.add(String.format("                 (s_lineEnable%dReg & s_tickDelayReg[%d])%s", idx, idx + 1,
                  (idx == dataLines - 1) ? ");" : "|"));
        }
      }
    }
    return contents.empty();
  }
  
  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      return getModuleFunctionalityLineEnables(theNetlist, attrs);
    } else {
      return getModuleFunctionalityByteEnables(theNetlist, attrs);
    }
  }
  
  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    Object busVal = attrs.getValue(RamAttributes.ATTR_DBUS);
    final var separate = busVal != null && busVal.equals(RamAttributes.BUS_SEP);
    Object trigger = attrs.getValue(StdAttr.TRIGGER);
    final var asynch = trigger == null || trigger.equals(StdAttr.TRIG_HIGH) || trigger.equals(StdAttr.TRIG_LOW);
    final var clearPin = attrs.getValue(RamAttributes.CLEAR_PIN) == null ? false : attrs.getValue(RamAttributes.CLEAR_PIN);
    final var isLineControlled = attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES);
    return (separate && !asynch && !clearPin) 
          || (isLineControlled && !clearPin);
  }
}
