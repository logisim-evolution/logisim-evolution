/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class ArithmeticLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Arithmetic";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Adder.class, S.getter("adderComponent"), "adder.gif"),
    new FactoryDescription(Subtractor.class, S.getter("subtractorComponent"), "subtractor.gif"),
    new FactoryDescription(Multiplier.class, S.getter("multiplierComponent"), "multiplier.gif"),
    new FactoryDescription(Divider.class, S.getter("dividerComponent"), "divider.gif"),
    new FactoryDescription(Negator.class, S.getter("negatorComponent"), "negator.gif"),
    new FactoryDescription(Exponentiator.class, S.getter("exponentiatorComponent"), "exponentiator.gif"),
    new FactoryDescription(SquareRoot.class, S.getter("squareRootComponent"), "squareroot.gif"),
    new FactoryDescription(Absolute.class, S.getter("absoluteComponent"), "absolute.gif"),
    new FactoryDescription(Comparator.class, S.getter("comparatorComponent"), "comparator.gif"),
    new FactoryDescription(MinMax.class, S.getter("minMaxComponent"), "minmax.gif"),
    new FactoryDescription(Shifter.class, S.getter("shifterComponent"), "shifter.gif"),
    new FactoryDescription(BitAdder.class, S.getter("bitAdderComponent"), "bitadder.gif"),
    new FactoryDescription(BitFinder.class, S.getter("bitFinderComponent"), "bitfindr.gif"),
    new FactoryDescription(FpAdder.class, S.getter("fpAdderComponent"), "adder.gif"),
    new FactoryDescription(FpSubtractor.class, S.getter("fpSubtractorComponent"), "subtractor.gif"),
    new FactoryDescription(FpMultiplier.class, S.getter("fpMultiplierComponent"), "multiplier.gif"),
    new FactoryDescription(FpDivider.class, S.getter("fpDividerComponent"), "divider.gif"),
    new FactoryDescription(FpNegator.class, S.getter("fpNegatorComponent"), "negator.gif"),
    new FactoryDescription(FpExponentiator.class, S.getter("fpExponentiatorComponent"), "exponentiator.gif"),
    new FactoryDescription(FpLogarithm.class, S.getter("fpLogarithmComponent"), "logarithm.gif"),
    new FactoryDescription(FpSquareRoot.class, S.getter("fpSquareRootComponent"), "squareroot.gif"),
    new FactoryDescription(FpAbsolute.class, S.getter("fpAbsoluteComponent"), "absolute.gif"),
    new FactoryDescription(FpComparator.class, S.getter("fpComparatorComponent"), "comparator.gif"),
    new FactoryDescription(FpMinMax.class, S.getter("fpMinMaxComponent"), "minmax.gif"),
    new FactoryDescription(FpRound.class, S.getter("fpRoundComponent"), "round.gif"),
    new FactoryDescription(FpTrigonometry.class, S.getter("fpTrigonometryComponent"), "trigonometry.gif"),
    new FactoryDescription(FpClassificator.class, S.getter("fpClassificatorComponent"), "fpclassificator.gif"),
    new FactoryDescription(FpToFp.class, S.getter("fpToFpComponent")),
    new FactoryDescription(FpToInt.class, S.getter("fpToIntComponent")),
    new FactoryDescription(IntToFp.class, S.getter("intToFPComponent")),
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("arithmeticLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(ArithmeticLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
