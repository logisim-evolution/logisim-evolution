/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JMenu;
import javax.swing.JMenuItem;

class MenuProject extends Menu {
  private static final long serialVersionUID = 1L;
  private final LogisimMenuBar menubar;
  private final MyListener myListener = new MyListener();
  private final MenuItemImpl addCircuit = new MenuItemImpl(this, LogisimMenuBar.ADD_CIRCUIT);
  private final MenuItemImpl addVhdl = new MenuItemImpl(this, LogisimMenuBar.ADD_VHDL);
  private final MenuItemImpl importVhdl = new MenuItemImpl(this, LogisimMenuBar.IMPORT_VHDL);
  private final JMenu loadLibrary = new JMenu();
  private final JMenuItem loadBuiltin = new JMenuItem();
  private final JMenuItem loadLogisim = new JMenuItem();
  private final JMenuItem loadJar = new JMenuItem();
  private final JMenuItem unload = new JMenuItem();
  private final MenuItemImpl moveUp = new MenuItemImpl(this, LogisimMenuBar.MOVE_CIRCUIT_UP);
  private final MenuItemImpl moveDown = new MenuItemImpl(this, LogisimMenuBar.MOVE_CIRCUIT_DOWN);
  private final MenuItemImpl remove = new MenuItemImpl(this, LogisimMenuBar.REMOVE_CIRCUIT);
  private final MenuItemImpl setAsMain = new MenuItemImpl(this, LogisimMenuBar.SET_MAIN_CIRCUIT);
  private final MenuItemImpl revertAppearance =
      new MenuItemImpl(this, LogisimMenuBar.REVERT_APPEARANCE);
  private final MenuItemImpl layout = new MenuItemImpl(this, LogisimMenuBar.EDIT_LAYOUT);
  private final MenuItemImpl appearance = new MenuItemImpl(this, LogisimMenuBar.EDIT_APPEARANCE);
  private final MenuItemImpl toggleLayoutAppearance =
      new MenuItemImpl(this, LogisimMenuBar.TOGGLE_APPEARANCE);
  private final MenuItemImpl analyze = new MenuItemImpl(this, LogisimMenuBar.ANALYZE_CIRCUIT);
  private final MenuItemImpl stats = new MenuItemImpl(this, LogisimMenuBar.CIRCUIT_STATS);
  private final JMenuItem options = new JMenuItem();

  MenuProject(LogisimMenuBar menubar) {
    this.menubar = menubar;

    menubar.registerItem(LogisimMenuBar.ADD_CIRCUIT, addCircuit);
    menubar.registerItem(LogisimMenuBar.ADD_VHDL, addVhdl);
    menubar.registerItem(LogisimMenuBar.IMPORT_VHDL, importVhdl);
    loadBuiltin.addActionListener(myListener);
    loadLogisim.addActionListener(myListener);
    loadJar.addActionListener(myListener);
    unload.addActionListener(myListener);
    menubar.registerItem(LogisimMenuBar.MOVE_CIRCUIT_UP, moveUp);
    menubar.registerItem(LogisimMenuBar.MOVE_CIRCUIT_DOWN, moveDown);
    menubar.registerItem(LogisimMenuBar.SET_MAIN_CIRCUIT, setAsMain);
    menubar.registerItem(LogisimMenuBar.REMOVE_CIRCUIT, remove);
    menubar.registerItem(LogisimMenuBar.REVERT_APPEARANCE, revertAppearance);
    menubar.registerItem(LogisimMenuBar.EDIT_LAYOUT, layout);
    menubar.registerItem(LogisimMenuBar.EDIT_APPEARANCE, appearance);
    menubar.registerItem(LogisimMenuBar.TOGGLE_APPEARANCE, toggleLayoutAppearance);
    menubar.registerItem(LogisimMenuBar.ANALYZE_CIRCUIT, analyze);
    menubar.registerItem(LogisimMenuBar.CIRCUIT_STATS, stats);
    options.addActionListener(myListener);

    loadLibrary.add(loadBuiltin);
    loadLibrary.add(loadLogisim);
    loadLibrary.add(loadJar);

    add(addCircuit);
    add(addVhdl);
    add(importVhdl);
    add(loadLibrary);
    add(unload);
    addSeparator();
    add(moveUp);
    add(moveDown);
    add(setAsMain);
    add(remove);
    add(revertAppearance);
    addSeparator();
    add(layout);
    add(appearance);
    addSeparator();
    add(analyze);
    add(stats);
    addSeparator();
    add(options);

    final var known = menubar.getSaveProject() != null;
    loadLibrary.setEnabled(known);
    loadBuiltin.setEnabled(known);
    loadLogisim.setEnabled(known);
    loadJar.setEnabled(known);
    unload.setEnabled(known);
    options.setEnabled(known);
    computeEnabled();
  }

  @Override
  void computeEnabled() {
    setEnabled(
        menubar.getSaveProject() != null
            || addCircuit.hasListeners()
            || addVhdl.hasListeners()
            || importVhdl.hasListeners()
            || moveUp.hasListeners()
            || moveDown.hasListeners()
            || setAsMain.hasListeners()
            || remove.hasListeners()
            || layout.hasListeners()
            || revertAppearance.hasListeners()
            || appearance.hasListeners()
            || analyze.hasListeners()
            || stats.hasListeners());
    menubar.fireEnableChanged();
  }

  public void localeChanged() {
    setText(S.get("projectMenu"));
    addCircuit.setText(S.get("projectAddCircuitItem"));
    addVhdl.setText(S.get("projectAddVhdlItem"));
    importVhdl.setText(S.get("projectImportVhdlItem"));
    loadLibrary.setText(S.get("projectLoadLibraryItem"));
    loadBuiltin.setText(S.get("projectLoadBuiltinItem"));
    loadLogisim.setText(S.get("projectLoadLogisimItem"));
    loadJar.setText(S.get("projectLoadJarItem"));
    unload.setText(S.get("projectUnloadLibrariesItem"));
    moveUp.setText(S.get("projectMoveCircuitUpItem"));
    moveDown.setText(S.get("projectMoveCircuitDownItem"));
    setAsMain.setText(S.get("projectSetAsMainItem"));
    remove.setText(S.get("projectRemoveCircuitItem"));
    revertAppearance.setText(S.get("projectRevertAppearanceItem"));
    layout.setText(S.get("projectEditCircuitLayoutItem"));
    appearance.setText(S.get("projectEditCircuitAppearanceItem"));
    toggleLayoutAppearance.setText(S.get("projectToggleCircuitAppearanceItem"));
    analyze.setText(S.get("projectAnalyzeCircuitItem"));
    stats.setText(S.get("projectGetCircuitStatisticsItem"));
    options.setText(S.get("projectOptionsItem"));
  }

  private class MyListener implements ActionListener {
    @Override
    public void actionPerformed(ActionEvent event) {
      final var src = event.getSource();
      final var proj = menubar.getSaveProject();
      if (proj == null) return;
      if (src == loadBuiltin) {
        ProjectLibraryActions.doLoadBuiltinLibrary(proj);
      } else if (src == loadLogisim) {
        ProjectLibraryActions.doLoadLogisimLibrary(proj);
      } else if (src == loadJar) {
        ProjectLibraryActions.doLoadJarLibrary(proj);
      } else if (src == unload) {
        ProjectLibraryActions.doUnloadLibraries(proj);
      } else if (src == options) {
        proj.getOptionsFrame().setVisible(true);
      }
    }
  }
}
