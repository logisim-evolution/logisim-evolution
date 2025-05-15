/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.menu;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.prefs.PrefMonitorKeyStroke;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.ButtonGroup;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JRadioButtonMenuItem;
import javax.swing.KeyStroke;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

@SuppressWarnings("serial")
public class MenuSimulate extends Menu {

  public static final Double[] SUPPORTED_TICK_FREQUENCIES = {
      2048000.0, 1024000.0, 512000.0, 256000.0, 128000.0, 64000.0, 32000.0, 16000.0, 8000.0, 4000.0,
      2000.0, 1000.0, 512.0, 256.0, 128.0, 64.0, 32.0, 16.0, 8.0, 4.0, 2.0, 1.0, 0.5, 0.25
  };
  private final LogisimMenuBar menubar;
  private final MyListener myListener = new MyListener();
  private final MenuItemCheckImpl runToggle;
  private final JMenuItem reset = new JMenuItem();
  private final MenuItemImpl step;
  private final MenuItemImpl vhdlSimFiles;
  private final MenuItemCheckImpl simulateVhdlEnable;
  private final MenuItemCheckImpl ticksEnabled;
  private final MenuItemImpl tickHalf;
  private final MenuItemImpl tickFull;
  private final JMenu tickFreq = new JMenu();
  private final TickFrequencyChoice[] tickFreqs =
      new TickFrequencyChoice[SUPPORTED_TICK_FREQUENCIES.length];
  private final JMenu downStateMenu = new JMenu();
  private final ArrayList<CircuitStateMenuItem> downStateItems = new ArrayList<>();
  private final JMenu upStateMenu = new JMenu();
  private final ArrayList<CircuitStateMenuItem> upStateItems = new ArrayList<>();
  private final JMenuItem log = new JMenuItem();
  private final JMenuItem test = new JMenuItem();
  private final JMenuItem assemblyWindow = new JMenuItem();
  AssemblyWindow assWin = null;
  private CircuitState currentState = null;
  private CircuitState bottomState = null;
  private Simulator currentSim = null;
  private final int menuMask;

