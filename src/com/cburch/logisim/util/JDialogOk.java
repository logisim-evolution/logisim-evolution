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

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dialog;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JPanel;

public abstract class JDialogOk extends JDialog {
	private class MyListener extends WindowAdapter implements ActionListener {
		public void actionPerformed(ActionEvent e) {
			Object src = e.getSource();
			if (src == ok) {
				okClicked();
				dispose();
			} else if (src == cancel) {
				cancelClicked();
				dispose();
			}
		}

		@Override
		public void windowClosing(WindowEvent e) {
			JDialogOk.this.removeWindowListener(this);
			cancelClicked();
			dispose();
		}
	}

	private static final long serialVersionUID = 1L;

	private JPanel contents = new JPanel(new BorderLayout());
	protected JButton ok = new JButton(Strings.get("dlogOkButton"));
	protected JButton cancel = new JButton(Strings.get("dlogCancelButton"));

	public JDialogOk(Dialog parent, String title, boolean model) {
		super(parent, title, true);
		configure();
	}

	public JDialogOk(Frame parent, String title, boolean model) {
		super(parent, title, true);
		configure();
	}

	public void cancelClicked() {
	}

	private void configure() {
		MyListener listener = new MyListener();
		this.addWindowListener(listener);
		ok.addActionListener(listener);
		cancel.addActionListener(listener);

		Box buttons = Box.createHorizontalBox();
		buttons.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
		buttons.add(Box.createHorizontalGlue());
		buttons.add(ok);
		buttons.add(Box.createHorizontalStrut(10));
		buttons.add(cancel);
		buttons.add(Box.createHorizontalGlue());

		Container pane = super.getContentPane();
		pane.add(contents, BorderLayout.CENTER);
		pane.add(buttons, BorderLayout.SOUTH);
	}

	@Override
	public Container getContentPane() {
		return contents;
	}

	public abstract void okClicked();

}
