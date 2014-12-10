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

package com.cburch.logisim.gui.start;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
//MAC import java.io.File;

import net.roydesign.event.ApplicationEvent;
import net.roydesign.mac.MRJAdapter;

//MAC import com.apple.eawt.Application;
//MAC import com.apple.eawt.ApplicationAdapter;
import com.cburch.logisim.gui.prefs.PreferencesFrame;
import com.cburch.logisim.proj.ProjectActions;

class MacOsAdapter { // MAC extends ApplicationAdapter {

	private static class MyListener implements ActionListener {
		public void actionPerformed(ActionEvent event) {
			ApplicationEvent event2 = (ApplicationEvent) event;
			int type = event2.getType();
			switch (type) {
			case ApplicationEvent.ABOUT:
				About.showAboutDialog(null);
				break;
			case ApplicationEvent.QUIT_APPLICATION:
				ProjectActions.doQuit();
				break;
			case ApplicationEvent.OPEN_DOCUMENT:
				Startup.doOpen(event2.getFile());
				break;
			case ApplicationEvent.PRINT_DOCUMENT:
				Startup.doPrint(event2.getFile());
				break;
			case ApplicationEvent.PREFERENCES:
				PreferencesFrame.showPreferences();
				break;
			}
		}
	}

	static void addListeners(boolean added) {
		MyListener myListener = new MyListener();
		if (!added)
			MRJAdapter.addOpenDocumentListener(myListener);
		if (!added)
			MRJAdapter.addPrintDocumentListener(myListener);
		MRJAdapter.addPreferencesListener(myListener);
		MRJAdapter.addQuitApplicationListener(myListener);
		MRJAdapter.addAboutListener(myListener);
	}

	/*
	 * MAC public void handleOpenFile(com.apple.eawt.ApplicationEvent event) {
	 * Startup.doOpen(new File(event.getFilename())); }
	 * 
	 * public void handlePrintFile(com.apple.eawt.ApplicationEvent event) {
	 * Startup.doPrint(new File(event.getFilename())); }
	 * 
	 * public void handlePreferences(com.apple.eawt.ApplicationEvent event) {
	 * PreferencesFrame.showPreferences(); }
	 */

	public static void register() {
		// MAC Application.getApplication().addApplicationListener(new
		// MacOsAdapter());
	}
}