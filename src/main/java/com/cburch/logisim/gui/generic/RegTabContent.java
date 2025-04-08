/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.generic;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.log.LoggableContract;
import com.cburch.logisim.gui.main.Frame;
import static com.cburch.logisim.gui.Strings.S;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectEvent;
import com.cburch.logisim.proj.ProjectListener;
import com.cburch.logisim.std.memory.Register;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.util.Comparator;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.Arrays;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RegTabContent extends JScrollPane
    implements LocaleListener, Simulator.Listener, ProjectListener, CircuitListener {
  private final JPanel panel = new JPanel(new GridBagLayout());
  private final GridBagConstraints gridConstraints = new GridBagConstraints();
  private final Project proj;
  private final MyLabel hdrName = new MyLabel("", Font.ITALIC | Font.BOLD, false, Color.LIGHT_GRAY);
  private final MyLabel hdrValue = new MyLabel("", Font.BOLD, false, Color.LIGHT_GRAY);
  private boolean showing = false;
  private CircuitState circuitState;
  private final ArrayList<Circuit> circuits = new ArrayList<>();
  private final CopyOnWriteArrayList<Watcher> watchers = new CopyOnWriteArrayList<>();

  public RegTabContent(Frame frame) {
    super();
    setViewportView(panel);
    proj = frame.getProject();
    getVerticalScrollBar().setUnitIncrement(16);

    gridConstraints.fill = GridBagConstraints.HORIZONTAL;
    gridConstraints.anchor = GridBagConstraints.FIRST_LINE_START;
    gridConstraints.ipady = 2;

    localeChanged();
    LocaleManager.addLocaleListener(this);

    addComponentListener(new ComponentAdapter() {
      public void componentShown(ComponentEvent e) {
        showing = true;
        fill();
        proj.getSimulator().addSimulatorListener(RegTabContent.this);
      }

      public void componentHidden(ComponentEvent e) {
        showing = false;
        proj.getSimulator().removeSimulatorListener(RegTabContent.this);
      }
    });

    proj.addProjectListener(this);

    clear();
    circuitState = proj.getCircuitState();
    fill();
  }

  @Override
  public void projectChanged(ProjectEvent event) {
    if (event.getAction() == ProjectEvent.ACTION_SET_STATE) {
      clear();
      circuitState = proj.getCircuitState();
      fill();
    }
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    if (circuitState != null && event.getAction() != CircuitEvent.ACTION_INVALIDATE) {
      clear();
      fill();
    }
  }

  private void clear() {
    if (circuits.isEmpty()) return;
    for (final var circ : circuits) {
      circ.removeCircuitListener(this);
    }
    circuits.clear();
    watchers.clear();
    panel.removeAll();
    gridConstraints.weighty = 0;
    gridConstraints.gridy = 0;
    gridConstraints.gridx = 0;
    gridConstraints.weightx = 0.7;
    panel.add(hdrName, gridConstraints);
    gridConstraints.gridx = 1;
    gridConstraints.weightx = 0.3;
    panel.add(hdrValue, gridConstraints);
  }

  private void fill() {
    if (!showing || circuitState == null) return;
    if (circuits.isEmpty()) {
      enumerate();
    }
    updateWatchers();
    writeValuesToLabels();
  }

  private void updateWatchers() {
    for (final var watcher : watchers) {
      watcher.update();
    }
  }

  public void writeValuesToLabels() {
    if (!showing || circuitState == null) return;
    for (final var watcher : watchers) {
      watcher.writeToLabel();
    }
  }

  private void enumerate() {
    if (circuitState == null) return;
    Circuit circ = circuitState.getCircuit();
    enumerate(null, circ, circuitState);
    gridConstraints.weighty = 1;
    gridConstraints.gridy++;
    gridConstraints.gridx = 0;
    gridConstraints.weightx = 1;
    panel.add(new MyLabel("", 0, false, null), gridConstraints); // padding at bottom
    panel.validate();
  }

  private void enumerate(String prefix, Circuit circ, CircuitState cs) {
    if (!circuits.contains(circ)) {
      circuits.add(circ);
      circ.addCircuitListener(this);
    }
    enumerateLoggables(prefix, circ, cs);
    enumerateSubcircuits(prefix, circ, cs);
  }

  private void enumerateLoggables(String prefix, Circuit circ, CircuitState cs) {
    final var names = new HashMap<Component, String>();

    for (final var comp : circ.getNonWires()) {
      AttributeSet as = comp.getAttributeSet();
      if (!as.containsAttribute(Register.ATTR_SHOW_IN_TAB)) continue;
      if (!as.getValue(Register.ATTR_SHOW_IN_TAB)) continue;
      final var log = (LoggableContract) comp.getFeature(LoggableContract.class);
      if (log == null) continue;
      var name = log.getLogName(null);
      if (name == null) {
        name = comp.getFactory().getName() + comp.getLocation();
      }
      names.put(comp, name);
    }
    if (names.isEmpty()) return;
    final var comps = names.keySet().toArray();
    Arrays.sort(comps, new CompareByNameLocEtc(names));
    for (final var obj : comps) {
      final var comp = (Component) obj;
      var name = names.get(comp);
      if (prefix != null) {
        name = prefix + "/" + name;
      }
      final var log = (LoggableContract) comp.getFeature(LoggableContract.class);
      gridConstraints.gridy++;
      gridConstraints.gridx = 0;
      panel.add(new MyLabel(name, Font.ITALIC, true, null), gridConstraints);
      gridConstraints.gridx = 1;
      final var label = new MyLabel("-", 0, false, null);
      panel.add(label, gridConstraints);
      watchers.add(new Watcher(log, cs, label));
    }
  }

  private void enumerateSubcircuits(String prefix, Circuit circ, CircuitState cs) {
    final var names = new HashMap<Component, String>();

    for (final var comp : circ.getNonWires()) {
      if (!(comp.getFactory() instanceof SubcircuitFactory)) continue;
      final var factory = (SubcircuitFactory) comp.getFactory();
      var name = comp.getAttributeSet().getValue(StdAttr.LABEL);
      if (name == null || name.equals("")) {
        name = factory.getSubcircuit().getName() + comp.getLocation();
      }
      names.put(comp, name);
    }
    if (names.isEmpty()) return;
    final var comps = names.keySet().toArray();
    Arrays.sort(comps, new CompareByNameLocEtc(names));
    for (final var obj : comps) {
      Component comp = (Component) obj;
      var name = names.get(comp);
      if (prefix != null) {
        name = prefix + "/" + name;
      }
      final var factory = (SubcircuitFactory) comp.getFactory();
      final var substate = factory.getSubstate(cs, comp);
      enumerate(name, factory.getSubcircuit(), substate);
    }
  }

  @Override
  public void localeChanged() {
    hdrName.setText(S.get("registerTabNameTitle"));
    hdrValue.setText(S.get("registerTabValueTitle"));
  }

  @Override
  public void simulatorReset(Simulator.Event e) {
    updateWatchers();
  }

  @Override
  public void propagationCompleted(Simulator.Event e) {
    updateWatchers();
  }

  @Override
  public void simulatorStateChanged(Simulator.Event e) {
  }

  private static class CompareByNameLocEtc implements Comparator<Object> {
    HashMap<Component, String> names;
    CompareByNameLocEtc(HashMap<Component, String> names) {
      this.names = names;
    }
    public int compare(Object left, Object right) {
      final var leftName = names.get((Component) left);
      final var rightName = names.get((Component) right);
      int ret = leftName.compareToIgnoreCase(rightName);
      if (ret == 0) {
        ret = ((Component) left).getLocation().compareTo(((Component) right).getLocation());
      }
      if (ret == 0) {
        ret = left.hashCode() - right.hashCode(); // last resort, for stability
      }
      return ret;
    }
  }

  private static class MyLabel extends JLabel {
    private MyLabel(String text, int style, boolean small, Color bgColor) {
      super(text);
      if (bgColor != null) {
        setOpaque(true);
        setBackground(bgColor);
        setBorder(BorderFactory.createMatteBorder(0, 4, 0, 4, bgColor));
      } else {
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
      }
      if (style == 0 && !small) return;
      Font font = getFont();
      if (style != 0) {
        font = font.deriveFont(style);
      }
      if (small) {
        font = font.deriveFont(font.getSize2D() - 2);
      }
      setFont(font);
    }
  }

  private static class Watcher {
    final LoggableContract log;
    final CircuitState cs;
    final MyLabel label;
    Value val;
    Watcher(LoggableContract log, CircuitState cs, MyLabel label) {
      this.log = log;
      this.cs = cs;
      this.label = label;
      update();
    }

    void update() {
      final var newVal = log.getLogValue(cs, null);
      if (val == null && newVal == null
          || (val != null && newVal != null && val.equals(newVal))) {
        return;
      }
      val = newVal;
    }

    void writeToLabel() {
      label.setText(val == null ? "-" : val.toHexString());
    }
  }
}
