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
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class PortHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var portType = componentInfo.getComponent().getAttributeSet().getValue(PortIo.ATTR_DIR);
    var nrOfPins = componentInfo.getComponent().getAttributeSet().getValue(PortIo.ATTR_SIZE).getWidth();
    if (portType == PortIo.INPUT) {
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInputStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        contents.add("{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            Hdl.getBusName(componentInfo, busIndex, nets),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex);
      }
    } else if (portType == PortIo.OUTPUT) {
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleOutputStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        contents.add("{{assign}} {{1}}{{<}}{{2}}{{3}}{{4}}{{>}}{{=}}{{5}};",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex,
            Hdl.getBusName(componentInfo, busIndex, nets));
      }
    } else {
      // first we handle the input connections, and after that the output connections
      var outputIndex = 0;
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInOutStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        final var inputIndex = (portType == PortIo.INOUTSE) ? (busIndex + 1) : (busIndex * 2 + 1);
        outputIndex = inputIndex + 1;
        contents.add("{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            Hdl.getBusName(componentInfo, inputIndex, nets),
            LOCAL_INOUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex);
      }
      var enableIndex = 0;
      nrOfPins = componentInfo.getComponent().getAttributeSet().getValue(PortIo.ATTR_SIZE).getWidth();
      for (var busIndex = 0; nrOfPins > 0; busIndex++) {
        final var startIndex = componentInfo.getLocalBubbleInOutStartId() + busIndex * BitWidth.MAXWIDTH;
        final var nrOfBitsInThisBus = Math.min(nrOfPins, BitWidth.MAXWIDTH);
        nrOfPins -= nrOfBitsInThisBus;
        final var endIndex = startIndex + nrOfBitsInThisBus - 1;
        if ((portType != PortIo.INOUTSE) && (busIndex > 0)) enableIndex += 2;
        // simple case first, we have a single output enable
        if (portType == PortIo.INOUTSE) {
          if (Hdl.isVhdl()) {
            contents.addVhdlKeywords().add("{{1}}({{2}} {{downto}} {{3}}) <= {{4}} {{when}} {{5}} = '1' {{else}} ({{others}} => 'Z');",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                endIndex,
                startIndex,
                Hdl.getBusName(componentInfo, outputIndex++, nets),
                Hdl.getNetName(componentInfo, enableIndex, true, nets));
          } else {
            contents.add("assign {{1}}[{{2}}:{{3}}] = ({{4}}) ? {{5}} : {{6}}'bZ;",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                endIndex,
                startIndex,
                Hdl.getNetName(componentInfo, enableIndex, true, nets),
                Hdl.getBusName(componentInfo, outputIndex++, nets),
                nrOfBitsInThisBus);
          }
        } else {
          // we have to enumerate over each and every bit
          for (var busBitIndex = 0; busBitIndex < nrOfBitsInThisBus; busBitIndex++) {
            if (Hdl.isVhdl()) {
              contents.add("{{1}}({{2}}) <= {{3}} {{when}} {{4}} = '1' {{else}} 'Z';",
                  LOCAL_INOUT_BUBBLE_BUS_NAME,
                  startIndex + busBitIndex,
                  Hdl.getBusEntryName(componentInfo, outputIndex, true, busBitIndex, nets),
                  Hdl.getBusEntryName(componentInfo, enableIndex, true, busBitIndex, nets));
            } else {
              contents.add("assign {{1}}[{{2}}] = ({{3}}) ? {{4}} : 1'bZ;",
                  LOCAL_INOUT_BUBBLE_BUS_NAME,
                  startIndex + busBitIndex,
                  Hdl.getBusEntryName(componentInfo, enableIndex, true, busBitIndex, nets),
                  Hdl.getBusEntryName(componentInfo, outputIndex, true, busBitIndex, nets));
            }
          }
          outputIndex++;
        }
      }
    }
    return contents;
  }
}
