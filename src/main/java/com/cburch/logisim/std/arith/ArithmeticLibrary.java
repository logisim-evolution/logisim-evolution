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
    new FactoryDescription(BitFinder.class, S.getter("bitFinderComponent"), "bitfindr.gif")
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
