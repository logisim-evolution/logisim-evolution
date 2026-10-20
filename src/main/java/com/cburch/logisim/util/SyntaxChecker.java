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
import com.cburch.logisim.prefs.AppPreferences;
import java.util.regex.Pattern;

public final class SyntaxChecker {

  private static final Pattern variablePattern = Pattern.compile("^([a-zA-Z]+\\w*)");
  private static final Pattern forbiddenPattern = Pattern.compile("__");

  private SyntaxChecker() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  public static String getErrorMessage(String val) {
    return getErrorMessage(val, null);
  }

  public static String getErrorMessage(String val, String hdlType) {
    if (StringUtil.isNullOrEmpty(val)) return null;
    if (HdlGeneratorFactory.NONE.equals(hdlType)) return null;

    var messageBuilder = new StringBuilder();
    buildVariableErrorMessage(val, messageBuilder);
    final var hdl = CorrectLabel.hdlCorrectLabel(val, hdlType);
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

      // We don't check this case when the variable starts with a digit
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
    return isVariableNameAcceptable(val, null, showDialog);
  }

  public static boolean isVariableNameAcceptable(String val, String hdlType, Boolean showDialog) {
    final var message = getErrorMessage(val, hdlType);
    if (message != null && showDialog) {
      OptionPane.showMessageDialog(null, message.concat("\n" + S.get("variableNameNotAcceptable")));
    }
    return message == null;
  }

  public static boolean isVariableNameAcceptableForCurrentHdl(String val, Boolean showDialog) {
    return isVariableNameAcceptable(val, AppPreferences.HdlType.get(), showDialog);
  }

  public static boolean namesEqual(String first, String second, String hdlType) {
    if (HdlGeneratorFactory.VHDL.equals(hdlType)) return first.equalsIgnoreCase(second);
    return first.equals(second);
  }

  public static boolean namesEqualForCurrentHdl(String first, String second) {
    return namesEqual(first, second, AppPreferences.HdlType.get());
  }
}
