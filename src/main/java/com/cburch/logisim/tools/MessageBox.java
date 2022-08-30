/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.tools;

import com.cburch.logisim.gui.generic.OptionPane;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MessageBox {

  final String description;
  final String title;
  final int type;
  JTextArea textArea;

  public MessageBox(String title, String description, int type) {
    this.title = title;
    this.description = description;
    this.type = type;
  }

  public void show() {

    if (description.contains("\n") || description.length() > 60) {
      var lines = 1;
      for (var pos = description.indexOf('\n');
          pos >= 0;
          pos = description.indexOf('\n', pos + 1)) {
        lines++;
      }
      lines = Math.max(4, Math.min(lines, 7));

      textArea = new JTextArea(lines, 100);
      textArea.setEditable(false);
      textArea.setText(description);
      textArea.setCaretPosition(0);

      final var scrollPane = new JScrollPane(textArea);
      scrollPane.setPreferredSize(new Dimension(640, 480));
      OptionPane.showMessageDialog(null, scrollPane, title, type);
    } else {
      OptionPane.showMessageDialog(null, description, title, type);
    }
  }
}
