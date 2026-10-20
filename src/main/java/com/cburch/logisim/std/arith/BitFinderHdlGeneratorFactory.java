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
import java.util.Map;
import java.util.TreeMap;

public class BitFinderHdlGeneratorFactory extends AbstractHdlGeneratorFactory {

  private static final String NR_OF_INPUT_BITS_STRING = "nrOfInputBits";
  private static final int NR_OF_INPUT_BITS_ID = -1;
  private static final String NR_OF_INDEX_BITS_STRING = "nrOfIndexBits";
  private static final int NR_OF_INDEX_BITS_ID = -2;
  private static final String FIND_MODE_STRING = "findMode";
  private static final int FIND_MODE_ID = -3;

  private static final Map<AttributeOption, Integer> FIND_MODE_MAP =
      Map.of(
          BitFinder.LOW_ONE,
          0,
          BitFinder.HIGH_ONE,
          1,
          BitFinder.LOW_ZERO,
          2,
          BitFinder.HIGH_ZERO,
          3);

  public BitFinderHdlGeneratorFactory() {
    super();
    myParametersList
        .add(NR_OF_INPUT_BITS_STRING, NR_OF_INPUT_BITS_ID)
        .add(
            NR_OF_INDEX_BITS_STRING,
            NR_OF_INDEX_BITS_ID,
            HdlParameters.MAP_CONSTANT,
            1)
        .add(
            FIND_MODE_STRING,
            FIND_MODE_ID,
            HdlParameters.MAP_ATTRIBUTE_OPTION,
            BitFinder.TYPE,
            FIND_MODE_MAP);
    getWiresPortsDuringHDLWriting = true;
  }

  @Override
  public void getGenerationTimeWiresPorts(Netlist theNetlist, AttributeSet attrs) {
    final var inputBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var indexBits = BitFinder.computeOutputBits(inputBits);

    myPorts
        .add(
            Port.INPUT,
            "inputVector",
            inputBits == 1 ? 1 : NR_OF_INPUT_BITS_ID,
            BitFinder.INPUT)
        .add(Port.OUTPUT, "present", 1, BitFinder.PRESENT)
        .add(
            Port.OUTPUT,
            "index",
            indexBits == 1 ? 1 : NR_OF_INDEX_BITS_ID,
            BitFinder.INDEX);

    if (inputBits > 1) {
      myWires
          .addRegister("s_present", 1)
          .addRegister("s_index", indexBits == 1 ? 1 : NR_OF_INDEX_BITS_ID);
    }
  }

  @Override
  protected Map<String, String> getParameterMap(AttributeSet attrs) {
    if (attrs == null) return new TreeMap<>();

    final var parameters = new TreeMap<>(super.getParameterMap(attrs));
    parameters.put(
        NR_OF_INDEX_BITS_STRING,
        Integer.toString(BitFinder.computeOutputBits(attrs.getValue(StdAttr.WIDTH).getWidth())));
    return parameters;
  }

