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

public class TimelineParam {

	public static String[] units = { "Hz", "KHz", "MHz", "GHz" };

	private String selectedUnit;
	private String signalClkName;
	private int frequency;

	public TimelineParam(String timelineString) {
		String[] elems = timelineString.split(":");
		this.selectedUnit = elems[3];
		this.signalClkName = elems[1];
		this.frequency = Integer.parseInt(elems[2]);
	}

	public TimelineParam(String selectedUnit, String signalClkName,
			int frequency) {
		this.selectedUnit = selectedUnit;
		this.signalClkName = signalClkName;
		this.frequency = frequency;
	}

	public int getFrequency() {
		return frequency;
	}

	public String getSelectedUnit() {
		return selectedUnit;
	}

	public String getSignalClkName() {
		return signalClkName;
	}

	public String toString() {
		return "clk:" + signalClkName + ":" + frequency + ":" + selectedUnit;
	}
}
