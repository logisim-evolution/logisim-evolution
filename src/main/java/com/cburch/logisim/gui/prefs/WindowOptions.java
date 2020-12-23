/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.data.Direction;
import com.cburch.logisim.fpga.gui.ZoomSlider;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.Projects;
import com.cburch.logisim.util.TableLayout;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.JTextArea;

class WindowOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private PrefBoolean[] checks;
  private PrefOptionList toolbarPlacement;
  private ZoomSlider ZoomValue;
  private JLabel lookfeelLabel;
  private JLabel ZoomLabel;
  private JLabel Importanta;
  private JTextArea Importantb;

  private class ZoomChange implements ChangeListener, ActionListener {

    @Override
    public void stateChanged(ChangeEvent e) {
      JSlider source = (JSlider) e.getSource();
      if (!source.getValueIsAdjusting()) {
        int value = (int) source.getValue();
        AppPreferences.SCALE_FACTOR.set((double) value / 100.0);
        List<Project> nowOpen = Projects.getOpenProjects();
        for (Project proj : nowOpen) {
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      }
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (e.getSource().equals(LookAndFeel)) {
        if (LookAndFeel.getSelectedIndex() != Index) {
          Index = LookAndFeel.getSelectedIndex();
          AppPreferences.LookAndFeel.set(LFInfos[Index].getClassName());
        }
      } else if (e.getActionCommand().equals("reset")) {
        AppPreferences.resetWindow();
        List<Project> nowOpen = Projects.getOpenProjects();
        for (Project proj : nowOpen) {
          proj.getFrame().resetLayout();
          proj.getFrame().revalidate();
          proj.getFrame().repaint();
        }
      }
    }
  }

  private JComboBox<String> LookAndFeel;
  private int Index = 0;
  private LookAndFeelInfo[] LFInfos;

  public WindowOptions(PreferencesFrame window) {
    super(window);
 
    checks =
        new PrefBoolean[] {
          new PrefBoolean(AppPreferences.SHOW_TICK_RATE, S.getter("windowTickRate")),
        };

    toolbarPlacement =
        new PrefOptionList(
            AppPreferences.TOOLBAR_PLACEMENT,
            S.getter("windowToolbarLocation"),
            new PrefOption[] {
              new PrefOption(Direction.NORTH.toString(), Direction.NORTH.getDisplayGetter()),
              new PrefOption(Direction.SOUTH.toString(), Direction.SOUTH.getDisplayGetter()),
              new PrefOption(Direction.EAST.toString(), Direction.EAST.getDisplayGetter()),
              new PrefOption(Direction.WEST.toString(), Direction.WEST.getDisplayGetter()),
              new PrefOption(AppPreferences.TOOLBAR_HIDDEN, S.getter("windowToolbarHidden"))
            });
   
    JPanel panel = new JPanel(new TableLayout(2));
    panel.add(toolbarPlacement.getJLabel());
    panel.add(toolbarPlacement.getJComboBox());
  
    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));
    
    Importanta = new JLabel(S.get("windowToolbarPleaserestart"));
    Importanta.setFont(Importanta.getFont().deriveFont(Font.ITALIC));
    panel.add(Importanta);
    
    Importantb = new JTextArea(S.get("windowToolbarImportant"));
    Importantb.setFont(Importantb.getFont().deriveFont(Font.ITALIC));
    panel.add(Importantb);
    
    ZoomLabel = new JLabel(S.get("windowToolbarZoomfactor"));
    ZoomValue =
        new ZoomSlider(
            JSlider.HORIZONTAL, 100, 300, (int) (AppPreferences.SCALE_FACTOR.get() * 100));

    panel.add(ZoomLabel);
    panel.add(ZoomValue);
    
    ZoomChange Listener = new ZoomChange();
    ZoomValue.addChangeListener(Listener);
    
    panel.add(new JLabel(" "));
    panel.add(new JLabel(" "));
    
    int index = 0;
    LookAndFeel = new JComboBox<String>();
    LookAndFeel.setSize(50, 20);
    
    LFInfos = UIManager.getInstalledLookAndFeels();
    for (LookAndFeelInfo info : LFInfos) {
      LookAndFeel.insertItemAt(info.getName(), index);
      if (info.getClassName().equals(AppPreferences.LookAndFeel.get())) {
        LookAndFeel.setSelectedIndex(index);
        Index = index;
      }
      index++;
    }    
    lookfeelLabel = new JLabel(S.get("windowToolbarLookandfeel"));
    panel.add(lookfeelLabel);
    panel.add(LookAndFeel);
    LookAndFeel.addActionListener(Listener);

    setLayout(new TableLayout(1));
    JButton but = new JButton();
    but.addActionListener(Listener);
    but.setActionCommand("reset");
    but.setText(S.get("windowToolbarReset"));
    add(but);
    for (int i = 0; i < checks.length; i++) {
      add(checks[i]);
    }
    add(panel);
  }

  @Override
  public String getHelpText() {
    return S.get("windowHelp");
  }

  @Override
  public String getTitle() {
    return S.get("windowTitle");
  }

  @Override
  public void localeChanged() {
    for (int i = 0; i < checks.length; i++) {
      checks[i].localeChanged();
    }
    toolbarPlacement.localeChanged();
    ZoomLabel.setText(S.get("windowToolbarZoomfactor"));
    lookfeelLabel.setText(S.get("windowToolbarLookandfeel"));
    Importanta.setText(S.get("windowToolbarPleaserestart"));
    Importantb.setText(S.get("windowToolbarImportant"));
  }
}
