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

package com.cburch.gray;

import java.util.Arrays;
import java.util.List;

import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;

/** The library of components that the user can access. */
public class Components extends Library {
	/**
	 * The list of all tools contained in this library. Technically, libraries
	 * contain tools, which is a slightly more general concept than components;
	 * practically speaking, though, you'll most often want to create AddTools
	 * for new components that can be added into the circuit.
	 */
	private List<AddTool> tools;

	/**
	 * Constructs an instance of this library. This constructor is how Logisim
	 * accesses first when it opens the JAR file: It looks for a no-arguments
	 * constructor method of the user-designated class.
	 */
	public Components() {
		tools = Arrays.asList(new AddTool[] {
				new AddTool(new GrayIncrementer()),
				new AddTool(new SimpleGrayCounter()),
				new AddTool(new GrayCounter()), });
	}

	/** Returns the name of the library that the user will see. */
	@Override
	public String getDisplayName() {
		return "Gray Tools";
	}

	/** Returns a list of all the tools available in this library. */
	@Override
	public List<AddTool> getTools() {
		return tools;
	}

	public boolean removeLibrary(String Name) {
		return false;
	}
}
