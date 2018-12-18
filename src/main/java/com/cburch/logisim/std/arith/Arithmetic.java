/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim.std.arith;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Arithmetic extends Library {
	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("Adder", Strings.getter("adderComponent"),
					"adder.gif", "Adder"),
			new FactoryDescription("Subtractor",
					Strings.getter("subtractorComponent"), "subtractor.gif",
					"Subtractor"),
			new FactoryDescription("Multiplier",
					Strings.getter("multiplierComponent"), "multiplier.gif",
					"Multiplier"),
			new FactoryDescription("Divider",
					Strings.getter("dividerComponent"), "divider.gif",
					"Divider"),
			new FactoryDescription("Negator",
					Strings.getter("negatorComponent"), "negator.gif",
					"Negator"),
			new FactoryDescription("Comparator",
					Strings.getter("comparatorComponent"), "comparator.gif",
					"Comparator"),
			new FactoryDescription("Shifter",
					Strings.getter("shifterComponent"), "shifter.gif",
					"Shifter"),
			new FactoryDescription("BitAdder",
					Strings.getter("bitAdderComponent"), "bitadder.gif",
					"BitAdder"),
			new FactoryDescription("BitFinder",
					Strings.getter("bitFinderComponent"), "bitfindr.gif",
					"BitFinder"),};

	private List<Tool> tools = null;

	public Arithmetic() {
	}

	@Override
	public String getDisplayName() {
		return Strings.get("arithmeticLibrary");
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
