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
 * TTL 74x594: 8-bit serial-in, parallel-out shift register with an output storage register.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc594.pdf">TI SN74HC594</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT594.pdf">Nexperia 74HC594</a>
 * data sheets. {@code SRCLK} shifts {@code SER} into {@code QA} and toward {@code QH}. {@code
 * RCLK} copies the shift register into the storage register, which drives {@code QA}..{@code QH}.
 * When both clocks rise in the same step, storage keeps the pre-shift value, so a tied clock
 * leaves the shift register one pulse ahead. {@code SRCLR} low clears only the shift register.
 * {@code RCLR} low clears only the storage register. Both clears override their own clock, and the
 * parallel outputs stay driven. {@code QH'} is the shift-register serial output, not an inversion;
 * Nexperia calls it {@code Q7S}. Nexperia also names {@code QA}..{@code QH} as {@code Q0}..{@code
 * Q7}, {@code SER} as {@code DS}, {@code SRCLK} as {@code SHCP}, {@code RCLK} as {@code STCP},
 * {@code SRCLR} as {@code SHR} and {@code RCLR} as {@code STR}. Nanosecond delays are not modeled.
 */
public class Ttl74594 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must be unique string among all tools.
   */
  public static final String _ID = "74594";

  public static final int DELAY = 1;
  private static final int STAGES = 8;

  // Storage outputs. QA is the first stage; QH is the last.
  public static final byte QB = 1;
  public static final byte QC = 2;
  public static final byte QD = 3;
  public static final byte QE = 4;
  public static final byte QF = 5;
  public static final byte QG = 6;
  public static final byte QH = 7;
  public static final byte QA = 15;

  /** Serial output of the shift register. Nexperia calls this pin {@code Q7S}. */
  public static final byte QHP = 9;

  // Inputs
  public static final byte SRCLR = 10;
  public static final byte SRCLK = 11;
  public static final byte RCLK = 12;
  public static final byte RCLR = 13;
  public static final byte SER = 14;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] STORAGE_OUTPUTS = {QA, QB, QC, QD, QE, QF, QG, QH};
  private static final byte[] OUTPUT_PINS = {QB, QC, QD, QE, QF, QG, QH, QHP, QA};
  private static final String[] PORT_NAMES = {
    "QB", "QC", "QD", "QE", "QF", "QG", "QH", "QH'", "nSRCLR", "SRCLK", "RCLK", "nRCLR", "SER", "QA"
  };
  private static final String[] PIN_NAMES = {
    "QB", "QC", "QD", "QE", "QF", "QG", "QH", null,
    "QH'", "nSRCLR", "SRCLK", "RCLK", "nRCLR", "SER", "QA", null
  };

  /** Creates a 74594 shift register with a continuously driven output register. */
  public Ttl74594() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74594HdlGenerator());
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
    g.drawRect((x + 14), (y + 11), 12, 12);
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 11));
    g.drawLine((x + 14), (((y + 11)) + 6) - 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 14), (((y + 11)) + 6) + 3, ((x + 14)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 30), (y + 11), 12, 12);
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + height - 13));
    g.drawLine((x + 10), (y + height - 13), (x + 36), (y + height - 13));
    g.drawLine((x + 36), (y + height - 13), (x + 36), ((y + 11)) + 12);
    g.drawLine((x + 30), (((y + 11)) + 6) - 3, ((x + 30)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 30), (((y + 11)) + 6) + 3, ((x + 30)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 46), (y + 11), 12, 12);
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 52), (y + height - 13));
    g.drawLine((x + 52), (y + height - 13), (x + 52), ((y + 11)) + 12);
    g.drawLine((x + 46), (((y + 11)) + 6) - 3, ((x + 46)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 46), (((y + 11)) + 6) + 3, ((x + 46)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 62), (y + 11), 12, 12);
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 68), (y + height - 13));
    g.drawLine((x + 68), (y + height - 13), (x + 68), ((y + 11)) + 12);
    g.drawLine((x + 62), (((y + 11)) + 6) - 3, ((x + 62)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 62), (((y + 11)) + 6) + 3, ((x + 62)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 78), (y + 11), 12, 12);
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 84), (y + height - 13));
    g.drawLine((x + 84), (y + height - 13), (x + 84), ((y + 11)) + 12);
    g.drawLine((x + 78), (((y + 11)) + 6) - 3, ((x + 78)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 78), (((y + 11)) + 6) + 3, ((x + 78)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 94), (y + 11), 12, 12);
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 100), (y + height - 13));
    g.drawLine((x + 100), (y + height - 13), (x + 100), ((y + 11)) + 12);
    g.drawLine((x + 94), (((y + 11)) + 6) - 3, ((x + 94)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 94), (((y + 11)) + 6) + 3, ((x + 94)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 110), (y + 11), 12, 12);
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 116), (y + height - 13));
    g.drawLine((x + 116), (y + height - 13), (x + 116), ((y + 11)) + 12);
    g.drawLine((x + 110), (((y + 11)) + 6) - 3, ((x + 110)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 110), (((y + 11)) + 6) + 3, ((x + 110)) + 4, ((y + 11)) + 6);
    g.drawRect((x + 126), (y + 11), 12, 12);
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 132), (y + height - 13));
    g.drawLine((x + 132), (y + height - 13), (x + 132), ((y + 11)) + 12);
    g.drawLine((x + 126), (((y + 11)) + 6) - 3, ((x + 126)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 126), (((y + 11)) + 6) + 3, ((x + 126)) + 4, ((y + 11)) + 6);
    g.drawLine((x + 26), ((y + 11)) + 6, (x + 26), ((y + 11)) + 6);
    g.drawLine((x + 42), ((y + 11)) + 6, (x + 42), ((y + 11)) + 6);
    g.drawLine((x + 58), ((y + 11)) + 6, (x + 58), ((y + 11)) + 6);
    g.drawLine((x + 74), ((y + 11)) + 6, (x + 74), ((y + 11)) + 6);
    g.drawLine((x + 90), ((y + 11)) + 6, (x + 90), ((y + 11)) + 6);
    g.drawLine((x + 106), ((y + 11)) + 6, (x + 106), ((y + 11)) + 6);
    g.drawLine((x + 122), ((y + 11)) + 6, (x + 122), ((y + 11)) + 6);
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 9), (y + 13));
    g.drawLine((x + 9), (y + 13), (x + 9), ((y + 11)) + 6);
    g.drawLine((x + 20), (y + 27), (x + 132), (y + 27));
    g.drawLine((x + 20), (y + 27), (x + 20), (y + 11));
    g.drawLine((x + 20), (y + 27), (x + 20), ((y + 11)) + 14);
    g.drawLine((x + 36), (y + 27), (x + 36), (y + 11));
    g.drawLine((x + 36), (y + 27), (x + 36), ((y + 11)) + 14);
    g.drawLine((x + 52), (y + 27), (x + 52), (y + 11));
    g.drawLine((x + 52), (y + 27), (x + 52), ((y + 11)) + 14);
    g.drawLine((x + 68), (y + 27), (x + 68), (y + 11));
    g.drawLine((x + 68), (y + 27), (x + 68), ((y + 11)) + 14);
    g.drawLine((x + 84), (y + 27), (x + 84), (y + 11));
    g.drawLine((x + 84), (y + 27), (x + 84), ((y + 11)) + 14);
    g.drawLine((x + 100), (y + 27), (x + 100), (y + 11));
    g.drawLine((x + 100), (y + 27), (x + 100), ((y + 11)) + 14);
    g.drawLine((x + 116), (y + 27), (x + 116), (y + 11));
    g.drawLine((x + 116), (y + 27), (x + 116), ((y + 11)) + 14);
    g.drawLine((x + 132), (y + 27), (x + 132), (y + 11));
    g.drawLine((x + 132), (y + 27), (x + 132), ((y + 11)) + 14);
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 20), (y + 13));
    g.drawLine((x + 20), (y + 13), (x + 20), (y + 27));
    structureBubble(g, (x + 130), (y + 7));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 22), (y + 13));
    g.drawLine((x + 22), (y + 13), (x + 22), (y + 20));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var shiftEdge = data.updateClock(state.getPortValue(pinNrToPortNr(SRCLK)), 0);
    final var storeEdge = data.updateClock(state.getPortValue(pinNrToPortNr(RCLK)), 1);
    final var shiftCleared = state.getPortValue(pinNrToPortNr(SRCLR)) == Value.FALSE;
    final var storeCleared = state.getPortValue(pinNrToPortNr(RCLR)) == Value.FALSE;
    final var beforeShift = data.copyShift();

    if (shiftCleared) {
      data.clearShift();
    } else if (shiftEdge) {
      data.shiftIn(state.getPortValue(pinNrToPortNr(SER)));
    }
    if (storeCleared) {
      data.clearStorage();
    } else if (storeEdge) {
      data.loadStorage(shiftEdge && !shiftCleared ? beforeShift : data.copyShift());
    }

    for (var stage = 0; stage < STAGES; stage++) {
      state.setPort(pinNrToPortNr(STORAGE_OUTPUTS[stage]), data.storage[stage], DELAY);
    }
    state.setPort(pinNrToPortNr(QHP), data.shift[STAGES - 1], DELAY);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(SRCLK), pinNrToPortNr(RCLK)};
  }

  /**
   * Shift register, storage register and the previous level of both clocks.
   *
   * <p>Index 0 is {@code QA}. Startup follows {@link AppPreferences#Memory_Startup_Unknown}.
   */
  private static final class State extends ClockState implements InstanceData {
    private Value[] shift = new Value[STAGES];
    private Value[] storage = new Value[STAGES];

    private State() {
      final var initial = AppPreferences.Memory_Startup_Unknown.get() ? Value.UNKNOWN : Value.FALSE;
      Arrays.fill(shift, initial);
      Arrays.fill(storage, initial);
    }

    private void clearShift() {
      Arrays.fill(shift, Value.FALSE);
    }

    private void clearStorage() {
      Arrays.fill(storage, Value.FALSE);
    }

    private void shiftIn(Value serial) {
      for (var stage = STAGES - 1; stage > 0; stage--) {
        shift[stage] = shift[stage - 1];
      }
      shift[0] = serial;
    }

    private Value[] copyShift() {
      return shift.clone();
    }

    private void loadStorage(Value[] captured) {
      storage = captured.clone();
    }

    @Override
    public State clone() {
      final var copy = (State) super.clone();
      copy.shift = shift.clone();
      copy.storage = storage.clone();
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
