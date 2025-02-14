/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.util.LineBuffer;

public class PortHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    final var contents = LineBuffer.getHdlBuffer();
    final var portType = componentInfo.getComponent().getAttributeSet().getValue(PortIo.ATTR_DIR);
    var nrOfPins = componentInfo.getComponent().getAttributeSet().getValue(PortIo.ATTR_SIZE).getWidth();
    final var startIndex = componentInfo.getLocalBubbleInputStartId();
    final var endIndex = startIndex + nrOfPins - 1;
    if (portType == PortIo.INPUT) {
      if (nrOfPins == 1) {
        contents.add(
            "{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{>}};",
            Hdl.getNetName(componentInfo, 0, true, nets),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            endIndex);
      } else {
        contents.add(
            "{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            Hdl.getBusName(componentInfo, 0, nets),
            LOCAL_INPUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex);
      }
    } else if (portType == PortIo.OUTPUT) {
      if (nrOfPins == 1) {
        contents.add(
            "{{assign}} {{1}}{{<}}{{2}}{{>}}{{=}}{{3}};",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.getNetName(componentInfo, 0, true, nets));
      } else {
        contents.add(
            "{{assign}} {{1}}{{<}}{{2}}{{3}}{{4}}{{>}}{{=}}{{5}};",
            LOCAL_OUTPUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex,
            Hdl.getBusName(componentInfo, 0, nets));
      }
    } else {
      // first we handle the input connections, and after that the output connections
      if (nrOfPins == 1) {
        contents.add(
            "{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{>}};",
            Hdl.getNetName(componentInfo, 2, true, nets),
            LOCAL_INOUT_BUBBLE_BUS_NAME,
            endIndex);
      } else {
        contents.add(
            "{{assign}} {{1}}{{=}}{{2}}{{<}}{{3}}{{4}}{{5}}{{>}};",
            Hdl.getBusName(componentInfo, 2, nets),
            LOCAL_INOUT_BUBBLE_BUS_NAME,
            endIndex,
            Hdl.vectorLoopId(),
            startIndex);
      }
      // simple case first, we have a single output enable
      if (portType == PortIo.INOUTSE || nrOfPins == 1) {
        if (Hdl.isVhdl()) {
          if (nrOfPins == 1) {
            contents
                .addVhdlKeywords()
                .add(
                    "{{1}}({{2}}) <= {{3}} {{when}} {{4}} = '1' {{else}} ({{others}} => 'Z');",
                    LOCAL_INOUT_BUBBLE_BUS_NAME,
                    startIndex,
                    Hdl.getNetName(componentInfo, 1, true, nets),
                    Hdl.getNetName(componentInfo, 0, true, nets));
          } else {
            contents
                .addVhdlKeywords()
                .add(
                    "{{1}}({{2}} {{downto}} {{3}}) <= {{4}} {{when}} {{5}} = '1' {{else}} ({{others}} => 'Z');",
                    LOCAL_INOUT_BUBBLE_BUS_NAME,
                    endIndex,
                    startIndex,
                    Hdl.getBusName(componentInfo, 1, nets),
                    Hdl.getNetName(componentInfo, 0, true, nets));
          }
        } else {
          if (nrOfPins == 1) {
            contents.add(
                "assign {{1}}[{{2}}] = ({{3}}) ? {{4}} : {{5}}'bZ;",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                startIndex,
                Hdl.getNetName(componentInfo, 0, true, nets),
                Hdl.getNetName(componentInfo, 1, true, nets),
                nrOfPins);
          } else {
            contents.add(
                "assign {{1}}[{{2}}:{{3}}] = ({{4}}) ? {{5}} : {{6}}'bZ;",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                endIndex,
                startIndex,
                Hdl.getNetName(componentInfo, 0, true, nets),
                Hdl.getBusName(componentInfo, 1, nets),
                nrOfPins);
          }
        }
      } else {
        // we have to enumerate over each and every bit
        for (var busBitIndex = 0; busBitIndex < nrOfPins; busBitIndex++) {
          if (Hdl.isVhdl()) {
            contents.addVhdlKeywords().add(
                "{{1}}({{2}}) <= {{3}} {{when}} {{4}} = '1' {{else}} 'Z';",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                startIndex + busBitIndex,
                Hdl.getBusEntryName(componentInfo, 1, true, busBitIndex, nets),
                Hdl.getBusEntryName(componentInfo, 0, true, busBitIndex, nets));
          } else {
            contents.add(
                "assign {{1}}[{{2}}] = ({{3}}) ? {{4}} : 1'bZ;",
                LOCAL_INOUT_BUBBLE_BUS_NAME,
                startIndex + busBitIndex,
                Hdl.getBusEntryName(componentInfo, 0, true, busBitIndex, nets),
                Hdl.getBusEntryName(componentInfo, 1, true, busBitIndex, nets));
          }
        }
      }
    }
    return contents;
  }
}
