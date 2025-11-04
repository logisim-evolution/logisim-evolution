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

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.JMenu;
import javax.swing.KeyStroke;
import javax.swing.JOptionPane;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

class MenuEdit extends Menu {
  private static final long serialVersionUID = 1L;
  private final LogisimMenuBar menubar;
  private final JMenuItem undo = new JMenuItem();
  private final JMenu undoHistory = new JMenu();
  private final JMenuItem redo = new JMenuItem();
  private final JMenu redoHistory = new JMenu();
  private final JMenuItem clearHistory = new JMenuItem();
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

    final var menuMask = getToolkit().getMenuShortcutKeyMaskEx();

    undo.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_UNDO).getWithMask(0));
    redo.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_REDO).getWithMask(0));
    cut.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_X, menuMask));
    copy.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_C, menuMask));
    paste.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_V, menuMask));
    delete.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0));
    dup.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_MENU_DUPLICATE)
        .getWithMask(0));
    selall.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_A, menuMask));
    raise.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuMask));
    lower.setAccelerator(KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuMask));
    raiseTop.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuMask | InputEvent.SHIFT_DOWN_MASK));
    lowerBottom.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuMask | InputEvent.SHIFT_DOWN_MASK));

    /* add myself to hotkey sync */
    AppPreferences.gui_sync_objects.add(this);

    add(undo);
    add(undoHistory);
    add(redo);
    add(redoHistory);
    add(clearHistory);
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
      clearHistory.addActionListener(myListener);
      redoHistory.addMenuListener(new MenuListener() {
        @Override
        public void menuSelected(MenuEvent e) {
          populateRedoHistoryMenu();
        }
        @Override public void menuDeselected(MenuEvent e) {
          /* Do nothing */
        }
        @Override public void menuCanceled(MenuEvent e) {
          /* Do nothing */
        }
      });

      undoHistory.addMenuListener(new MenuListener() {
        @Override
        public void menuSelected(MenuEvent e) {
          populateUndoHistoryMenu();
        }
        @Override public void menuDeselected(MenuEvent e) { /* Do nothing */ }
        @Override public void menuCanceled(MenuEvent e) { /* Do nothing */ }
      });
    }

    undo.setEnabled(false);
    undoHistory.setEnabled(false);
    redo.setEnabled(false);
    redoHistory.setEnabled(false);
    clearHistory.setEnabled(false);
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

  public void hotkeyUpdate() {
    undo.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_UNDO).getWithMask(0));
    redo.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_REDO).getWithMask(0));
    dup.setAccelerator(((PrefMonitorKeyStroke) AppPreferences.HOTKEY_EDIT_MENU_DUPLICATE)
        .getWithMask(0));
  }

  @Override
  protected void computeEnabled() {
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
    undoHistory.setText(S.get("editUndoHistoryMenu"));
    redoHistory.setText(S.get("editRedoHistoryMenu"));
    clearHistory.setText(S.get("editClearHistoryAction"));
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

  private void populateUndoHistoryMenu() {
    undoHistory.removeAll();
    final var proj = menubar.getSaveProject();
    if (proj == null || proj.getLastAction() == null) {
      JMenuItem disabledItem = new JMenuItem(S.get("editCantUndoItem"));
      disabledItem.setEnabled(false);
      undoHistory.add(disabledItem);
    } else {
      java.util.List<com.cburch.logisim.proj.Action> actions = proj.getUndoActions();
      for (final Action action : actions) {
        JMenuItem actionItem = new JMenuItem(action.getName());
        actionItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            final var currentProj = menubar.getSaveProject();
            if (currentProj != null) {
              currentProj.undoUpTo(action);
            }
          }
        });
        undoHistory.add(actionItem);
      }
    }
  }

  private void populateRedoHistoryMenu() {
    redoHistory.removeAll();
    final var proj = menubar.getSaveProject();
    if (proj == null || !proj.getCanRedo()) {
      JMenuItem disabledItem = new JMenuItem(S.get("editCantRedoItem"));
      disabledItem.setEnabled(false);
      redoHistory.add(disabledItem);
    } else {
      java.util.List<com.cburch.logisim.proj.Action> actions = proj.getRedoActions();
      for (final Action action : actions) {
          JMenuItem actionItem = new JMenuItem(action.getName());
          actionItem.addActionListener(new ActionListener() {
          @Override
          public void actionPerformed(ActionEvent e) {
            final var currentProj = menubar.getSaveProject();
            if (currentProj != null) {
              currentProj.redoUpTo(action);
            }
          }
        });
        redoHistory.add(actionItem);
      }
    }
  }

  private class MyListener implements ProjectListener, ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
      final var proj = menubar.getSaveProject();
      if (src == undo && proj != null) {
        proj.undoAction();
      } else if (src == redo && proj != null) {
        proj.redoAction();
      } else if (src == clearHistory && proj != null) {
        final var result = JOptionPane.showConfirmDialog(
            proj.getFrame(),
            S.get("clearHistoryWarningMessage"),
            S.get("clearHistoryWarningTitle"),
            JOptionPane.OK_CANCEL_OPTION,
            JOptionPane.WARNING_MESSAGE
        );
        if (result == JOptionPane.OK_OPTION) {
          proj.discardAllEdits();
        }
      }
    }

    @Override
    public void projectChanged(ProjectEvent e) {
      final var proj = menubar.getSaveProject();
      final var last = (proj != null) ? proj.getLastAction() : null;
      if (last == null) {
        undo.setText(S.get("editCantUndoItem"));
        undo.setEnabled(false);
        undoHistory.setEnabled(false);
      } else {
        undo.setText(S.get("editUndoItem", last.getName()));
        undo.setEnabled(true);
        undoHistory.setEnabled(true);
      }

      final var next = (proj == null || !proj.getCanRedo()) ? null : proj.getLastRedoAction();
      final boolean canRedo = (next != null);

      if (next != null) {
        redo.setText(S.get("editRedoItem", next.getName()));
        redo.setEnabled(true);
      } else {
        redo.setText(S.get("editCantRedoItem"));
        redo.setEnabled(false);
      }
      redoHistory.setEnabled(canRedo);

      final var historyExists = (last != null || canRedo);
      clearHistory.setEnabled(historyExists);
    }
  }
}
