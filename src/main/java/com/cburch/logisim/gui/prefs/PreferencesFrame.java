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

package com.cburch.logisim.gui.prefs;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;

import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;

public class PreferencesFrame extends LFrame {
	private class MyListener implements ActionListener, LocaleListener {
		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			if (src == close) {
				WindowEvent e = new WindowEvent(PreferencesFrame.this,
						WindowEvent.WINDOW_CLOSING);
				PreferencesFrame.this.processWindowEvent(e);
			}
		}

		public void localeChanged() {
			setTitle(Strings.get("preferencesFrameTitle"));
			for (int i = 0; i < panels.length; i++) {
				tabbedPane.setTitleAt(i, panels[i].getTitle());
				tabbedPane.setToolTipTextAt(i, panels[i].getToolTipText());
				panels[i].localeChanged();
			}
			close.setText(Strings.get("closeButton"));
		}
	}

	private static class WindowMenuManager extends WindowMenuItemManager
			implements LocaleListener {
		private PreferencesFrame window = null;

		WindowMenuManager() {
			super(Strings.get("preferencesFrameMenuItem"), true);
			LocaleManager.addLocaleListener(this);
		}

		@Override
		public JFrame getJFrame(boolean create) {
			if (create) {
				if (window == null) {
					window = new PreferencesFrame();
					frameOpened(window);
				}
			}
			return window;
		}

		public void localeChanged() {
			setText(Strings.get("preferencesFrameMenuItem"));
		}
	}

	public static void initializeManager() {
		MENU_MANAGER = new WindowMenuManager();
	}

	public static void showPreferences() {
		JFrame frame = MENU_MANAGER.getJFrame(true);
		frame.setVisible(true);
	}

	private static final long serialVersionUID = 1L;

	private static WindowMenuManager MENU_MANAGER = null;

	private MyListener myListener = new MyListener();
	private OptionsPanel[] panels;
	private JTabbedPane tabbedPane;

	private JButton close = new JButton();

	private PreferencesFrame() {
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		setJMenuBar(null);

		panels = new OptionsPanel[] { new TemplateOptions(this),
				new IntlOptions(this), new WindowOptions(this),
				new LayoutOptions(this), new ExperimentalOptions(this),
				new SoftwaresOptions(this), new FPGAOptions(this), };
		tabbedPane = new JTabbedPane();
		int intlIndex = -1;
		for (int index = 0; index < panels.length; index++) {
			OptionsPanel panel = panels[index];
			tabbedPane.addTab(panel.getTitle(), null, panel,
					panel.getToolTipText());
			if (panel instanceof IntlOptions)
				intlIndex = index;
		}

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(close);
		close.addActionListener(myListener);

		Container contents = getContentPane();
		tabbedPane.setPreferredSize(new Dimension(Toolkit.getDefaultToolkit().getScreenSize().width/2, 
				Toolkit.getDefaultToolkit().getScreenSize().height/2));
		contents.add(tabbedPane, BorderLayout.CENTER);
		contents.add(buttonPanel, BorderLayout.SOUTH);

		if (intlIndex >= 0)
			tabbedPane.setSelectedIndex(intlIndex);

		LocaleManager.addLocaleListener(myListener);
		myListener.localeChanged();
		pack();
	}
}
