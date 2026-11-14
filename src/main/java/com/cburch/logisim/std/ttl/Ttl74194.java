/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.Path2D;

/**
 * TTL 74x194: 4-bit bidirectional universal shift register
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls194a.pdf">74LS194 datasheet</a>.
 */
public class Ttl74194 extends AbstractTtlGate {
  enum Mode {
    HOLD,
    SHIFT_RIGHT,
    SHIFT_LEFT,
    LOAD
  }

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74194";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Inputs
  public static final byte S0 = 9;
  public static final byte S1 = 10;

  public static final byte SR = 2;
  public static final byte SL = 7;

  public static final byte CLK = 11;
  public static final byte nCLR = 1;

  public static final byte A = 3;
  public static final byte B = 4;
  public static final byte C = 5;
  public static final byte D = 6;

  // Outputs
  public static final byte QA = 15;
  public static final byte QB = 14;
  public static final byte QC = 13;
  public static final byte QD = 12;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] DATA = new byte[] {D, C, B, A};

  private InstanceState _state;

  public Ttl74194() {
    super(
        _ID,
        (byte) 16,
        new byte[] {QA, QB, QC, QD},
        new String[] {
          "MR", "DSR", "D0", "D1", "D2", "D3", "DSL", "S0", "S1", "CP", "Q3", "Q2", "Q1", "Q0"
        },
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * One register block. Q0–Q3 leave the top edge. Every input, including the clock and both mode
   * bits, enters the bottom edge. D0–D3 share one bus.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var ffLeft = x + 22;
    final var rowTop = y + 11;
    final var rowBottom = rowTop + 14;

    g.drawRect(ffLeft, rowTop, 76, 14);
    final var previousFont = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 7));
    GraphicsUtil.drawText(
        g, "COUNTER", ffLeft + 38, rowTop + 7, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
    g.setFont(previousFont);
    drawClockWedge(g, x + 84, rowBottom);
    drawBubble(g, x + 36, rowBottom + 1);

    wire(g, pinX(x, QA), pinTop, pinX(x, QA), rowTop);
    wire(g, pinX(x, QB), pinTop, pinX(x, QB), rowTop);
    wire(g, pinX(x, QC), pinTop, pinX(x, QC), rowTop);
    wire(g, pinX(x, QD), pinTop, pinX(x, QD), rowTop);

    wire(g, pinX(x, CLK), pinTop, pinX(x, CLK), y + 29, x + 84, y + 29, x + 84, rowBottom);
    wire(g, pinX(x, S1), pinTop, pinX(x, S1), y + 33, x + 76, y + 33, x + 76, rowBottom);
    wire(g, pinX(x, S0), pinTop, pinX(x, S0), y + 37, x + 68, y + 37, x + 68, rowBottom);
    wire(g, pinX(x, SL), pinBottom, pinX(x, SL), y + 41, x + 60, y + 41, x + 60, rowBottom);
    final var busY = y + 45;
    wire(g, pinX(x, A), busY, pinX(x, A), pinBottom);
    wire(g, pinX(x, B), busY, pinX(x, B), pinBottom);
    wire(g, pinX(x, C), busY, pinX(x, C), pinBottom);
    wire(g, pinX(x, D), busY, pinX(x, D), pinBottom);
    final var busLeft = pinX(x, A) - 0.5f;
    final var busRight = pinX(x, D) + 0.5f;
    drawBus(g, busLeft, busY, x + 52, busY, x + 52, rowBottom);
    drawBus(g, x + 51f, busY, busRight, busY);
    wire(g, pinX(x, SR), pinBottom, pinX(x, SR), y + 49, x + 44, y + 49, x + 44, rowBottom);
    wire(g, pinX(x, nCLR), pinBottom, pinX(x, nCLR), y + 45, x + 36, y + 45, x + 36, rowBottom + 2);
  }

  /** IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
   *
   * @param dsPinNr datasheet pin number
   * @return port number
   */
  private byte pinNrToPortNr(byte dsPinNr) {
    return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
  }

  /** Gets the current state of the specified pin
   *
   * @param dsPinNr datasheet pin number
   * @return the current state of the specified pin
   */
  private Value getPort(byte dsPinNr) {
    return _state.getPortValue(pinNrToPortNr(dsPinNr));
  }

  /** Sets the specified pin to the specified value
   *
   * @param dsPinNr datasheet pin number
   * @param v the value for the pin
   */
  private void setPort(byte dsPinNr, Value v) {
    _state.setPort(pinNrToPortNr(dsPinNr), v, DELAY);
  }

  /**
   * Gets the instance data
   *
   * @return the instance data
   */
  private ShiftRegisterData getData() {
    var data = (ShiftRegisterData) _state.getData();

    if (data == null) {
      data = new ShiftRegisterData(BitWidth.ONE, 4);
      _state.setData(data);
    }

    return data;
  }

  /**
   * Predicate which is true when the clock was triggered
   *
   * @return true when the clock was triggered
   */
  private boolean isTriggered() {
    return getData().updateClock(getPort(CLK), StdAttr.TRIG_RISING);
  }

  /**
   * Decodes the mode inputs
   *
   * @return the detected mode
   */
  private Mode getMode() {
    final var mode = ((getPort(S1) == Value.TRUE) ? 2 : 0) + ((getPort(S0) == Value.TRUE) ? 1 : 0);

    return Mode.values()[mode];
  }

  /**
   * Update the state of the internal register
   */
  private void propagateRegister() {
    if (getPort(nCLR) == Value.FALSE) { // CLR is active low and clear is async
      getData().clear();
    } else if (isTriggered()) {
      switch (getMode()) {
        case LOAD:
          for (var i = 0; i < 4; i++) {
            getData().set(i, getPort(DATA[i]));
          }
          break;
        case SHIFT_LEFT:
          getData().pushUp(getPort(SL));
          break;
        case SHIFT_RIGHT:
          getData().pushDown(getPort(SR));
          break;
        case HOLD:
          // Nothing to do
          break;
      }
    }
  }

  /**
   * Drive the output buffers
   */
  private void propagateOutputs() {
    setPort(QA, getData().get(3));  // Most significant bit
    setPort(QB, getData().get(2));
    setPort(QC, getData().get(1));
    setPort(QD, getData().get(0));  // Least significant bit
  }

  @Override
  public void propagateTtl(InstanceState state) {
    _state = state;

    propagateRegister();
    propagateOutputs();
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CLK)};
  }

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  /** Datasheet pin column for the east-facing internal view. */
  private static int pinX(int x, int pin) {
    return pin <= GND ? x + (pin - 1) * 20 + 10 : x + (VCC - pin) * 20 + 10;
  }

  /** Active-low mark. Even 2×2 oval, center on the wire axis, touching the block. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Thick bus. Ends may sit on a half pixel so the stroke meets the side of a tap. */
  private static void drawBus(Graphics g, float... points) {
    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    final var path = new Path2D.Float();
    path.moveTo(points[0], points[1]);
    for (var i = 2; i < points.length; i += 2) {
      path.lineTo(points[i], points[i + 1]);
    }
    g2.draw(path);
    g2.setStroke(previous);
  }

  /** Clock mark on the bottom edge. Base is 4 pixels; the point is inside the block. */
  private static void drawClockWedge(Graphics g, int centerX, int edgeY) {
    g.drawPolygon(
        new int[] {centerX - 2, centerX + 2, centerX},
        new int[] {edgeY, edgeY, edgeY - 4},
        3);
  }
}
