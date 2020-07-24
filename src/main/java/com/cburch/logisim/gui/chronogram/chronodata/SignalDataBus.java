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

package com.cburch.logisim.gui.chronogram.chronodata;

import com.cburch.logisim.util.Icons;
import java.util.ArrayList;
import javax.swing.ImageIcon;

/*
 * Contains bus specific data
 * */
public class SignalDataBus extends SignalData {

  public static String[] signalFormat = {"binary", "hexadecimal", "octal", "signed", "unsigned"};
  private String format = "hexadecimal";
  private boolean expanded = false;

  public SignalDataBus(String name, ArrayList<String> data) {
    super(name, data);
  }

  public String getFormat() {
    return format;
  }

  public ImageIcon getIcon() {
    return (ImageIcon) Icons.getIcon("chronoBus.gif");
  }

  public String getSelectedValue() {
    return data.size() > 0 ? getValueInFormat(data.get(selectedValuePos)) : "";
  }

  public String getValueInFormat(String binaryVal) {
    try {
      if (format.equals("binary")) return binaryVal;
      else if (format.equals("hexadecimal")) return toHexa(binaryVal);
      else if (format.equals("octal")) return toOctal(binaryVal);
      else if (format.equals("signed")) return toSigned(binaryVal);
      else if (format.equals("unsigned")) return toUnsigned(binaryVal);
      else return binaryVal;
    } catch (Exception e) {
      return binaryVal;
    }
  }

  public boolean isExpanded() {
    return expanded;
  }

  public void setExpanded(boolean val) {
    expanded = val;
  }

  public void setFormat(String format) {
    this.format = format;
  }

  private String toHexa(String bin) {
    int valInInt = Integer.parseInt(bin, 2);
    return Integer.toHexString(valInInt).toUpperCase();
  }

  private String toOctal(String bin) {
    int valInInt = Integer.parseInt(bin, 2);
    return Integer.toOctalString(valInInt).toUpperCase();
  }

  /*
   * Return the Two's complements for negative number The size of 'bin' is
   * used for the binary length
   */
  private String toSigned(String bin) {
    // bin positive
    if (bin.charAt(0) == '0') return toUnsigned(bin);

    // two's complement
    int valInInt = Integer.parseInt(bin, 2);
    String ones = "";
    for (int i = 0; i < bin.length(); ++i) ones += "1";
    valInInt ^= Integer.parseInt(ones, 2);
    valInInt += 1;
    return Integer.toString(-valInInt);
  }

  private String toUnsigned(String bin) {
    int valInInt = Integer.parseInt(bin, 2);
    return Integer.toString(valInInt);
  }
}
