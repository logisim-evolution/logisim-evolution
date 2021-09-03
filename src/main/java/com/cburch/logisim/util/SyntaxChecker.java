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
