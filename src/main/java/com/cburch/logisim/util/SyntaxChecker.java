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

package com.cburch.logisim.util;

import static com.cburch.logisim.util.Strings.S;

import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.hdlgenerator.HDLGeneratorFactory;
import com.cburch.logisim.gui.generic.OptionPane;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SyntaxChecker {

  public static String getErrorMessage(String val) {
    if (val.length() == 0) return null;
    if (val.length() > 0) {
      variableMatcher = variablePattern.matcher(val);
      forbiddenMatcher = forbiddenPattern.matcher(val);
      final var hdl = CorrectLabel.hdlCorrectLabel(val);
      var message = "";
      if (!variableMatcher.matches()) {
        message = message.concat(S.get("variableInvalidCharacters"));
      }
      if (forbiddenMatcher.find()) {
        message = message.concat(S.get("variableDoubleUnderscore"));
      }
      if (hdl != null) {
        message = message.concat(hdl.equals(HDLGeneratorFactory.VHDL)
                                ? S.get("variableVHDLKeyword")
                                : S.get("variableVerilogKeyword"));
      }
      if (val.endsWith("_")) {
        message = message.concat(S.get("variableEndsWithUndescore"));
      }
      if (message.length() == 0)
        return null;
      else
        return message;
    }
    return null;
  }

  public static boolean isVariableNameAcceptable(String val, Boolean showDialog) {
    final var message = getErrorMessage(val);
    if (message != null && showDialog) {
      OptionPane.showMessageDialog(null, message.concat("\n" + S.get("variableNameNotAcceptable")));
    }
    return message == null;
  }

  private static final Pattern variablePattern = Pattern.compile("^([a-zA-Z]+\\w*)");
  private static final Pattern forbiddenPattern = Pattern.compile("__");

  private static Matcher forbiddenMatcher;
  private static Matcher variableMatcher;
}
