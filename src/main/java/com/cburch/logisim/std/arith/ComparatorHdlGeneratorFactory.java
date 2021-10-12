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
import java.util.Map;

public class ComparatorHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_BITS_STRING = "nrOfBits";
  private static final int NR_OF_BITS_ID = -1;
  private static final String TWOS_COMPLEMENT_STRING = "twosComplement";
  private static final int TWOS_COMPLEMENT_ID = -2;

  public static final Map<AttributeOption, Integer> SIGNED_MAP = new HashMap<>() {{
      put(Comparator.UNSIGNED_OPTION, 0);
      put(Comparator.SIGNED_OPTION, 1);
    }};


  public ComparatorHdlGeneratorFactory() {
    super();
    myParametersList
        .addBusOnly(NR_OF_BITS_STRING, NR_OF_BITS_ID)
        .add(TWOS_COMPLEMENT_STRING, TWOS_COMPLEMENT_ID, HdlParameters.MAP_ATTRIBUTE_OPTION, Comparator.MODE_ATTR,
            SIGNED_MAP);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    myPorts
        .add(Port.INPUT, "dataA", NR_OF_BITS_ID, Comparator.IN0, StdAttr.WIDTH)
        .add(Port.INPUT, "dataB", NR_OF_BITS_ID, Comparator.IN1, StdAttr.WIDTH)
        .add(Port.OUTPUT, "aGreaterThanB", 1, Comparator.GT)
        .add(Port.OUTPUT, "aEqualsB", 1, Comparator.EQ)
        .add(Port.OUTPUT, "aLessThanB", 1, Comparator.LT);
    if (attrs.getValue(StdAttr.WIDTH).getWidth() > 1)
      myWires
          .addWire("s_signedLess", 1)
          .addWire("s_unsignedLess", 1)
          .addWire("s_signedGreater", 1)
          .addWire("s_unsignedGreater", 1);
  }


  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var contents = LineBuffer.getBuffer().pair("twosComplement", TWOS_COMPLEMENT_STRING);
    final var nrOfBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    if (Hdl.isVhdl()) {
      if (nrOfBits == 1) {
        contents.empty().addVhdlKeywords().add("""
            aEqualsB <= dataA {{xnor}} dataB;
            aLessThanB <= dataA {{and}} {{not}}(dataB) {{when}} {{twosComplement}} = 1 {{else}} {{not}}(dataA) {{and}} dataB;
            aGreaterThanB <= {{not}}(dataA) {{and}} dataB {{when}} {{twosComplement}} = 1 {{else}} dataA {{and}} {{not}}(dataB);
            """);
      } else {
        contents.empty().addVhdlKeywords().add("""
            s_signedLess      <= '1' {{when}} signed(dataA) < signed(dataB) {{else}} '0';
            s_unsignedLess    <= '1' {{when}} unsigned(dataA) < unsigned(dataB) {{else}} '0';
            s_signedGreater   <= '1' {{when}} signed(dataA) > signed(dataB) {{else}} '0';
            s_unsignedGreater <= '1' {{when}} unsigned(dataA) > unsigned(dataB) {{else}} '0';

            aEqualsB      <= '1' {{when}} dataA = dataB ELSE '0';
            aGreaterThanB <= s_signedGreater {{when}} {{twosComplement}} = 1 {{else}} s_unsignedGreater;
            aLessThanB    <= s_signedLess {{when}} {{twosComplement}} = 1 {{else}} s_unsignedLess;
            """);
      }
    } else {
      if (nrOfBits == 1) {
        contents.add("""
            assign aEqualsB      = (dataA == dataB);
            assign aLessThanB    = (dataA < dataB);
            assign aGreaterThanB = (dataA > dataB);
            """);
      } else {
        contents.add("""
            assign s_signedLess      = ($signed(dataA) < $signed(dataB));
            assign s_unsignedLess    = (dataA < dataB);
            assign s_signedGreater   = ($signed(dataA) > $signed(dataB));
            assign s_unsignedGreater = (dataA > dataB);

            assign aEqualsB      = (dataA == dataB);
            assign aGreaterThanB = ({{twosComplement}}==1) ? s_signedGreater : s_unsignedGreater;
            assign aLessThanB    = ({{twosComplement}}==1) ? s_signedLess : s_unsignedLess;
            """);
      }
    }
    return contents.empty();
  }
}
