/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.Main;
import java.awt.Component;
import java.awt.Frame;
import javax.swing.Icon;
import javax.swing.JOptionPane;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/*
 * This class calls the JOptionPane when in gui mode and does a log
 * when in tty mode.
 */

public class OptionPane {
  public static final int YES_NO_OPTION = JOptionPane.YES_NO_OPTION;
  public static final int YES_NO_CANCEL_OPTION = JOptionPane.YES_NO_CANCEL_OPTION;
  public static final int YES_OPTION = JOptionPane.YES_OPTION;
  public static final int CANCEL_OPTION = JOptionPane.CANCEL_OPTION;
  public static final int OK_CANCEL_OPTION = JOptionPane.OK_CANCEL_OPTION;
  public static final int OK_OPTION = JOptionPane.OK_OPTION;

  public static final int ERROR_MESSAGE = JOptionPane.ERROR_MESSAGE;
  public static final int WARNING_MESSAGE = JOptionPane.WARNING_MESSAGE;
  public static final int QUESTION_MESSAGE = JOptionPane.QUESTION_MESSAGE;
  public static final int INFORMATION_MESSAGE = JOptionPane.INFORMATION_MESSAGE;
  public static final int PLAIN_MESSAGE = JOptionPane.PLAIN_MESSAGE;

  public static final int CLOSED_OPTION = JOptionPane.CLOSED_OPTION;

  static final Logger logger = LoggerFactory.getLogger(OptionPane.class);

  public static void showMessageDialog(Component parentComponent, Object message) {
    if (Main.hasGui()) {
      JOptionPane.showMessageDialog(parentComponent, message);
    } else if (message instanceof String msg) logger.info(msg);
  }

  public static void showMessageDialog(
      Component parentComponent, Object message, String title, int messageType) {
    if (Main.hasGui()) {
      JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
    } else if (message instanceof String) {
      String logMessage = title + ":" + message;
      switch (messageType) {
        case ERROR_MESSAGE:
          logger.error(logMessage);
          break;
        case OptionPane.WARNING_MESSAGE:
          logger.warn(logMessage);
          break;
        default:
          logger.info(logMessage);
          break;
      }
    }
  }

  public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
    return Main.hasGui()
        ? JOptionPane.showConfirmDialog(parentComponent, message, title, optionType)
        : CANCEL_OPTION;
  }

  public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType, int messageType) {
    return Main.hasGui()
        ? JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType)
        : CANCEL_OPTION;
  }

  public static String showInputDialog(Object message) {
    return Main.hasGui() ? JOptionPane.showInputDialog(message) : null;
  }

  public static String showInputDialog(Component parentComponent, Object message, String title, int messageType) {
    return Main.hasGui()
        ? JOptionPane.showInputDialog(parentComponent, message, title, messageType)
        : null;
  }

  public static Object showInputDialog(
      Component parentComponent,
      Object message,
      String title,
      int messageType,
      Icon icon,
      Object[] selectionValues,
      Object initialSelectionValue) {
    return Main.hasGui()
        ? JOptionPane.showInputDialog(parentComponent, message, title, messageType, icon, selectionValues, initialSelectionValue)
        : null;
  }

  public static int showOptionDialog(
      Component parentComponent,
      Object message,
      String title,
      int optionType,
      int messageType,
      Icon icon,
      Object[] options,
      Object initialValue) {
    return Main.hasGui()
        ? JOptionPane.showOptionDialog(parentComponent, message, title, optionType, messageType, icon, options, initialValue)
        : CLOSED_OPTION;
  }

  public static Frame getFrameForComponent(Component parentComponent) {
    return Main.hasGui() ? JOptionPane.getFrameForComponent(parentComponent) : null;
  }
}
