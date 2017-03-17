package com.cburch.logisim.gui.prefs;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.prefs.PreferenceChangeEvent;
import java.util.prefs.PreferenceChangeListener;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JTextField;

import com.bfh.logisim.fpgagui.FPGACommanderGui;
import com.cburch.logisim.gui.scale.ScaledButton;
import com.cburch.logisim.gui.scale.ScaledLabel;
import com.cburch.logisim.gui.scale.ScaledTextField;
import com.cburch.logisim.prefs.AppPreferences;

@SuppressWarnings("serial")
public class FPGAOptions extends OptionsPanel {

	private class MyListener implements ActionListener,
	PreferenceChangeListener {

		@Override
		public void actionPerformed(ActionEvent ae) {
			Object source = ae.getSource();
			
			if (source==WorkSpaceButton) {
				FPGACommanderGui.selectWorkSpace(frame);
			}
		}

		@Override
		public void preferenceChange(PreferenceChangeEvent pce) {
			String property = pce.getKey();

			if (property.equals(AppPreferences.FPGA_Workspace.getIdentifier())) {
				WorkSpacePath.setText(AppPreferences.FPGA_Workspace.get());
			}
		}

}

	private MyListener myListener = new MyListener();
	private JLabel WorkspaceLabel;
	private JTextField WorkSpacePath;
	private JButton WorkSpaceButton;
	private PreferencesFrame frame;
	
	public FPGAOptions(PreferencesFrame frame) {
		super(frame);
		this.frame=frame;
		AppPreferences.getPrefs().addPreferenceChangeListener(myListener);
		
		WorkspaceLabel = new ScaledLabel(Strings.get("FPGAWorkSpace"));
		WorkSpacePath = new ScaledTextField(32);
		WorkSpacePath.setText(AppPreferences.FPGA_Workspace.get());
		WorkSpacePath.setEditable(false);
		WorkSpaceButton = new ScaledButton();
		WorkSpaceButton.addActionListener(myListener);
		WorkSpaceButton.setText(Strings.get("Browse"));
		
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(layout);
		
		c.insets = new Insets(2, 4, 4, 2);
		c.anchor = GridBagConstraints.BASELINE_LEADING;
		
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		add(WorkspaceLabel,c);
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		add(WorkSpaceButton,c);
		c.gridx = 1;
		c.gridy = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		add(WorkSpacePath,c);
	}

	@Override
	public String getHelpText() {
		return Strings.get("FPGAHelp");
	}

	@Override
	public String getTitle() {
		return Strings.get("FPGATitle");
	}

	@Override
	public void localeChanged() {
		WorkspaceLabel.setText(Strings.get("FPGAWorkSpace"));
		WorkSpaceButton.setText(Strings.get("Browse"));
	}

}
