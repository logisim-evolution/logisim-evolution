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

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.TickAware;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * TTL 74x123: dual retriggerable monostable multivibrator with reset.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT123.pdf">74HC123 datasheet</a>.
 * Each half follows the datasheet function table. External timing pins are not logic nets: the
 * simulator has no analog RC model. Pulse width uses the 5 V formula {@code tW = 0.45 × Rext(kΩ) ×
 * Cext(pF)} nanoseconds, for Cext above 10 nF, and is counted in simulator ticks: {@code max(1,
 * round(tW × tickFrequency))}. The width is stored on the instance when the pulse starts. A short
 * pulse at a low tick rate therefore lasts one tick. There is no HDL model.
 */
public class Ttl74123 extends AbstractTtlGate implements TickAware {
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
   * Converts an external RC pair into a pulse length in simulator ticks.
   *
   * @param rextKOhm external resistor in kiloohms
   * @param cextPf external capacitor in picofarads
   * @param tickFrequencyHz simulator tick frequency in hertz
   * @return pulse length in ticks, at least one
   */
  public static int widthTicks(int rextKOhm, int cextPf, double tickFrequencyHz) {
    if (tickFrequencyHz <= 0) {
      return 1;
    }
    final var ticks =
        Math.round(PULSE_WIDTH_FACTOR_SECONDS * rextKOhm * cextPf * tickFrequencyHz);
    if (ticks < 1) {
      return 1;
    }
    if (ticks > Integer.MAX_VALUE) {
      return Integer.MAX_VALUE;
    }
    return (int) ticks;
  }

  /**
   * Ends any pulse whose captured width has elapsed. Returns true when an output must change.
   * {@link #tick} performs the same update for a placed component.
   */
  boolean expire(InstanceState state, int ticks) {
    final var data = (MonostableData) state.getData();
    return data != null && data.expire(ticks);
  }

  @Override
  public boolean tick(CircuitState state, int ticks, Component comp) {
    final var data = (MonostableData) state.getData(comp);
    return data != null && data.expire(ticks);
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
    for (var half = 0; half < HALVES; half++) {
      updateHalf(state, data, half);
    }
  }

  private void drawTiming(Graphics2D gfx, InstancePainter painter, int x, int y, int height) {
    final var data = (MonostableData) painter.getData();
    final var frequency = tickFrequencyHz(painter);
    gfx.setFont(new Font(Font.DIALOG_INPUT, Font.BOLD, 9));
    for (var half = 0; half < HALVES; half++) {
      final var ticks =
          widthTicks(
              painter.getAttributeValue(rextAttribute(half)),
              painter.getAttributeValue(cextAttribute(half)),
              frequency);
      final var active = data != null && data.halves[half].active;
      gfx.setColor(active ? Value.TRUE.getColor() : Color.BLACK);
      GraphicsUtil.drawCenteredText(gfx, ticks + "t", x + 48 + half * 64, y + height / 2 + 8);
    }
    gfx.setColor(Color.BLACK);
  }

  private void updateHalf(InstanceState state, MonostableData data, int half) {
    final var section = data.halves[half];
    final var inputA = state.getPortValue(A_PORTS[half]);
    final var inputB = state.getPortValue(B_PORTS[half]);
    final var reset = state.getPortValue(RD_PORTS[half]);
    if (reset == Value.FALSE) {
      section.active = false;
    } else if (reset == Value.TRUE && triggered(section, inputA, inputB, reset)) {
      section.active = true;
      section.startedAt = state.getTickCount();
      section.widthTicks =
          widthTicks(
              state.getAttributeValue(rextAttribute(half)),
              state.getAttributeValue(cextAttribute(half)),
              tickFrequencyHz(state));
    }
    section.inputA = inputA;
    section.inputB = inputB;
    section.reset = reset;
    state.setPort(Q_PORTS[half], section.active ? Value.TRUE : Value.FALSE, DELAY);
    state.setPort(QBAR_PORTS[half], section.active ? Value.FALSE : Value.TRUE, DELAY);
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

  private double tickFrequencyHz(InstanceState state) {
    final var project = projectOf(state);
    if (project != null && project.getSimulator() != null) {
      final var frequency = project.getSimulator().getTickFrequency();
      if (frequency > 0) {
        return frequency;
      }
    }
    return 1.0;
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

  private static final class Half {
    private Value inputA = Value.UNKNOWN;
    private Value inputB = Value.UNKNOWN;
    private Value reset = Value.UNKNOWN;
    private boolean active;
    private int startedAt;
    private int widthTicks = 1;

    private Half copy() {
      final var copy = new Half();
      copy.inputA = inputA;
      copy.inputB = inputB;
      copy.reset = reset;
      copy.active = active;
      copy.startedAt = startedAt;
      copy.widthTicks = widthTicks;
      return copy;
    }
  }

  private static final class MonostableData implements InstanceData {
    private final Half[] halves = {new Half(), new Half()};

    @Override
    public MonostableData clone() {
      final var copy = new MonostableData();
      for (var half = 0; half < HALVES; half++) {
        copy.halves[half] = halves[half].copy();
      }
      return copy;
    }

    private boolean expire(int ticks) {
      var dirty = false;
      for (final var half : halves) {
        if (!half.active) {
          continue;
        }
        final var elapsed = (long) ticks - half.startedAt;
        if (elapsed >= half.widthTicks) {
          half.active = false;
          dirty = true;
        }
      }
      return dirty;
    }
  }
}
