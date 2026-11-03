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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * Logic diagram from the data sheet. SER, SRCLK and active-low SRCLR enter the shift register,
   * and QH' leaves its last stage. An eight-bit bus copies the shift register into storage on
   * RCLK. Storage drives QA..QH through three-state buffers. OE meets an active-low enable buffer
   * whose output ends on the slanted side of each channel. QH' bypasses storage and OE.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var shiftLeft = x + 100;
    final var shiftTop = y + 14;
    final var shiftBottom = y + 23;
    final var storeLeft = x + 6;
    final var storeTop = y + 35;
    final var storeBottom = y + 43;
    final var railY = y + 51;

    g.drawRect(shiftLeft, shiftTop, 40, shiftBottom - shiftTop);
    g.drawRect(storeLeft, storeTop, 130, storeBottom - storeTop);
    drawTriangle(g, x + 26, y + 27, x + 34, y + 27, x + 30, y + 23);
    drawTriangle(g, x + 143, y + 27, x + 149, y + 27, x + 146, y + 31);
    for (var px : new int[] {10, 30, 50, 70, 90, 110, 130}) {
      drawTriangle(g, x + px - 4, y + 45, x + px + 4, y + 45, x + px, y + 49);
    }

    final var previous = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 3));
    // Dialog 3: SHIFT is 9×5 and STORE is 10×5, both inside the block and clear of the clock mark.
    GraphicsUtil.drawCenteredText(g, "SHIFT", x + 120, y + 18);
    GraphicsUtil.drawCenteredText(g, "STORE", x + 71, y + 38);
    GraphicsUtil.drawCenteredText(g, "8", x + 106, y + 27);
    g.setFont(previous);

    wire(g, x + 50, pinTop, x + 50, y + 17, shiftLeft, y + 17);
    wire(g, x + 110, pinTop, x + 110, shiftTop);
    drawClockTop(g, x + 110, shiftTop);
    wire(g, x + 130, pinTop, x + 130, y + 12);
    drawBubble(g, x + 130, y + 13);
    wire(g, x + 140, y + 21, x + 150, y + 21, x + 150, pinTop);
    wire(g, x + 90, pinTop, x + 90, y + 13, x + 4, y + 13, x + 4, y + 39, storeLeft, y + 39);
    drawClockSide(g, storeLeft, y + 39);
    wire(g, x + 112, shiftBottom, x + 112, storeTop);
    g.drawLine(x + 110, y + 26, x + 114, y + 28);

    wire(g, x + 70, pinTop, x + 70, y + 9, x + 146, y + 9, x + 146, y + 25);
    drawBubble(g, x + 146, y + 26);
    wire(g, x + 146, y + 31, x + 146, y + 33, x + 40, y + 33, x + 40, y + 25, x + 32, y + 25);
    wire(g, x + 146, y + 33, x + 146, railY, x + 20, railY);
    drawNode(g, x + 146, y + 33);

    wire(g, x + 30, pinTop, x + 30, y + 23);
    wire(g, x + 18, storeTop, x + 18, y + 29, x + 30, y + 29, x + 30, y + 27);
    for (var px : new int[] {10, 30, 50, 70, 90, 110, 130}) {
      wire(g, x + px, storeBottom, x + px, y + 45);
      wire(g, x + px, y + 49, x + px, pinBottom);
      wire(g, x + px + 10, railY, x + px + 10, y + 47, x + px + 2, y + 47);
      if (px > 10) {
        drawNode(g, x + px + 10, railY);
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

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Closed buffer or inverter triangle. */
  private static void drawTriangle(
      Graphics g, int x0, int y0, int x1, int y1, int x2, int y2) {
    g.drawPolygon(new int[] {x0, x1, x2}, new int[] {y0, y1, y2}, 3);
  }

  /** Active-low mark. Even 2×2 oval, center on the wire axis. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of three branches. The oval center is the common point. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Clock mark on the top edge. The point is inside the block. */
  private static void drawClockTop(Graphics g, int centerX, int edgeY) {
    g.drawLine(centerX - 3, edgeY, centerX, edgeY + 4);
    g.drawLine(centerX + 3, edgeY, centerX, edgeY + 4);
  }

  /** Clock mark on the left edge. The point is inside the block. */
  private static void drawClockSide(Graphics g, int edgeX, int centerY) {
    g.drawLine(edgeX, centerY - 3, edgeX + 4, centerY);
    g.drawLine(edgeX, centerY + 3, edgeX + 4, centerY);
  }

}
