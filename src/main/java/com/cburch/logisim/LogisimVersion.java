/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim;

/** Logisim follows Semantic Versioning https://semver.org/ */
public class LogisimVersion {
  private int major = 0;
  private int minor = 0;
  private int patch = 0;
  private String suffix = "";

  public LogisimVersion(int major, int minor, int patch) {
    this(major, minor, patch, "");
  }

  public LogisimVersion(int major, int minor, int patch, String suffix) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;
    if (suffix == null) {
      suffix = "";
    }
    this.suffix = suffix.strip();
  }

  /**
   * Parse a string containing a version number and returns the corresponding LogisimVersion object.
   * No exception is thrown if the version string contains non-integers, because literal values are
   * allowed.
   *
   * <p>Supported version string formats are `X.Y.Z` or `X.Y.Z-SUFFIX`
   *
   * @return LogisimVersion built from the string passed as parameter
   */
  public static LogisimVersion fromString(String versionString) {
    var major = 0;
    var minor = 0;
    var patch = 0;
    var suffix = "";

    // Let's see if we have suffix segment or not.
    final var segments = versionString.split("-");

    if (segments.length > 0) {
      if (segments.length == 2) {
        suffix = segments[1].strip();
      }

      final var parts = segments[0].split("\\.");
      try {
        if (parts.length >= 1) major = Integer.parseInt(parts[0]);
        if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
        if (parts.length >= 3) patch = Integer.parseInt(parts[2]);
      } catch (NumberFormatException ignored) {
        // Just ignore. We will just fall back to `0`
      }
    }

    return new LogisimVersion(major, minor, patch, suffix);
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
    String result = major + "." + minor + "." + patch;
    if (!suffix.equals("")) {
      result += "-" + suffix;
    }
    return result;
  }
}
