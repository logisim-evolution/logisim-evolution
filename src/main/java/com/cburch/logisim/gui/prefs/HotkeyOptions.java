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
import java.util.prefs.BackingStoreException;

import static com.cburch.logisim.gui.Strings.S;

class HotkeyOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  public static final PrefMonitor<KeyStroke>[] hotkeys=new PrefMonitor[]{
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
    this.setLayout(new TableLayout(1));
    final var listener = new SettingsChangeListener(this);
    /* TODO: localize */
    JLabel headerLabel=new JLabel("Note that the hotkeys on the menubar need to be started with CTRL.");
    add(headerLabel);

    JPanel p=new JPanel();
    p.setLayout(new TableLayout(2));
    for(int i=0;i<hotkeys.length;i++){
      key_labels[i] = new JLabel(((PrefMonitorKeyStroke)hotkeys[i]).getName()+"  ");
      key_buttons[i]=new JButton(((PrefMonitorKeyStroke) hotkeys[i]).getString());
      key_buttons[i].addActionListener(listener);
      key_buttons[i].setActionCommand(i+"");
      p.add(key_labels[i]);
      p.add(key_buttons[i]);
    }
    add(p);

    JButton resetBtn=new JButton("Reset to default");
    resetBtn.addActionListener(new ActionListener() {
      @Override
      public void actionPerformed(ActionEvent e) {
        AppPreferences.resetHotkeys();
      }
    });
    add(resetBtn);
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
    public int code;
    public int modifier;
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
      JPanel sub=new JPanel();
      JButton ok=new JButton("OK");
      JButton cancel=new JButton("Cancel");

      ok.setFocusable(false);
      ok.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          if(code!=0){
            HotkeyOptions.hotkeys[index].set(KeyStroke.getKeyStroke(code,modifier));
            try {
              AppPreferences.getPrefs().flush();
              owner.key_buttons[index].setText(((PrefMonitorKeyStroke)HotkeyOptions.hotkeys[index]).getString());
            } catch (BackingStoreException ex) {
              throw new RuntimeException(ex);
            }
          }
          dl.setVisible(false);
        }
      });
      cancel.setFocusable(false);
      cancel.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          dl.setVisible(false);
        }
      });

      sub.setLayout(new TableLayout(2));
      p.setLayout(new TableLayout(1));

      sub.add(ok);
      sub.add(cancel);

      JLabel waitingLabel=new JLabel("Receiving Your Input Key");
      p.add(waitingLabel);
      p.add(sub);

      dl.addKeyListener(new keycaptureListener(dl,waitingLabel,((PrefMonitorKeyStroke)owner.hotkeys[index]).isMenuHotkey(),this));
      dl.setContentPane(p);
      dl.setLocationRelativeTo(null);
      dl.setSize(400,100);
      dl.setVisible(true);
    }

    @Override
    public void stateChanged(ChangeEvent e) {

    }
  }

  private class keycaptureListener implements KeyListener {
    private JDialog dialog;
    private JLabel label;
    private boolean isMenuKey;
    private SettingsChangeListener scl;

    public keycaptureListener(JDialog j,JLabel l, boolean is_menu_key,SettingsChangeListener se){
      dialog=j;
      label=l;
      isMenuKey=is_menu_key;
      scl=se;
    }
    @Override
    public void keyTyped(KeyEvent e) {

    }

    @Override
    public void keyPressed(KeyEvent e) {
      if(e.getKeyCode()>=32) {
        int modifier=e.getModifiersEx();
        if(isMenuKey){
          if((modifier&KeyEvent.CTRL_DOWN_MASK)!=KeyEvent.CTRL_DOWN_MASK){
            /* TODO: localize */
            label.setText("This key combo must start with CTRL.");
            scl.code=0;
            scl.modifier=0;
            return;
          }
        }
        int code=e.getKeyCode();
        scl.code=code;
        scl.modifier=modifier;
        label.setText(KeyEvent.getModifiersExText(modifier)+" + "+KeyEvent.getKeyText(code));
      }
    }

    @Override
    public void keyReleased(KeyEvent e) {

    }
  }
}
