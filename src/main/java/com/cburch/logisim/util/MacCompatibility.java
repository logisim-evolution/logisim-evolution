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

import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
// import net.roydesign.mac.MRJAdapter;

import javax.swing.JMenuBar;

public class MacCompatibility {
	
	private static boolean runningOnMac = System.getProperty("os.name").toLowerCase().indexOf("mac") >= 0;
	private static boolean usingScreenMenuBar = runningOnMac;
	
	public static boolean isRunningOnMac() {
		return runningOnMac;
	}
	
	public static boolean isAboutAutomaticallyPresent() {
		return runningOnMac;
	}

	public static boolean isPreferencesAutomaticallyPresent() {
		return runningOnMac;
	}

	public static boolean isQuitAutomaticallyPresent() {
		return runningOnMac;
	}

	public static boolean isSwingUsingScreenMenuBar() {
		return usingScreenMenuBar;
	}


	public static void setFileCreatorAndType(File dest, String app, String type)
			throws IOException {
		// DHH File creator and type have never been required on Mac OS X. Mac OS X uses the file extension.
		// This method is a hold over from Mac OS 9 and Classic. It should be removed.
		// Note you can then remove the imports for java.io from this file.
	}

	public static void setFramelessJMenuBar(JMenuBar menubar) {
		try {
			//DHH This method allows the app to run without a frame on the Mac. The menu will still show.
			if (runningOnMac) {
				Desktop.getDesktop().setDefaultMenuBar(menubar);
			}
		} catch (Exception t) {
			usingScreenMenuBar = false;
		}
	}

	// DHH I would eliminate this reference. Use isRunningOnMac() if needed.
	// But the only place I see this used is in CanvasPane. It sets always show scroll bars.
	// That is not necessary in Mac UI. Just use the default for everybody.
	public static final double mrjVersion = runningOnMac ? 0.0 : -1.0;
	
	public static double javaVersion() {
		double version = Double.parseDouble(System.getProperty("java.specification.version"));
		return version;
	}

	// DHH This shouldn't be necessary
	/*
	static {
        double versionValue;
        try {
                versionValue = MRJAdapter.mrjVersion;
        } catch (Exception t) {
                versionValue = 0.0;
        }
        mrjVersion = versionValue;
	}       
	*/
	private MacCompatibility() {
	}

}
