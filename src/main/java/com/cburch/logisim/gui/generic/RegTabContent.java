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
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

public class RegTabContent extends JScrollPane
    implements LocaleListener, Simulator.Listener, ProjectListener, CircuitListener {
  private JPanel panel = new JPanel(new GridBagLayout());
  private GridBagConstraints c = new GridBagConstraints();
  private Project proj;
  private MyLabel hdrName = new MyLabel("", Font.ITALIC | Font.BOLD, false, Color.LIGHT_GRAY);
  private MyLabel hdrValue = new MyLabel("", Font.BOLD, false, Color.LIGHT_GRAY);
  private boolean showing = false;
  private CircuitState circuitState;
  private ArrayList<Circuit> circuits = new ArrayList<>();
  private ArrayList<Watcher> watchers = new ArrayList<>();

  public RegTabContent(Frame frame) {
    super();
    setViewportView(panel);
    proj = frame.getProject();
    getVerticalScrollBar().setUnitIncrement(16);

    c.fill = GridBagConstraints.HORIZONTAL;
    c.anchor = GridBagConstraints.FIRST_LINE_START;
    c.ipady = 2;

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

  void clear() {
    if (circuits.isEmpty())
      return;
    for (Circuit circ : circuits)
      circ.removeCircuitListener(this);
    circuits.clear();
    watchers.clear();
    panel.removeAll();
    c.weighty = 0;
    c.gridy = 0;
    c.gridx = 0;
    c.weightx = 0.7;
    panel.add(hdrName, c);
    c.gridx = 1;
    c.weightx = 0.3;
    panel.add(hdrValue, c);
  }

  public void fill() {
    if (!showing || circuitState == null)
      return;
    if (circuits.isEmpty())
      enumerate();
    updateWatchers();
    writeLabels();
  }

  public void updateWatchers() {
    for (Watcher w : watchers)
      w.update();
  }

  public void writeLabels() {
    if (!showing || circuitState == null)
      return;
    for (final var watcher : watchers) {
      watcher.writeToLabel();
    }
  }

  private void enumerate() {
    if (circuitState == null)
      return;
    Circuit circ = circuitState.getCircuit();
    enumerate(null, circ, circuitState);
    c.weighty = 1;
    c.gridy++;
    c.gridx = 0;
    c.weightx = 1;
    panel.add(new MyLabel("", 0, false, null), c); // padding at bottom
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
    HashMap<Component, String> names = new HashMap<>();

    for (Component comp : circ.getNonWires()) {
      AttributeSet as = comp.getAttributeSet();
      if (!as.containsAttribute(Register.ATTR_SHOW_IN_TAB))
        continue;
      if (!as.getValue(Register.ATTR_SHOW_IN_TAB))
        continue;
      LoggableContract log = (LoggableContract) comp.getFeature(LoggableContract.class);
      if (log == null)
        continue;
      String name = log.getLogName(null);
      if (name == null)
        name = comp.getFactory().getName() + comp.getLocation();
      names.put(comp, name);
    }
    if (names.isEmpty())
      return;
    Object[] comps = names.keySet().toArray();
    Arrays.sort(comps, new CompareByNameLocEtc(names));
    for (Object o : comps) {
      Component comp = (Component) o;
      String name = names.get(comp);
      if (prefix != null)
        name = prefix + "/" + name;
      LoggableContract log = (LoggableContract) comp.getFeature(LoggableContract.class);
      Value val = log.getLogValue(cs, null);
      c.gridy++;
      c.gridx = 0;
      panel.add(new MyLabel(name, Font.ITALIC, true, null), c);
      c.gridx = 1;
      MyLabel v = new MyLabel("-", 0, false, null);
      panel.add(v, c);
      watchers.add(new Watcher(log, cs, v));
    }
  }

  private static class Watcher {
    LoggableContract log;
    CircuitState cs;
    MyLabel label;
    Value val;
    Watcher(LoggableContract log, CircuitState cs, MyLabel label) {
      this.log = log;
      this.cs = cs;
      this.label = label;
      update();
    }
    void update() {
      Value newVal = log.getLogValue(cs, null);
      if (val == null && newVal == null
          || (val != null && newVal != null && val.equals(newVal)))
        return;
      val = newVal;
    }

    void writeToLabel() {
      label.setText(val == null ? "-" : val.toHexString());
    }
  }

  private void enumerateSubcircuits(String prefix, Circuit circ, CircuitState cs) {
    HashMap<Component, String> names = new HashMap<>();

    for (Component comp : circ.getNonWires()) {
      if (!(comp.getFactory() instanceof SubcircuitFactory))
        continue;
      SubcircuitFactory factory = (SubcircuitFactory) comp.getFactory();
      String name = comp.getAttributeSet().getValue(StdAttr.LABEL);
      if (name == null || name.equals(""))
        name = factory.getSubcircuit().getName() + comp.getLocation();
      names.put(comp, name);
    }
    if (names.isEmpty())
      return;
    Object[] comps = names.keySet().toArray();
    Arrays.sort(comps, new CompareByNameLocEtc(names));
    for (Object o : comps) {
      Component comp = (Component) o;
      String name = names.get(comp);
      if (prefix != null)
        name = prefix + "/" + name;
      SubcircuitFactory factory = (SubcircuitFactory) comp.getFactory();
      CircuitState substate = factory.getSubstate(cs, comp);
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
    public int compare(Object a, Object b) {
      String aName = names.get((Component) a);
      String bName = names.get((Component) b);
      int d = aName.compareToIgnoreCase(bName);
      if (d == 0)
        d = ((Component) a).getLocation().compareTo(((Component) b).getLocation());
      if (d == 0)
        d = a.hashCode() - b.hashCode(); // last resort, for stability
      return d;
    }
  }

  private static class MyLabel extends JLabel {
    private MyLabel(String text, int style, boolean small, Color bg) {
      super(text);
      if (bg != null) {
        setOpaque(true);
        setBackground(bg);
        setBorder(BorderFactory.createMatteBorder(0, 4, 0, 4, bg));
      } else {
        setBorder(BorderFactory.createEmptyBorder(0, 4, 0, 4));
      }
      if (style == 0 && !small)
        return;
      Font f = getFont();
      if (style != 0)
        f = f.deriveFont(style);
      if (small)
        f = f.deriveFont(f.getSize2D() - 2);
      setFont(f);
    }
  }
}
