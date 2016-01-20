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

package com.cburch.logisim.tools;

import java.util.Collections;
import java.util.List;

import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.util.ListUtil;

public abstract class Library {
	public boolean contains(ComponentFactory query) {
		return indexOf(query) >= 0;
	}

	public boolean containsFromSource(Tool query) {
		for (Tool tool : getTools()) {
			if (tool.sharesSource(query)) {
				return true;
			}
		}
		return false;
	}

	public String getDisplayName() {
		return getName();
	}

	public List<?> getElements() {
		return ListUtil.joinImmutableLists(getTools(), getLibraries());
	}

	public List<Library> getLibraries() {
		return Collections.emptyList();
	}

	public Library getLibrary(String name) {
		for (Library lib : getLibraries()) {
			if (lib.getName().equals(name)) {
				return lib;
			}
		}
		return null;
	}
	
	public abstract boolean removeLibrary(String name);

	public String getName() {
		return getClass().getName();
	}

	public Tool getTool(String name) {
		for (Tool tool : getTools()) {
			if (tool.getName().equals(name)) {
				return tool;
			}
		}
		return null;
	}

	public abstract List<? extends Tool> getTools();

	public int indexOf(ComponentFactory query) {
		int index = -1;
		for (Tool obj : getTools()) {
			index++;
			if (obj instanceof AddTool) {
				AddTool tool = (AddTool) obj;
				if (tool.getFactory() == query)
					return index;
			}
		}
		return -1;
	}

	public boolean isDirty() {
		return false;
	}

	@Override
	public String toString() {
		return getName();
	}

}
