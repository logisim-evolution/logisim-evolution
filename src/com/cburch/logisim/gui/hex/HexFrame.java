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

package com.cburch.logisim.gui.hex;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.io.File;
import java.io.IOException;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.hex.HexEditor;
import com.cburch.hex.HexModel;
import com.cburch.logisim.gui.generic.LFrame;
import com.cburch.logisim.gui.menu.LogisimMenuBar;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.JFileChoosers;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import com.cburch.logisim.util.WindowMenuItemManager;

public class HexFrame extends LFrame {
	private class EditListener implements ActionListener, ChangeListener {
		private Clip clip = null;

		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == LogisimMenuBar.CUT) {
				getClip().copy();
				editor.delete();
			} else if (src == LogisimMenuBar.COPY) {
				getClip().copy();
			} else if (src == LogisimMenuBar.PASTE) {
				getClip().paste();
			} else if (src == LogisimMenuBar.DELETE) {
				editor.delete();
			} else if (src == LogisimMenuBar.SELECT_ALL) {
				editor.selectAll();
			}
		}

		private void enableItems(LogisimMenuBar menubar) {
			boolean sel = editor.selectionExists();
			boolean clip = true; // TODO editor.clipboardExists();
			menubar.setEnabled(LogisimMenuBar.CUT, sel);
			menubar.setEnabled(LogisimMenuBar.COPY, sel);
			menubar.setEnabled(LogisimMenuBar.PASTE, clip);
			menubar.setEnabled(LogisimMenuBar.DELETE, sel);
			menubar.setEnabled(LogisimMenuBar.SELECT_ALL, true);
		}

		private Clip getClip() {
			if (clip == null)
				clip = new Clip(editor);
			return clip;
		}

		private void register(LogisimMenuBar menubar) {
			menubar.addActionListener(LogisimMenuBar.CUT, this);
			menubar.addActionListener(LogisimMenuBar.COPY, this);
			menubar.addActionListener(LogisimMenuBar.PASTE, this);
			menubar.addActionListener(LogisimMenuBar.DELETE, this);
			menubar.addActionListener(LogisimMenuBar.SELECT_ALL, this);
			enableItems(menubar);
		}

		public void stateChanged(ChangeEvent e) {
			enableItems((LogisimMenuBar) getJMenuBar());
		}
	}

	private class MyListener implements ActionListener, LocaleListener {
		private File lastFile = null;

		public void actionPerformed(ActionEvent event) {
			Object src = event.getSource();
			if (src == open) {
				JFileChooser chooser = JFileChoosers.createSelected(lastFile);
				chooser.setDialogTitle(Strings.get("openButton"));
				int choice = chooser.showOpenDialog(HexFrame.this);
				if (choice == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						HexFile.open(model, f);
						lastFile = f;
					} catch (IOException e) {
						JOptionPane.showMessageDialog(HexFrame.this,
								e.getMessage(),
								Strings.get("hexOpenErrorTitle"),
								JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (src == save) {
				JFileChooser chooser = JFileChoosers.createSelected(lastFile);
				chooser.setDialogTitle(Strings.get("saveButton"));
				int choice = chooser.showSaveDialog(HexFrame.this);
				if (choice == JFileChooser.APPROVE_OPTION) {
					File f = chooser.getSelectedFile();
					try {
						HexFile.save(f, model);
						lastFile = f;
					} catch (IOException e) {
						JOptionPane.showMessageDialog(HexFrame.this,
								e.getMessage(),
								Strings.get("hexSaveErrorTitle"),
								JOptionPane.ERROR_MESSAGE);
					}
				}
			} else if (src == close) {
				WindowEvent e = new WindowEvent(HexFrame.this,
						WindowEvent.WINDOW_CLOSING);
				HexFrame.this.processWindowEvent(e);
			}
		}

		public void localeChanged() {
			setTitle(Strings.get("hexFrameTitle"));
			open.setText(Strings.get("openButton"));
			save.setText(Strings.get("saveButton"));
			close.setText(Strings.get("closeButton"));
		}
	}

	private class WindowMenuManager extends WindowMenuItemManager implements
			LocaleListener {
		WindowMenuManager() {
			super(Strings.get("hexFrameMenuItem"), false);
			LocaleManager.addLocaleListener(this);
		}

		@Override
		public JFrame getJFrame(boolean create) {
			return HexFrame.this;
		}

		public void localeChanged() {
			setText(Strings.get("hexFrameMenuItem"));
		}
	}

	private static final long serialVersionUID = 1L;

	private WindowMenuManager windowManager = new WindowMenuManager();
	private EditListener editListener = new EditListener();
	private MyListener myListener = new MyListener();
	private HexModel model;
	private HexEditor editor;
	private JButton open = new JButton();
	private JButton save = new JButton();
	private JButton close = new JButton();

	public HexFrame(Project proj, HexModel model) {
		setDefaultCloseOperation(HIDE_ON_CLOSE);

		LogisimMenuBar menubar = new LogisimMenuBar(this, proj);
		setJMenuBar(menubar);
		menubar.disableFile();
		menubar.disableProject();

		this.model = model;
		this.editor = new HexEditor(model);

		JPanel buttonPanel = new JPanel();
		buttonPanel.add(open);
		buttonPanel.add(save);
		buttonPanel.add(close);
		open.addActionListener(myListener);
		save.addActionListener(myListener);
		close.addActionListener(myListener);

		Dimension pref = editor.getPreferredSize();
		JScrollPane scroll = new JScrollPane(editor,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		pref.height = Math.min(pref.height, pref.width * 3 / 2);
		scroll.setPreferredSize(pref);
		scroll.getViewport().setBackground(editor.getBackground());

		Container contents = getContentPane();
		contents.add(scroll, BorderLayout.CENTER);
		contents.add(buttonPanel, BorderLayout.SOUTH);

		LocaleManager.addLocaleListener(myListener);
		myListener.localeChanged();
		pack();

		Dimension size = getSize();
		Dimension screen = getToolkit().getScreenSize();
		if (size.width > screen.width || size.height > screen.height) {
			size.width = Math.min(size.width, screen.width);
			size.height = Math.min(size.height, screen.height);
			setSize(size);
		}

		editor.getCaret().addChangeListener(editListener);
		editor.getCaret().setDot(0, false);
		editListener.register(menubar);
	}

	@Override
	public void setVisible(boolean value) {
		if (value && !isVisible()) {
			windowManager.frameOpened(this);
		}
		super.setVisible(value);
	}
}
