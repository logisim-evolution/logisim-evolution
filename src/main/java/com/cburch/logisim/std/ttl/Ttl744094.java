/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstanceData;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import java.util.Arrays;

/**
 * TTL 74x4094: 8-bit shift-and-store register with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc4094.pdf">CD74HC4094</a> and Nexperia 74HC4094
 * data sheets. A rising {@code CP} shifts {@code D} into {@code QP0} and toward {@code QP7}.
 * {@code QS1} is that last shift-register stage. A falling {@code CP} copies {@code QS1} into
 * {@code QS2}, so the cascading output stays stable while the next package samples {@code CP}.
 * {@code STR} is a level: while it is high the storage register follows the shift register, and a
 * low {@code STR} holds storage. {@code OE} high drives storage onto {@code QP0}..{@code QP7};
 * any other level releases those pins. {@code QS1} and {@code QS2} stay driven. TI names the same
 * pins {@code STROBE}, {@code DATA} and {@code Q0}..{@code Q7}. On the package, {@code QP4}
 * to {@code QP7} are pins 14, 13, 12 and 11. Nanosecond delays are not modeled.
 */
public class Ttl744094 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "744094";

  public static final int DELAY = 1;
  private static final int STAGES = 8;

  public static final byte STR = 1;
  public static final byte D = 2;
  public static final byte CP = 3;
  public static final byte QP0 = 4;
  public static final byte QP1 = 5;
  public static final byte QP2 = 6;
  public static final byte QP3 = 7;
  public static final byte GND = 8;
  public static final byte QS1 = 9;
  public static final byte QS2 = 10;
  public static final byte QP7 = 11;
  public static final byte QP6 = 12;
  public static final byte QP5 = 13;
  public static final byte QP4 = 14;
  public static final byte OE = 15;
  public static final byte VCC = 16;

  private static final byte[] PARALLEL = {QP0, QP1, QP2, QP3, QP4, QP5, QP6, QP7};
  private static final byte[] OUTPUT_PINS = {QP0, QP1, QP2, QP3, QS1, QS2, QP4, QP5, QP6, QP7};
  private static final String[] PORT_NAMES = {
    "STR (Strobe)",
    "D (Data)",
    "CP (Clock)",
    "QP0",
    "QP1",
    "QP2",
    "QP3",
    "QS1",
    "QS2",
    "QP7",
    "QP6",
    "QP5",
    "QP4",
    "OE (Output enable, active high)"
  };
  private static final String[] PIN_NAMES = {
    "STR", "D", "CP", "QP0", "QP1", "QP2", "QP3", null,
    "QS1", "QS2", "QP7", "QP6", "QP5", "QP4", "OE", null
  };

  /** Creates a 744094 shift-and-store register with three-state parallel outputs. */
  public Ttl744094() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl744094HdlGenerator());
  }

  /**
   * Converts a 1-based datasheet pin number to a 0-based Logisim port index.
   *
   * <p>Power pins are omitted from the port list.
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  static byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  private static State getData(InstanceState state) {
    var data = (State) state.getData();
    if (data == null) {
      data = new State();
      state.setData(data);
    }
    return data;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    g.drawRect((x + 14), (y + height - 28), 12, 12);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 20), (y + height - 13));
    g.drawLine((x + 20), (y + height - 13), (x + 20), ((y + height - 28)) + 12);
    g.drawLine((x + 14), (((y + height - 28)) + 6) - 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 14), (((y + height - 28)) + 6) + 3, ((x + 14)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 30), (y + height - 28), 12, 12);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 36), (y + height - 13));
    g.drawLine((x + 36), (y + height - 13), (x + 36), ((y + height - 28)) + 12);
    g.drawLine((x + 30), (((y + height - 28)) + 6) - 3, ((x + 30)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 30), (((y + height - 28)) + 6) + 3, ((x + 30)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 46), (y + height - 28), 12, 12);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 52), (y + height - 13));
    g.drawLine((x + 52), (y + height - 13), (x + 52), ((y + height - 28)) + 12);
    g.drawLine((x + 46), (((y + height - 28)) + 6) - 3, ((x + 46)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 46), (((y + height - 28)) + 6) + 3, ((x + 46)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 62), (y + height - 28), 12, 12);
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 68), (y + height - 13));
    g.drawLine((x + 68), (y + height - 13), (x + 68), ((y + height - 28)) + 12);
    g.drawLine((x + 62), (((y + height - 28)) + 6) - 3, ((x + 62)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 62), (((y + height - 28)) + 6) + 3, ((x + 62)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 78), (y + height - 28), 12, 12);
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 84), (y + 13));
    g.drawLine((x + 84), (y + 13), (x + 84), (y + height - 28));
    g.drawLine((x + 78), (((y + height - 28)) + 6) - 3, ((x + 78)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 78), (((y + height - 28)) + 6) + 3, ((x + 78)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 94), (y + height - 28), 12, 12);
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 100), (y + 13));
    g.drawLine((x + 100), (y + 13), (x + 100), (y + height - 28));
    g.drawLine((x + 94), (((y + height - 28)) + 6) - 3, ((x + 94)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 94), (((y + height - 28)) + 6) + 3, ((x + 94)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 110), (y + height - 28), 12, 12);
    g.drawLine((x + 90), (y + 7), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 116), (y + 13));
    g.drawLine((x + 116), (y + 13), (x + 116), (y + height - 28));
    g.drawLine((x + 110), (((y + height - 28)) + 6) - 3, ((x + 110)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 110), (((y + height - 28)) + 6) + 3, ((x + 110)) + 4, ((y + height - 28)) + 6);
    g.drawRect((x + 126), (y + height - 28), 12, 12);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 132), (y + 13));
    g.drawLine((x + 132), (y + 13), (x + 132), (y + height - 28));
    g.drawLine((x + 126), (((y + height - 28)) + 6) - 3, ((x + 126)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 126), (((y + height - 28)) + 6) + 3, ((x + 126)) + 4, ((y + height - 28)) + 6);
    g.drawLine((x + 26), ((y + height - 28)) + 6, (x + 26), ((y + height - 28)) + 6);
    g.drawLine((x + 42), ((y + height - 28)) + 6, (x + 42), ((y + height - 28)) + 6);
    g.drawLine((x + 58), ((y + height - 28)) + 6, (x + 58), ((y + height - 28)) + 6);
    g.drawLine((x + 74), ((y + height - 28)) + 6, (x + 74), ((y + height - 28)) + 6);
    g.drawLine((x + 90), ((y + height - 28)) + 6, (x + 90), ((y + height - 28)) + 6);
    g.drawLine((x + 106), ((y + height - 28)) + 6, (x + 106), ((y + height - 28)) + 6);
    g.drawLine((x + 122), ((y + height - 28)) + 6, (x + 122), ((y + height - 28)) + 6);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 9), (y + height - 13));
    g.drawLine((x + 9), (y + height - 13), (x + 9), ((y + height - 28)) + 6);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 62), (y + height - 13));
    g.drawLine((x + 62), (y + height - 13), (x + 62), (y + 20));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var clock = state.getPortValue(pinNrToPortNr(CP));
    final var rising = data.isRising(clock);
    final var falling = data.isFalling(clock);
    data.remember(clock);

    if (rising) {
      data.shiftIn(state.getPortValue(pinNrToPortNr(D)));
    }
    if (falling) {
      data.captureSerial();
    }
    if (state.getPortValue(pinNrToPortNr(STR)) == Value.TRUE) {
      data.followShift();
    }

    final var enabled = state.getPortValue(pinNrToPortNr(OE)) == Value.TRUE;
    for (var stage = 0; stage < STAGES; stage++) {
      final var output = enabled ? data.storage[stage] : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(PARALLEL[stage]), output, DELAY);
    }
    state.setPort(pinNrToPortNr(QS1), data.shift[STAGES - 1], DELAY);
    state.setPort(pinNrToPortNr(QS2), data.qs2, DELAY);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  /**
   * Shift register, storage register, delayed serial bit and the previous clock level.
   *
   * <p>Index 0 is {@code QP0}. Startup follows {@link AppPreferences#Memory_Startup_Unknown}.
   */
  private static final class State implements InstanceData {
    private Value[] shift = new Value[STAGES];
    private Value[] storage = new Value[STAGES];
    private Value qs2;
    private Value lastCp = Value.FALSE;

    private State() {
      final var initial =
          AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      Arrays.fill(shift, initial);
      Arrays.fill(storage, initial);
      qs2 = initial;
    }

    private boolean isRising(Value clock) {
      return lastCp == Value.FALSE && clock == Value.TRUE;
    }

    private boolean isFalling(Value clock) {
      return lastCp == Value.TRUE && clock == Value.FALSE;
    }

    private void remember(Value clock) {
      if (clock != null && clock != Value.NIL) {
        lastCp = clock;
      }
    }

    private void shiftIn(Value serial) {
      for (var stage = STAGES - 1; stage > 0; stage--) {
        shift[stage] = shift[stage - 1];
      }
      shift[0] = serial;
    }

    private void captureSerial() {
      qs2 = shift[STAGES - 1];
    }

    private void followShift() {
      storage = shift.clone();
    }

    @Override
    public State clone() {
      final var copy = new State();
      copy.shift = shift.clone();
      copy.storage = storage.clone();
      copy.qs2 = qs2;
      copy.lastCp = lastCp;
      return copy;
    }
  }

  private static int structureX(int originX, int pins, int pin) {
    if (pin <= pins / 2) {
      return originX + (pin - 1) * 20 + 10;
    }
    return originX + (pins - pin) * 20 + 10;
  }

  private static int structureY(int originY, int bodyHeight, int pins, int pin) {
    if (pin <= pins / 2) {
      return originY + bodyHeight - AbstractTtlGate.PIN_HEIGHT;
    }
    return originY + AbstractTtlGate.PIN_HEIGHT;
  }

  private static void structureBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 2, centerY - 2, 4, 4);
  }

  private static void structureDot(
      Graphics g, int centerX, int centerY, TtlRegisterData data, int word, int bit) {
    if (data == null) {
      return;
    }
    final var value = data.getValue(word);
    if (bit < 0 || bit >= value.getWidth()) {
      return;
    }
    final var bitValue = value.get(bit);
    g.setColor(bitValue.getColor());
    g.fillOval(centerX - 3, centerY - 3, 6, 6);
    g.setColor(Color.WHITE);
    GraphicsUtil.drawCenteredText(g, bitValue.toDisplayString(), centerX, centerY);
    g.setColor(Color.BLACK);
  }

}
