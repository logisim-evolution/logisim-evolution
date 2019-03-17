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

package com.cburch.logisim.fpga.fpgagui;

import com.cburch.logisim.fpga.designrulecheck.SimpleDRCContainer;

public class FPGAReportGui extends FPGAReport {
	private FPGAReportTabbedPane myCommander = null;

	public FPGAReportGui(FPGACommanderGui parent) {
		myCommander = parent.getReporterGui();
	}

	@Override
	public void AddErrorIncrement(String Message) {
		myCommander.AddErrors(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_NORMAL,true));
	}

	@Override
	public void AddError(Object Message) {
		if (Message instanceof String)
			myCommander.AddErrors(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_NORMAL));
		else
			myCommander.AddErrors(Message);
	}

	@Override
	public void AddFatalError(String Message) {
		myCommander.AddErrors(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_FATAL));
	}

	@Override
	public void AddSevereError(String Message) {
		myCommander.AddErrors(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_SEVERE));
	}

	@Override
	public void AddInfo(String Message) {
		myCommander.AddInfo(Message);
	}

	@Override
	public void AddSevereWarning(String Message) {
		myCommander.AddWarning(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_SEVERE));
	}

	@Override
	public void AddWarningIncrement(String Message) {
		myCommander.AddWarning(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_NORMAL,true));
	}

	@Override
	public void AddWarning(Object Message) {
		if (Message instanceof String)
			myCommander.AddWarning(new SimpleDRCContainer(Message,SimpleDRCContainer.LEVEL_NORMAL));
		else
			myCommander.AddWarning(Message);
	}

	@Override
	public void ClsScr() {
		myCommander.ClearConsole();
	}

	@Override
	public void print(String Message) {
		myCommander.AddConsole(Message);
	}
}
