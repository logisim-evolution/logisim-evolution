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
package com.hepia.logisim.chronodata;

import java.util.ArrayList;

import javax.swing.ImageIcon;

import com.cburch.logisim.util.Icons;

/**
 * Contains all data about one signal: signal values, the selected value, the
 * choosed format...
 */
public class SignalData {

	private String name;
	protected int selectedValuePos = 0;
	protected ArrayList<String> data;

	public SignalData(String name, ArrayList<String> data) {
		this.name = name;
		this.data = data;
	}

	public ImageIcon getIcon() {
		return (ImageIcon) Icons.getIcon("chronoSignal.gif");
	}

	public String getName() {
		return name;
	}

	public String getSelectedValue() {
		return data.size() > 0 ? data.get(selectedValuePos) : "";
	}

	public ArrayList<String> getSignalValues() {
		return data;
	}

	public void setSelectedValuePos(int pos) {
		if (pos < data.size() - 1)
			selectedValuePos = pos;
	}

}
