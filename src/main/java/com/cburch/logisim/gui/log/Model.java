/*
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

package com.cburch.logisim.gui.log;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.std.wiring.Clock;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Model implements CircuitListener {

  public static final int STEP = 10;
  public static final int REAL = 20;
  public static final int CLOCK = 30;
  public static final int COARSE = 1;
  public static final int FINE = 2;
  
  public static class Event {};
  
  public interface Listener{
    public void signalsReset(Event event);
    public void signalsExtended(Event event);
    public void filePropertyChanged(Event event);
    public void selectionChanged(Event event);    
    public void modeChanged(Event event);
    public void historyLimitChanged(Event event);
  }
  
  CircuitState circuitState;
  private ArrayList<SignalInfo> info = new ArrayList<>();
  private ArrayList<Signal> signals = new ArrayList<>();
  private long tEnd = 1; // signals go from 0 <= t < tEnd
  private Signal spotlight;
  private EventSourceWeakSupport<Listener> listeners = new EventSourceWeakSupport<>();
  private boolean fileEnabled = false;
  private File file = null;
  private boolean fileHeader = true;
  private boolean selected = false;
  private LogThread logger = null;
  private int mode = STEP, granularity = COARSE;
  private long timeScale = 5000, gateDelay = 10;
  private int cycleLength = 2;
  private int historyLimit = 400;
  
  public Model(CircuitState root) {
    circuitState = root;
   // Add top-level pins, clocks, etc.
    Circuit circ = circuitState.getCircuit();
    for (Component comp : circ.getNonWires()) {
      SignalInfo item = makeIfDefaultComponent(circ, comp);
      if (item == null)
        continue;
      info.add(item);
    }

    // sort: inputs before outputs, then by location 
    // inputs are things like Button, Clock, Pin(input), and Random
    Location.sortHorizontal(info);
    int n = info.size();
    for (int i = 0; i < n; i++) {
      SignalInfo item = info.get(i);
      if (!item.isInput(null)) {
        info.add(info.remove(i));
        i--;
        n--;
      }
    }

    // set up initial signal values (after sorting)
    for (int i = 0; i < info.size(); i++) {
      SignalInfo item = info.get(i);
      signals.add(new Signal(i, item, item.fetchValue(circuitState),
            1, tEnd - 1, historyLimit));
    }

    if (containsAnyClock(circ))
      mode = CLOCK;
    // Listen for new pins, clocks, etc.
    circuitState.getCircuit().addCircuitListener(this);
  }

  private void renumberSignals() {
    for (int i = 0; i < signals.size(); i++)
      signals.get(i).idx = i;
  }

  public void addOrMove(List<SignalInfo> items, int idx) {
    int changed = items.size();
    for (SignalInfo item : items) {
      int i = info.indexOf(item);
      if (i < 0) {
        info.add(idx, item); // put new item at idx
        signals.add(idx,
            new Signal(idx, item, item.fetchValue(circuitState),
              1, tEnd - 1, historyLimit));
        idx++;
      } else if (i > idx) {
        info.add(idx, info.remove(i)); // move later item up
        signals.add(idx, signals.remove(i));
        idx++;
      } else if (i < idx) {
        info.add(idx-1, info.remove(i)); // move earlier item down
        signals.add(idx-1, signals.remove(i));
      } else {
        changed--;
      }
    }
    if (changed > 0) {
      renumberSignals();
      fireSelectionChanged(new Model.Event());
    }
  }

  public int remove(List<SignalInfo> items) {
    int count = 0;
    for (SignalInfo item : items) {
      int idx = info.indexOf(item);
      if (idx < 0)
        continue;
      info.remove(idx);
      signals.remove(idx);
      count++;
    }
    if (count > 0) {
      if (spotlight != null && items.contains(spotlight))
        spotlight = null;
      renumberSignals();
      fireSelectionChanged(new Model.Event());
    }
    return count;
  }

  public void move(int[] fromIndex, int toIndex) {
    int n = fromIndex.length;
    if (n == 0)
      return;
    Arrays.sort(fromIndex);
    int a = fromIndex[0];
    int b = fromIndex[n-1];
    if (a < 0 || b > info.size())
      return; // invalid selection
    if (a <= toIndex && toIndex <= b && b-a+1 == n)
      return; // no-op
    ArrayList<SignalInfo> items = new ArrayList<>();
    ArrayList<Signal> vals = new ArrayList<>();
    for (int i = n-1; i >= 0; i--) {
      if (fromIndex[i] < toIndex)
        toIndex--;
      items.add(info.remove(fromIndex[i]));
      vals.add(signals.remove(fromIndex[i]));
    }
    for (int i = n-1; i >= 0; i--) {
      info.add(toIndex, items.get(i));
      signals.add(toIndex, vals.get(i));
      toIndex++;
    }
    fireSelectionChanged(new Model.Event());
  }

  public void remove(int idx) {
    if (spotlight != null && signals.get(idx) == spotlight)
      spotlight = null;
    info.remove(idx);
    signals.remove(idx);
    fireSelectionChanged(new Model.Event());
  }

  public void remove(SignalInfo item) {
    remove(info.indexOf(item));
  }

  public long getTimeScale() {
    return timeScale;
  }

  public long getGateDelay() {
    return gateDelay;
  }

  public int getCycleLength() {
    return cycleLength;
  }

  public boolean isStepMode() { return mode == STEP; }
  public boolean isRealMode() { return mode == REAL; }
  public boolean isClockMode() { return mode == CLOCK; }
  public boolean isFine() { return granularity == FINE; }

  public int getHistoryLimit() {
    return historyLimit;
  }
  public void setHistoryLimit(int limit) {
    if (historyLimit == limit)
      return;
    historyLimit = limit;
    fireHistoryLimitChanged(null);
  }

  static boolean containsAnyClock(Circuit circ) {
    for (Component comp : circ.getNonWires())
      if (containsAnyClock(comp))
        return true;
    return false;
  }

  static boolean containsAnyClock(Component comp) {
    if (comp.getFactory() instanceof Clock) {
      return true;
    } else if (comp.getFactory() instanceof SubcircuitFactory) {
      SubcircuitFactory f = (SubcircuitFactory)comp.getFactory();
      if (containsAnyClock(f.getSubcircuit()))
        return true;
    }
    return false;
  }

  public void setStepMode(long t, long d) {
    int g = d > 0 ? FINE : COARSE;
    if (mode == STEP && granularity == g && timeScale == t && gateDelay == d)
      return;
    timeScale = t;
    gateDelay = d;
    setMode(STEP, g);
  }

  public void setRealMode(long t, boolean fine) {
    int g = fine ? FINE : COARSE;
    if (mode == REAL && granularity == g && timeScale == t)
      return;
    timeScale = t;
    setMode(REAL, g);
  }

  public void setClockMode(long t, int n, long d) {
    int g = d > 0 ? FINE : COARSE;
    if (mode == CLOCK && granularity == g && timeScale == t && cycleLength == n && gateDelay == d)
      return;
    timeScale = t;
    cycleLength = n;
    gateDelay = d;
    setMode(CLOCK, g);
  }

  private void setMode(int m, int g) {
    mode = m;
    granularity = g;
    fireModeChanged(null);
  }

  @Override
  public void circuitChanged(CircuitEvent event) {
    int action = event.getAction();
    // todo: gracefully handle pin width changes, other circuit changes
    if (action == CircuitEvent.TRANSACTION_DONE) {
      Circuit circ = circuitState.getCircuit();
      ReplacementMap repl = event.getResult().getReplacementMap(circ);
      if (repl == null || repl.isEmpty()) return;
      for (Component comp : repl.getAdditions()) {
        if (!repl.getReplacedBy(comp).isEmpty())
          continue;
        if (mode == STEP && containsAnyClock(comp))
          setMode(CLOCK, granularity);
        SignalInfo item = makeIfDefaultComponent(circ, comp);
        if (item == null)
          continue;
        addAndInitialize(item);
      }
    }
  }

  private SignalInfo makeIfDefaultComponent(Circuit circ, Component comp) {
    CircuitState circuitState = getCircuitState();
    if (circ != circuitState.getCircuit()) return null;
    if (comp.getFactory() instanceof SubcircuitFactory) return null;
    Loggable log = (Loggable)comp.getFeature(Loggable.class);
    if (log == null) return null;
    Object[] opts = log.getLogOptions(circuitState);
    if (opts != null && opts.length > 0) return null;
    Component[] path = new Component[] { comp };
    return new SignalInfo(this, path, null);
  }

  // Add top-level pins, etc.
  private void addIfDefaultComponent(Circuit circ, Component comp) {
    SignalInfo item = makeIfDefaultComponent(circ, comp);
    if (item == null || info.contains(item)) return;
    addAndInitialize(item);
  }

  private void addAndInitialize(SignalInfo item) {
    if (info.contains(item))
      return;
    int idx = info.size();
    info.add(item);
    signals.add(idx, new Signal(idx, item, item.fetchValue(circuitState), 1, tEnd - 1, historyLimit));
    fireSelectionChanged(null);
  }
  
  public void addSignalValues(Value[] vals, long duration) {
    for (int i = 0; i < signals.size() && i < vals.length; i++)
      signals.get(i).extend(vals[i], duration);
    tEnd += duration;
  }

  public void addModelListener(Listener l) {
    listeners.add(l);
  }

  public void removeModelListener(Listener l) {
    listeners.remove(l);
  }
  

  private void fireSignalsReset(Event e) {
    for (Listener l : listeners) l.signalsReset(e);
  }

  private void fireSignalsExtended(Event e) {
    for (Listener l : listeners) l.signalsExtended(e);
  }

  private void fireFilePropertyChanged(Event e) {
    for (Listener l : listeners)  l.filePropertyChanged(e);
  }
  
  private void fireModeChanged(Event e) {
    for (Listener l : listeners) l.modeChanged(e);
  }

  private void fireHistoryLimitChanged(Event e) {
    for (Listener l : listeners) l.historyLimitChanged(e);
  }

  void fireSelectionChanged(Event e) {
    for (Listener l : listeners) l.selectionChanged(e);
  }

  public CircuitState getCircuitState() {
    return circuitState;
  }

  public File getFile() {
    return file;
  }

  public boolean getFileHeader() {
    return fileHeader;
  }

  public int getSignalCount() {
    return signals.size();
  }

  public ArrayList<Signal> getSignals() {
    return signals;
  }

  public long getEndTime() {
    return tEnd;
  }

  public SignalInfo getItem(int idx) {
    return info.get(idx);
  }

  public Signal getSignal(int idx) {
    return signals.get(idx);
  }

  public Signal getSignal(SignalInfo item) {
    return signals.get(info.indexOf(item));
  }

  public int indexOf(SignalInfo item) {
    return info.indexOf(item);
  }

  public boolean isFileEnabled() {
    return fileEnabled;
  }

  public boolean isSelected() {
    return selected;
  }

  public void propagationCompleted() {
    long duration = timeScale; // todo: fixme, use correct duration based on mode
    for (Signal s : signals) {
      Value v = s.info.fetchValue(circuitState);
      s.extend(v, duration);
    }
    tEnd += duration;
    fireSignalsExtended(null);
  }

  public void simulatorReset() {
    // long duration = isFine() ? gateDelay : timeScale;
    long duration = 1;
    for (Signal s: signals) {
      Value v = s.info.fetchValue(circuitState);
      s.reset(v, duration);
    }
    tEnd = duration;
  }

  public void setFile(File value) {
    if (file == null ? value == null : file.equals(value)) return;
    file = value;
    fileEnabled = file != null;
    fireFilePropertyChanged(null);
  }

  public void setFileEnabled(boolean value) {
    if (fileEnabled == value) return;
    fileEnabled = value;
    fireFilePropertyChanged(null);
  }

  public void setFileHeader(boolean value) {
    if (fileHeader == value) return;
    fileHeader = value;
    fireFilePropertyChanged(null);
  }

  public void setSelected(boolean value) {
    if (selected == value) return;
    selected = value;
    if (selected) {
      logger = new LogThread(this);
      logger.start();
    } else {
      if (logger != null) logger.cancel();
      logger = null;
      fileEnabled = false;
    }
    fireFilePropertyChanged(null);
  }

  //In the ChronoPanel, the (at most one) signal under the mouse is
  // highlighted. Multiple other rows can be selected by clicking. This
  // code maybe should be put elsewhere.
  public Signal getSpotlight() {
    return spotlight;
  }

  public Signal setSpotlight(Signal s) {
    Signal old = spotlight;
    spotlight = s;
    return old;
  }

  public static String formatDuration(long t) {
    if (t < 1000 || (t % 100) != 0)
      return S.fmt("nsFormat", String.format("%d", t));
    else if (t < 1000000 || (t % 100000) != 0)
      return S.fmt("usFormat", String.format("%.1f", t/1000.0));
    else if (t < 100000000 || (t % 100000000) != 0)
      return S.fmt("msFormat", String.format("%.1f", t/1000000.0));
    else
      return S.fmt("sFormat", String.format("%.1f", t/1000000000.0));
  }

}
