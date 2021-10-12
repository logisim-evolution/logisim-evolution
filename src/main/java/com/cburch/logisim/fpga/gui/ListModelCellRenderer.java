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

import com.cburch.logisim.fpga.designrulecheck.SimpleDrcContainer;
import com.cburch.logisim.gui.icons.DrcIcon;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ListModelCellRenderer extends JLabel implements ListCellRenderer<Object> {
  /** */
  private static final long serialVersionUID = 1L;

  private final boolean CountLines;

  private static final Color FATAL = Color.RED;
  private static final Color SEVERE = Color.yellow;
  private static final Color NORMAL = Color.LIGHT_GRAY;
  private static final Color ADDENDUM = Color.GRAY;

  private static final DrcIcon NoDRC = new DrcIcon(false);
  private static final DrcIcon DRCError = new DrcIcon(true);

  public ListModelCellRenderer(boolean countLines) {
    CountLines = countLines;
    setOpaque(true);
  }

  @Override
  public Component getListCellRendererComponent(
      JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    SimpleDrcContainer msg = null;
    setBackground(list.getBackground());
    setForeground(list.getForeground());
    StringBuilder Line = new StringBuilder();
    if (value instanceof SimpleDrcContainer cont) {
      msg = cont;
    }
    setIcon((msg != null && msg.isDrcInfoPresent()) ? DRCError : NoDRC);
    if (msg != null) {
      switch (msg.getSeverity()) {
        case SimpleDrcContainer.LEVEL_SEVERE:
          setForeground(SEVERE);
          break;
        case SimpleDrcContainer.LEVEL_FATAL:
          setBackground(FATAL);
          setForeground(list.getBackground());
          break;
        default:
          setForeground(NORMAL);
      }
    }
    if (value.toString().contains("BUG")) {
      setBackground(Color.MAGENTA);
      setForeground(Color.black);
    }
    if (CountLines) {
      if (msg != null) {
        if (msg.getSupressCount()) {
          setForeground(ADDENDUM);
          Line.append("       ");
        } else {
          int line = msg.getListNumber();
          if (line < 10) {
            Line.append("    ");
          } else if (line < 100) {
            Line.append("   ");
          } else if (line < 1000) {
            Line.append("  ");
          } else if (line < 10000) {
            Line.append(" ");
          }
          Line.append(line).append("> ");
        }
      } else {
        if (index < 9) {
          Line.append("    ");
        } else if (index < 99) {
          Line.append("   ");
        } else if (index < 999) {
          Line.append("  ");
        } else if (index < 9999) {
          Line.append(" ");
        }
        Line.append(index + 1).append("> ");
      }
    }
    if (msg != null) {
      switch (msg.getSeverity()) {
        case SimpleDrcContainer.LEVEL_SEVERE:
          Line.append(S.get("SEVERE_MSG")).append(" ");
          break;
        case SimpleDrcContainer.LEVEL_FATAL:
          Line.append(S.get("FATAL_MSG")).append(" ");
          break;
      }
      if (msg.hasCircuit()) {
        Line.append(msg.getCircuit().getName()).append(": ");
      }
    }
    Line.append(value);
    setText(Line.toString());
    setEnabled(list.isEnabled());
    setFont(list.getFont());
    return this;
  }
}
