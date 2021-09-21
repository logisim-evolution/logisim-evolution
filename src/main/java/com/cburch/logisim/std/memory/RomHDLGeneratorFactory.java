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
import com.cburch.logisim.fpga.designrulecheck.NetlistComponent;
import com.cburch.logisim.fpga.hdlgenerator.HDL;
import com.cburch.logisim.fpga.hdlgenerator.InlinedHDLGeneratorFactory;
import com.cburch.logisim.fpga.hdlgenerator.WithSelectHDLGenerator;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.LineBuffer;

import java.util.ArrayList;

public class RomHDLGeneratorFactory extends InlinedHDLGeneratorFactory {

  @Override
  public ArrayList<String> getInlinedCode(Netlist nets, Long componentId, NetlistComponent componentInfo, String circuitName) {
    AttributeSet attrs = componentInfo.getComponent().getAttributeSet();
    final var addressWidth = attrs.getValue(Mem.ADDR_ATTR).getWidth();
    final var dataWidth = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var romContents = attrs.getValue(Rom.CONTENTS_ATTR);
    final var generator = (new WithSelectHDLGenerator(componentInfo.getComponent().getAttributeSet().getValue(StdAttr.LABEL),
        HDL.getBusName(componentInfo, RamAppearance.getAddrIndex(0, attrs), nets), addressWidth,
        HDL.getBusName(componentInfo, RamAppearance.getDataOutIndex(0, attrs), nets), dataWidth))
        .setDefault(0L);
    for (var addr = 0L; addr < (1L << addressWidth); addr++) {
      final var romValue = romContents.get(addr);
      if (romValue != 0L) generator.add(addr, romValue);
    }
    return LineBuffer.getBuffer().add(generator.getHdlCode()).getWithIndent(3);
  }

  @Override
  public boolean isHDLSupportedTarget(AttributeSet attrs) {
    if (attrs == null) return false;
    if (attrs.getValue(Mem.LINE_ATTR) == null) return false;
    return attrs.getValue(Mem.LINE_ATTR).equals(Mem.SINGLE);
  }
}
