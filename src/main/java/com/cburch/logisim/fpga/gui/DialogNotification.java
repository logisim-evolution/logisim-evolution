/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.gui.icons.ErrorIcon;
import com.cburch.logisim.gui.icons.WarningIcon;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;

public class DialogNotification {

  public static void showDialogNotification(Frame parent, String type, String string) {
    JDialog dialog = new JDialog(parent, type, true);
    JLabel pic = new JLabel();
    if (type.equals("Warning")) {
      pic.setIcon(new WarningIcon());
    } else {
      pic.setIcon(new ErrorIcon());
    }
    GridBagLayout dialogLayout = new GridBagLayout();
    dialog.setLayout(dialogLayout);
    GridBagConstraints c = new GridBagConstraints();
    JLabel message = new JLabel(string);
    JButton close = new JButton(S.get("FpgaBoardClose"));
    ActionListener actionListener =
        e -> dialog.dispose();
    close.addActionListener(actionListener);

    c.gridx = 0;
    c.gridy = 0;
    c.ipadx = 20;
    dialog.add(pic, c);

    c.gridx = 1;
    c.gridy = 0;
    dialog.add(message, c);

    c.gridx = 1;
    c.gridy = 1;
    dialog.add(close, c);
    dialog.pack();
    dialog.setLocationRelativeTo(parent);
    dialog.setAlwaysOnTop(true);
    dialog.setVisible(true);
  }

}
