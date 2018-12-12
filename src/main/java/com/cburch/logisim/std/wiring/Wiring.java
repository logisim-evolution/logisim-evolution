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

package com.cburch.logisim.std.wiring;

import java.util.ArrayList;
import java.util.List;

import com.cburch.logisim.circuit.SplitterFactory;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeOption;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.FactoryDescription;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Wiring extends Library {

	static final AttributeOption GATE_TOP_LEFT = new AttributeOption("tl",
			Strings.getter("wiringGateTopLeftOption"));
	static final AttributeOption GATE_BOTTOM_RIGHT = new AttributeOption("br",
			Strings.getter("wiringGateBottomRightOption"));
	static final Attribute<AttributeOption> ATTR_GATE = Attributes.forOption(
			"gate", Strings.getter("wiringGateAttr"), new AttributeOption[] {
					GATE_TOP_LEFT, GATE_BOTTOM_RIGHT });

	private static Tool[] ADD_TOOLS = { new AddTool(SplitterFactory.instance),
			new AddTool(Pin.FACTORY), new AddTool(Probe.FACTORY),
			new AddTool(Tunnel.FACTORY), new AddTool(PullResistor.FACTORY),
			new AddTool(Clock.FACTORY), new AddTool(PowerOnReset.FACTORY) , 
			new AddTool(Constant.FACTORY), };

	private static FactoryDescription[] DESCRIPTIONS = {
			new FactoryDescription("Power", Strings.getter("powerComponent"),
					"power.gif", "Power"),
			new FactoryDescription("Ground", Strings.getter("groundComponent"),
					"ground.gif", "Ground"),
			new FactoryDescription("Transistor",
					Strings.getter("transistorComponent"), "trans0.gif",
					"Transistor"),
			new FactoryDescription("Transmission Gate",
					Strings.getter("transmissionGateComponent"),
					"transmis.gif", "TransmissionGate"),
			new FactoryDescription("Bit Extender",
					Strings.getter("extenderComponent"), "extender.gif",
					"BitExtender"), };

	private List<Tool> tools = null;

	public Wiring() {
	}

	@Override
	public String getDisplayName() {
		return Strings.get("wiringLibrary");
	}

	@Override
	public String getName() {
		return "Wiring";
	}

	@Override
	public List<Tool> getTools() {
		if (tools == null) {
			List<Tool> ret = new ArrayList<Tool>(ADD_TOOLS.length
					+ DESCRIPTIONS.length);
			for (Tool a : ADD_TOOLS) {
				ret.add(a);
			}
			ret.addAll(FactoryDescription.getTools(Wiring.class, DESCRIPTIONS));
			tools = ret;
		}
		return tools;
	}
	public boolean removeLibrary(String Name) {
		return false;
	}
}
