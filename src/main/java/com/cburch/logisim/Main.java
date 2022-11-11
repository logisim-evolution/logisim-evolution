/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import com.cburch.logisim.file.Loader;
import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.gui.start.GuiInterface;
import com.cburch.logisim.gui.start.TtyInterface;
import com.cburch.logisim.prefs.AppPreferences;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

public class Main {
  public static final int EXITCODE_OK = 0;
  public static final int EXITCODE_ARG_ERROR = 1;
  public static final int EXITCODE_OSCILLATION = 1;
  public static final int EXITCODE_TEST_FAIL = -1;
  public static final int EXITCODE_UNCAUGHT_EXCEPTION = -1; // should never happen, theoretically

  /**
   * Application entry point.
   *
   * @param args Optional arguments.
   */
  public static void main(String[] args) {
    System.setProperty("apple.awt.application.name", BuildInfo.name);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        FlatLightLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();

        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        UIManager.put(
            "ToolTip.font",
            new FontUIResource("SansSerif", Font.BOLD, AppPreferences.getScaled(12)));
      }
    } catch (ClassNotFoundException
        | UnsupportedLookAndFeelException
        | IllegalAccessException
        | InstantiationException e) {
      e.printStackTrace();
    }

    final var startup = new Startup(args);
    final var loader = new Loader(null);
    int exitCode = EXITCODE_OK;
    switch (startup.task) {
      case NONE:
        break;

      case ERROR:
        exitCode = EXITCODE_ARG_ERROR;
        break;

      case GUI:
        try {
          final var gui = new GuiInterface(startup);
          exitCode = gui.run(loader);
        } catch (Throwable e) {
          final var strWriter = new StringWriter();
          final var printWriter = new PrintWriter(strWriter);
          e.printStackTrace(printWriter);
          OptionPane.showMessageDialog(null, strWriter.toString());
          exitCode = EXITCODE_UNCAUGHT_EXCEPTION;
        }
        break;

      default:
        useGui = false;
        try {
          final var tty = new TtyInterface(startup);
          exitCode = tty.run(loader);
        } catch (Exception e) {
          e.printStackTrace();
          exitCode = EXITCODE_UNCAUGHT_EXCEPTION;
        }
        break;
    }

    if (exitCode != EXITCODE_OK) System.exit(exitCode);
  }

  // FIXME: figure out how to NOT have this global
  public static boolean useGui = true;

  // FloppyDisk unicode character: https://charbase.com/1f4be-unicode-floppy-disk
  public static final String DIRTY_MARKER = "\ud83d\udcbe";
}
