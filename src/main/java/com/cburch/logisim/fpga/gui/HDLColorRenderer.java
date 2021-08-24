/*
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

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

@SuppressWarnings("serial")
public class HDLColorRenderer extends JLabel implements TableCellRenderer {
  public static final String NO_SUPPORT_STRING = "HDL_NOT_SUPPORTED";
  public static final String SUPPORT_STRING = "HDL_SUPPORTED";
  public static final String UNKNOWN_STRING = "HDL_UNKNOWN";
  public static final String REQUIRED_FIELD_STRING = ">_HDL_REQUIRED_FIELD_<";
  private static final ArrayList<String> CorrectStrings = new ArrayList<>();

  Border border = null;

  public HDLColorRenderer() {
    setOpaque(true);
    CorrectStrings.clear();
    CorrectStrings.add(NO_SUPPORT_STRING);
    CorrectStrings.add(UNKNOWN_STRING);
  }

  @Override
  public Component getTableCellRendererComponent(
      JTable table, Object Info, boolean isSelected, boolean hasFocus, int row, int column) {
    /* we have a difference between the first row and the rest */
    if (row == 0) {
      String value = (String) Info;
      boolean passive = value.equals(NO_SUPPORT_STRING);
      Color newColor = (passive) ? Color.red : Color.green;
      if (value.equals(UNKNOWN_STRING)) newColor = table.getGridColor();
      if (column == 1) setBackground(newColor);
      setForeground(Color.black);
      if (column == 0) setText(value);
      else {
        if (value.equals(NO_SUPPORT_STRING)) setText(S.get("FPGANotSupported"));
        else if (value.equals(SUPPORT_STRING)) setText(S.get("FPGASupported"));
        else setText(S.get("FPGAUnknown"));
      }
      setHorizontalAlignment(JLabel.CENTER);
    } else {
      String myInfo = (String) Info;
      if (myInfo != null && myInfo.equals(REQUIRED_FIELD_STRING)) {
        setBackground(Color.YELLOW);
        setForeground(Color.BLUE);
        setText("HDL Required");
        setHorizontalAlignment(JLabel.CENTER);
        setBorder(null);
      } else if (myInfo != null
          && myInfo.contains("#")
          && myInfo.indexOf('#') == 0
          && (myInfo.length() == 7 || myInfo.length() == 9)) {
        int red, green, blue, alpha;
        red = Integer.valueOf(myInfo.substring(1, 3), 16);
        green = Integer.valueOf(myInfo.substring(3, 5), 16);
        blue = Integer.valueOf(myInfo.substring(5, 7), 16);
        alpha = myInfo.length() == 7 ? 255 : Integer.valueOf(myInfo.substring(7, 9), 16);
        setBackground(new Color(red, green, blue, alpha));
        setText("");
        setBorder(null);
      } else {
        Color newColor = isSelected ? Color.lightGray : Color.white;
        setBackground(newColor);
        setForeground(Color.black);
        setText((String) Info);
        setHorizontalAlignment(JLabel.LEFT);
        setBorder(null);
      }
    }
    return this;
  }
}
