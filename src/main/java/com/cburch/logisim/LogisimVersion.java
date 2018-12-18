/*******************************************************************************
 * This file is part of logisim-evolution.
 *
 *   logisim-evolution is free software: you can redistribute it and/or modify
 *   it under the terms of the GNU General Public License as published by
 *   the Free Software Foundation, either version 3 of the License, or
 *   (at your option) any later version.
 *
 *   logisim-evolution is distributed in the hope that it will be useful,
 *   but WITHOUT ANY WARRANTY; without even the implied warranty of
 *   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *   GNU General Public License for more details.
 *
 *   You should have received a copy of the GNU General Public License
 *   along with logisim-evolution.  If not, see <http://www.gnu.org/licenses/>.
 *
 *   Original code by Carl Burch (http://www.cburch.com), 2011.
 *   Subsequent modifications by :
 *     + Haute École Spécialisée Bernoise
 *       http://www.bfh.ch
 *     + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *       http://hepia.hesge.ch/
 *     + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *       http://www.heig-vd.ch/
 *   The project is currently maintained by :
 *     + REDS Institute - HEIG-VD
 *       Yverdon-les-Bains, Switzerland
 *       http://reds.heig-vd.ch
 *******************************************************************************/

package com.cburch.logisim;

public class LogisimVersion {
	/**
	 * Create a new version object for the current Logisim instance (the
	 * constructor is private) where the revision number is set to its default
	 * value and no variant is used
	 */
	public static LogisimVersion get(int major, int minor, int release) {
		return (get(major, minor, release, FINAL_REVISION, ""));
	}

	/**
	 * Create a new version object for the current Logisim instance (the
	 * constructor is private) where no variant is used
	 */
	public static LogisimVersion get(int major, int minor, int release,
			int revision) {
		return (get(major, minor, release, revision, ""));
	}

	/**
	 * Create a new version object for the current Logisim instance (the
	 * constructor is private)
	 */
	public static LogisimVersion get(int major, int minor, int release,
			int revision, String variant) {
		return (new LogisimVersion(major, minor, release, revision, variant));
	}

	/**
	 * Create a new version object for the current Logisim instance (the
	 * constructor is private) where the revision field is set to its default
	 * value
	 */
	public static LogisimVersion get(int major, int minor, int release,
			String variant) {
		return (get(major, minor, release, FINAL_REVISION, variant));
	}

	/**
	 * Parse a string containing a version number and returns the corresponding
	 * LogisimVersion object. No exception is thrown if the version string
	 * contains non-integers, because literal values are allowed.
	 *
	 * @return LogisimVersion built from the string passed as parameter
	 */
	public static LogisimVersion parse(String versionString) {
		String[] parts = versionString.split("\\.");
		int major = 0;
		int minor = 0;
		int release = 0;
		int revision = FINAL_REVISION;
		String variant = "";

		if (versionString.isEmpty()) {
			// Return the default values for an empty version string
			return (new LogisimVersion(major, minor, release, revision, variant));
		}

		try {
			if (parts.length >= 1)
				major = Integer.parseInt(parts[0]);
			if (parts.length >= 2)
				minor = Integer.parseInt(parts[1]);
			if (parts.length >= 3)
				release = Integer.parseInt(parts[2]);
			if (parts.length >= 4)
				revision = Integer.parseInt(parts[3]);
			if (parts.length >= 5)
				variant = parts[4];
		} catch (NumberFormatException e) {
		}
		return (new LogisimVersion(major, minor, release, revision, variant));
	}

	public static final int FINAL_REVISION = Integer.MAX_VALUE / 4;
	private int major;

	private int minor;

	private int release;

	private int revision;

	private String variant;

	private String repr;

	private LogisimVersion(int major, int minor, int release, int revision,
			String variant) {
		this.major = major;
		this.minor = minor;
		this.release = release;
		this.revision = revision;
		this.variant = variant;
		this.repr = null;
	}

	/**
	 * Compare two Logisim version, returning whether the one passed as
	 * parameter is newer than the current one or not
	 *
	 * @return Negative value if the current version is older than the one
	 *         passed as parameter
	 */
	public int compareTo(LogisimVersion other) {
		int ret = this.major - other.major;

		if (ret != 0) {
			return ret;
		} else {
			ret = this.minor - other.minor;
			if (ret != 0) {
				return (ret);
			} else {
				ret = this.release - other.release;
				if (ret != 0) {
					return (ret);
				} else {
					ret = this.revision - other.revision;
					if (ret != 0) {
						return (ret);
					} else {
						return (this.variant.compareTo(other.variant));
					}
				}
			}
		}
	}

	/**
	 * Compares two Logisim version numbers.
	 */
	@Override
	public boolean equals(Object other) {
		if (other instanceof LogisimVersion) {
			LogisimVersion o = (LogisimVersion) other;
			return (this.major == o.major && this.minor == o.minor
					&& this.release == o.release && this.revision == o.revision && this.variant == o.variant);
		} else {
			return (false);
		}
	}

	/**
	 * Build the hash code starting from the version number
	 */
	@Override
	public int hashCode() {
		int ret = major * 31 + minor;
		ret = ret * 31 + release;
		ret = ret * 31 + revision;
		return (ret);
	}

	/**
	 * If the considered Logisim version includes a tracker, returns true.
	 * Assumption: the tracker is identified by a variant equals to "t"
	 */
	public boolean hasTracker() {
		return (variant.equals("t"));
	}

	public String mainVersion() {
		return (major + "." + minor + "." + release);
	}

	public String rev() {
		if (revision != FINAL_REVISION) {
			return ("rev. " + revision);
		} else {
			return ("");
		}
	}

	@Override
	public String toString() {
		String ret = repr;

		if (ret == null) {
			ret = major + "." + minor + "." + release;
			if (revision != FINAL_REVISION)
				ret += "." + revision;
			if (variant != "")
				ret += "." + variant;
			repr = ret;
		}
		return (ret);
	}

}
