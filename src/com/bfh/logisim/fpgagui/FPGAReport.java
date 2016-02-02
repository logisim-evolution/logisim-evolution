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

package com.bfh.logisim.fpgagui;

public class FPGAReport {
	private FPGACommanderGui myCommander;

	public FPGAReport(FPGACommanderGui parrent) {
		myCommander = parrent;
	}

	public void AddError(Object Message) {
		myCommander.AddErrors(Message);
	}

	public void AddFatalError(String Message) {
		myCommander.AddErrors("***FATAL*** " + Message);
	}

	public void AddInfo(Object Message) {
		myCommander.AddInfo(Message);
	}

	public void AddSevereError(String Message) {
		myCommander.AddErrors("**SEVERE** " + Message);
	}

	public void AddSevereWarning(String Message) {
		myCommander.AddWarning("**SEVERE** " + Message);
	}

	public void AddWarning(Object Message) {
		myCommander.AddWarning(Message);
	}

	public void ClsScr() {
		myCommander.ClearConsole();
	}

	public void print(String Message) {
		myCommander.AddConsole(Message);
	}
}
