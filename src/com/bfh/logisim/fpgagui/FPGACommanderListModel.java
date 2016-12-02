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

package com.bfh.logisim.fpgagui;

import java.awt.Color;
import java.awt.Component;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

import javax.swing.AbstractListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

import com.bfh.logisim.designrulecheck.SimpleDRCContainer;
import com.cburch.logisim.util.Icons;

@SuppressWarnings("serial")
public class FPGACommanderListModel extends  AbstractListModel<Object> {
	
	private static Color FATAL = Color.RED;
	private static Color SEVERE = Color.yellow;
	private static Color NORMAL = Color.LIGHT_GRAY;
	private static Color ADDENDUM = Color.GRAY;

	public class ListModelCellRenderer extends JLabel implements ListCellRenderer<Object> {
		
		private boolean CountLines;
		
        
		public ListModelCellRenderer(boolean countLines) {
			CountLines = countLines;
		}
		
		@Override
		public Component getListCellRendererComponent(
				JList<? extends Object> list, 
				Object value, 
				int index,
				boolean isSelected, 
				boolean cellHasFocus) {
			SimpleDRCContainer msg = null;
			setBackground(list.getBackground());
			setForeground(list.getForeground());
			StringBuffer Line = new StringBuffer();
			setIcon(Icons.getIcon("empty.png")); /* place holder too make space for the trace icon */
			if (value instanceof SimpleDRCContainer) {
				msg = (SimpleDRCContainer) value;
			}
			if (msg != null) {
				if (msg.DRCInfoPresent()) {
		        	setIcon(Icons.getIcon("drc_trace.png"));
				} 
				switch (msg.Severity()) {
					case SimpleDRCContainer.LEVEL_SEVERE :
						setForeground(SEVERE);
						break;
					case SimpleDRCContainer.LEVEL_FATAL :
						setBackground(FATAL);
						setForeground(list.getBackground());
						break;
					default : 
						setForeground(NORMAL);
				}
			}
			if (value.toString().contains("BUG")) {
				setBackground(Color.MAGENTA);
				setForeground(Color.black);
			}
			if (CountLines) {
				if (msg != null) {
					if (msg.SupressCount()) {
						setForeground(ADDENDUM);
						Line.append("       ");
					} else {
						int line = msg.GetListNumber();
						if (line < 10) {
							Line.append("    ");
						} else if (line < 100) {
							Line.append("   ");
						} else if (line < 1000) {
							Line.append("  ");
						} else if (line < 10000) {
							Line.append(" ");
						}
						Line.append(Integer.toString(line) + "> ");
					}
				} else {
					if (index < 9) {
						Line.append("    ");
					} else if (index < 99) {
						Line.append("   ");
					} else if (index < 999) {
						Line.append("  ");
					} else if (index < 9999) {
						Line.append(" ");
					}
					Line.append(Integer.toString(index + 1) + "> ");
				}
			}
			if (msg != null) {
				switch (msg.Severity()) {
					case SimpleDRCContainer.LEVEL_SEVERE :
						Line.append(Strings.get("SEVERE_MSG")+" ");
						break;
					case SimpleDRCContainer.LEVEL_FATAL :
						Line.append(Strings.get("FATAL_MSG")+" ");
						break;
				}
				if (msg.HasCircuit()) {
					Line.append(msg.GetCircuit().getName()+": ");
				}
			}
			Line.append(value.toString());
			setText(Line.toString());
			setEnabled(list.isEnabled());
			setFont(list.getFont());
			setOpaque(true);
			return this;
		}
	}

	private ArrayList<Object> myData;
	private Set<ListDataListener> myListeners;
	private int count = 0;
	
	public FPGACommanderListModel() {
		myData = new ArrayList<Object>();
		myListeners = new HashSet<ListDataListener>();
	}
	
	public ListCellRenderer<Object> getMyRenderer(boolean CountLines) {
		return new ListModelCellRenderer(CountLines);
	}
	
	public void clear() {
		myData.clear();
		count = 0;
		FireEvent(null);
	}
	
	public void add(Object toAdd) {
		count++;
		if (toAdd instanceof SimpleDRCContainer) {
			SimpleDRCContainer add = (SimpleDRCContainer) toAdd;
			if (add.SupressCount())
				count--;
			else
				add.SetListNumber(count);
		}
		myData.add(toAdd);
		FireEvent(null);
	}
	
	public int getCountNr() {
		return count;
	}
	
	@Override
	public int getSize() {
		return myData.size();
	}

	@Override
	public Object getElementAt(int index) {
		if (index < myData.size())
			return myData.get(index);
		return null;
	}

	@Override
	public void addListDataListener(ListDataListener l) {
		myListeners.add(l);
	}

	@Override
	public void removeListDataListener(ListDataListener l) {
		myListeners.remove(l);
	}
	
	private void FireEvent(ListDataEvent e) {
		for (ListDataListener l :myListeners)
			l.contentsChanged(e);
	}

}
