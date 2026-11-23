/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.formdev.flatlaf.util.SystemInfo;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;
import java.util.regex.Pattern;

public final class SystemTheme {
  public enum Result { LIGHT, DARK, UNKNOWN }

  private static final long TIMEOUT_MS = 2000;
  private static final Pattern WINDOWS_VALUE =
      Pattern.compile("AppsUseLightTheme\\s+REG_DWORD\\s+0x([0-9a-fA-F]{1,8})(?![0-9a-fA-F])");
  private static final Pattern PORTAL_VALUE = Pattern.compile("uint32\\s+(\\d+)");

  private SystemTheme() {
    throw new IllegalStateException("Utility class. No instantiation allowed.");
  }

  /** Returns the system theme, or UNKNOWN if it could not be read. */
  public static Result detect() {
    if (SystemInfo.isMacOS) {
      return parseMac(run("defaults", "read", "-g", "AppleInterfaceStyle"));
    } else if (SystemInfo.isWindows) {
      return parseWindows(run("reg", "query",
          "HKCU\\Software\\Microsoft\\Windows\\CurrentVersion\\Themes\\Personalize",
          "/v", "AppsUseLightTheme"));
    } else if (SystemInfo.isLinux) {
      return linux(
          run("gdbus", "call", "--session",
              "--dest", "org.freedesktop.portal.Desktop",
              "--object-path", "/org/freedesktop/portal/desktop",
              "--method", "org.freedesktop.portal.Settings.Read",
              "org.freedesktop.appearance", "color-scheme"),
          () -> Files.exists(Path.of("/.flatpak-info"))
              // Inside Flatpak gsettings may not see the host settings and answer "default".
              ? null
              : run("gsettings", "get", "org.gnome.desktop.interface", "color-scheme"));
    }
    return Result.UNKNOWN;
  }

  // The parse methods get null if the command could not run, and an empty string if it ran but
  // printed nothing. On macOS and Windows the latter means that the setting is not present,
  // which is how light mode shows up.
  static Result parseMac(String output) {
    if (output == null) return Result.UNKNOWN;
    final var text = output.trim();
    if (text.isEmpty()) return Result.LIGHT;
    return text.equalsIgnoreCase("Dark") ? Result.DARK : Result.UNKNOWN;
  }

  static Result parseWindows(String output) {
    if (output == null) return Result.UNKNOWN;
    final var matcher = WINDOWS_VALUE.matcher(output);
    if (matcher.find()) {
      return Long.parseLong(matcher.group(1), 16) == 0 ? Result.DARK : Result.LIGHT;
    }
    return output.trim().isEmpty() ? Result.LIGHT : Result.UNKNOWN;
  }

  // The portal returns 0 (no preference), 1 (dark) or 2 (light).
  static Result parseLinux(String output) {
    if (output == null) return Result.UNKNOWN;
    final var matcher = PORTAL_VALUE.matcher(output);
    if (!matcher.find()) return Result.UNKNOWN;
    switch (matcher.group(1)) {
      case "1":
        return Result.DARK;
      case "0":
      case "2":
        return Result.LIGHT;
      default:
        return Result.UNKNOWN;
    }
  }

  static Result parseGsettings(String output) {
    if (output == null) return Result.UNKNOWN;
    final var text = output.trim().replace("'", "");
    switch (text) {
      case "prefer-dark":
        return Result.DARK;
      case "prefer-light":
      case "default":
        return Result.LIGHT;
      default:
        return Result.UNKNOWN;
    }
  }

  // The gsettings command only runs if the portal gave no usable answer.
  static Result linux(String portalOutput, Supplier<String> gsettingsOutput) {
    final var result = parseLinux(portalOutput);
    if (result != Result.UNKNOWN) return result;
    return parseGsettings(gsettingsOutput.get());
  }

  private static String run(String... command) {
    try {
      final var process = new ProcessBuilder(command)
          .redirectError(ProcessBuilder.Redirect.DISCARD)
          .start();
      if (!process.waitFor(TIMEOUT_MS, TimeUnit.MILLISECONDS)) {
        process.destroyForcibly();
        return null;
      }
      if (process.exitValue() != 0) return "";
      return new String(process.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    } catch (InterruptedException e) {
      Thread.currentThread().interrupt();
      return null;
    } catch (Exception e) {
      // The command is missing or failed to start.
      return null;
    }
  }
}
