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

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.KeyboardFocusManager;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.KeyStroke;

public abstract class JDialogOk extends JDialog {
  private class MyListener extends WindowAdapter implements ActionListener {
    public void actionPerformed(ActionEvent e) {
      Object src = e.getSource();
      if (src == ok) {
        okClicked();
        dispose();
      } else if (src == cancel) {
        cancelClicked();
        dispose();
      }
    }

    @Override
    public void windowClosing(WindowEvent e) {
      JDialogOk.this.removeWindowListener(this);
      cancelClicked();
      dispose();
    }
  }

  private static final long serialVersionUID = 1L;

  private JPanel contents = new JPanel(new BorderLayout());
  protected JButton ok = new JButton(S.get("dlogOkButton"));
  protected JButton cancel = new JButton(S.get("dlogCancelButton"));
  protected Window parent;
  
  public JDialogOk(String title) {
    super(KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
        title, Dialog.ModalityType.APPLICATION_MODAL);
    parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    configure();
  }

public void cancelClicked() {}

  private void configure() {
    MyListener listener = new MyListener();
    this.addWindowListener(listener);
    ok.addActionListener(listener);
    cancel.addActionListener(listener);

    Box buttons = Box.createHorizontalBox();
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    buttons.add(Box.createHorizontalStrut(10));
    buttons.add(cancel);
    buttons.add(Box.createHorizontalGlue());

    Container pane = super.getContentPane();
    pane.add(contents, BorderLayout.CENTER);
    pane.add(buttons, BorderLayout.SOUTH);
    
    getRootPane().registerKeyboardAction(new ActionListener() {
        public void actionPerformed(ActionEvent e) { setVisible(false); cancelClicked(); dispose(); }
      }, KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0), JComponent.WHEN_IN_FOCUSED_WINDOW);

    addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        ok.requestFocus();
        e.getWindow().removeWindowListener(this);
      }
    });
  }

  @Override
  public Container getContentPane() {
    return contents;
  }

  public void pack() {
    super.pack();
    while (parent != null && !parent.isShowing())
      parent = parent.getOwner();
    setLocationRelativeTo(parent);
    parent = null;
  }

  public abstract void okClicked();
}
