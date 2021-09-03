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
    } else if (message instanceof String) logger.info((String) message);
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

  public static int showConfirmDialog(
      Component parentComponent, Object message, String title, int optionType) {
    if (Main.hasGui())
      return JOptionPane.showConfirmDialog(parentComponent, message, title, optionType);
    return CANCEL_OPTION;
  }

  public static int showConfirmDialog(
      Component parentComponent, Object message, String title, int optionType, int messageType) {
    if (Main.hasGui())
      return JOptionPane.showConfirmDialog(
          parentComponent, message, title, optionType, messageType);
    return CANCEL_OPTION;
  }

  public static String showInputDialog(Object message) {
    if (Main.hasGui()) return JOptionPane.showInputDialog(message);
    return null;
  }

  public static String showInputDialog(
      Component parentComponent, Object message, String title, int messageType) {
    if (Main.hasGui())
      return JOptionPane.showInputDialog(parentComponent, message, title, messageType);
    return null;
  }

  public static Object showInputDialog(
      Component parentComponent,
      Object message,
      String title,
      int messageType,
      Icon icon,
      Object[] selectionValues,
      Object initialSelectionValue) {
    if (Main.hasGui())
      return JOptionPane.showInputDialog(
          parentComponent,
          message,
          title,
          messageType,
          icon,
          selectionValues,
          initialSelectionValue);
    return null;
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
    if (Main.hasGui())
      return JOptionPane.showOptionDialog(
          parentComponent, message, title, optionType, messageType, icon, options, initialValue);
    return CLOSED_OPTION;
  }

  public static Frame getFrameForComponent(Component parentComponent) {
    if (Main.hasGui()) return JOptionPane.getFrameForComponent(parentComponent);
    return null;
  }
}
