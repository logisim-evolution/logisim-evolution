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

package com.bfh.logisim.fpgamenu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

import com.bfh.logisim.fpgaboardeditor.BoardDialog;
import com.bfh.logisim.fpgagui.FPGACommanderGui;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;

@SuppressWarnings("serial")
public class MenuFPGA extends JMenu implements ActionListener {
	private Project ThisCircuit;
	private JMenuItem BoardEditor = new JMenuItem();
	private JMenuItem FPGACommander = new JMenuItem();
	private BoardDialog Editor = null;
	private FPGACommanderGui Commander = null;

	public MenuFPGA(JFrame parent, LogisimMenuBar menubar, Project proj) {
		ThisCircuit = proj;

		BoardEditor.addActionListener(this);
		FPGACommander.addActionListener(this);

		add(BoardEditor);
		add(FPGACommander);
		setEnabled(parent instanceof Frame);
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == BoardEditor) {
			if (Editor == null) {
				Editor = new BoardDialog();
			} else {
				if (!Editor.isActive()) {
					Editor.setActive();
				}
			}
		} else if (src == FPGACommander) {
			if (Commander == null)
				Commander = new FPGACommanderGui(ThisCircuit);
			Commander.ShowGui();
		}
	}

	public void localeChanged() {
		this.setText(Strings.get("FPGAMenu"));
		BoardEditor.setText(Strings.get("BoardEditor"));
		FPGACommander.setText(Strings.get("FPGA Commander"));
	}

}
