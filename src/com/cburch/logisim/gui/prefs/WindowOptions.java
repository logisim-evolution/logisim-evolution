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

import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.TableLayout;

class WindowOptions extends OptionsPanel {
	private static final long serialVersionUID = 1L;
	private PrefBoolean[] checks;
	private PrefOptionList toolbarPlacement;
	private ZoomSlider ZoomValue;
	
	private class ZoomChange implements ChangeListener,ActionListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			JSlider source = (JSlider)e.getSource();
			if (!source.getValueIsAdjusting()) {
				int value = (int) source.getValue();
				AppPreferences.SCALE_FACTOR.set((double)value/100.0);
				localeChanged();
			}
		}

		@Override
		public void actionPerformed(ActionEvent e) {
			if (e.getSource().equals(LookAndFeel)) {
				if (LookAndFeel.getSelectedIndex()!= Index) {
					Index = LookAndFeel.getSelectedIndex();
					AppPreferences.LookAndFeel.set(LFInfos[Index].getClassName());
				}
			}
		}
		
	}

    private JComboBox<String> LookAndFeel;
    private int Index = 0;
    private LookAndFeelInfo[] LFInfos;

	
	public WindowOptions(PreferencesFrame window) {
		super(window);
		JLabel ZoomLabel = new JLabel();
		

		checks = new PrefBoolean[] { new PrefBoolean(
				AppPreferences.SHOW_TICK_RATE, Strings.getter("windowTickRate")), };

		toolbarPlacement = new PrefOptionList(AppPreferences.TOOLBAR_PLACEMENT,
				Strings.getter("windowToolbarLocation"), new PrefOption[] {
						new PrefOption(Direction.NORTH.toString(),
								Direction.NORTH.getDisplayGetter()),
						new PrefOption(Direction.SOUTH.toString(),
								Direction.SOUTH.getDisplayGetter()),
						new PrefOption(Direction.EAST.toString(),
								Direction.EAST.getDisplayGetter()),
						new PrefOption(Direction.WEST.toString(),
								Direction.WEST.getDisplayGetter()),
						new PrefOption(AppPreferences.TOOLBAR_DOWN_MIDDLE,
								Strings.getter("windowToolbarDownMiddle")),
						new PrefOption(AppPreferences.TOOLBAR_HIDDEN,
								Strings.getter("windowToolbarHidden")) });

		JPanel panel = new JPanel(new TableLayout(2));
		panel.add(toolbarPlacement.getJLabel());
		panel.add(toolbarPlacement.getJComboBox());
		
		ZoomLabel.setText("Zoom factor:");
		ZoomValue = new ZoomSlider(JSlider.HORIZONTAL,100,300,(int)(AppPreferences.SCALE_FACTOR.get()*100));
		
		panel.add(new JLabel(" "));
		panel.add(new JLabel(" "));
		JLabel important = new JLabel(Strings.get("windowToolbarPleaserestart"));
	    important.setFont(important.getFont().deriveFont(Font.ITALIC));
	    panel.add(important);
	    important = new JLabel(Strings.get("windowToolbarImportant"));
	    important.setFont(important.getFont().deriveFont(Font.ITALIC));
	    panel.add(important);
		panel.add(ZoomLabel);
		panel.add(ZoomValue);
		ZoomChange Listener = new ZoomChange();
		ZoomValue.addChangeListener(Listener);
	    int index = 0;
	    LookAndFeel = new JComboBox<String>();
	    LFInfos = UIManager.getInstalledLookAndFeels();
		for (LookAndFeelInfo info : LFInfos) {
			LookAndFeel.insertItemAt(info.getName(), index);
			if (info.getClassName().equals(AppPreferences.LookAndFeel.get())) {
				LookAndFeel.setSelectedIndex(index);
				Index = index;
			}
			index++;
		}
		panel.add(new JLabel(Strings.get("windowToolbarLookandfeel")));
		panel.add(LookAndFeel);
		LookAndFeel.addActionListener(Listener);

	    
		setLayout(new TableLayout(1));
		for (int i = 0; i < checks.length; i++) {
			add(checks[i]);
		}
		add(panel);
	}

	@Override
	public String getHelpText() {
		return Strings.get("windowHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("windowTitle");
	}

	@Override
	public void localeChanged() {
		for (int i = 0; i < checks.length; i++) {
			checks[i].localeChanged();
		}
		toolbarPlacement.localeChanged();
	}
}
