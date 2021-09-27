/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHDLGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

import java.util.ArrayList;

public class PortHDLGeneratorFactory extends InlinedHDLGeneratorFactory {

  @Override
  public ArrayList<String> getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var portType = componentInfo.getComponent().getAttributeSet().getValue(PortIO.ATTR_DIR);
    var nrOfPins = componentInfo.getComponent().getAttributeSet().getValue(PortIO.ATTR_SIZE).getWidth();
    if (portType == PortIO.INPUT) {
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInputStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        contents.add("{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            HDL.getBusName(componentInfo, busIndex, nets),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            endIndex,
            HDL.vectorLoopId(),
            startIndex);
      }
    } else if (portType == PortIO.OUTPUT) {
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleOutputStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        contents.add("{{assign}} {{1}}{{<}}{{2}}{{3}}{{4}}{{>}}{{=}}{{5}};",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            endIndex,
            HDL.vectorLoopId(),
            startIndex,
            HDL.getBusName(componentInfo, busIndex, nets));
      }
    } else {
      // first we handle the input connections, and after that the output connections
      var outputIndex = 0;
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInOutStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        final var inputIndex = (portType == PortIO.INOUTSE) ? (busIndex + 1) : (busIndex * 2 + 1);
        outputIndex = inputIndex + 1;
        contents.add("{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            HDL.getBusName(componentInfo, inputIndex, nets),
            LOCAL_INOUT_BUBBLE_BUS_NAME,
            endIndex,
            HDL.vectorLoopId(),
            startIndex);
      }
      var enableIndex = 0;
      nrOfPins = componentInfo.getComponent().getAttributeSet().getValue(PortIO.ATTR_SIZE).getWidth();
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInOutStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        if ((portType != PortIO.INOUTSE) && (busIndex > 0)) enableIndex += 2;
        // simple case first, we have a single output enable
        if (portType == PortIO.INOUTSE) {
          if (HDL.isVhdl()) {
            contents.add("{{1}}({{2}} DOWNTO {{3}}) <= {{4}} WHEN {{5}} = '1' ELSE (OTHERS => 'Z');",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                endIndex,
                startIndex,
                HDL.getBusName(componentInfo, outputIndex++, nets),
                HDL.getNetName(componentInfo, enableIndex, true, nets));
          } else {
            contents.add("assign {{1}}[{{2}}:{{3}}] = ({{4}}) ? {{5}} : {{6}}'bZ;",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                endIndex,
                startIndex,
                HDL.getNetName(componentInfo, enableIndex, true, nets),
                HDL.getBusName(componentInfo, outputIndex++, nets),
                nrOfBitsInThisBus);
          }
        } else {
          // we have to enumerate over each and every bit
          for (var busBitIndex = 0; busBitIndex < nrOfBitsInThisBus; busBitIndex++) {
            if (HDL.isVhdl()) {
              contents.add("{{1}}({{2}}) <= {{3}} WHEN {{4}} = '1' ELSE 'Z';",
                  LOCAL_INOUT_BUBBLE_BUS_NAME,
                  startIndex + busBitIndex,
                  HDL.getBusEntryName(componentInfo, outputIndex, true, busBitIndex, nets),
                  HDL.getBusEntryName(componentInfo, enableIndex, true, busBitIndex, nets));
            } else {
              contents.add("assign {{1}}[{{2}}] = ({{3}}) ? {{4}} : 1'bZ;",
                  LOCAL_INOUT_BUBBLE_BUS_NAME,
                  startIndex + busBitIndex,
                  HDL.getBusEntryName(componentInfo, enableIndex, true, busBitIndex, nets),
                  HDL.getBusEntryName(componentInfo, outputIndex, true, busBitIndex, nets));
            }
          }
          outputIndex++;
        }
      }
    }
    return contents.getWithIndent(3);
  }
}
