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

package com.cburch.logisim.data;

import java.awt.Window;

import javax.swing.JTextField;

import com.cburch.logisim.util.StringGetter;

public abstract class Attribute<V> {
	private String name;
	private StringGetter disp;
	private boolean hidden = false;
	
	public Attribute() {
		hidden = true;
		name = "Dummy";
	}

	public Attribute(String name, StringGetter disp) {
		this.name = name;
		this.disp = disp;
	}

	protected java.awt.Component getCellEditor(V value) {
		return new JTextField(toDisplayString(value));
	}

	public java.awt.Component getCellEditor(Window source, V value) {
		return getCellEditor(value);
	}

	public String getDisplayName() {
		return (disp != null) ? disp.toString() : name;
	}

	public String getName() {
		return name;
	}

	public abstract V parse(String value);

	public String toDisplayString(V value) {
		return value == null ? "" : value.toString();
	}

	public String toStandardString(V value) {
		String oldString = value.toString();
		String newString = oldString.replaceAll("[\u0000-\u001f]", "");
		newString = newString.replaceAll("&#.*?;", "");
		return newString;
	}
	
	public boolean isHidden() {
		return hidden;
	}

	@Override
	public String toString() {
		return name;
	}
}
