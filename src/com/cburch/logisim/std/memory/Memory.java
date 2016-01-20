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

package com.cburch.logisim.std.memory;

import java.util.List;

import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Memory extends Library {
	protected static final int DELAY = 5;

	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("D Flip-Flop",
					Strings.getter("dFlipFlopComponent"), "dFlipFlop.gif",
					"DFlipFlop"),
			new FactoryDescription("T Flip-Flop",
					Strings.getter("tFlipFlopComponent"), "tFlipFlop.gif",
					"TFlipFlop"),
			new FactoryDescription("J-K Flip-Flop",
					Strings.getter("jkFlipFlopComponent"), "jkFlipFlop.gif",
					"JKFlipFlop"),
			new FactoryDescription("S-R Flip-Flop",
					Strings.getter("srFlipFlopComponent"), "srFlipFlop.gif",
					"SRFlipFlop"),
			new FactoryDescription("Register",
					Strings.getter("registerComponent"), "register.gif",
					"Register"),
			new FactoryDescription("Counter",
					Strings.getter("counterComponent"), "counter.gif",
					"Counter"),
			new FactoryDescription("Shift Register",
					Strings.getter("shiftRegisterComponent"), "shiftreg.gif",
					"ShiftRegister"),
			new FactoryDescription("Random", Strings.getter("randomComponent"),
					"random.gif", "Random"),
			new FactoryDescription("RAM", Strings.getter("ramComponent"),
					"ram.gif", "Ram"),
			new FactoryDescription("ROM", Strings.getter("romComponent"),
					"rom.gif", "Rom"), };

	private List<Tool> tools = null;

	public Memory() {
	}

	@Override
	public String getDisplayName() {
		return Strings.get("memoryLibrary");
	}

	@Override
	public String getName() {
		return "Memory";
	}

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(Memory.class, DESCRIPTIONS);
		}
		return tools;
	}

	public boolean removeLibrary(String Name) {
		return false;
	}
}
