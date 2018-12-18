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

package com.cburch.logisim.gui.log;

import java.awt.BorderLayout;

import javax.swing.JScrollPane;

class ScrollPanel extends LogPanel {
	private static final long serialVersionUID = 1L;
	private TablePanel table;

	public ScrollPanel(LogFrame frame) {
		super(frame);
		this.table = new TablePanel(frame);
		JScrollPane pane = new JScrollPane(table,
				JScrollPane.VERTICAL_SCROLLBAR_ALWAYS,
				JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
		pane.setVerticalScrollBar(table.getVerticalScrollBar());
		setLayout(new BorderLayout());
		add(pane);
	}

	@Override
	public String getHelpText() {
		return table.getHelpText();
	}

	@Override
	public String getTitle() {
		return table.getTitle();
	}

	@Override
	public void localeChanged() {
		table.localeChanged();
	}

	@Override
	public void modelChanged(Model oldModel, Model newModel) {
		table.modelChanged(oldModel, newModel);
	}
}
