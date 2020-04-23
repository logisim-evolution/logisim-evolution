/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
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
 */

package com.cburch.logisim.fpga.menu;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.BoardEditor;
import com.cburch.logisim.fpga.gui.FPGACommander;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

@SuppressWarnings("serial")
public class MenuFPGA extends JMenu implements ActionListener {
  private Project ThisCircuit;
  private JMenuItem BoardEditor = new JMenuItem();
  private JMenuItem FPGACommander = new JMenuItem();
  private BoardEditor Editor = null;
  private FPGACommander Commander = null;

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
        Editor = new BoardEditor();
      } else {
        if (!Editor.isActive()) {
          Editor.setActive();
        }
      }
    } else if (src == FPGACommander) {
      if (Commander == null) Commander = new FPGACommander(ThisCircuit);
      Commander.ShowGui();
    }
  }

  public void localeChanged() {
    this.setText(S.get("FPGAMenu"));
    BoardEditor.setText(S.get("FPGABoardEditor"));
    FPGACommander.setText(S.get("FPGACommander"));
  }
}
