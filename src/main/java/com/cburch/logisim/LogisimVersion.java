/*
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim;

/**
 * Logisim follows Semantic Versioning
 * https://semver.org/
 */
public class LogisimVersion {
  private int major = 0;
  private int minor = 0;
  private int patch = 0;

  public LogisimVersion(int major, int minor, int patch) {
    this.major = major;
    this.minor = minor;
    this.patch = patch;

  }

  /**
   * Parse a string containing a version number and returns the corresponding LogisimVersion object.
   * No exception is thrown if the version string contains non-integers, because literal values are
   * allowed.
   *
   * @return LogisimVersion built from the string passed as parameter
   */
  public static LogisimVersion fromString(String versionString) {
    String[] parts = versionString.split("\\.");
    int major = 0;
    int minor = 0;
    int patch = 0;

    if (versionString.isEmpty()) {
      return new LogisimVersion(major, minor, patch);
    }

    try {
      if (parts.length >= 1) major = Integer.parseInt(parts[0]);
      if (parts.length >= 2) minor = Integer.parseInt(parts[1]);
      if (parts.length >= 3) patch = Integer.parseInt(parts[2]);
    } catch (NumberFormatException ignored) {
    }

    return new LogisimVersion(major, minor, patch);
  }

  /**
   * Compare two Logisim versions, returning positive non-zero value whether the one passed as parameter is newer than the
   * current one, equal (0) or older (negative non-zero value)
   *
   * @return Negative value if the current version is older than the one passed as parameter, zero if equal, positive if newer.
   */
  public int compareTo(LogisimVersion other) {
    int result = this.major - other.major;

    if (result == 0) {
      result = this.minor - other.minor;
      if (result == 0) {
        result = this.patch - other.patch;
      }
    }

    return result;
  }

  /** Build the hash code starting from the version number */
  @Override
  public int hashCode() {
    int ret = major * 31 + minor;
    ret = ret * 31 + patch;
    return ret;
  }

  @Override
  public String toString() {
    return major + "." + minor + "." + patch;
  }
}
