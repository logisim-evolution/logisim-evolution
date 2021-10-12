/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
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
    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();
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

  private final JPanel contents = new JPanel(new BorderLayout());
  protected final JButton ok = new JButton(S.get("dlogOkButton"));
  protected final JButton cancel = new JButton(S.get("dlogCancelButton"));
  protected Window parent;

  public JDialogOk(String title) {
    this(title, true);
  }

  public JDialogOk(String title, boolean withCancel) {
    super(
        KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow(),
        title,
        Dialog.ModalityType.APPLICATION_MODAL);
    parent = KeyboardFocusManager.getCurrentKeyboardFocusManager().getActiveWindow();
    configure(withCancel);
  }

  public void cancelClicked() {}

  private void configure(boolean withCancel) {
    final var listener = new MyListener();
    this.addWindowListener(listener);
    ok.addActionListener(listener);
    cancel.addActionListener(listener);

    final var buttons = Box.createHorizontalBox();
    buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
    buttons.add(Box.createHorizontalGlue());
    buttons.add(ok);
    if (withCancel) {
      buttons.add(Box.createHorizontalStrut(10));
      buttons.add(cancel);
    }
    buttons.add(Box.createHorizontalGlue());

    final var pane = super.getContentPane();
    pane.add(contents, BorderLayout.CENTER);
    pane.add(buttons, BorderLayout.SOUTH);

    getRootPane()
        .registerKeyboardAction(
            e -> {
              setVisible(false);
              cancelClicked();
              dispose();
            },
            KeyStroke.getKeyStroke(KeyEvent.VK_ESCAPE, 0),
            JComponent.WHEN_IN_FOCUSED_WINDOW);

    addWindowListener(
        new WindowAdapter() {
          @Override
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

  @Override
  public void pack() {
    super.pack();
    while (parent != null && !parent.isShowing()) parent = parent.getOwner();
    setLocationRelativeTo(parent);
    parent = null;
  }

  public abstract void okClicked();
}
