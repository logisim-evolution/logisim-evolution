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
import java.awt.Color;
import java.awt.Font;
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
    drawStructure(g, x, y, height);
  }

  /**
   * Logic diagram from the data sheet. SER, SRCLK and SRCLR meet the top edge of the shift
   * register, and QH' leaves its last stage. All eight stages cross into the storage register,
   * which has its own clock and clear and drives QA..QH. A clock mark is the flip-flop triangle,
   * with its point inside the block. The serial wire hops over the storage controls.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var shiftClockX = pinX(x, 11);
    final var shiftClearX = pinX(x, 10);
    final var shiftLeft = shiftClockX - 14;
    final var shiftRight = shiftClearX + 10;
    final var shiftTop = y + 19;
    final var shiftBottom = y + 31;
    final var shiftMid = shiftTop + (shiftBottom - shiftTop) / 2;
    final var storeLeft = pinX(x, 1) - 4;
    final var storeRight = pinX(x, 7) + 4;
    final var storeBottom = pinBottom - 6;
    final var storeTop = storeBottom - 8;
    final var serX = pinX(x, 14);
    final var serY = y + 12;
    final var serEntryX = shiftLeft + 8;
    final var storeClearX = pinX(x, 13);
    final var storeClockX = pinX(x, 12);
    final var busX = shiftLeft + (shiftRight - shiftLeft) / 2;

    g.drawRect(shiftLeft, shiftTop, shiftRight - shiftLeft, shiftBottom - shiftTop);
    g.drawRect(storeLeft, storeTop, storeRight - storeLeft, storeBottom - storeTop);
    final var clockDepth = 4;
    labelBlock(g, "SHIFT", 6, shiftLeft, shiftTop + clockDepth, shiftRight, shiftBottom);
    labelBlock(g, "STORE", 6, storeLeft, storeTop, storeRight, storeBottom);

    wire(g, serX, pinTop, serX, serY);
    wire(g, serX, serY, storeClearX - 3, serY);
    drawHop(g, storeClearX, serY);
    wire(g, storeClearX + 3, serY, storeClockX - 3, serY);
    drawHop(g, storeClockX, serY);
    wire(g, storeClockX + 3, serY, serEntryX, serY, serEntryX, shiftTop);

    wire(g, storeClearX, pinTop, storeClearX, storeTop - 4);
    drawBubble(g, storeClearX, storeTop);
    wire(g, storeClockX, pinTop, storeClockX, storeTop);
    drawClock(g, storeClockX, storeTop);
    wire(g, shiftClockX, pinTop, shiftClockX, shiftTop);
    drawClock(g, shiftClockX, shiftTop);
    wire(g, shiftClearX, pinTop, shiftClearX, shiftTop - 4);
    drawBubble(g, shiftClearX, shiftTop);
    wire(g, shiftRight, shiftMid, x + 150, shiftMid, x + 150, pinTop);

    final var busMid = shiftBottom + (storeTop - shiftBottom) / 2;
    g.drawLine(busX, shiftBottom, busX, storeTop);
    g.drawLine(busX - 2, busMid - 1, busX + 2, busMid + 1);
    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    drawGlyphCenter(g, "8", busX - 6, busMid);
    g.setFont(previous);

    wire(g, x + 30, storeTop, x + 30, pinTop);
    for (var pin = 1; pin <= 7; pin++) {
      final var px = x + (pin - 1) * 20 + 10;
      g.drawLine(px, storeBottom, px, pinBottom);
    }
  }

  /** Places the ink of {@code text} so its bounds are centered on {@code x}, {@code y}. */
  private static void drawGlyphCenter(Graphics g, String text, int x, int y) {
    final var bounds = g.getFontMetrics().getStringBounds(text, g);
    final var baselineX = (int) Math.round(x - bounds.getX() - bounds.getWidth() / 2.0);
    final var baselineY = (int) Math.round(y - bounds.getY() - bounds.getHeight() / 2.0);
    g.drawString(text, baselineX, baselineY);
  }

  /** Datasheet pin center. Pins 1..8 run along the lower edge, 9..16 back along the upper edge. */
  private static int pinX(int originX, int pin) {
    return pin <= 8 ? originX + (pin - 1) * 20 + 10 : originX + (16 - pin) * 20 + 10;
  }
  private static void drawHop(Graphics g, int centerX, int y) {
    g.drawArc(centerX - 3, y - 3, 6, 6, 0, 180);
  }

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Active-low input. The bubble touches the block edge. */
  private static void drawBubble(Graphics g, int x, int edgeY) {
    g.drawOval(x - 2, edgeY - 4, 4, 4);
  }

  /** Flip-flop clock. The base lies on the block edge and the point is inside. */
  private static void drawClock(Graphics g, int x, int edgeY) {
    g.drawLine(x - 3, edgeY, x, edgeY + 4);
    g.drawLine(x + 3, edgeY, x, edgeY + 4);
  }

  /** Centers the ink of {@code name} in the rectangle, not the font baseline. */
  private static void labelBlock(
      Graphics g, String name, int size, int left, int top, int right, int bottom) {
    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, size));
    drawGlyphCenter(g, name, left + (right - left) / 2, top + (bottom - top) / 2);
    g.setFont(previous);
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

}
