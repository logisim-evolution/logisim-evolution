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

package com.cburch.logisim.circuit;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JMenuItem;

import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.StringGetter;

class SplitterDistributeItem extends JMenuItem implements ActionListener {
	private static final long serialVersionUID = 1L;
	private Project proj;
	private Splitter splitter;
	private int order;

	public SplitterDistributeItem(Project proj, Splitter splitter, int order) {
		this.proj = proj;
		this.splitter = splitter;
		this.order = order;
		addActionListener(this);

		SplitterAttributes attrs = (SplitterAttributes) splitter
				.getAttributeSet();
		byte[] actual = attrs.bit_end;
		byte[] desired = SplitterAttributes.computeDistribution(attrs.fanout,
				actual.length, order);
		boolean same = actual.length == desired.length;
		for (int i = 0; same && i < desired.length; i++) {
			if (actual[i] != desired[i]) {
				same = false;
			}
		}
		setEnabled(!same);
		setText(toGetter().toString());
	}

	public void actionPerformed(ActionEvent e) {
		SplitterAttributes attrs = (SplitterAttributes) splitter
				.getAttributeSet();
		byte[] actual = attrs.bit_end;
		byte[] desired = SplitterAttributes.computeDistribution(attrs.fanout,
				actual.length, order);
		CircuitMutation xn = new CircuitMutation(proj.getCircuitState()
				.getCircuit());
		for (int i = 0, n = Math.min(actual.length, desired.length); i < n; i++) {
			if (actual[i] != desired[i]) {
				xn.set(splitter, attrs.getBitOutAttribute(i),
						Integer.valueOf(desired[i]));
			}
		}
		proj.doAction(xn.toAction(toGetter()));
	}

	private StringGetter toGetter() {
		if (order > 0) {
			return Strings.getter("splitterDistributeAscending");
		} else {
			return Strings.getter("splitterDistributeDescending");
		}
	}
	
}
