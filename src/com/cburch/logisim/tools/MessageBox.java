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
package com.cburch.logisim.tools;

import java.awt.Dimension;

import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class MessageBox {

	String description;
	String title;
	int type;
	JTextArea textArea;

	public MessageBox(String title, String description, int type) {
		this.title = title;
		this.description = description;
		this.type = type;
	}

	// public void setDescription(String description) {
	// this.description = description;
	// }
	//
	// public void append(String line) {
	// textArea.append(line);
	// }

	// public void refresh() {
	// textArea.setText(description);
	// textArea.update(textArea.getGraphics());
	// }

	public void show() {

		if (description.contains("\n") || description.length() > 60) {
			int lines = 1;
			for (int pos = description.indexOf('\n'); pos >= 0; pos = description
					.indexOf('\n', pos + 1)) {
				lines++;
			}
			lines = Math.max(4, Math.min(lines, 7));

			textArea = new JTextArea(lines, 100);
			textArea.setEditable(false);
			textArea.setText(description);
			textArea.setCaretPosition(0);

			JScrollPane scrollPane = new JScrollPane(textArea);
			scrollPane.setPreferredSize(new Dimension(640, 480));
			JOptionPane.showMessageDialog(null, scrollPane, title, type);
		} else {
			JOptionPane.showMessageDialog(null, description, title, type);
		}
	}

}
