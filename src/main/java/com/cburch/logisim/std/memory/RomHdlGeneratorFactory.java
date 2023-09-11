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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.Hdl;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHdlGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHdlGenerator;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

public class RomHdlGeneratorFactory extends InlinedHdlGeneratorFactory {

  @Override
  public LineBuffer getInlinedCode(
      Netlist nets, Long componentId, netlistComponent componentInfo, String circuitName) {
    AttributeSet attrs = componentInfo.getComponent().getAttributeSet();
    final var addressWidth = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var dataWidth = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var romContents = attrs.getValue(Rom.CONTENTS_ATTR);
    final var generator =
        (new WithSelectHdlGenerator(
                componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL),
                Hdl.getBusName(componentInfo, RamAppearance.getAddrIndex(0, attrs), nets),
                addressWidth,
                Hdl.getBusName(componentInfo, RamAppearance.getDataOutIndex(0, attrs), nets),
                dataWidth))
            .setDefault(0L);
    for (var addr = 0L; addr < (1L << addressWidth); addr++) {
      final var romValue = romContents.get(addr);
      if (romValue != 0L) generator.add(addr, romValue);
    }
    return LineBuffer.getBuffer().add(generator.getHdlCode());
  }

  @Override
  public boolean isHdlSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    if (attrs.getValue(Mem.LINE_ATTR) == null) return false;
    return attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE);
  }
}
