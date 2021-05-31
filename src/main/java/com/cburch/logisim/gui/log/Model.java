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
import com.cburch.logisim.circuit.RadixOption;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.util.EventSourceWeakSupport;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Model implements CircuitListener, SignalInfo.Listener {

  public static final int STEP = 10;
  public static final int REAL = 20;
  private static final int CLOCKED = 30; // above this are all clocked modes
  public static final int CLOCK_DUAL = 30;
  public static final int CLOCK_RISING = 40;
  public static final int CLOCK_FALLING = 50;
  public static final int CLOCK_HIGH = 60;
  public static final int CLOCK_LOW = 70;
  public static final int COARSE = 1;
  public static final int FINE = 2;
  
  public static class Event {}

  public interface Listener{
    void signalsReset(Event event);
    void signalsExtended(Event event);
    void filePropertyChanged(Event event);
    void selectionChanged(Event event);
    void modeChanged(Event event);
    void historyLimitChanged(Event event);
  }
  
  final CircuitState circuitState;
  private final ArrayList<SignalInfo> info = new ArrayList<>();
  private final ArrayList<Signal> signals = new ArrayList<>();
  private long tEnd = -1; // signals go from 0 <= t < tEnd
  private Signal spotlight;
  private SignalInfo clockSource;
  private Value curClockVal;
  private final EventSourceWeakSupport<Listener> listeners = new EventSourceWeakSupport<>();
  private boolean fileEnabled = false;
  private File file = null;
  private boolean fileHeader = true;
  private boolean selected = false;
  private LogThread logger = null;
  private int mode = STEP, granularity = COARSE;
  private long timeScale = 5000, gateDelay = 200;
  private int historyLimit = 400;
  private long elapsedSinceTrigger;
  private long lastRealtimeUpdate;
  
  public Model(CircuitState root) {
    circuitState = root;
   // Add top-level pins, clocks, etc.
    Circuit circ = circuitState.getCircuit();
    for (Component comp : circ.getNonWires()) {
      SignalInfo item = makeIfDefaultComponent(comp);
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

    ArrayList<SignalInfo> clocks = ComponentSelector.findClocks(circ);
    if (clocks != null && clocks.size() == 1) {
      // If one clock is present, we use CLOCK mode with that as the source.
      clockSource = clocks.get(0);
    } else if (clocks != null && clocks.size() > 1) {
      // If multiple are present, ask user to select, with STEP as fallback.
      clockSource = ClockSource.doClockMultipleObserverDialog(circ);
      if (clockSource != null
          && (clockSource.getComponent().getFactory() instanceof Pin)
          && (clockSource.getDepth() == 1))
        circuitState.setTemporaryClock(clockSource.getComponent());
    }
    if (clockSource == null) {
      Component clk = circuitState.getTemporaryClock();
      if (clk != null)
        clockSource = new SignalInfo(circ, new Component[] { clk }, null);
    }
    if (clockSource != null) {
      if (!info.contains(clockSource))
        info.add(0, clockSource); // put it at the top of the list
      else
        info.add(0, info.remove(info.indexOf(clockSource)));
      mode = CLOCK_DUAL;
      curClockVal = clockSource.fetchValue(circuitState);
    }

    // set up initial signal values (after sorting)
    long duration = captureContinuous() ? gateDelay : timeScale;
    for (int i = 0; i < info.size(); i++) {
      SignalInfo item = info.get(i);
      signals.add(new Signal(i, item, item.fetchValue(circuitState),
            duration, 0, historyLimit));
    }
    tEnd = duration;

    // Listen for new pins, clocks, etc., and changes to Signals
    for (SignalInfo item : info)
      item.setListener(this); // includes clock source
    circ.addCircuitListener(this);
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
        item.setListener(this);
      } else if (i < idx) {
        info.add(idx-1, info.remove(i)); // move earlier item down
        signals.add(idx-1, signals.remove(i));
      } else {
        changed--;
      }
    }
    if (changed > 0) {
      renumberSignals();
      fireSelectionChanged(null);
    }
  }

  public boolean addOrMoveSignals(List<Signal> items, int idx) {
    int changed = items.size();
    long newEnd = tEnd;
    for (Signal item : items) {
      if (item.info.getTopLevelCircuit() != getCircuit()) {
        changed--; // attempt to paste component from wrong circuit
        continue;
      }
      int i = info.indexOf(item.info);
      if (i < 0) {
        info.add(idx, item.info); // put new item at idx
        // bring signal into sync with others
        item.resize(historyLimit);
        long d = item.getEndTime();
        if (d < newEnd) {
          item.extend(newEnd - d);
        } else if (d > newEnd) {
          for (Signal s : signals)
            s.extend(d - newEnd);
          newEnd = d;
        }
        signals.add(idx, item);
        idx++;
        item.info.setListener(this);
      } else if (i > idx) {
        info.add(idx, info.remove(i)); // move later item up
        signals.add(idx, signals.remove(i));
        idx++;
      } else if (i < idx-1) {
        info.add(idx-1, info.remove(i)); // move earlier item down
        signals.add(idx-1, signals.remove(i));
      } else {
        changed--; // no change to existing item
      }
    }
    if (changed == 0 && newEnd == tEnd)
      return false;
    tEnd = newEnd;
    renumberSignals();
    fireSelectionChanged(null);
    return true;
  }

  @Override
  public void signalInfoNameChanged(SignalInfo s) {
    fireSelectionChanged(null);
  }

  @Override
  public void signalInfoObsoleted(SignalInfo s) {
    if (s == clockSource) {
      clockSource.setListener(null); // redundant if info contains s
      clockSource = null;
      if (mode >= CLOCKED)
        setStepMode(isFine(), timeScale, gateDelay);
    }
    remove(s);
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
      item.setListener(null);
    }
    if (count > 0) {
      if (spotlight != null && items.contains(spotlight))
        spotlight = null;
      renumberSignals();
      fireSelectionChanged(null);
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
    renumberSignals();
    fireSelectionChanged(null);
  }

  public void remove(int idx) {
    if (spotlight != null && signals.get(idx) == spotlight)
      spotlight = null;
    info.remove(idx).setListener(null);
    signals.remove(idx);
    renumberSignals();
    fireSelectionChanged(null);
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

  public boolean isStepMode() { return mode == STEP; }
  public boolean isRealMode() { return mode == REAL; }
  public boolean isClockMode() { return mode >= CLOCKED; }
  public int getClockDiscipline() { return mode >= CLOCKED ? mode : 0; }
  public boolean isFine() { return granularity == FINE; }
  public boolean isCoarse() { return granularity != FINE; }

  private static final Value HI = Value.TRUE;
  private static final Value LO = Value.FALSE;

  private boolean captureContinuous() {
    return isFine()
      || (mode == CLOCK_HIGH && curClockVal.equals(HI))
      || (mode == CLOCK_LOW && curClockVal.equals(LO));
  }

  public int getHistoryLimit() {
    return historyLimit;
  }
  public void setHistoryLimit(int limit) {
    if (historyLimit == limit)
      return;
    historyLimit = limit;
    for (Signal s : signals)
      s.resize(historyLimit);
    fireHistoryLimitChanged(null);
  }

  public void setStepMode(boolean fine, long t, long d) {
    int g = fine ? FINE : COARSE;
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

  public void setClockMode(boolean fine, int discipline, long t, long d) {
    int g = fine ? FINE : COARSE;
    if (clockSource != null && mode == discipline && granularity == g && timeScale == t && gateDelay == d)
      return;
    if (clockSource == null) {
      Circuit circ = circuitState.getCircuit();
      Component tmpClk = circuitState.getTemporaryClock();
      // select a clock source now
      ArrayList<SignalInfo> clocks = ComponentSelector.findClocks(circ);
      if (clocks != null && clocks.size() == 1) {
        // If one clock is present, just use that.
        clockSource = clocks.get(0);
      } else if (clocks != null && clocks.size() > 1) {
        // If multiple are present, ask user to select
        clockSource = ClockSource.doClockMultipleObserverDialog(circ);
      } else if (tmpClk != null) {
        // No clocks, but user already chose a temporary clock.
        clockSource = new SignalInfo(circ, new Component[] { tmpClk }, null);
      } else if (clocks != null) {
        // No actual or temporary, but other suitable things, ask user to select
        clockSource = ClockSource.doClockMissingObserverDialog(circ);
      }
      if (clockSource == null) {
        // go back to current mode
        setMode(mode, granularity);
        return;
      }
      if ((clockSource.getComponent().getFactory() instanceof Pin)
          && (clockSource.getDepth() == 1))
        circuitState.setTemporaryClock(clockSource.getComponent());
      // Add the clock as a courtesy, even though this is not required.
      if (!info.contains(clockSource)) {
        info.add(0, clockSource); // put it at the top of the list
        signals.add(0,
            new Signal(0, clockSource, clockSource.fetchValue(circuitState),
              1, tEnd - 1, historyLimit));
        clockSource.setListener(this);
        fireSelectionChanged(null);
      }
    }
    timeScale = t;
    gateDelay = d;
    setMode(discipline, g);
  }

  private void setMode(int m, int g) {
    mode = m;
    granularity = g;
    simulatorReset();
    fireSignalsExtended(null); // reset, not extended, but works fine for now
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
        SignalInfo item = makeIfDefaultComponent(comp);
        if (item == null)
          continue;
        addAndInitialize(item, true);
      }
    }
  }

  private SignalInfo makeIfDefaultComponent(Component comp) {
    if (comp.getFactory() instanceof SubcircuitFactory) return null;
    Loggable log = (Loggable)comp.getFeature(Loggable.class);
    if (log == null) return null;
    Object[] opts = log.getLogOptions();
    if (opts != null && opts.length > 0) return null;
    Component[] path = new Component[] { comp };
    return new SignalInfo(circuitState.getCircuit(), path, null);
  }

  private Signal addAndInitialize(SignalInfo item, boolean fireUpdate) {
    int idx = info.indexOf(item);
    if (idx >= 0)
      return signals.get(idx);
    idx = info.size();
    info.add(item);
    Signal s = new Signal(idx, item, item.fetchValue(circuitState),
        1, tEnd - 1, historyLimit);
    signals.add(idx, s);
    item.setListener(this);
    if (fireUpdate)
      fireSelectionChanged(null);
    return s;
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

  public Circuit getCircuit() {
    return circuitState == null ? null : circuitState.getCircuit();
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

  public long getStartTime() {
    // if any signals are full (due to history limit), don't show
    // earlier data (it looks funny in histogram and table)
    long t = 0;
    for (Signal s: signals)
      t = Math.max(t, s.omittedDataTime());
    return t;
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


  private void extendWithOldValues(long duration) {
    for (Signal s : signals) {
      Value v = s.info.fetchValue(circuitState);
      s.extend(duration);
    }    
    elapsedSinceTrigger += duration;
    tEnd += duration;
    fireSignalsExtended(null);
  }

  private void extendWithNewValues(long duration) {
    for (Signal s : signals) {
      Value v = s.info.fetchValue(circuitState);
      s.extend(v, duration);
    }
    elapsedSinceTrigger += duration;
    tEnd += duration;
    fireSignalsExtended(null);
  }

  private void replaceWithNewValues(long duration) {
    for (Signal s : signals) {
      Value v = s.info.fetchValue(circuitState);
      s.replaceRecent(v, duration);
    }
    fireSignalsExtended(null); // changed, not extended, but works fine for now
  }

  public void propagationCompleted(boolean ticked, boolean stepped, boolean propagated) {
    if (!stepped && !propagated) {
      // No signals have changed. This was a nudge that resulted in no signal
      // changes, or a tick in single-step mode that hasn't yet propagated
      // anywhere. There is nothing to record.
      return;
    }
    if (isCoarse() && !propagated) {
      // This is a transient fluctuation that can be entirely ignored.
      return;
    }
    if (mode == STEP)
      updateSignalsStepMode(propagated);
    else if (mode == REAL)
      updateSignalsRealMode();
    else if (mode >= CLOCKED)
      updateSignalsClockMode();
  }

  private void updateSignalsStepMode(boolean stable) {
    long duration = stable ? timeScale : gateDelay;
    extendWithNewValues(duration);
  }

  private void updateSignalsRealMode() {
    long now = System.nanoTime();
    double duration = (now - lastRealtimeUpdate) * (double)timeScale / 1000000000;
    extendWithNewValues(Math.max((long)duration, 1));
    lastRealtimeUpdate = now;
  }

  private void updateSignalsClockMode() {
    // We ignore the simulator's notion of ticked, relying instead on looking
    // at specific transitions or levels of the chosen clockSource.
    Value v = clockSource.fetchValue(circuitState);
    ClockSource.CycleInfo cc = ClockSource.getCycleInfo(clockSource);
    if ((mode == CLOCK_HIGH && v.equals(HI)) || (mode == CLOCK_LOW && v.equals(LO))) {
      // Active level-senstive clock, either fine or coarse. Finish out
      // previous stable period, then start a new active period counting as
      // gate-delay.
      long activeDuration = (mode == CLOCK_HIGH ? cc.hi : cc.lo) * timeScale;
      long stableDuration = (mode == CLOCK_HIGH ? cc.lo : cc.hi) * timeScale;
      if (!v.equals(curClockVal)) {
        if (elapsedSinceTrigger < activeDuration) {
          extendWithOldValues(stableDuration - elapsedSinceTrigger);
        }
        elapsedSinceTrigger = 0;
        curClockVal = v;
      }
      long duration = gateDelay;
      extendWithNewValues(duration);
    } else if (mode == CLOCK_HIGH || mode == CLOCK_LOW) {
      // Inactive level-sensitive clock, either fine or coarse.
      long activeDuration = (mode == CLOCK_HIGH ? cc.hi : cc.lo) * timeScale;
      long stableDuration = (mode == CLOCK_HIGH ? cc.lo : cc.hi) * timeScale;
      if (!v.equals(curClockVal)) {
        // just went inactive, so finish out the active period
        // then start a new stable period
        if (elapsedSinceTrigger < activeDuration)
          extendWithOldValues(activeDuration - elapsedSinceTrigger);
        elapsedSinceTrigger = 0;
        curClockVal = v;
        long duration = isFine() ? gateDelay : stableDuration;
        extendWithNewValues(duration);
      } else if (isCoarse()) {
        // back-date transient changes to the start of current stable period
        replaceWithNewValues(stableDuration);
      } else {
        // fine-grained, but still inactive
        long duration = gateDelay;
        extendWithNewValues(duration);
      }
    } else {
      // Edge-triggered clock, fine or coarse.
      int ticks = (mode == CLOCK_DUAL)
          ? (v.equals(Value.FALSE) ? cc.lo : cc.hi)
          : cc.ticks;
      int prevTicks = (mode == CLOCK_DUAL)
          ? (v.equals(Value.FALSE) ? cc.hi : cc.lo)
          : cc.ticks;
      long stableDuration = timeScale * ticks;
      long prevDuration = timeScale * prevTicks;
      long duration = isFine() ? gateDelay : stableDuration;
      boolean triggered = (mode == CLOCK_DUAL && !v.equals(curClockVal))
          || (mode == CLOCK_RISING && v.equals(HI) && !curClockVal.equals(HI))
          || (mode == CLOCK_FALLING && v.equals(LO) && !curClockVal.equals(LO));
      curClockVal = v;
      if (triggered) {
        // Finish out previous stable period, then start a new one.
        if (isFine() && elapsedSinceTrigger < prevDuration)
          extendWithOldValues(prevDuration - elapsedSinceTrigger);
        elapsedSinceTrigger = 0;
        extendWithNewValues(duration);
      } else if (isCoarse()) {
        // back-date transient changes to the start of current stable period
        replaceWithNewValues(stableDuration);
      } else {
        // fine-grained, transient changes
        extendWithNewValues(duration);
      }
    }
  }

  public void simulatorReset() {
    long duration;
    if (mode >= CLOCKED) {
      curClockVal = clockSource.fetchValue(circuitState);
      ClockSource.CycleInfo cc = ClockSource.getCycleInfo(clockSource);
      if (captureContinuous()) { // fine-grained, or active level-sensitive clock
          duration = gateDelay;
      } else if (mode == CLOCK_HIGH || mode == CLOCK_LOW) { // inactive level-sensitive
        long activeDuration = (mode == CLOCK_HIGH ? cc.hi : cc.lo) * timeScale;
        long stableDuration = (mode == CLOCK_HIGH ? cc.lo : cc.hi) * timeScale;
        duration = isFine() ? gateDelay : stableDuration;
      } else { // edge-triggered clock, fine or coarse
        int ticks = (mode == CLOCK_DUAL)
            ? (curClockVal.equals(Value.FALSE) ? cc.lo : cc.hi)
            : cc.ticks;
        long stableDuration = timeScale * ticks;
        duration = isFine() ? gateDelay : stableDuration;
      }
    } else if (mode == STEP) {
      duration = timeScale;
    } else { // mode == REAL
      duration = gateDelay;
    }
    if (mode == REAL)
      lastRealtimeUpdate = System.nanoTime();
    elapsedSinceTrigger = 0;
    for (Signal s: signals) {
      Value v = s.info.fetchValue(circuitState);
      s.reset(v, duration);
    }
    elapsedSinceTrigger += duration;
    tEnd = duration;
  }

  public void setFile(File value) {
    if (Objects.equals(file, value)) return;
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

  public SignalInfo getClockSourceInfo() {
    return clockSource;
  }

  public void setClockSourceInfo(SignalInfo item) {
    if (clockSource == item)
      return;
    clockSource = item;
    fireModeChanged(null);
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

  public void setRadix(SignalInfo s, RadixOption value) {
    if (s.setRadix(value))
      fireSelectionChanged(null);
  }

  public void checkForClocks() {
    Component clk = circuitState.getTemporaryClock();
    if (clk != null && (clockSource == null || clockSource.getComponent() != clk)) {
      // User requested manual tick, there were no actual or temporary clocks,
      // so user selected a temporary clock, so we should now use it.
      if (clockSource != null && !info.contains(clockSource))
        clockSource.setListener(null);
      clockSource = null;
      setStepMode(isFine(), timeScale, gateDelay); // serves as fallback
      setClockMode(isFine(), CLOCK_DUAL, timeScale, gateDelay);
    }
  }
}
