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

package com.cburch.logisim.std.gates;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;

public class Gates extends Library {
	private List<Tool> tools = null;

	public Gates() {
		tools = Arrays.asList(new Tool[] { new AddTool(NotGate.FACTORY),
				new AddTool(Buffer.FACTORY), new AddTool(AndGate.FACTORY),
				new AddTool(OrGate.FACTORY), new AddTool(NandGate.FACTORY),
				new AddTool(NorGate.FACTORY), new AddTool(XorGate.FACTORY),
				new AddTool(XnorGate.FACTORY),
				new AddTool(OddParityGate.FACTORY),
				new AddTool(EvenParityGate.FACTORY),
				new AddTool(ControlledBuffer.FACTORY_BUFFER),
				new AddTool(ControlledBuffer.FACTORY_INVERTER), });
	}

	@Override
	public String getDisplayName() {
		return Strings.get("gatesLibrary");
	}

	@Override
	public String getName() {
		return "Gates";
	}

	@Override
	public List<Tool> getTools() {
		return tools;
	}

	public boolean removeLibrary(String Name) {
		return false;
	}
}
