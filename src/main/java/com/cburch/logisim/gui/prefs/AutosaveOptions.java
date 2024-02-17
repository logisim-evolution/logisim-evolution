package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorBoolean;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

public class AutosaveOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final MyListener myListener = new MyListener();
  private final JCheckBox enableAutosaves;
  private final JLabel autosaveIntervalText = new JLabel();
  private final JTextField autosaveInterval = new JTextField();

  public AutosaveOptions(PreferencesFrame window) {
    super(window);

    enableAutosaves = ((PrefMonitorBoolean) AppPreferences.AUTOSAVE_ENABLED).getCheckBox();
    autosaveInterval.addActionListener(myListener);

    final var gridbag = new GridBagLayout();
    final var gbc = new GridBagConstraints();
    gridbag.setConstraints(this, gbc);
    setLayout(gridbag);
    gbc.weightx = 1.0;
    gbc.gridx = 0;
    gbc.gridy = 0;
    gbc.anchor = GridBagConstraints.LINE_START;
    add(enableAutosaves, gbc);

    gbc.gridy = 1;
    final var panel = new JPanel();
    panel.add(autosaveIntervalText);
    panel.add(autosaveInterval);
    add(panel, gbc);

    myListener.computeEnable();
    myListener.setContent();

    AppPreferences.addPropertyChangeListener(AppPreferences.AUTOSAVE_ENABLE, myListener);
  }

  @Override
  public String getHelpText() {
    return S.get("autosaveHelp");
  }

  @Override
  public String getTitle() {
    return S.get("autosaveTitle");
  }

  @Override
  public void localeChanged() {
    enableAutosaves.setText(S.get("autosaveEnabled"));
    autosaveIntervalText.setText(S.get("autosaveInterval"));
  }

  private class MyListener implements ActionListener, PropertyChangeListener {

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if (evt.getPropertyName().equals(AppPreferences.AUTOSAVE_ENABLE)) {
        computeEnable();
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      var val = -1;
      try {
        val = Integer.parseInt(autosaveInterval.getText());
      } catch (NumberFormatException ignored) {
      }
      if (val <= 0 || val > 10000) {
        setContent();
      } else {
        AppPreferences.AUTOSAVE_INTERVAL.set(val);
      }
    }

    private void computeEnable() {
      final var enable = AppPreferences.AUTOSAVE_ENABLED.getBoolean();
      autosaveIntervalText.setEnabled(enable);
      autosaveInterval.setEnabled(enable);
    }

    private void setContent() {
      autosaveInterval.setText(AppPreferences.AUTOSAVE_INTERVAL.get().toString());
    }
  }
}
