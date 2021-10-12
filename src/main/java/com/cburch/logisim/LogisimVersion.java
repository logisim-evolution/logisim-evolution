/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

import java.util.regex.Pattern;

/**
 * Version string handling class. Supported version formats:
 *
 * - X.Y.Z
 * - X.Y.Zsuffix
 * - X.Y.Z-suffix
 *
 * where X, Y, Z must be positive integer, while suffix is optional
 * string starting with a letter. Suffix can be separated from X.Y.Z part
 * with (optional) dash ("-") character.
 *
 * NOTE: toString() form uses no dash separator form by default, however
 * if object is obtained via `fromString()` call, and version string contains
 * the "-" separator, output returned by `toString()` will also include
 * separator character.
 */
public class LogisimVersion {
  private int major = 0;
  private int minor = 0;
  private int patch = 0;
  private String separator = "";
  private String suffix = "";

  private LogisimVersion() {
    // private
  }

  public LogisimVersion(int major, int minor, int patch) {
    this(major, minor, patch, "");
  }

  public LogisimVersion(int major, int minor, int patch, String suffix) {
    suffix = (suffix == null) ? "" : suffix;
    final var versionString = String.format("%d.%d.%d%s", major, minor, patch, suffix);
    initFromVersionString(versionString);
  }

  /**
   * Parse a string containing a version number and returns the corresponding LogisimVersion object.
   * No exception is thrown if the version string contains non-integers, because literal values are
   * allowed.
   *
   * <p>Supported version string formats are `X.Y.Z`, `X.Y.Z-suffix` or `X.Y.Zsuffix`.
   *
   * @return LogisimVersion built from the string passed as parameter
   */
  public static LogisimVersion fromString(String versionString) {
    return new LogisimVersion().initFromVersionString(versionString);
  }

  private LogisimVersion initFromVersionString(String versionString) throws IllegalArgumentException {
    var major = 0;
    var minor = 0;
    var patch = 0;
    var separator = "";
    var suffix = "";

    var pattern = "^(\\d+.\\d+.\\d+)(.*)$";
    var m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(versionString);
    if (m.matches()) {
      final var verStr = m.group(1);
      final var sufStr = m.group(2);

      final var parts = m.group(1).split("\\.");
      try {
        if (parts.length >= 1) major = Integer.parseInt(parts[0]);
        if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
        if (parts.length >= 3) patch = Integer.parseInt(parts[2]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException(String.format("Version segments must be non-negative integers, '%s' found.", verStr));
      }

      // suffix part
      if (sufStr != null) {
        if (sufStr.length() == 1) {
          pattern = "^[a-z]+$";
          m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sufStr);
          if (!m.matches()) {
            throw new IllegalArgumentException(
                String.format("Suffix must start with a letter, '%s' found.", sufStr));
          }
          suffix = sufStr;
        } else if (sufStr.length() > 1) {
          pattern = "^(-)?([a-z][a-z\\d]*)$";
          m = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE).matcher(sufStr);
          if (!m.matches()) {
            throw new IllegalArgumentException(
                String.format("Invalid version suffix format. '%s' found.", sufStr));
          }
          final var sep = m.group(1);
          separator = (sep != null) ? sep : "";
          final var s = m.group(2);
          suffix = (s != null) ? s : "";
        }
      }
    }

    this.major = major;
    this.minor = minor;
    this.patch = patch;
    this.separator = separator;
    this.suffix = suffix;

    return this;
  }

  /**
   * Compare two Logisim versions, returning positive non-zero value whether the one passed as
   * parameter is newer than the current one, equal (0) or older (negative non-zero value).
   *
   * @return Negative value if the current version is older than the one passed as parameter, zero
   *     if equal, positive if newer.
   */
  public int compareTo(LogisimVersion other) {
    var result = this.major - other.major;

    if (result == 0) {
      result = this.minor - other.minor;
      if (result == 0) {
        result = this.patch - other.patch;
      }
    }

    // TODO: we do not understand what suffix means. The only rule here is that "no suffix"
    // means stable version, while presence of suffix indicates unstable one, so in case
    // all other values are equal, "no suffix" is considered newer.
    if (result == 0) {
      if (this.suffix.equals("") && !other.suffix.equals("")) {
        result = 1; // this one is newer
      } else if (!this.suffix.equals("") && other.suffix.equals("")) {
        result = -1; // this one is older
      }
    }

    return result;
  }

  /**
   * Returns TRUE if version is considered stable, false otherwise. Note the implementation is
   * plaind dumb and relies on presence of version suffix (which, if not empty means non-stable
   * release).
   *
   * @return
   */
  public boolean isStable() {
    return suffix.equals("");
  }

  /** Build the hash code starting from the version number. */
  @Override
  public int hashCode() {
    return (major * 31 + minor) * 31 + patch + suffix.hashCode();
  }

  @Override
  public String toString() {
    var sfx = "";
    if (suffix != "") {
      sfx = separator + suffix;
    }
    return format(major, minor, patch, sfx);
  }

  public static String format(int major, int minor, int patch) {
    return format(major, minor, patch, "");
  }
  public static String format(int major, int minor, int patch, String suffix) {
    var result = String.format("%d.%d.%d", major, minor, patch);
    if (!suffix.equals("")) result += suffix;
    return result;
  }

}
