/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.ComponentDataGuiProvider;
import com.cburch.logisim.circuit.Simulator;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Locale;
import java.util.function.LongSupplier;
import javax.swing.Timer;

/**
 * TTL 74x123: dual retriggerable monostable multivibrator with reset.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT123.pdf">74HC123 datasheet</a>.
 * Each half follows the datasheet function table. External timing pins are not logic nets: the
 * simulator has no analog RC model. Pulse width uses the 5 V formula {@code tW = 0.45 × Rext(kΩ) ×
 * Cext(pF)} nanoseconds, for Cext above 10 nF. That width is remembered when the pulse starts and
 * elapses while the simulation is running. It does not depend on the tick frequency. Stopping the
 * simulation freezes the time that remains. A Swing timer wakes the simulator; its resolution is
 * about 1 ms, so a shorter pulse ends on that wake-up. There is no HDL model.
 */
public class Ttl74123 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74123";

  public static final int PORT_INDEX_1A = 0;
  public static final int PORT_INDEX_1B = 1;
  public static final int PORT_INDEX_1RD = 2;
  public static final int PORT_INDEX_1QBAR = 3;
  public static final int PORT_INDEX_2Q = 4;
  public static final int PORT_INDEX_2A = 5;
  public static final int PORT_INDEX_2B = 6;
  public static final int PORT_INDEX_2RD = 7;
  public static final int PORT_INDEX_2QBAR = 8;
  public static final int PORT_INDEX_1Q = 9;

  /** Timing factor from the datasheet: tW(ns) = K × Rext(kΩ) × Cext(pF), K = 0.45 at 5 V. */
  public static final double PULSE_WIDTH_FACTOR_SECONDS = 0.45e-9;

  /** External resistor for section 1. Typical choices are 10 kΩ, 100 kΩ, and 1 MΩ. */
  public static final Ttl74123RcAttribute REXT_1 =
      Ttl74123RcAttribute.resistor("1Rext", S.getter("ttl74123Rext1"));
  /** External capacitor for section 1. Typical choices run from 10 nF to 1000 µF. */
  public static final Ttl74123RcAttribute CEXT_1 =
      Ttl74123RcAttribute.capacitor("1Cext", S.getter("ttl74123Cext1"));
  /** External resistor for section 2. Typical choices are 10 kΩ, 100 kΩ, and 1 MΩ. */
  public static final Ttl74123RcAttribute REXT_2 =
      Ttl74123RcAttribute.resistor("2Rext", S.getter("ttl74123Rext2"));
  /** External capacitor for section 2. Typical choices run from 10 nF to 1000 µF. */
  public static final Ttl74123RcAttribute CEXT_2 =
      Ttl74123RcAttribute.capacitor("2Cext", S.getter("ttl74123Cext2"));

  /** Clock used to measure a pulse. Tests replace it; the simulator uses {@link System#nanoTime}. */
  private static LongSupplier clock = System::nanoTime;

  private static final int DELAY = 4;
  private static final int HALVES = 2;
  private static final int DEFAULT_REXT_KOHM = 10;
  private static final int DEFAULT_CEXT_PF = 100_000;
  private static final byte[] OUTPUT_PORTS = {4, 5, 12, 13};
  private static final byte[] UNUSED_PINS = {6, 7, 14, 15};
  private static final String[] PORT_NAMES = {
    "1A (negative-edge trigger)",
    "1B (positive-edge trigger)",
    "1RD (direct reset, active low)",
    "1Q\\ (active low)",
    "2Q",
    "2A (negative-edge trigger)",
    "2B (positive-edge trigger)",
    "2RD (direct reset, active low)",
    "2Q\\ (active low)",
    "1Q"
  };
  private static final int[] A_PORTS = {PORT_INDEX_1A, PORT_INDEX_2A};
  private static final int[] B_PORTS = {PORT_INDEX_1B, PORT_INDEX_2B};
  private static final int[] RD_PORTS = {PORT_INDEX_1RD, PORT_INDEX_2RD};
  private static final int[] Q_PORTS = {PORT_INDEX_1Q, PORT_INDEX_2Q};
  private static final int[] QBAR_PORTS = {PORT_INDEX_1QBAR, PORT_INDEX_2QBAR};

  /** Creates a 74123 dual retriggerable monostable. */
  public Ttl74123() {
    super(_ID, (byte) 16, OUTPUT_PORTS, UNUSED_PINS, PORT_NAMES, null);
    setAttributes(
        new Attribute[] {
          StdAttr.FACING,
          TtlLibrary.VCC_GND,
          TtlLibrary.DRAW_INTERNAL_STRUCTURE,
          StdAttr.LABEL,
          REXT_1,
          CEXT_1,
          REXT_2,
          CEXT_2
        },
        new Object[] {
          Direction.EAST,
          false,
          false,
          "",
          DEFAULT_REXT_KOHM,
          DEFAULT_CEXT_PF,
          DEFAULT_REXT_KOHM,
          DEFAULT_CEXT_PF
        });
  }

  /**
   * Converts an external RC pair into a pulse length in nanoseconds.
   *
   * @param rextKOhm external resistor in kiloohms
   * @param cextPf external capacitor in picofarads
   * @return pulse length in nanoseconds, at least one
   */
  public static long widthNanos(int rextKOhm, int cextPf) {
    final var nanos = Math.round(PULSE_WIDTH_FACTOR_SECONDS * rextKOhm * cextPf * 1_000_000_000.0);
    return nanos < 1 ? 1 : nanos;
  }

  /** Short label for a pulse length, such as {@code 450 µs} or {@code 3 s}. */
  static String formatWidth(long nanos) {
    if (nanos >= 1_000_000_000L) {
      return trim(nanos / 1_000_000_000.0) + " s";
    }
    if (nanos >= 1_000_000L) {
      return trim(nanos / 1_000_000.0) + " ms";
    }
    if (nanos >= 1_000L) {
      return trim(nanos / 1_000.0) + " µs";
    }
    return nanos + " ns";
  }

  /** Replaces the pulse clock. Passing null restores {@link System#nanoTime}. */
  static void setClock(LongSupplier supplier) {
    clock = supplier == null ? System::nanoTime : supplier;
  }

  /**
   * Ends any pulse whose captured width has elapsed at {@code nowNanos}. A frozen pulse, while the
   * simulation is stopped, does not end. Returns true when an output must change.
   */
  boolean expire(InstanceState state, long nowNanos) {
    final var data = (MonostableData) state.getData();
    return data != null && data.expire(nowNanos);
  }

  /** Freezes or continues the pulse with the simulation running flag. */
  void setSimulationRunning(InstanceState state, boolean running) {
    final var data = (MonostableData) state.getData();
    if (data != null) {
      data.setSimulationRunning(running);
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1A", "1B", "1RD", "1nQ", "2Q", "2C", "2RC", null,
          "2A", "2B", "2RD", "2nQ", "1Q", "1C", "1RC", null
        });
    drawTiming(gfx, painter, x, y, height);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    data.attach(state);
    data.expire(clock.getAsLong());
    for (var half = 0; half < HALVES; half++) {
      updateHalf(state, data, half);
    }
    data.schedule();
  }

  private static String trim(double value) {
    final var text = String.format(Locale.ROOT, "%.3f", value);
    var end = text.length();
    while (end > 0 && text.charAt(end - 1) == '0') {
      end--;
    }
    if (end > 0 && text.charAt(end - 1) == '.') {
      end--;
    }
    return text.substring(0, end);
  }

  private void drawTiming(Graphics2D gfx, InstancePainter painter, int x, int y, int height) {
    final var data = (MonostableData) painter.getData();
    gfx.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 9));
    for (var half = 0; half < HALVES; half++) {
      final var label =
          formatWidth(
              widthNanos(
                  painter.getAttributeValue(rextAttribute(half)),
                  painter.getAttributeValue(cextAttribute(half))));
      gfx.setColor(data != null && data.isActive(half) ? Value.TRUE.getColor() : Color.BLACK);
      GraphicsUtil.drawCenteredText(gfx, label, x + 48 + half * 64, y + height / 2 + 8);
    }
    gfx.setColor(Color.BLACK);
  }

  private void updateHalf(InstanceState state, MonostableData data, int half) {
    final var active =
        data.apply(
            half,
            state.getPortValue(A_PORTS[half]),
            state.getPortValue(B_PORTS[half]),
            state.getPortValue(RD_PORTS[half]),
            widthNanos(
                state.getAttributeValue(rextAttribute(half)),
                state.getAttributeValue(cextAttribute(half))));
    state.setPort(Q_PORTS[half], active ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(QBAR_PORTS[half], active ? Value.FALSE : Value.TRUE, DELAY);
  }

  /**
   * Datasheet edges: rising B while A is low, falling A while B is high, or rising reset while A
   * is low and B is high. Only a fully defined 0/1 transition counts. An undefined reset neither
   * clears nor triggers, so a pulse already in progress continues.
   */
  private static boolean triggered(Half section, Value inputA, Value inputB, Value reset) {
    final var risingB =
        section.inputB == Value.FALSE && inputB == Value.TRUE && inputA == Value.FALSE;
    final var fallingA =
        section.inputA == Value.TRUE && inputA == Value.FALSE && inputB == Value.TRUE;
    final var risingReset =
        section.reset == Value.FALSE
            && reset == Value.TRUE
            && inputA == Value.FALSE
            && inputB == Value.TRUE;
    return risingB || fallingA || risingReset;
  }

  private static Attribute<Integer> rextAttribute(int half) {
    return half == 0 ? REXT_1 : REXT_2;
  }

  private static Attribute<Integer> cextAttribute(int half) {
    return half == 0 ? CEXT_1 : CEXT_2;
  }

  private static Project projectOf(InstanceState state) {
    if (state instanceof InstancePainter painter) {
      final var circuitState = painter.getCircuitState();
      return circuitState == null ? null : circuitState.getProject();
    }
    return state.getProject();
  }

  private static MonostableData getStateData(InstanceState state) {
    var data = (MonostableData) state.getData();
    if (data == null) {
      data = new MonostableData();
      state.setData(data);
    }
    return data;
  }

  /** Swing timers cannot wait less than this. A shorter pulse ends on the next wake-up. */
  private static int delayMillis(long deadline, long now) {
    final var remaining = deadline - now;
    if (remaining < 1_000_000L) {
      return 1;
    }
    final var millis = (remaining + 500_000L) / 1_000_000L;
    return millis > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) millis;
  }

  private static final class Half {
    private Value inputA = Value.UNKNOWN;
    private Value inputB = Value.UNKNOWN;
    private Value reset = Value.UNKNOWN;
    private boolean active;
    private boolean frozen;
    private long endsAt;
    private long remaining = 1;

    private Half copy() {
      final var copy = new Half();
      copy.inputA = inputA;
      copy.inputB = inputB;
      copy.reset = reset;
      copy.active = active;
      copy.frozen = frozen;
      copy.endsAt = endsAt;
      copy.remaining = remaining;
      return copy;
    }
  }

  private static final class MonostableData
      implements InstanceData, ComponentDataGuiProvider, Simulator.StatusListener, ActionListener {
    private final Half[] halves = {new Half(), new Half()};
    private Instance instance;
    private Simulator simulator;
    private Timer timer;
    private boolean listening;
    private boolean destroyed;
    private boolean simulationRunning = true;

    @Override
    public MonostableData clone() {
      final var copy = new MonostableData();
      synchronized (this) {
        for (var half = 0; half < HALVES; half++) {
          copy.halves[half] = halves[half].copy();
        }
        copy.simulationRunning = simulationRunning;
      }
      return copy;
    }

    @Override
    public void destroy() {
      final Simulator watched;
      synchronized (this) {
        if (destroyed) {
          return;
        }
        destroyed = true;
        stopTimer();
        watched = listening ? simulator : null;
        listening = false;
      }
      if (watched != null) {
        watched.removeSimulatorListener(this);
      }
    }

    @Override
    public void simulatorReset(Simulator.Event event) {
      destroy();
    }

    @Override
    public void simulatorStateChanged(Simulator.Event event) {
      setSimulationRunning(event.getSource().isAutoPropagating());
    }

    @Override
    public void actionPerformed(ActionEvent event) {
      final boolean dirty;
      synchronized (this) {
        if (destroyed || !simulationRunning || event.getSource() != timer) {
          return;
        }
        dirty = expireLocked(clock.getAsLong());
      }
      if (dirty) {
        wake();
      }
      schedule();
    }

    private void attach(InstanceState state) {
      final var current = state.getInstance();
      synchronized (this) {
        if (current != null) {
          instance = current;
        }
        if (listening || destroyed) {
          return;
        }
      }
      final var project = projectOf(state);
      final var sim = project == null ? null : project.getSimulator();
      if (sim == null) {
        return;
      }
      synchronized (this) {
        if (listening || destroyed) {
          return;
        }
        simulator = sim;
        listening = true;
      }
      sim.addSimulatorListener(this);
      final boolean removed;
      synchronized (this) {
        removed = destroyed;
        if (removed) {
          listening = false;
        }
      }
      if (removed) {
        sim.removeSimulatorListener(this);
        return;
      }
      adoptRunning(sim.isAutoPropagating());
    }

    private synchronized boolean apply(
        int half, Value inputA, Value inputB, Value reset, long width) {
      final var section = halves[half];
      if (reset == Value.FALSE) {
        section.active = false;
        section.frozen = false;
      } else if (reset == Value.TRUE && triggered(section, inputA, inputB, reset)) {
        start(section, width);
      }
      section.inputA = inputA;
      section.inputB = inputB;
      section.reset = reset;
      return section.active;
    }

    private synchronized boolean isActive(int half) {
      return halves[half].active;
    }

    private synchronized boolean expire(long now) {
      return expireLocked(now);
    }

    private void setSimulationRunning(boolean running) {
      if (adoptRunning(running)) {
        schedule();
      }
    }

    private void schedule() {
      synchronized (this) {
        if (simulator == null || destroyed || !simulationRunning) {
          stopTimer();
          return;
        }
        final var soonest = soonestDeadline();
        if (soonest == Long.MAX_VALUE) {
          stopTimer();
          return;
        }
        final var delay = delayMillis(soonest, clock.getAsLong());
        if (timer == null) {
          timer = new Timer(delay, this);
          timer.setRepeats(false);
        } else {
          timer.stop();
          timer.setInitialDelay(delay);
        }
        timer.start();
      }
    }

    private boolean adoptRunning(boolean running) {
      synchronized (this) {
        if (destroyed || running == simulationRunning) {
          return false;
        }
        simulationRunning = running;
        final var now = clock.getAsLong();
        if (running) {
          resume(now);
        } else {
          freeze(now);
          stopTimer();
        }
        return true;
      }
    }

    private void start(Half section, long width) {
      section.active = true;
      section.remaining = width;
      if (simulationRunning) {
        section.frozen = false;
        section.endsAt = clock.getAsLong() + width;
      } else {
        section.frozen = true;
      }
    }

    private boolean expireLocked(long now) {
      var dirty = false;
      for (final var half : halves) {
        if (!half.active || half.frozen) {
          continue;
        }
        if (now >= half.endsAt) {
          half.active = false;
          dirty = true;
        }
      }
      return dirty;
    }

    private void freeze(long now) {
      for (final var half : halves) {
        if (!half.active || half.frozen) {
          continue;
        }
        final var left = half.endsAt - now;
        half.remaining = left > 0 ? left : 0;
        half.frozen = true;
      }
    }

    private void resume(long now) {
      for (final var half : halves) {
        if (!half.active || !half.frozen) {
          continue;
        }
        half.endsAt = now + half.remaining;
        half.frozen = false;
      }
    }

    private long soonestDeadline() {
      var soonest = Long.MAX_VALUE;
      for (final var half : halves) {
        if (half.active && !half.frozen && half.endsAt < soonest) {
          soonest = half.endsAt;
        }
      }
      return soonest;
    }

    private void stopTimer() {
      if (timer != null) {
        timer.stop();
      }
    }

    private void wake() {
      if (instance != null) {
        instance.fireInvalidated();
      }
      if (simulator != null) {
        simulator.nudge();
      }
    }
  }
}
