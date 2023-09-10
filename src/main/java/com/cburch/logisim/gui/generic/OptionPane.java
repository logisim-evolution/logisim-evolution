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

/**
 * Provides a unified interface for displaying various types of dialogs.
 *
 * This class wraps the functionality of {@link JOptionPane} and provides additional logging capabilities when the
 * application runs in a non-GUI (tty) mode. In GUI mode, dialogs are shown as they would be using the
 * {@link JOptionPane}, while in non-GUI mode, relevant messages are logged instead of displaying a dialog.
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

  /**
   * Displays a message dialog with the specified message.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message to be displayed.
   */
  public static void showMessageDialog(Component parentComponent, Object message) {
    if (Main.hasGui()) {
      JOptionPane.showMessageDialog(parentComponent, message);
    } else if (message instanceof String msg) {
      logger.info(msg);
    }
  }

  /**
   * Displays a message dialog with the specified message, title, and message type.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message object to be displayed (String, JPanel...)
   * @param title           The title of the dialog.
   * @param messageType     The type of message to be displayed, e.g., ERROR_MESSAGE.
   */
  public static void showMessageDialog(
      Component parentComponent, Object message, String title, int messageType) {
    if (Main.hasGui()) {
      JOptionPane.showMessageDialog(parentComponent, message, title, messageType);
    } else if (message instanceof String) {
      final var logMessage = title + ":" + message;
      switch (messageType) {
        case ERROR_MESSAGE -> logger.error(logMessage);
        case OptionPane.WARNING_MESSAGE -> logger.warn(logMessage);
        default -> logger.info(logMessage);
      }
    }
  }

  /**
   * Displays a confirmation dialog with the specified message and title.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message to be displayed.
   * @param title           The title of the dialog.
   * @param optionType      Specifies the set of options available on the dialog.
   *
   * @return The option chosen by the user, or CANCEL_OPTION if the GUI is not available.
   */
  public static int showConfirmDialog(Component parentComponent, Object message, String title, int optionType) {
    return Main.hasGui()
        ? JOptionPane.showConfirmDialog(parentComponent, message, title, optionType)
        : CANCEL_OPTION;
  }

  /**
   * Displays a confirmation dialog with the specified message, title, option type, and message type.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message to be displayed.
   * @param title           The title of the dialog.
   * @param optionType      Specifies the set of options available on the dialog.
   * @param messageType     The type of message to be displayed.
   *
   * @return The option chosen by the user, or CANCEL_OPTION if the GUI is not available.
   */
  public static int showConfirmDialog(Component parentComponent, Object message, String title,
                                      int optionType, int messageType) {
    return Main.hasGui()
        ? JOptionPane.showConfirmDialog(parentComponent, message, title, optionType, messageType)
        : CANCEL_OPTION;
  }

  /**
   * Displays an input dialog with the specified message.
   *
   * @param message The message to be displayed.
   *
   * @return The input provided by the user, or null if the GUI is not available.
   */
  public static String showInputDialog(Object message) {
    return Main.hasGui()
            ? JOptionPane.showInputDialog(message)
            : null;
  }

  /**
   * Displays an input dialog with the specified message, title, and message type.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message to be displayed.
   * @param title           The title of the dialog.
   * @param messageType     The type of message to be displayed.
   *
   * @return The input provided by the user, or null if the GUI is not available.
   */
  public static String showInputDialog(Component parentComponent, Object message, String title, int messageType) {
    return Main.hasGui()
        ? JOptionPane.showInputDialog(parentComponent, message, title, messageType)
        : null;
  }

  /**
   * Displays an input dialog with the specified message, title, message type, icon, selection values, and initial
   * selection value.
   *
   * @param parentComponent       The parent component of the dialog.
   * @param message               The message to be displayed.
   * @param title                 The title of the dialog.
   * @param messageType           The type of message to be displayed.
   * @param icon                  The icon to be displayed.
   * @param selectionValues       The array of values the user can select from.
   * @param initialSelectionValue The initial value selected.
   *
   * @return The input provided by the user, or null if the GUI is not available.
   */
  public static Object showInputDialog(Component parentComponent,
                                       Object message,
                                       String title,
                                       int messageType,
                                       Icon icon,
                                       Object[] selectionValues,
                                       Object initialSelectionValue) {
    return Main.hasGui()
        ? JOptionPane.showInputDialog(parentComponent, message, title, messageType,
            icon, selectionValues, initialSelectionValue)
        : null;
  }

  /**
   * Displays an option dialog with the specified message, title, option type, message type, icon, options, and
   * initial value.
   *
   * @param parentComponent The parent component of the dialog.
   * @param message         The message to be displayed.
   * @param title           The title of the dialog.
   * @param optionType      Specifies the set of options available on the dialog.
   * @param messageType     The type of message to be displayed.
   * @param icon            The icon to be displayed.
   * @param options         The array of options the user can select from.
   * @param initialValue    The initial value selected.
   *
   * @return The option chosen by the user, or CLOSED_OPTION if the GUI is not available.
   */
  public static int showOptionDialog(Component parentComponent,
                                     Object message,
                                     String title,
                                     int optionType,
                                     int messageType,
                                     Icon icon,
                                     Object[] options,
                                     Object initialValue) {
    return Main.hasGui()
        ? JOptionPane.showOptionDialog(parentComponent, message, title, optionType,
            messageType, icon, options, initialValue)
        : CLOSED_OPTION;
  }

  public static Frame getFrameForComponent(Component parentComponent) {
    return Main.hasGui()
            ? JOptionPane.getFrameForComponent(parentComponent)
            : null;
  }
}
