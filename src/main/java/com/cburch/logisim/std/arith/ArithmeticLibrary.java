/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.std.arith;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import java.util.List;

public class ArithmeticLibrary extends Library {
  /**
   * Unique identifier of the library, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all libraries.
   */
  public static final String _ID = "Arithmetic";

  private static final FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription(Adder.class, S.getter("adderComponent"), "adder.gif"),
    new FactoryDescription(Subtractor.class, S.getter("subtractorComponent"), "subtractor.gif"),
    new FactoryDescription(Multiplier.class, S.getter("multiplierComponent"), "multiplier.gif"),
    new FactoryDescription(Divider.class, S.getter("dividerComponent"), "divider.gif"),
    new FactoryDescription(Negator.class, S.getter("negatorComponent"), "negator.gif"),
    new FactoryDescription(Comparator.class, S.getter("comparatorComponent"), "comparator.gif"),
    new FactoryDescription(Shifter.class, S.getter("shifterComponent"), "shifter.gif"),
    new FactoryDescription(BitAdder.class, S.getter("bitAdderComponent"), "bitadder.gif"),
    new FactoryDescription(BitFinder.class, S.getter("bitFinderComponent"), "bitfindr.gif"),
    new FactoryDescription(FPAdder.class, S.getter("fpAdderComponent"), "adder.gif"),
    new FactoryDescription(FPSubtractor.class, S.getter("fpSubtractorComponent"), "subtractor.gif"),
    new FactoryDescription(FPMultiplier.class, S.getter("fpMultiplierComponent"), "multiplier.gif"),
    new FactoryDescription(FPDivider.class, S.getter("fpDividerComponent"), "divider.gif"),
    new FactoryDescription(FPNegator.class, S.getter("fpNegatorComponent"), "negator.gif"),
    new FactoryDescription(FPComparator.class, S.getter("fpComparatorComponent"), "comparator.gif"),
    new FactoryDescription(FPToInt.class, S.getter("fpToIntComponent")),
    new FactoryDescription(IntToFP.class, S.getter("intToFPComponent")),
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

  public boolean removeLibrary(String Name) {
    return false;
  }
}
