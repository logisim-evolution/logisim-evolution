/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.menu;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.gui.BoardEditor;
import com.cburch.logisim.fpga.gui.FpgaCommander;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

@SuppressWarnings("serial")
public class MenuFpga extends JMenu implements ActionListener {
  private final Project ThisCircuit;
  private final JMenuItem BoardEditor = new JMenuItem();
  private final JMenuItem FPGACommander = new JMenuItem();
  private BoardEditor Editor = null;
  private FpgaCommander Commander = null;

  public MenuFpga(JFrame parent, LogisimMenuBar menubar, Project proj) {
    ThisCircuit = proj;

    BoardEditor.addActionListener(this);
    FPGACommander.addActionListener(this);

    add(BoardEditor);
    add(FPGACommander);
    setEnabled(parent instanceof Frame);
  }

  @Override
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
      if (Commander == null) Commander = new FpgaCommander(ThisCircuit);
      Commander.showGui();
    }
  }

  public void localeChanged() {
    this.setText(S.get("FPGAMenu"));
    BoardEditor.setText(S.get("FPGABoardEditor"));
    FPGACommander.setText(S.get("FPGACommander"));
  }
}
