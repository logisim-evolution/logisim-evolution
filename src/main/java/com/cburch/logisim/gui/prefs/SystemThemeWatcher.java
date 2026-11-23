/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.SystemTheme;
import com.formdev.flatlaf.FlatDarculaLaf;
import com.formdev.flatlaf.FlatIntelliJLaf;
import java.beans.PropertyChangeListener;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.prefs.Preferences;
import javax.swing.SwingUtilities;

public final class SystemThemeWatcher {
  private static final long POLL_SECONDS = 3;

  // Kept in a field because preference listeners are held weakly.
  private static final PropertyChangeListener listener = event -> {
    // When the box is ticked, keep the current theme for its mode before following the system.
    if (AppPreferences.FollowSystemTheme.get()) {
      remember(AppPreferences.LookAndFeel.get());
    }
    update();
  };

  private static ScheduledExecutorService poller;
  private static volatile SystemTheme.Result lastResult;

  private SystemThemeWatcher() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /** Starts watching if the preference is on, and follows later changes to it. */
  public static void init() {
    AppPreferences.FollowSystemTheme.addPropertyChangeListener(listener);
    update();
  }

  /**
   * Switches the stored look and feel to the remembered one for the system theme. An unknown
   * system theme leaves it unchanged.
   *
   * @return true if the look and feel was changed
   */
  public static boolean useSystemTheme(SystemTheme.Result result) {
    final var current = AppPreferences.LookAndFeel.get();
    final var chosen = lookAndFeelFor(AppPreferences.getPrefs(), result, current);
    if (chosen.equals(current)) return false;
    AppPreferences.LookAndFeel.set(chosen);
    return true;
  }

  /** Remembers a look and feel as the user's own for its mode. */
  public static void remember(String lookAndFeel) {
    remember(AppPreferences.getPrefs(), lookAndFeel, AppPreferences.isDarkTheme(lookAndFeel));
  }

  static void remember(Preferences node, String lookAndFeel, boolean dark) {
    node.put(dark ? AppPreferences.DARK_LOOK_AND_FEEL_KEY
        : AppPreferences.LIGHT_LOOK_AND_FEEL_KEY, lookAndFeel);
  }

  static String rememberedLookAndFeel(Preferences node, boolean dark) {
    return dark
        ? node.get(AppPreferences.DARK_LOOK_AND_FEEL_KEY, FlatDarculaLaf.class.getName())
        : node.get(AppPreferences.LIGHT_LOOK_AND_FEEL_KEY, FlatIntelliJLaf.class.getName());
  }

  // Only reads the node, so an automatic switch never changes what the user chose.
  static String lookAndFeelFor(Preferences node, SystemTheme.Result result, String current) {
    if (result == SystemTheme.Result.UNKNOWN) return current;
    return rememberedLookAndFeel(node, result == SystemTheme.Result.DARK);
  }

  private static synchronized void update() {
    if (AppPreferences.FollowSystemTheme.get()) {
      if (poller == null) {
        lastResult = null;
        poller = Executors.newSingleThreadScheduledExecutor(task -> {
          final var thread = new Thread(task, "SystemThemeWatcher");
          thread.setDaemon(true);
          return thread;
        });
        poller.scheduleWithFixedDelay(SystemThemeWatcher::poll, 0, POLL_SECONDS, TimeUnit.SECONDS);
      }
    } else if (poller != null) {
      poller.shutdownNow();
      poller = null;
    }
  }

  // Runs on the poller thread, because detection starts a process.
  private static void poll() {
    // An exception that escapes would cancel all later polls of the scheduled task.
    try {
      final var result = SystemTheme.detect();
      if (result == SystemTheme.Result.UNKNOWN || result == lastResult) return;
      lastResult = result;
      SwingUtilities.invokeLater(() -> apply(result));
    } catch (RuntimeException e) {
      // Try again at the next poll.
    }
  }

  private static void apply(SystemTheme.Result result) {
    if (!AppPreferences.FollowSystemTheme.get() || !useSystemTheme(result)) return;
    AppPreferences.applyThemeColors();
    WindowOptions.applyLookAndFeelGlobally(AppPreferences.LookAndFeel.get());
  }
}
