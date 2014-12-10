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

package com.cburch.logisim.util;

import java.io.File;
import java.io.IOException;

import javax.swing.JMenuBar;

import net.roydesign.mac.MRJAdapter;

public class MacCompatibility {
	public static boolean isAboutAutomaticallyPresent() {
		try {
			return MRJAdapter.isAboutAutomaticallyPresent();
		} catch (Exception t) {
			return false;
		}
	}

	public static boolean isPreferencesAutomaticallyPresent() {
		try {
			return MRJAdapter.isPreferencesAutomaticallyPresent();
		} catch (Exception t) {
			return false;
		}
	}

	public static boolean isQuitAutomaticallyPresent() {
		try {
			return MRJAdapter.isQuitAutomaticallyPresent();
		} catch (Exception t) {
			return false;
		}
	}

	public static boolean isSwingUsingScreenMenuBar() {
		try {
			return MRJAdapter.isSwingUsingScreenMenuBar();
		} catch (Exception t) {
			return false;
		}
	}

	public static void setFileCreatorAndType(File dest, String app, String type)
			throws IOException {
		IOException ioExcept = null;
		try {
			try {
				MRJAdapter.setFileCreatorAndType(dest, app, type);
			} catch (IOException e) {
				ioExcept = e;
			}
		} catch (Exception t) {
		}
		if (ioExcept != null)
			throw ioExcept;
	}

	public static void setFramelessJMenuBar(JMenuBar menubar) {
		try {
			MRJAdapter.setFramelessJMenuBar(menubar);
		} catch (Exception t) {
		}
	}

	public static final double mrjVersion;

	static {
		double versionValue;
		try {
			versionValue = MRJAdapter.mrjVersion;
		} catch (Exception t) {
			versionValue = 0.0;
		}
		mrjVersion = versionValue;
	}

	private MacCompatibility() {
	}

}
