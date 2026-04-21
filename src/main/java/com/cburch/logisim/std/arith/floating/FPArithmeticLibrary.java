/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.arith.floating;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class FPArithmeticLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "FPArithmetic";

  private static final FactoryDescription[] DESCRIPTIONS = {
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
    new FactoryDescription(IntToFp.class, S.getter("intToFPComponent"))
  };

  private List<Tool> tools = null;

  @Override
  public String getDisplayName() {
    return S.get("fpArithmeticLibrary");
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(FPArithmeticLibrary.class, DESCRIPTIONS);
    }
    return tools;
  }
}
