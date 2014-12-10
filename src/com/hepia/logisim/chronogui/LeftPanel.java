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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.util.HashMap;

import javax.swing.AbstractAction;
import javax.swing.JTable;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;

import com.hepia.logisim.chronodata.SignalData;

/**
 * Chronogram's left side Panel Composed of one signalNameHead and multiple
 * SignalName
 */
public class LeftPanel extends ChronoPanelTemplate {

	private static final long serialVersionUID = 1L;
	private ChronoFrame mChronoFrame;
	private CommonPanelParam mCommonPanelParam;
	private DrawAreaEventManager mDrawAreaEventManager;
	private JTable table;
	private Object[][] tableData;
	private HashMap<SignalData, Integer> signalDataPositionInTable; // to have
																	// direct
																	// access
																	// from
																	// signalData
																	// to
																	// position
																	// in JTable
	private SignalData[] reverseSignalDataPositionInTable; // and the reverse
															// access

	public LeftPanel(ChronoFrame chronoFrame,
			DrawAreaEventManager drawAreaEventManager) {
		this.mChronoFrame = chronoFrame;
		this.mCommonPanelParam = chronoFrame.getCommonPanelParam();
		this.mDrawAreaEventManager = drawAreaEventManager;
		this.setLayout(new BorderLayout());
		this.setBackground(Color.white);

		if (mChronoFrame.getChronoData().size() <= 1)
			return;

		String[] names = { "", Strings.get("SignalNameName"),
				Strings.get("SignalNameValue") };
		tableData = new Object[mChronoFrame.getChronoData().size() - 1][3];
		signalDataPositionInTable = new HashMap<SignalData, Integer>();
		reverseSignalDataPositionInTable = new SignalData[mChronoFrame
				.getChronoData().size() - 1];

		// add the signal name rows
		int pos = 0;
		for (String signalName : mChronoFrame.getChronoData().getSignalOrder()) {
			if (!signalName.equals("sysclk")) {
				SignalData signalData = mChronoFrame.getChronoData().get(
						signalName);
				signalDataPositionInTable.put(signalData, pos);
				reverseSignalDataPositionInTable[pos] = signalData;
				Object[] currentData = { signalData.getIcon(), signalName, "-" };
				tableData[pos++] = currentData;
			}
		}
		// creates the JTable
		DefaultTableModel model = new DefaultTableModel(tableData, names);
		table = new JTable(model) {
			private static final long serialVersionUID = 1L;

			@SuppressWarnings({ "unchecked", "rawtypes" })
			public Class getColumnClass(int column) {
				return getValueAt(0, column).getClass();
			}

			@Override
			public boolean isCellEditable(int rowIndex, int colIndex) {
				if (colIndex == 1) {
					return true; // Disallow the editing of any cell
				}
				return false;
			}
		};
		table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_F2, 0),
				"none");
		table.getActionMap().put("none", new AbstractAction() {
			private static final long serialVersionUID = 1L;

			public void actionPerformed(ActionEvent e) {
				;
			}
		});
		table.addKeyListener(chronoFrame);
		table.setRowHeight(mCommonPanelParam.getSignalHeight());
		table.getColumnModel().getColumn(0).setMaxWidth(10);

		// on mouse over
		table.addMouseMotionListener(new MouseMotionAdapter() {
			@Override
			public void mouseMoved(MouseEvent e) {
				int row = table.rowAtPoint(e.getPoint());
				if (row > -1 && e.getComponent() instanceof JTable) {
					table.clearSelection();
					table.setRowSelectionInterval(row, row);
					mDrawAreaEventManager
							.fireMouseEntered(reverseSignalDataPositionInTable[row]);
				}
			}
		});

		// popup on right click on a row
		table.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseReleased(MouseEvent e) {
				int row = table.getSelectedRow();
				if (row > -1 && SwingUtilities.isRightMouseButton(e)
						&& e.getComponent() instanceof JTable) {
					PopupMenu pm = new PopupMenu(mDrawAreaEventManager,
							reverseSignalDataPositionInTable[row]);
					pm.doPop(e);
				}
			}
		});

		// table header
		JTableHeader header = table.getTableHeader();
		Dimension d = header.getPreferredSize();
		d.height = mCommonPanelParam.getHeaderHeight();
		header.setPreferredSize(d);

		this.add(header, BorderLayout.NORTH);
		this.add(table, BorderLayout.CENTER);
	}

	/**
	 * Highlight a signal
	 */
	public void highlight(SignalData signalToHighlight) {
		int pos = signalDataPositionInTable.get(signalToHighlight);
		table.getSelectionModel().clearSelection();
		table.getSelectionModel().addSelectionInterval(pos, pos);
	}

	/**
	 * Refresh the display of each signal value in the left bar
	 */
	public void refreshSignalsValues() {
		int tickWidth = mChronoFrame.getRightPanel().getTickWidth();
		int elementPosition = (mChronoFrame.getRightPanel()
				.getMousePosXClicked() + tickWidth) / tickWidth;
		setSignalsValues(elementPosition);
	}

	/**
	 * Refresh the display of each signal value
	 * 
	 * @param elementPosition
	 *            the element in chronoData that contains the data to be
	 *            displayed
	 */
	public void setSignalsValues(int elementPosition) {
		int pos = 0;
		for (String signalName : mChronoFrame.getChronoData().getSignalOrder()) {
			if (!signalName.equals("sysclk")) {
				SignalData signalData = mChronoFrame.getChronoData().get(
						signalName);
				signalData.setSelectedValuePos(elementPosition);
				table.setValueAt(signalData.getSelectedValue(), pos++, 2);
			}
		}
	}
}
