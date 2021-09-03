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

import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

class MenuEdit extends Menu {
  private static final long serialVersionUID = 1L;
  private final LogisimMenuBar menubar;
  private final JMenuItem undo = new JMenuItem();
  private final JMenuItem redo = new JMenuItem();
  private final MenuItemImpl cut = new MenuItemImpl(this, LogisimMenuBar.CUT);
  private final MenuItemImpl copy = new MenuItemImpl(this, LogisimMenuBar.COPY);
  private final MenuItemImpl paste = new MenuItemImpl(this, LogisimMenuBar.PASTE);
  private final MenuItemImpl delete = new MenuItemImpl(this, LogisimMenuBar.DELETE);
  private final MenuItemImpl dup = new MenuItemImpl(this, LogisimMenuBar.DUPLICATE);
  private final MenuItemImpl selall = new MenuItemImpl(this, LogisimMenuBar.SELECT_ALL);
  private final MenuItemImpl raise = new MenuItemImpl(this, LogisimMenuBar.RAISE);
  private final MenuItemImpl lower = new MenuItemImpl(this, LogisimMenuBar.LOWER);
  private final MenuItemImpl raiseTop = new MenuItemImpl(this, LogisimMenuBar.RAISE_TOP);
  private final MenuItemImpl lowerBottom = new MenuItemImpl(this, LogisimMenuBar.LOWER_BOTTOM);
  private final MenuItemImpl addCtrl = new MenuItemImpl(this, LogisimMenuBar.ADD_CONTROL);
  private final MenuItemImpl remCtrl = new MenuItemImpl(this, LogisimMenuBar.REMOVE_CONTROL);
  private final MyListener myListener = new MyListener();

  public MenuEdit(LogisimMenuBar menubar) {
    this.menubar = menubar;

    int menuMask = getToolkit().getMenuShortcutKeyMaskEx();
    undo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask));
    redo.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_Z, menuMask | KeyEvent.SHIFT_DOWN_MASK));
    cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask));
    copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask));
    paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask));
    delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    dup.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_D, menuMask));
    selall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask));
    raise.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuMask));
    lower.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuMask));
    raiseTop.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuMask | KeyEvent.SHIFT_DOWN_MASK));
    lowerBottom.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuMask | KeyEvent.SHIFT_DOWN_MASK));

    add(undo);
    add(redo);
    addSeparator();
    add(cut);
    add(copy);
    add(paste);
    addSeparator();
    add(delete);
    add(dup);
    add(selall);
    addSeparator();
    add(raise);
    add(lower);
    add(raiseTop);
    add(lowerBottom);
    addSeparator();
    add(addCtrl);
    add(remCtrl);

    final var proj = menubar.getSaveProject();
    if (proj != null) {
      proj.addProjectListener(myListener);
      undo.addActionListener(myListener);
      redo.addActionListener(myListener);
    }

    undo.setEnabled(false);
    redo.setEnabled(false);
    menubar.registerItem(LogisimMenuBar.CUT, cut);
    menubar.registerItem(LogisimMenuBar.COPY, copy);
    menubar.registerItem(LogisimMenuBar.PASTE, paste);
    menubar.registerItem(LogisimMenuBar.DELETE, delete);
    menubar.registerItem(LogisimMenuBar.DUPLICATE, dup);
    menubar.registerItem(LogisimMenuBar.SELECT_ALL, selall);
    menubar.registerItem(LogisimMenuBar.RAISE, raise);
    menubar.registerItem(LogisimMenuBar.LOWER, lower);
    menubar.registerItem(LogisimMenuBar.RAISE_TOP, raiseTop);
    menubar.registerItem(LogisimMenuBar.LOWER_BOTTOM, lowerBottom);
    menubar.registerItem(LogisimMenuBar.ADD_CONTROL, addCtrl);
    menubar.registerItem(LogisimMenuBar.REMOVE_CONTROL, remCtrl);
    computeEnabled();
  }

  @Override
  void computeEnabled() {
    setEnabled(
        menubar.getSaveProject() != null
            || cut.hasListeners()
            || copy.hasListeners()
            || paste.hasListeners()
            || delete.hasListeners()
            || dup.hasListeners()
            || selall.hasListeners()
            || raise.hasListeners()
            || lower.hasListeners()
            || raiseTop.hasListeners()
            || lowerBottom.hasListeners()
            || addCtrl.hasListeners()
            || remCtrl.hasListeners());
  }

  public void localeChanged() {
    this.setText(S.get("editMenu"));
    myListener.projectChanged(null);
    cut.setText(S.get("editCutItem"));
    copy.setText(S.get("editCopyItem"));
    paste.setText(S.get("editPasteItem"));
    delete.setText(S.get("editClearItem"));
    dup.setText(S.get("editDuplicateItem"));
    selall.setText(S.get("editSelectAllItem"));
    raise.setText(S.get("editRaiseItem"));
    lower.setText(S.get("editLowerItem"));
    raiseTop.setText(S.get("editRaiseTopItem"));
    lowerBottom.setText(S.get("editLowerBottomItem"));
    addCtrl.setText(S.get("editAddControlItem"));
    remCtrl.setText(S.get("editRemoveControlItem"));
  }

  private class MyListener implements ProjectListener, ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      final var proj = menubar.getSaveProject();
      if (src == undo && proj != null)
        proj.undoAction();
      else if (src == redo && proj != null)
        proj.redoAction();
    }

    @Override
    public void projectChanged(ProjectEvent e) {
      final var proj = menubar.getSaveProject();
      final var last = (proj != null) ? proj.getLastAction() : null;
      if (last == null) {
        undo.setText(S.get("editCantUndoItem"));
        undo.setEnabled(false);
      } else {
        undo.setText(S.get("editUndoItem", last.getName()));
        undo.setEnabled(true);
      }

      final var next = (proj == null || !proj.getCanRedo()) ? null : proj.getLastRedoAction();
      if (next != null) {
        redo.setText(S.get("editRedoItem", next.getName()));
        redo.setEnabled(true);
      } else {
        redo.setText(S.get("editCantRedoItem"));
        redo.setEnabled(false);
      }
    }
  }
}
