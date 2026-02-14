/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import com.cburch.logisim.generated.BuildInfo;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.prefs.AppPreferences;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import com.formdev.flatlaf.FlatLightLaf;
import com.formdev.flatlaf.themes.FlatMacDarkLaf;
import com.formdev.flatlaf.themes.FlatMacLightLaf;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;
import javax.swing.plaf.FontUIResource;

public class Main {
  /**
   * Application entry point.
   *
   * @param args Optional arguments.
   */
  public static void main(String[] args) {
    // To print debug log level information, run with:
    //    java -Dorg.slf4j.simpleLogger.defaultLogLevel=debug
    // or uncomment next line
    // System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "debug");

    System.setProperty("apple.awt.application.name", BuildInfo.name);
    try {
      if (!GraphicsEnvironment.isHeadless()) {
        FlatLightLaf.installLafInfo();
        FlatDarkLaf.installLafInfo();
        FlatIntelliJLaf.installLafInfo();
        FlatDarculaLaf.installLafInfo();
        FlatMacLightLaf.installLafInfo();
        FlatMacDarkLaf.installLafInfo();
        
        UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
        
        // Apply global font preference
        final var appFont = AppPreferences.APP_FONT.get();
        if (appFont != null && !appFont.isBlank()) {
          updateGlobalFont(appFont);
        } else {
          UIManager.put(
              "ToolTip.font",
            new FontUIResource("SansSerif", Font.BOLD, AppPreferences.getScaled(12)));
        }
      }
    } catch (ClassNotFoundException
        | UnsupportedLookAndFeelException
        | IllegalAccessException
        | InstantiationException e) {
      e.printStackTrace();
    }

    final var startup = Startup.parseArgs(args);
    if (startup == null) System.exit(10);
    if (startup.shallQuit()) System.exit(0);

    try {
      startup.run();
    } catch (Throwable e) {
      final var strWriter = new StringWriter();
      final var printWriter = new PrintWriter(strWriter);
      e.printStackTrace(printWriter);
      OptionPane.showMessageDialog(null, strWriter.toString());
      System.exit(100);
    }
  }

  public static boolean headless = false;

  // FloppyDisk unicode character: https://charbase.com/1f4be-unicode-floppy-disk
  public static final String DIRTY_MARKER = "\ud83d\udcbe";

  public static boolean hasGui() {
    return !headless;
  }

  /**
   * Updates all UI defaults to use the specified font family.
   * It ensures that the preferred font is applied across
   * all Look and Feels (FlatLaf, Metal, Nimbus, etc.).
   */
  private static void updateGlobalFont(String appFont) {
    try {
      com.formdev.flatlaf.FlatLaf.setPreferredFontFamily(appFont);
    } catch (Throwable ignored) {
      // Ignore if FlatLaf is not available or fails
    }

    // This catches standard Swing themes (e.g., Metal, System)
    final var defaults = UIManager.getDefaults();
    final var keysEnumeration = defaults.keys();
    final var keysList = new java.util.ArrayList<Object>();
    while (keysEnumeration.hasMoreElements()) {
      keysList.add(keysEnumeration.nextElement());
    }

    for (final var key : keysList) {
      final var value = defaults.get(key);
      if (value instanceof FontUIResource) {
        final var originalFont = (Font) value;
        // If the user selected a weighted font (e.g., Medium, Light), force PLAIN to avoid faux bolding.
        // Otherwise (standard font), respect the component's original style (Bold vs Plain).
        final var preferredStyle = AppPreferences.getPreferredFontStyle(appFont);
        final var newStyle = (preferredStyle == Font.PLAIN) 
            ? Font.PLAIN 
            : originalFont.getStyle();
            
        defaults.put(key, new FontUIResource(appFont, newStyle, originalFont.getSize()));
      }
    }

    UIManager.put("ToolTip.font", 
        new FontUIResource(appFont, AppPreferences.getPreferredFontStyle(appFont), AppPreferences.getScaled(12)));
  }
}
