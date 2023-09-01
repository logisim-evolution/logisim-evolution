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
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.gui.generic.OptionPane;
import java.util.regex.Pattern;

public final class SyntaxChecker {

  private static final Pattern variablePattern = Pattern.compile("^([a-zA-Z]+\\w*)");
  private static final Pattern forbiddenPattern = Pattern.compile("__");

  private SyntaxChecker() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static String getErrorMessage(String val) {
    if (StringUtil.isNullOrEmpty(val)) return null;
    var messageBuilder = new StringBuilder();
    buildVariableErrorMessage(val, messageBuilder);
    final var hdl = CorrectLabel.hdlCorrectLabel(val);
    if (hdl != null) {
      messageBuilder.append(hdl.equals(HdlGeneratorFactory.VHDL)
          ? S.get("variableVHDLKeyword")
          : S.get("variableVerilogKeyword"));
    }
    if (val.endsWith("_")) {
      messageBuilder.append(S.get("variableEndsWithUndescore"));
    }
    var message = messageBuilder.toString();
    return (message.length() == 0) ? null : message;
  }

  private static void buildVariableErrorMessage(String val, StringBuilder messageBuilder) {
    final var variableMatcher = variablePattern.matcher(val);
    final var forbiddenMatcher = forbiddenPattern.matcher(val);
    if (!variableMatcher.matches()) {
      messageBuilder.append(S.get("variableInvalidCharacters"));
    }
    if (Character.isDigit(val.charAt(0))) {
      messageBuilder.append(S.get("variableStartsWithDigit"));
    } else {

      // we don't check this case when the variable starts with a digit
      // because this would match the initial digit, we don't want that.
      variableMatcher.reset();
      int firstIllegalCharacterIndex = variableMatcher.find() ? variableMatcher.end() : 0;
      if (firstIllegalCharacterIndex != val.length()) {
        char firstIllegalCharacter = val.charAt(firstIllegalCharacterIndex);
        messageBuilder.append(S.get("variableIllegalCharacter",
            String.valueOf(firstIllegalCharacter)));
      }
    }
    if (forbiddenMatcher.find()) {
      messageBuilder.append(S.get("variableDoubleUnderscore"));
    }
  }

  public static boolean isVariableNameAcceptable(String val, Boolean showDialog) {
    final var message = getErrorMessage(val);
    if (message != null && showDialog) {
      OptionPane.showMessageDialog(null, message.concat("\n" + S.get("variableNameNotAcceptable")));
    }
    return message == null;
  }
}
