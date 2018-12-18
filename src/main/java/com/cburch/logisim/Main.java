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

import java.awt.GraphicsEnvironment;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;

import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UnsupportedLookAndFeelException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cburch.logisim.gui.start.Startup;
import com.cburch.logisim.prefs.AppPreferences;

public class Main {
	public static void main(String[] args) throws Exception {
		try {
			if (!GraphicsEnvironment.isHeadless())  {
				UIManager.setLookAndFeel(AppPreferences.LookAndFeel.get());
			}
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (InstantiationException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		} catch (UnsupportedLookAndFeelException e) {
			e.printStackTrace();
		}
		Startup startup = Startup.parseArgs(args);
		if (startup == null) {
			System.exit(0);
		} else {
			// If the auto-updater actually performed an update, then quit the
			// program, otherwise continue with the execution
			if (!startup.autoUpdate()) {
				try {
					startup.run();
				} catch (Throwable e) {
					Writer result = new StringWriter();
					PrintWriter printWriter = new PrintWriter(result);
					e.printStackTrace(printWriter);
					if (GraphicsEnvironment.isHeadless()) {
						System.out.println(result.toString());
					} else {
						JOptionPane.showMessageDialog(null, result.toString());
					}
					System.exit(-1);
				}
			}
		}
	}

	final static Logger logger = LoggerFactory.getLogger(Main.class);

	public static final LogisimVersion VERSION = LogisimVersion.get(2, 15, 0,
			LogisimVersion.FINAL_REVISION);

	public static final String VERSION_NAME = VERSION.toString();
	public static final int COPYRIGHT_YEAR = 2018;

	public static boolean ANALYZE = false;
	/**
	 * This flag enables auto-updates. It is true by default, so that users
	 * normally check for updates at startup. On the other hand, this might be
	 * annoying for developers, therefore we let them disable it from the
	 * command line with the '-noupdates' option.
	 */
	public static boolean UPDATE = true;

	/**
	 * URL for the automatic updater
	 */
	public static final String UPDATE_URL = "http://reds-data.heig-vd.ch/logisim-evolution/logisim_evolution_version.xml";

}
