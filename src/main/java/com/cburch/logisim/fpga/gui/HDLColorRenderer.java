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

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.Border;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;

@SuppressWarnings("serial")
public class HDLColorRenderer extends JLabel implements TableCellRenderer {
  public static final String VHDLSupportString = "VHDL_SUPPORTED";
  public static final String VERILOGSupportString = "VERILOG_SUPPORTED";
  public static final String NoSupportString = "HDL_NOT_SUPPORTED";
  public static final String UnKnownString = "HDL_UNKNOWN";
  public static final String RequiredFieldString = ">_HDL_REQUIRED_FIELD_<";
  private static final ArrayList<String> CorrectStrings = new ArrayList<String>();

  Border border = null;

  public HDLColorRenderer() {
    setOpaque(true);
    CorrectStrings.clear();
    CorrectStrings.add(VERILOGSupportString);
    CorrectStrings.add(NoSupportString);
    CorrectStrings.add(UnKnownString);
    CorrectStrings.add(VHDLSupportString);
  }

  public Component getTableCellRendererComponent(
      JTable table, Object Info, boolean isSelected, boolean hasFocus, int row, int column) {
    /* we have a difference between the first row and the rest */
    if (row == 0) {
      String value = (String) Info;
      boolean passive = value.equals(NoSupportString);
      Color newColor = (passive) ? Color.red : Color.green;
      if (value.equals(UnKnownString)) newColor = table.getGridColor();
      setBackground(newColor);
      setForeground(Color.black);
      setText(
          CorrectStrings.contains(value)
              ? column == 0 ? HDLGeneratorFactory.VHDL : HDLGeneratorFactory.VERILOG
              : value);
      setHorizontalAlignment(JLabel.CENTER);
      if (border == null)
        border = BorderFactory.createMatteBorder(2, 5, 2, 5, table.getGridColor());
      setBorder(border);
    } else {
      String myInfo = (String) Info;
      if (myInfo != null && myInfo.equals(RequiredFieldString)) {
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
