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

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.util.StringUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;

class MenuEdit extends Menu {
  private class MyListener implements ProjectListener, ActionListener {

    public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();
      Project proj = menubar.getProject();
      if (src == undo) {
        if (proj != null) {
          proj.undoAction();
        }
      } else if (src == redo) {
        if (proj != null) {
          proj.redoAction();
        }
      }
    }

    public void projectChanged(ProjectEvent e) {
      Project proj = menubar.getProject();
      Action last = proj == null ? null : proj.getLastAction();
      if (last == null) {
        undo.setText(S.get("editCantUndoItem"));
        undo.setEnabled(false);
      } else {
        undo.setText(StringUtil.format(S.get("editUndoItem"), last.getName()));
        undo.setEnabled(true);
      }
      
      Action next = (proj == null || !proj.getCanRedo()) ? null : proj.getLastRedoAction();
      if (next != null) {
        redo.setText(S.fmt("editRedoItem", next.getName()));
        redo.setEnabled(true);
      } else {
        redo.setText(S.get("editCantRedoItem"));
        redo.setEnabled(false);
      }
    }
  }

  private static final long serialVersionUID = 1L;

  private LogisimMenuBar menubar;
  private JMenuItem undo = new JMenuItem();
  private JMenuItem redo = new JMenuItem();
  private MenuItemImpl cut = new MenuItemImpl(this, LogisimMenuBar.CUT);
  private MenuItemImpl copy = new MenuItemImpl(this, LogisimMenuBar.COPY);
  private MenuItemImpl paste = new MenuItemImpl(this, LogisimMenuBar.PASTE);
  private MenuItemImpl delete = new MenuItemImpl(this, LogisimMenuBar.DELETE);
  private MenuItemImpl dup = new MenuItemImpl(this, LogisimMenuBar.DUPLICATE);
  private MenuItemImpl selall = new MenuItemImpl(this, LogisimMenuBar.SELECT_ALL);
  private MenuItemImpl raise = new MenuItemImpl(this, LogisimMenuBar.RAISE);
  private MenuItemImpl lower = new MenuItemImpl(this, LogisimMenuBar.LOWER);
  private MenuItemImpl raiseTop = new MenuItemImpl(this, LogisimMenuBar.RAISE_TOP);
  private MenuItemImpl lowerBottom = new MenuItemImpl(this, LogisimMenuBar.LOWER_BOTTOM);
  private MenuItemImpl addCtrl = new MenuItemImpl(this, LogisimMenuBar.ADD_CONTROL);
  private MenuItemImpl remCtrl = new MenuItemImpl(this, LogisimMenuBar.REMOVE_CONTROL);
  private MyListener myListener = new MyListener();

  public MenuEdit(LogisimMenuBar menubar) {
    this.menubar = menubar;

    int menuMask = getToolkit().getMenuShortcutKeyMask();
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
    raiseTop.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_UP, menuMask | KeyEvent.SHIFT_DOWN_MASK));
    lowerBottom.setAccelerator(
        KeyStroke.getKeyStroke(KeyEvent.VK_DOWN, menuMask | KeyEvent.SHIFT_DOWN_MASK));

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

    Project proj = menubar.getProject();
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
        menubar.getProject() != null
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
}