  @Override
  public LineBuffer getModuleFunctionality(Netlist theNetlist, AttributeSet attrs) {
    final var inputBits = attrs.getValue(StdAttr.WIDTH).getWidth();
    final var indexBits = BitFinder.computeOutputBits(inputBits);
    final var contents =
        LineBuffer.getBuffer()
            .pair("findMode", FIND_MODE_STRING)
            .pair("nrOfInputBits", NR_OF_INPUT_BITS_STRING)
            .pair("nrOfIndexBits", NR_OF_INDEX_BITS_STRING)
            .addRemarkBlock(
                "Find modes: 0 = lowest one, 1 = highest one, "
                    + "2 = lowest zero, 3 = highest zero");

    if (inputBits == 1) {
      if (Hdl.isVhdl()) {
        contents.addVhdlKeywords().add(
            """
            present <= inputVector {{when}} {{findMode}} = 0 {{or}} {{findMode}} = 1 {{else}}
                       {{not}} inputVector;
            index   <= '0';
            """);
      } else {
        contents.add(
            """
            assign present = (({{findMode}} == 0) || ({{findMode}} == 1))
                                 ? inputVector : ~inputVector;
            assign index = 1'b0;
            """);
      }
      return contents.empty();
    }

    if (Hdl.isVhdl()) {
      contents
          .addVhdlKeywords()
          .add(
              """
              present <= s_present;
              index   <= s_index;

              findIndex : {{process}}(inputVector) {{is}}
                 {{variable}} found       : boolean;
                 {{variable}} indexValue  : natural {{range}} 0 {{to}} {{nrOfInputBits}} - 1;
                 {{variable}} targetValue : std_logic;
              {{begin}}
                 s_present <= '0';
              """)
          .add(indexBits == 1 ? "   s_index   <= '0';" : "   s_index   <= ({{others}} => '0');")
          .add(
              """
                 found      := false;
                 indexValue := 0;

                 {{if}} {{findMode}} = 0 {{or}} {{findMode}} = 1 {{then}}
                    targetValue := '1';
                 {{else}}
                    targetValue := '0';
                 {{end}} {{if}};

                 {{if}} {{findMode}} = 0 {{or}} {{findMode}} = 2 {{then}}
                    {{for}} bitIndex {{in}} 0 {{to}} {{nrOfInputBits}} - 1 {{loop}}
                       {{if}} {{not}} found {{and}} inputVector(bitIndex) = targetValue {{then}}
                          s_present <= '1';
                          indexValue := bitIndex;
                          found := true;
                       {{end}} {{if}};
                    {{end}} {{loop}};
                 {{else}}
                    {{for}} bitIndex {{in}} {{nrOfInputBits}} - 1 {{downto}} 0 {{loop}}
                       {{if}} {{not}} found {{and}} inputVector(bitIndex) = targetValue {{then}}
                          s_present <= '1';
                          indexValue := bitIndex;
                          found := true;
                       {{end}} {{if}};
                    {{end}} {{loop}};
                 {{end}} {{if}};

                 {{if}} found {{then}}
              """);
      if (indexBits == 1) {
        contents.add(
            """
                    {{if}} indexValue = 0 {{then}}
                       s_index <= '0';
                    {{else}}
                       s_index <= '1';
                    {{end}} {{if}};
            """);
      } else {
        contents.add(
            """
                    s_index <= std_logic_vector(to_unsigned(indexValue, {{nrOfIndexBits}}));
            """);
      }
      contents.add(
          """
                 {{end}} {{if}};
              {{end}} {{process}} findIndex;
              """);
    } else {
      contents.add(
          """
          integer bitIndex;

          assign present = s_present;
          assign index = s_index;

          always @(*)
          begin
             s_present = 1'b0;
          """);
      contents.add(indexBits == 1 ? "   s_index = 1'b0;" : "   s_index = 0;");
      contents.add(
          """
             if (({{findMode}} == 0) || ({{findMode}} == 2))
                begin
                   for (bitIndex = {{nrOfInputBits}} - 1; bitIndex >= 0; bitIndex = bitIndex - 1)
                      begin
                         if (inputVector[bitIndex] ==
                             ((({{findMode}} == 0) || ({{findMode}} == 1)) ? 1'b1 : 1'b0))
                            begin
                               s_present = 1'b1;
                               s_index = bitIndex;
                            end
                      end
                end
             else
                begin
                   for (bitIndex = 0; bitIndex < {{nrOfInputBits}}; bitIndex = bitIndex + 1)
                      begin
                         if (inputVector[bitIndex] ==
                             ((({{findMode}} == 0) || ({{findMode}} == 1)) ? 1'b1 : 1'b0))
                            begin
                               s_present = 1'b1;
                               s_index = bitIndex;
                            end
                      end
                end
          end
          """);
    }
    return contents.empty();
  }
}
