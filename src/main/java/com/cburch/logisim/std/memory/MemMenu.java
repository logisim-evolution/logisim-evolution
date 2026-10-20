/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class MemMenu implements ActionListener, MenuExtender {
  private final Mem factory;
  private final Instance instance;
  private Project proj;
  private Frame frame;
  private CircuitState circState;
  private JMenuItem edit;
  private JMenuItem clear;
  private JMenuItem load;
  private JMenuItem save;

  MemMenu(Mem factory, Instance instance) {
    this.factory = factory;
    this.instance = instance;
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Object src = evt.getSource();
    if (src == edit) doEdit();
    else if (src == clear) doClear();
    else if (src == load) doLoad();
    else if (src == save) doSave();
  }

  @Override
  public void configureMenu(JPopupMenu menu, Project proj) {
    this.proj = proj;
    this.frame = proj.getFrame();
    this.circState = proj.getCircuitState();

    Object attrs = instance.getAttributeSet();
    if (attrs instanceof RomAttributes) {
      ((RomAttributes) attrs).setProject(proj);
    }

    var enabled = circState != null;
    edit = createItem(enabled, S.get("ramEditMenuItem"));
    clear = createItem(enabled, S.get("ramClearMenuItem"));
    load = createItem(enabled, S.get("ramLoadMenuItem"));
    save = createItem(enabled, S.get("ramSaveMenuItem"));

    menu.addSeparator();
    menu.add(edit);
    menu.add(clear);
    menu.add(load);
    menu.add(save);
  }

  private JMenuItem createItem(boolean enabled, String label) {
    final var ret = new JMenuItem(label);
    ret.setEnabled(enabled);
    ret.addActionListener(this);
    return ret;
  }

  private void doClear() {
    final var s = factory.getState(instance, circState);
    final var isAllZero = s.getContents().isClear();
    if (isAllZero) return;

    int choice =
        OptionPane.showConfirmDialog(
            frame,
            S.get("ramConfirmClearMsg"),
            S.get("ramConfirmClearTitle"),
            OptionPane.YES_NO_OPTION);
    if (choice == OptionPane.YES_OPTION) {
      s.getContents().clear();
    }
  }

  private void doEdit() {
    if (factory.getState(instance, circState) == null) return;
    final var frame = factory.getHexFrame(proj, instance, circState);
    frame.setVisible(true);
    frame.toFront();
  }

  private void doLoad() {
    final var m = factory.getState(instance, circState).getContents();
    HexFile.open(m, frame, proj, instance);
  }

  private void doSave() {
    final var m = factory.getState(instance, circState).getContents();
    HexFile.save(m, frame, proj, instance);
  }
}
