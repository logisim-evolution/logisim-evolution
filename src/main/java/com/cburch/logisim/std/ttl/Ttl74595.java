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
import java.awt.Font;
import java.awt.Graphics;
import java.util.Arrays;

/**
 * TTL 74x595: 8-bit serial-in, parallel-out shift register with an output storage register.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc595.pdf">SN74HC595</a> and Nexperia 74HC595 data
 * sheets. {@code SRCLK} shifts {@code SER} into {@code QA} and toward {@code QH}. {@code RCLK}
 * copies the shift register into the storage register, which drives {@code QA}..{@code QH}. When
 * both clocks rise in the same step, storage keeps the pre-shift value, so a tied clock leaves the
 * shift register one pulse ahead. {@code SRCLR} low clears only the shift register. {@code OE} low
 * enables the storage outputs; any other level releases them. {@code QH'} follows the shift
 * register and is not released by {@code OE}. Nexperia names the same pins {@code Q0}..{@code Q7},
 * {@code DS}, {@code SHCP}, {@code STCP}, {@code MR} and {@code Q7S}. Nanosecond delays are not
 * modeled.
 */
public class Ttl74595 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74595";

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
  public static final byte OE = 13;
  public static final byte SER = 14;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] STORAGE_OUTPUTS = {QA, QB, QC, QD, QE, QF, QG, QH};
  private static final byte[] OUTPUT_PINS = {QB, QC, QD, QE, QF, QG, QH, QHP, QA};
  private static final String[] PORT_NAMES = {
    "QB", "QC", "QD", "QE", "QF", "QG", "QH", "QH'", "nSRCLR", "SRCLK", "RCLK", "nOE", "SER", "QA"
  };
  private static final String[] PIN_NAMES = {
    "QB", "QC", "QD", "QE", "QF", "QG", "QH", null,
    "QH'", "nSRCLR", "SRCLK", "RCLK", "nOE", "SER", "QA", null
  };

  /** Creates a 74595 shift register with a three-state output latch. */
  public Ttl74595() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74595HdlGenerator());
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

    public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var boxTop = y + 18;
    final var boxBottom = boxTop + 24;
    g.drawRect(x + 6, boxTop, 312, 24);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 9));
    GraphicsUtil.drawCenteredText(g, "REG", x + 162, y + 30);
    g.setFont(previousFont);
    for (var pin = 1; pin <= 16; pin++) {
      if (pin == 8 || pin == 16) {
        continue;
      }
      final var pinX = pin <= 8 ? x + (pin - 1) * 20 + 10 : x + (16 - pin) * 20 + 10;
      final var pinY =
          pin <= 8
              ? y + height - AbstractTtlGate.PIN_HEIGHT
              : y + AbstractTtlGate.PIN_HEIGHT;
      final var edge = pin <= 8 ? boxBottom : boxTop;
      if (pin == 13) {
        final var bubbleY = pin <= 8 ? edge + 4 : edge - 4;
        g.drawLine(pinX, pinY, pinX, pin <= 8 ? bubbleY + 2 : bubbleY - 2);
        structureBubble(g, pinX, bubbleY);
      } else {
        g.drawLine(pinX, pinY, pinX, edge);
      }
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getData(state);
    final var shiftEdge = data.updateClock(state.getPortValue(pinNrToPortNr(SRCLK)), 0);
    final var storeEdge = data.updateClock(state.getPortValue(pinNrToPortNr(RCLK)), 1);
    final var cleared = state.getPortValue(pinNrToPortNr(SRCLR)) == Value.FALSE;
    final var beforeShift = data.copyShift();

    if (cleared) {
      data.clearShift();
    } else if (shiftEdge) {
      data.shiftIn(state.getPortValue(pinNrToPortNr(SER)));
    }
    if (storeEdge) {
      data.loadStorage(shiftEdge && !cleared ? beforeShift : data.copyShift());
    }

    final var enabled = state.getPortValue(pinNrToPortNr(OE)) == Value.FALSE;
    for (var stage = 0; stage < STAGES; stage++) {
      final var output = enabled ? data.storage[stage] : Value.UNKNOWN;
      state.setPort(pinNrToPortNr(STORAGE_OUTPUTS[stage]), output, DELAY);
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
