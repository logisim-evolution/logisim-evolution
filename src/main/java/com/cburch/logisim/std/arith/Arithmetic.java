/**
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

public class Arithmetic extends Library {
  private static FactoryDescription[] DESCRIPTIONS = {
    new FactoryDescription("Adder", S.getter("adderComponent"), "adder.gif", "Adder"),
    new FactoryDescription(
        "Subtractor", S.getter("subtractorComponent"), "subtractor.gif", "Subtractor"),
    new FactoryDescription(
        "Multiplier", S.getter("multiplierComponent"), "multiplier.gif", "Multiplier"),
    new FactoryDescription("Divider", S.getter("dividerComponent"), "divider.gif", "Divider"),
    new FactoryDescription("Negator", S.getter("negatorComponent"), "negator.gif", "Negator"),
    new FactoryDescription(
        "Comparator", S.getter("comparatorComponent"), "comparator.gif", "Comparator"),
    new FactoryDescription("Shifter", S.getter("shifterComponent"), "shifter.gif", "Shifter"),
    new FactoryDescription("BitAdder", S.getter("bitAdderComponent"), "bitadder.gif", "BitAdder"),
    new FactoryDescription(
        "BitFinder", S.getter("bitFinderComponent"), "bitfindr.gif", "BitFinder"),
  };

  private List<Tool> tools = null;

  public Arithmetic() {}

  @Override
  public String getDisplayName() {
    return S.get("arithmeticLibrary");
  }

  @Override
  public String getName() {
    return "Arithmetic";
  }

  @Override
  public List<Tool> getTools() {
    if (tools == null) {
      tools = FactoryDescription.getTools(Arithmetic.class, DESCRIPTIONS);
    }
    return tools;
  }

  public boolean removeLibrary(String Name) {
    return false;
  }
}
