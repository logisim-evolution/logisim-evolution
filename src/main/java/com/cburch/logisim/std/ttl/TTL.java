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
 *******************************************************************************/

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import java.util.List;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class TTL extends Library {
	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("7400", S.getter("7400: quad 2-input NAND gate"), "ttl.gif", "Ttl7400"),
			new FactoryDescription("7402", S.getter("7402: quad 2-input NOR gate"), "ttl.gif", "Ttl7402"),
			new FactoryDescription("7404", S.getter("7404: hex inverter"), "ttl.gif", "Ttl7404"),
			new FactoryDescription("7408", S.getter("7408: quad 2-input AND gate"), "ttl.gif", "Ttl7408"),
			new FactoryDescription("7410", S.getter("7410: triple 3-input NAND gate"), "ttl.gif", "Ttl7410"),
			new FactoryDescription("7411", S.getter("7411: triple 3-input AND gate"), "ttl.gif", "Ttl7411"),
			new FactoryDescription("7413", S.getter("7413: dual 4-input NAND gate(schmitt-trigger)"), "ttl.gif", "Ttl7413"),
			new FactoryDescription("7414", S.getter("7414: hex inverter (schmitt-trigger)"), "ttl.gif", "Ttl7414"),
			new FactoryDescription("7418", S.getter("7418: dual 4-input NAND gate(schmitt-trigger)"), "ttl.gif", "Ttl7418"),
			new FactoryDescription("7419", S.getter("7419: hex inverter (schmitt-trigger)"), "ttl.gif", "Ttl7419"),
			new FactoryDescription("7420", S.getter("7420: dual 4-input NAND gate"), "ttl.gif", "Ttl7420"),
			new FactoryDescription("7421", S.getter("7421: dual 4-input AND gate"), "ttl.gif", "Ttl7421"),
			new FactoryDescription("7424", S.getter("7424: quad 2-input NAND gate (schmitt-trigger)"), "ttl.gif", "Ttl7424"),
			new FactoryDescription("7427", S.getter("7427: triple 3-input NOR gate"), "ttl.gif", "Ttl7427"),
			new FactoryDescription("7430", S.getter("7430: single 8-input NAND gate"), "ttl.gif", "Ttl7430"),
			new FactoryDescription("7432", S.getter("7432: quad 2-input OR gate"), "ttl.gif", "Ttl7432"),
			new FactoryDescription("7436", S.getter("7436: quad 2-input NOR gate"), "ttl.gif", "Ttl7436"),
			new FactoryDescription("7442", S.getter("7442: BCD to decimal decoder"), "ttl.gif", "Ttl7442"),
			new FactoryDescription("7443", S.getter("7443: Excess-3 to decimal decoder"), "ttl.gif", "Ttl7443"),
			new FactoryDescription("7444", S.getter("7444: Gray to decimal decoder"), "ttl.gif", "Ttl7444"),
			new FactoryDescription("7447", S.getter("7447: BCD to 7-segment decoder"), "ttl.gif", "Ttl7447"),
			new FactoryDescription("7451", S.getter("7451: dual AND-OR-INVERT gate"), "ttl.gif", "Ttl7451"),
			new FactoryDescription("7454", S.getter("7454: Four wide AND-OR-INVERT gate"), "ttl.gif", "Ttl7454"),
			new FactoryDescription("7458", S.getter("7458: dual AND-OR gate"), "ttl.gif", "Ttl7458"),
			new FactoryDescription("7464", S.getter("7464: 4-2-3-2 AND-OR-INVERT gate"), "ttl.gif", "Ttl7464"),
			new FactoryDescription("7474", S.getter("7474: dual D-Flipflops with preset and clear"), "ttl.gif", "Ttl7474"),
			new FactoryDescription("7485", S.getter("7485: 4-bit magnitude comparator"), "ttl.gif", "Ttl7485"),
			new FactoryDescription("7486", S.getter("7486: quad 2-input XOR gate"), "ttl.gif", "Ttl7486"),
			new FactoryDescription("74125",S.getter("74125: quad bus buffer, three-state outputs, negative enable"), "ttl.gif", "Ttl74125"),
			new FactoryDescription("74175",S.getter("74175: quad D-flipflop, asynchronous reset"), "ttl.gif", "Ttl74175"),
			new FactoryDescription("74165", S.getter("74165: 8-bit parallel-to-serial shift register"), "ttl.gif", "Ttl74165"),
			new FactoryDescription("74266", S.getter("74266: quad 2-input XNOR gate"), "ttl.gif", "Ttl74266"), 
			new FactoryDescription("74273", S.getter("74273: octal D-Flipflop with clear"), "ttl.gif", "Ttl74273"),
			new FactoryDescription("74283", S.getter("74283: 4-bit binary full adder"), "ttl.gif", "Ttl74283"),
			new FactoryDescription("74377", S.getter("74377: octal D-Flipflop with enable"), "ttl.gif", "Ttl74377"),
			};

	static final Attribute<Boolean> VCC_GND = Attributes.forBoolean("VccGndPorts", S.getter("VccGndPorts"));
	static final Attribute<Boolean> DRAW_INTERNAL_STRUCTURE = Attributes.forBoolean("ShowInternalStructure",
			S.getter("ShowInternalStructure"));

	private List<Tool> tools = null;

	public TTL() {
	}

	@Override
	public String getName() {
		return "TTL";
	}

	@Override
	public List<? extends Tool> getTools() {
		if (tools == null) {
			tools = FactoryDescription.getTools(TTL.class, DESCRIPTIONS);
		}
		return tools;
	}

	@Override
	public boolean removeLibrary(String name) {
		// TODO Auto-generated method stub
		return false;
	}

}
