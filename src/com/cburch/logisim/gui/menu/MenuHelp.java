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
package com.cburch.logisim.gui.menu;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.net.URL;
import java.util.Locale;

import javax.help.HelpSet;
import javax.help.JHelp;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.start.About;
import com.cburch.logisim.util.MacCompatibility;

class MenuHelp extends JMenu implements ActionListener {

	private static final long serialVersionUID = 1L;
	private LogisimMenuBar menubar;
	private JMenuItem tutorial = new JMenuItem();
	private JMenuItem guide = new JMenuItem();
	private JMenuItem library = new JMenuItem();
	private JMenuItem about = new JMenuItem();
	private HelpSet helpSet;
	private String helpSetUrl = "";
	private JHelp helpComponent;
	private LFrame helpFrame;

	public MenuHelp(LogisimMenuBar menubar) {
		this.menubar = menubar;

		tutorial.addActionListener(this);
		guide.addActionListener(this);
		library.addActionListener(this);
		about.addActionListener(this);

		add(tutorial);
		add(guide);
		add(library);
		if (!MacCompatibility.isAboutAutomaticallyPresent()) {
			addSeparator();
			add(about);
		}
	}

	public void actionPerformed(ActionEvent e) {
		Object src = e.getSource();
		if (src == guide) {
			showHelp("guide");
		} else if (src == tutorial) {
			showHelp("tutorial");
		} else if (src == library) {
			showHelp("libs");
		} else if (src == about) {
			About.showAboutDialog(menubar.getParentWindow());
		}
	}

	private void disableHelp() {
		guide.setEnabled(false);
		tutorial.setEnabled(false);
		library.setEnabled(false);
	}

	private void loadBroker() {
		String helpUrl = Strings.get("helpsetUrl");
		if (helpUrl == null) {
			helpUrl = "doc/doc_en.hs";
		}
		if (helpSet == null || helpFrame == null || !helpUrl.equals(helpSetUrl)) {
			ClassLoader loader = MenuHelp.class.getClassLoader();
			try {
				URL hsURL = HelpSet.findHelpSet(loader, helpUrl);
				if (hsURL == null) {
					disableHelp();
					JOptionPane.showMessageDialog(menubar.getParentWindow(),
							Strings.get("helpNotFoundError"));
					return;
				}
				helpSetUrl = helpUrl;
				helpSet = new HelpSet(null, hsURL);
				helpComponent = new JHelp(helpSet);
				if (helpFrame == null) {
					helpFrame = new LFrame();
					helpFrame.setTitle(Strings.get("helpWindowTitle"));
					helpFrame.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
					helpFrame.getContentPane().add(helpComponent);
					helpFrame.pack();
				} else {
					helpFrame.getContentPane().removeAll();
					helpFrame.getContentPane().add(helpComponent);
					helpComponent.revalidate();
				}
			} catch (Exception e) {
				disableHelp();
				e.printStackTrace();
				JOptionPane.showMessageDialog(menubar.getParentWindow(),
						Strings.get("helpUnavailableError"));
				return;
			}
		}
	}

	public void localeChanged() {
		this.setText(Strings.get("helpMenu"));
		if (helpFrame != null) {
			helpFrame.setTitle(Strings.get("helpWindowTitle"));
		}
		tutorial.setText(Strings.get("helpTutorialItem"));
		guide.setText(Strings.get("helpGuideItem"));
		library.setText(Strings.get("helpLibraryItem"));
		about.setText(Strings.get("helpAboutItem"));
		if (helpFrame != null) {
			helpFrame.setLocale(Locale.getDefault());
			loadBroker();
		}
	}

	private void showHelp(String target) {
		loadBroker();
		try {
			helpComponent.setCurrentID(target);
			helpFrame.toFront();
			helpFrame.setVisible(true);
		} catch (Exception e) {
			disableHelp();
			e.printStackTrace();
			JOptionPane.showMessageDialog(menubar.getParentWindow(),
					Strings.get("helpDisplayError"));
		}
	}
}
