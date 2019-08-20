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

package com.cburch.logisim.fpga.gui;

import static com.cburch.logisim.fpga.Strings.S;

import com.cburch.logisim.fpga.designrulecheck.SimpleDRCContainer;
import com.cburch.logisim.gui.icons.DrcIcon;
import java.awt.Color;
import java.awt.Component;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;

public class ListModelCellRenderer extends JLabel implements ListCellRenderer<Object> {
  /** */
  private static final long serialVersionUID = 1L;

  private boolean CountLines;

  private static Color FATAL = Color.RED;
  private static Color SEVERE = Color.yellow;
  private static Color NORMAL = Color.LIGHT_GRAY;
  private static Color ADDENDUM = Color.GRAY;
  
  private static final DrcIcon NoDRC = new DrcIcon(false);
  private static final DrcIcon DRCError = new DrcIcon(true);

  public ListModelCellRenderer(boolean countLines) {
    CountLines = countLines;
    setOpaque(true);
  }

  @Override
  public Component getListCellRendererComponent(
      JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
    SimpleDRCContainer msg = null;
    setBackground(list.getBackground());
    setForeground(list.getForeground());
    StringBuffer Line = new StringBuffer();
    if (value instanceof SimpleDRCContainer) {
      msg = (SimpleDRCContainer) value;
    }
    setIcon((msg != null && msg.DRCInfoPresent())?DRCError : NoDRC);
    if (msg != null) {
      switch (msg.Severity()) {
        case SimpleDRCContainer.LEVEL_SEVERE:
          setForeground(SEVERE);
          break;
        case SimpleDRCContainer.LEVEL_FATAL:
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
        if (msg.SupressCount()) {
          setForeground(ADDENDUM);
          Line.append("       ");
        } else {
          int line = msg.GetListNumber();
          if (line < 10) {
            Line.append("    ");
          } else if (line < 100) {
            Line.append("   ");
          } else if (line < 1000) {
            Line.append("  ");
          } else if (line < 10000) {
            Line.append(" ");
          }
          Line.append(Integer.toString(line) + "> ");
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
        Line.append(Integer.toString(index + 1) + "> ");
      }
    }
    if (msg != null) {
      switch (msg.Severity()) {
        case SimpleDRCContainer.LEVEL_SEVERE:
          Line.append(S.get("SEVERE_MSG") + " ");
          break;
        case SimpleDRCContainer.LEVEL_FATAL:
          Line.append(S.get("FATAL_MSG") + " ");
          break;
      }
      if (msg.HasCircuit()) {
        Line.append(msg.GetCircuit().getName() + ": ");
      }
    }
    Line.append(value.toString());
    setText(Line.toString());
    setEnabled(list.isEnabled());
    setFont(list.getFont());
    return this;
  }
}
