/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.gui.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitor;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.TableLayout;

import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;

import static com.cburch.logisim.gui.Strings.S;

class HotkeyOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private static final PrefMonitor<KeyStroke>[] hotkeys=new PrefMonitor[]{
          AppPreferences.HOTKEY_SIM_AUTO_PROPAGATE,
          AppPreferences.HOTKEY_SIM_RESET,
          AppPreferences.HOTKEY_SIM_STEP,
          AppPreferences.HOTKEY_SIM_TICK_HALF,
          AppPreferences.HOTKEY_SIM_TICK_FULL,
          AppPreferences.HOTKEY_SIM_TICK_ENABLED,
          AppPreferences.HOTKEY_EDIT_UNDO,
          AppPreferences.HOTKEY_EDIT_REDO,
          AppPreferences.HOTKEY_FILE_EXPORT,
          AppPreferences.HOTKEY_FILE_PRINT,
          AppPreferences.HOTKEY_FILE_QUIT,
  };

  private JLabel[] key_labels=new JLabel[hotkeys.length];
  private JButton[] key_buttons=new JButton[hotkeys.length];

  public HotkeyOptions(PreferencesFrame window) {
    super(window);
    this.setLayout(new TableLayout(2));
    final var listener = new SettingsChangeListener(this);
    for(int i=0;i<hotkeys.length;i++){
      key_labels[i] = new JLabel(((PrefMonitorKeyStroke)hotkeys[i]).getName()+"  ");
      key_buttons[i]=new JButton(hotkeys[i].get().toString());
      key_buttons[i].addActionListener(listener);
      key_buttons[i].setActionCommand(i+"");
      add(key_labels[i]);
      add(key_buttons[i]);
    }

  }

  @Override
  public String getHelpText() {
    /* TODO: localize */
    return "This is the help of hotkey settings. Remember to localize it.";
  }

  @Override
  public String getTitle() {
    /* TODO: localize */
    return "Hotkey Settings";
  }

  @Override
  public void localeChanged() {
    /* TODO: localize */
  }

  private class SettingsChangeListener implements ChangeListener, ActionListener {
    HotkeyOptions owner;
    public SettingsChangeListener(HotkeyOptions ht){
      owner=ht;
    }
    @Override
    public void actionPerformed(ActionEvent e) {
      int index=Integer.parseInt(e.getActionCommand());
      /* TODO:localize */
      JDialog dl=new JDialog(
              owner.getPreferencesFrame(),
              ((PrefMonitorKeyStroke)hotkeys[index]).getName(),
              true);
      JPanel p=new JPanel();
      p.setLayout(new TableLayout(1));
      JLabel waitingLabel=new JLabel("Receiving Your Input Key");
      p.add(waitingLabel);
      dl.addKeyListener(new keycaptureListener(dl,waitingLabel));
      dl.setContentPane(p);
      dl.setLocationRelativeTo(null);
      dl.setSize(300,100);
      dl.setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }
  }

  private class keycaptureListener implements KeyListener {
    private JDialog dialog;
    private JLabel label;

    public keycaptureListener(JDialog j,JLabel l){
      dialog=j;
      label=l;
    }
    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode()>=32) {
        int modifier=e.getModifiersEx();
        int code=e.getKeyCode();
        label.setText(KeyEvent.getModifiersExText(modifier)+" + "+KeyEvent.getKeyText(code));
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
  }
}
