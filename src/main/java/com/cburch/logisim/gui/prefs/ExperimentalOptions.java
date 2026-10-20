/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.prefs;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.prefs.AppPreferences;
import java.awt.BorderLayout;
import java.awt.Font;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JPanel;

class ExperimentalOptions extends OptionsPanel {
  private static final long serialVersionUID = 1L;
  private final JLabel accelRestart = new JLabel();
  private final PrefOptionList accel;
  private final JLabel simRestart = new JLabel();
  private final PrefOptionList simQueue;

  public ExperimentalOptions(PreferencesFrame window) {
    super(window);

    accel = new PrefOptionList(
        AppPreferences.GRAPHICS_ACCELERATION,
        S.getter("accelLabel"),
        new PrefOption[]{
            new PrefOption(AppPreferences.ACCEL_DEFAULT, S.getter("accelDefault")),
            new PrefOption(AppPreferences.ACCEL_NONE, S.getter("accelNone")),
            new PrefOption(AppPreferences.ACCEL_OPENGL, S.getter("accelOpenGL")),
            new PrefOption(AppPreferences.ACCEL_D3D, S.getter("accelD3D")),
            new PrefOption(AppPreferences.ACCEL_METAL, S.getter("accelMetal")),
        }
    );

    final var accelPanel = new JPanel(new BorderLayout());
    accelPanel.add(accel.getJLabel(), BorderLayout.LINE_START);
    accelPanel.add(accel.getJComboBox(), BorderLayout.CENTER);
    accelPanel.add(accelRestart, BorderLayout.PAGE_END);
    accelRestart.setFont(accelRestart.getFont().deriveFont(Font.ITALIC));
    final var accelPanel2 = new JPanel();
    accelPanel2.add(accelPanel);

    setLayout(new BoxLayout(this, BoxLayout.PAGE_AXIS));
    add(Box.createGlue());
    add(accelPanel2);

    simQueue = new PrefOptionList(
        AppPreferences.SIMULATION_QUEUE,
        S.getter("simQueueLabel"),
        new PrefOption[]{
            new PrefOption(AppPreferences.SIM_QUEUE_DEFAULT, S.getter("simQueueDefault")),
            new PrefOption(AppPreferences.SIM_QUEUE_PRIORITY, S.getter("simQueuePriority")),
            new PrefOption(AppPreferences.SIM_QUEUE_SPLAY, S.getter("simQueueSplay")),
            new PrefOption(AppPreferences.SIM_QUEUE_LINKED, S.getter("simQueueLinked")),
            new PrefOption(AppPreferences.SIM_QUEUE_LIST_OF_QUEUES, S.getter("simQueueListOfQueues")),
            new PrefOption(AppPreferences.SIM_QUEUE_TREE_OF_QUEUES, S.getter("simQueueTreeOfQueues"))
        }
    );
    final var simPanel = new JPanel(new BorderLayout());
    simPanel.add(simQueue.getJLabel(), BorderLayout.LINE_START);
    simPanel.add(simQueue.getJComboBox(), BorderLayout.CENTER);
    simPanel.add(simRestart, BorderLayout.PAGE_END);
    simRestart.setFont(simRestart.getFont().deriveFont(Font.ITALIC));
    final var simPanel2 = new JPanel();
    simPanel2.add(simPanel);

    add(simPanel2);
    add(Box.createGlue());
  }

  @Override
  public String getHelpText() {
    return S.get("experimentHelp");
  }

  @Override
  public String getTitle() {
    return S.get("experimentTitle");
  }

  @Override
  public void localeChanged() {
    accel.localeChanged();
    accelRestart.setText(S.get("accelRestartLabel"));
    simRestart.setText(S.get("simRestartLabel"));
  }
}
