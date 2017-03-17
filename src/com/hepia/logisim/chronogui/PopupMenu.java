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
package com.hepia.logisim.chronogui;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JPopupMenu;
import javax.swing.JRadioButtonMenuItem;

import com.hepia.logisim.chronodata.SignalData;
import com.hepia.logisim.chronodata.SignalDataBus;

class PopupContents extends JPopupMenu implements ActionListener {
	private static final long serialVersionUID = 1L;
	private DrawAreaEventManager mDrawAreaEventManager;
	private SignalDataBus signalDataBus;
	private JRadioButtonMenuItem expandBus;

	public PopupContents(DrawAreaEventManager drawAreaEventManager,
			SignalData signalData) {
		this.mDrawAreaEventManager = drawAreaEventManager;

		// For buses only:
		if (signalData instanceof SignalDataBus) {
			JMenu dataFormat;
			JRadioButtonMenuItem[] formats;
			signalDataBus = (SignalDataBus) signalData;
			// format choice
			dataFormat = new JMenu(Strings.get("BusFormat"));
			formats = new JRadioButtonMenuItem[5];
			ButtonGroup group = new ButtonGroup();
			for (int i = 0; i < SignalDataBus.signalFormat.length; ++i) {
				formats[i] = new JRadioButtonMenuItem(
						SignalDataBus.signalFormat[i]);
				formats[i].setActionCommand(SignalDataBus.signalFormat[i]);
				formats[i].addActionListener(this);
				group.add(formats[i]);
				dataFormat.add(formats[i]);
			}

			// default selection
			for (int i = 0; i < SignalDataBus.signalFormat.length; ++i)
				if (SignalDataBus.signalFormat[i].equals(signalDataBus
						.getFormat()))
					formats[i].setSelected(true);
			add(dataFormat);

			// expand
			expandBus = new JRadioButtonMenuItem(Strings.get("BusExpand"));
			expandBus.setSelected(signalDataBus.isExpanded());
			expandBus.addActionListener(this);
			add(expandBus);
		}
	}

	public void actionPerformed(ActionEvent e) {
		if (e.getSource() == expandBus) {
			if (signalDataBus.getSignalValues().size() > 0) {
				mDrawAreaEventManager.fireExpand(signalDataBus,
						!signalDataBus.isExpanded());
			}
		} else {
			if (signalDataBus != null) {
				mDrawAreaEventManager.fireSetCodingFormat(signalDataBus,
						e.getActionCommand());
			}
		}
	}
}

/**
 * Popup that appears when right click on a Signal (only a bus for now)
 */
public class PopupMenu extends MouseAdapter {

	private SignalData signalData;
	private DrawAreaEventManager mDrawAreaEventManager;

	public PopupMenu(DrawAreaEventManager drawAreaEventManager,
			SignalData signalData) {
		this.signalData = signalData;
		this.mDrawAreaEventManager = drawAreaEventManager;
	}

	public void doPop(MouseEvent e) {
		PopupContents menu = new PopupContents(mDrawAreaEventManager,
				signalData);
		menu.show(e.getComponent(), e.getX(), e.getY());
	}

	public void mousePressed(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}

	public void mouseReleased(MouseEvent e) {
		if (e.isPopupTrigger())
			doPop(e);
	}
}