  public MenuSimulate(LogisimMenuBar menubar) {
    this.menubar = menubar;
    runToggle = new MenuItemCheckImpl(this, LogisimMenuBar.SIMULATE_RUN_TOGGLE);
    step = new MenuItemImpl(this, LogisimMenuBar.SIMULATE_STEP);
    simulateVhdlEnable = new MenuItemCheckImpl(this, LogisimMenuBar.SIMULATE_VHDL_ENABLE);
    vhdlSimFiles = new MenuItemImpl(this, LogisimMenuBar.GENERATE_VHDL_SIM_FILES);
    ticksEnabled = new MenuItemCheckImpl(this, LogisimMenuBar.TICK_ENABLE);
    tickHalf = new MenuItemImpl(this, LogisimMenuBar.TICK_HALF);
    tickFull = new MenuItemImpl(this, LogisimMenuBar.TICK_FULL);

    menubar.registerItem(LogisimMenuBar.SIMULATE_RUN_TOGGLE, runToggle);
    menubar.registerItem(LogisimMenuBar.SIMULATE_STEP, step);
    menubar.registerItem(LogisimMenuBar.SIMULATE_VHDL_ENABLE, simulateVhdlEnable);
    menubar.registerItem(LogisimMenuBar.GENERATE_VHDL_SIM_FILES, vhdlSimFiles);
    menubar.registerItem(LogisimMenuBar.TICK_ENABLE, ticksEnabled);
    menubar.registerItem(LogisimMenuBar.TICK_HALF, tickHalf);
    menubar.registerItem(LogisimMenuBar.TICK_FULL, tickFull);

    menuMask = getToolkit().getMenuShortcutKeyMaskEx();
    /* Allow user itself to set the mask */
    runToggle.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_AUTO_PROPAGATE).getWithMask(0));
    reset.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_RESET).getWithMask(0));
    step.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_STEP).getWithMask(0));
    tickHalf.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_HALF).getWithMask(0));
    tickFull.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_FULL).getWithMask(0));
    ticksEnabled.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_ENABLED).getWithMask(0));

    /* add myself to hotkey sync */
    AppPreferences.gui_sync_objects.add(this);

    final var bgroup = new ButtonGroup();
    for (var i = 0; i < SUPPORTED_TICK_FREQUENCIES.length; i++) {
      tickFreqs[i] = new TickFrequencyChoice(SUPPORTED_TICK_FREQUENCIES[i]);
      bgroup.add(tickFreqs[i]);
      tickFreq.add(tickFreqs[i]);
    }

    add(runToggle);
    add(step);
    add(reset);
    add(simulateVhdlEnable);
    add(vhdlSimFiles);
    addSeparator();
    add(upStateMenu);
    add(downStateMenu);
    addSeparator();
    add(tickHalf);
    add(tickFull);
    add(ticksEnabled);
    add(tickFreq);
    addSeparator();
    add(log);
    add(test);
    addSeparator();
    add(assemblyWindow);

    setEnabled(false);
    runToggle.setEnabled(false);
    reset.setEnabled(false);
    step.setEnabled(false);
    simulateVhdlEnable.setEnabled(false);
    vhdlSimFiles.setEnabled(false);
    upStateMenu.setEnabled(false);
    downStateMenu.setEnabled(false);
    tickHalf.setEnabled(false);
    tickFull.setEnabled(false);
    ticksEnabled.setEnabled(false);
    tickFreq.setEnabled(false);

    runToggle.addChangeListener(myListener);
    menubar.addActionListener(LogisimMenuBar.SIMULATE_RUN_TOGGLE, myListener);
    menubar.addActionListener(LogisimMenuBar.SIMULATE_STEP, myListener);
    menubar.addActionListener(LogisimMenuBar.SIMULATE_VHDL_ENABLE, myListener);
    menubar.addActionListener(LogisimMenuBar.GENERATE_VHDL_SIM_FILES, myListener);
    menubar.addActionListener(LogisimMenuBar.TICK_ENABLE, myListener);
    menubar.addActionListener(LogisimMenuBar.TICK_HALF, myListener);
    menubar.addActionListener(LogisimMenuBar.TICK_FULL, myListener);
    // runToggle.addActionListener(myListener);
    reset.addActionListener(myListener);
    // step.addActionListener(myListener);
    // tickHalf.addActionListener(myListener);
    // tickFull.addActionListener(myListener);
    // ticksEnabled.addActionListener(myListener);
    log.addActionListener(myListener);
    test.addActionListener(myListener);
    assemblyWindow.addActionListener(myListener);

    computeEnabled();
  }

  public void hotkeyUpdate() {
    runToggle.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_AUTO_PROPAGATE).getWithMask(0));
    reset.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_RESET).getWithMask(0));
    step.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_STEP).getWithMask(0));
    tickHalf.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_HALF).getWithMask(0));
    tickFull.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_FULL).getWithMask(0));
    ticksEnabled.setAccelerator(((PrefMonitorKeyStroke)
        AppPreferences.HOTKEY_SIM_TICK_ENABLED).getWithMask(0));
  }

  public static List<String> getTickFrequencyStrings() {
    final var result = new ArrayList<String>();
    for (final var supportedTickFrequency : SUPPORTED_TICK_FREQUENCIES) {
      if (supportedTickFrequency < 1000) {
        final var small =
            (Math.abs(supportedTickFrequency - Math.round(supportedTickFrequency)) < 0.0001);
        final var freqHz =
            "" + ((small) ? (int) Math.round(supportedTickFrequency) : supportedTickFrequency);
        result.add(S.get("simulateTickFreqItem", freqHz));
      } else {
        final var kf = Math.round(supportedTickFrequency / 100) / 10.0;
        final var freqKhz = "" + ((kf == Math.round(kf)) ? (int) kf : kf);
        result.add(S.get("simulateTickKFreqItem", freqKhz));
      }
    }
    return result;
  }

  private void clearItems(ArrayList<CircuitStateMenuItem> items) {
    for (final var item : items) {
      item.unregister();
    }
    items.clear();
  }

  @Override
  protected void computeEnabled() {
    final var present = currentState != null;
    setEnabled(present);
    runToggle.setEnabled(present);
    reset.setEnabled(present);
    step.setEnabled(present);
    simulateVhdlEnable.setEnabled(present);
    vhdlSimFiles.setEnabled(present);
    upStateMenu.setEnabled(present);
    downStateMenu.setEnabled(present);
    tickHalf.setEnabled(present);
    tickFull.setEnabled(present);
    ticksEnabled.setEnabled(present);
    tickFreq.setEnabled(present);
    menubar.fireEnableChanged();
  }

  public void localeChanged() {
    this.setText(S.get("simulateMenu"));
    runToggle.setText(S.get("simulateRunItem"));
    reset.setText(S.get("simulateResetItem"));
    step.setText(S.get("simulateStepItem"));
    simulateVhdlEnable.setText(S.get("simulateVhdlEnableItem"));
    vhdlSimFiles.setText(S.get("simulateGenVhdlFilesItem"));
    tickHalf.setText(S.get("simulateTickHalfItem"));
    tickFull.setText(S.get("simulateTickFullItem"));
    ticksEnabled.setText(S.get("simulateTickItem"));
    tickFreq.setText(S.get("simulateTickFreqMenu"));

    for (final var freq : tickFreqs) {
      freq.localeChanged();
    }

    downStateMenu.setText(S.get("simulateDownStateMenu"));
    upStateMenu.setText(S.get("simulateUpStateMenu"));
    log.setText(S.get("simulateLogItem"));
    test.setText(S.get("simulateTestItem"));
    assemblyWindow.setText(S.get("simulateAssemblyViewer"));
  }

  private void recreateStateMenu(JMenu menu, List<CircuitStateMenuItem> items, int code) {
    menu.removeAll();
    menu.setEnabled(!items.isEmpty());
    var first = true;
    final var mask = getToolkit().getMenuShortcutKeyMaskEx();
    for (var i = items.size() - 1; i >= 0; i--) {
      final var item = items.get(i);
      menu.add(item);
      if (first) {
        item.setAccelerator(KeyStroke.getKeyStroke(code, mask));
        first = false;
      } else {
        item.setAccelerator(null);
      }
    }
  }

  private void recreateStateMenus() {
    recreateStateMenu(downStateMenu, downStateItems, KeyEvent.VK_RIGHT);
    recreateStateMenu(upStateMenu, upStateItems, KeyEvent.VK_LEFT);
  }

  public void setCurrentState(Simulator sim, CircuitState value) {
    if (currentState == value) {
      return;
    }
    final var oldSim = currentSim;
    final var oldState = currentState;
    currentSim = sim;
    currentState = value;
    if (bottomState == null) {
      bottomState = currentState;
    } else if (currentState == null) {
      bottomState = null;
    } else {
      var cur = bottomState;
      while (cur != null && cur != currentState) {
        cur = cur.getParentState();
      }
      if (cur == null) {
        bottomState = currentState;
      }
    }

    final var oldPresent = oldState != null;
    final var present = currentState != null;
    if (oldPresent != present) {
      computeEnabled();
    }

    if (currentSim != oldSim) {
      final var freq = currentSim == null ? 1.0 : currentSim.getTickFrequency();
      for (final var tickFrequencyChoice : tickFreqs) {
        tickFrequencyChoice.setSelected(Math.abs(tickFrequencyChoice.freq - freq) < 0.001);
      }

      if (oldSim != null) {
        oldSim.removeSimulatorListener(myListener);
      }
      if (currentSim != null) {
        currentSim.addSimulatorListener(myListener);
      }
      myListener.simulatorStateChanged(new Simulator.Event(sim, false, false, false));
    }

    clearItems(downStateItems);
    var cur = bottomState;
    while (cur != null && cur != currentState) {
      downStateItems.add(new CircuitStateMenuItem(cur));
      cur = cur.getParentState();
    }
    if (cur != null) {
      cur = cur.getParentState();
    }
    clearItems(upStateItems);
    while (cur != null) {
      upStateItems.add(0, new CircuitStateMenuItem(cur));
      cur = cur.getParentState();
    }
    recreateStateMenus();
  }

  private class CircuitStateMenuItem extends JMenuItem implements CircuitListener, ActionListener {

    private final CircuitState circuitState;

    public CircuitStateMenuItem(CircuitState circuitState) {
      this.circuitState = circuitState;

      final var circuit = circuitState.getCircuit();
      circuit.addCircuitListener(this);
      this.setText(circuit.getName());
      addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      menubar.fireStateChanged(currentSim, circuitState);
    }

    @Override
    public void circuitChanged(CircuitEvent event) {
      if (event.getAction() == CircuitEvent.ACTION_SET_NAME) {
        this.setText(circuitState.getCircuit().getName());
      }
    }

    void unregister() {
      circuitState.getCircuit().removeCircuitListener(this);
    }
  }

  private class MyListener implements ActionListener, Simulator.StatusListener, ChangeListener {

    @Override
    public void actionPerformed(ActionEvent e) {
      final var src = e.getSource();

      final var proj = menubar.getSimulationProject();
      if (proj == null) {
        return;
      }
      final var vhdl = proj.getVhdlSimulator();
      if (vhdl != null && (src == simulateVhdlEnable
          || src == LogisimMenuBar.SIMULATE_VHDL_ENABLE)) {
        vhdl.setEnabled(!vhdl.isEnabled());
      } else if (vhdl != null && (src == vhdlSimFiles
          || src == LogisimMenuBar.GENERATE_VHDL_SIM_FILES)) {
        vhdl.restart();
      } else if (src == log) {
        proj.getLogFrame().setVisible(true);
      } else if (src == test) {
        proj.getTestFrame().setVisible(true);
      }

      final var sim = proj.getSimulator();
      if (sim == null) {
        return;
      }

      if (src == LogisimMenuBar.SIMULATE_STOP) {
        sim.setAutoPropagation(false);
        proj.repaintCanvas();
      } else if (src == LogisimMenuBar.SIMULATE_RUN) {
        sim.setAutoPropagation(true);
        proj.repaintCanvas();
      } else if (src == runToggle || src == LogisimMenuBar.SIMULATE_RUN_TOGGLE) {
        sim.setAutoPropagation(!sim.isAutoPropagating());
        proj.repaintCanvas();
      } else if (src == reset) {
        /* Restart VHDL simulation (in QuestaSim) */
        if (vhdl != null && vhdl.isRunning()) {
          vhdl.reset();
          // Wait until the restart finishes, otherwise the signal reset will be
          // sent to the VHDL simulator before the sim is loaded and errors will
          // occur. Wait time (0.5 sec) is arbitrary.
          // FIXME: Find a better way to do blocking reset.
          try {
            Thread.sleep(500);
          } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
          }
        }
        sim.reset();
        proj.repaintCanvas();
      } else if (src == step || src == LogisimMenuBar.SIMULATE_STEP) {
        sim.setAutoPropagation(false);
        sim.step();
      } else if (src == tickHalf || src == LogisimMenuBar.TICK_HALF) {
        sim.tick(1);
      } else if (src == tickFull || src == LogisimMenuBar.TICK_FULL) {
        sim.tick(2);
      } else if (src == ticksEnabled || src == LogisimMenuBar.TICK_ENABLE) {
        sim.setAutoTicking(!sim.isAutoTicking());
      } else if (src == assemblyWindow) {
        if (assWin == null || !assWin.isVisible()) {
          assWin = new AssemblyWindow(proj);
          assWin.setVisible(true);
        } else {
          assWin.toFront();
        }
      }
    }

    @Override
    public void simulatorReset(Simulator.Event e) {
      updateSimulator(e);
    }

    @Override
    public void simulatorStateChanged(Simulator.Event e) {
      updateSimulator(e);
    }

    void updateSimulator(Simulator.Event e) {
      final var sim = e.getSource();
      if (sim != currentSim) {
        return;
      }
      computeEnabled();
      runToggle.setSelected(sim.isAutoPropagating());
      ticksEnabled.setSelected(sim.isAutoTicking());
      final var freq = sim.getTickFrequency();
      for (final var item : tickFreqs) {
        item.setSelected(freq == item.freq);
      }
    }

    @Override
    public void stateChanged(ChangeEvent e) {
      // do nothing
    }
  }

  private class TickFrequencyChoice extends JRadioButtonMenuItem implements ActionListener {

    private final double freq;

    public TickFrequencyChoice(double value) {
      freq = value;
      addActionListener(this);
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (currentSim != null) {
        currentSim.setTickFrequency(freq);
      }
    }

    public void localeChanged() {
      double f = freq;
      if (f < 1000) {
        final var small = Math.abs(f - Math.round(f)) < 0.0001;
        final var freqHz = "" + (small ? (int) Math.round(f) : f);
        setText(S.get("simulateTickFreqItem", freqHz));
      } else {
        final var kf = Math.round(f / 100) / 10.0;
        final var freqKhz = "" + ((kf == Math.round(kf)) ? (int) kf : kf);
        setText(S.get("simulateTickKFreqItem", freqKhz));
      }
    }
  }
}
