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

import com.cburch.logisim.util.UniquelyNamedThread;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.HashMap;

class LogThread extends UniquelyNamedThread implements Model.Listener {
  // file will be flushed with at least this frequency (ms)
  private static final int FLUSH_FREQUENCY = 500;

  // file will be closed after waiting this long between writes (ms)
  private static final int IDLE_UNTIL_CLOSE = 10000;

  private final Model model;
  private final Object lock = new Object();
  private boolean canceled = false;
  private PrintWriter writer = null;
  private boolean modeDirty = true;
  private boolean headerDirty = true;
  private long lastWrite = 0;
  private long timeNextWrite = 0; // done writing up to this time, exclusive
  private final HashMap<Signal, Signal.Iterator> cursors = new HashMap<>();

  public LogThread(Model model) {
    super("LogThread");
    this.model = model;
    model.addModelListener(this);
  }

  // precondition: lock held and writing()==true
  private void writeSignals() {
    if (writer == null) {
      try {
        writer = new PrintWriter(new FileWriter(model.getFile(), true));
      } catch (IOException e) {
        model.setFile(null);
        return;
      }
    }
    if (modeDirty) {
      var mode = model.isStepMode() ? "step"
          : model.isRealMode() ? "real-time"
          : "clocked";
      var gran = model.isFine() ? "fine" : "coarse";
      writer.println("# mode: " + mode + " granularity: " + gran);
      modeDirty = false;
    }
    if (headerDirty) {
      if (model.getFileHeader()) {
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < model.getSignalCount(); i++) {
          if (i > 0) buf.append("\t");
          buf.append(model.getItem(i).getDisplayName());
        }
        writer.println(buf);
      }
      headerDirty = false;
    }
    Signal.Iterator[] cur = new Signal.Iterator[model.getSignalCount()];
    for (int i = 0; i < model.getSignalCount(); i++) {
      Signal s = model.getSignal(i);
      cur[i] = cursors.get(s);
      if (cur[i] == null) {
        cur[i] = s.new Iterator(timeNextWrite);
        cursors.put(s, cur[i]);
      }
    }
    long timeStop = model.getEndTime();
    while (timeNextWrite < timeStop) {
      long duration = timeStop - timeNextWrite;
      StringBuilder buf = new StringBuilder();
      for (int i = 0; i < cur.length; i++) {
        if (i > 0)
          buf.append("\t");
        buf.append(cur[i].getFormattedValue());
        if (cur[i].duration < duration)
          duration = cur[i].duration;
      }
      // todo: only write duration if not in coarse-step or coarse-clock mode?
      writer.println(buf + "\t# " + Model.formatDuration(duration));
      for (Signal.Iterator c : cur)
        c.advance(duration);
      timeNextWrite += duration;
    }
    lastWrite = System.currentTimeMillis();
  }

  public void cancel() {
    synchronized (lock) {
      canceled = true;
      if (writer != null) {
        writer.close();
        writer = null;
      }
    }
  }

  @Override
  public void signalsReset(Model.Event event) {
    synchronized (lock) {
      if (writing()) {
        timeNextWrite = 0;
        cursors.clear();
        writeSignals();
      }
    }
  }

  @Override
  public void signalsExtended(Model.Event event) {
    synchronized (lock) {
      if (writing()) writeSignals();
    }
  }

  @Override
  public void filePropertyChanged(Model.Event event) {
    synchronized (lock) {
      if (writing()) {
        if (writer == null) writeSignals();
      } else {
        if (writer != null) {
          writer.close();
          writer = null;
        }
      }
    }
  }

  private boolean writing() {
    return !canceled && model.isSelected() && model.isFileEnabled() && model.getFile() != null;
  }

  @Override
  public void run() {
    while (!canceled) {
      synchronized (lock) {
        if (writer != null) {
          if (System.currentTimeMillis() - lastWrite > IDLE_UNTIL_CLOSE) {
            writer.close();
            writer = null;
          } else {
            writer.flush();
          }
        }
      }
      try {
        Thread.sleep(FLUSH_FREQUENCY);
      } catch (InterruptedException ignored) {
      }
    }
    synchronized (lock) {
      if (writer != null) {
        writer.close();
        writer = null;
      }
    }
  }

  @Override
  public void selectionChanged(Model.Event event) {
    synchronized (lock) {
      cursors.keySet().retainAll(model.getSignals()); // removes dead cursors
      headerDirty = true;
    }
  }

  @Override
  public void modeChanged(Model.Event event) {
    synchronized (lock) {
      modeDirty = true;
    }
  }

  @Override
  public void historyLimitChanged(Model.Event event) {}
}
