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
 * TTL 74x670: 4-by-4 register file with 3-state outputs
 * Model based on <a href="https://www.ti.com/lit/ds/symlink/sn74ls670.pdf">74LS670 datasheet</a>.
 */
public class Ttl74670 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   * Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74670";

  public static final int DELAY = 1;

  // IC pin indices as specified in the datasheet

  // Data inputs, numbered from zero. D0 is pin 15.
  public static final byte D0 = 15;
  public static final byte D1 = 1;
  public static final byte D2 = 2;
  public static final byte D3 = 3;

  public static final byte WA0 = 14;
  public static final byte WA1 = 13;

  public static final byte RA0 = 5;
  public static final byte RA1 = 4;

  public static final byte nWE = 12;
  public static final byte nRE = 11;

  // Data outputs, numbered from zero. Q0 is pin 10.
  public static final byte Q0 = 10;
  public static final byte Q1 = 9;
  public static final byte Q2 = 7;
  public static final byte Q3 = 6;

  // Power supply
  public static final byte GND = 8;
  public static final byte VCC = 16;

  private static final byte[] DATA_OUTPUTS = new byte[] { Q0, Q1, Q2, Q3 };

  public Ttl74670() {
    super(
        _ID,
        (byte) 16,
        DATA_OUTPUTS,
        new String[] {
          "D1", "D2", "D3", "RA1", "RA0", "Q3", "Q2",
          "Q1", "Q0", "nRE", "nWE", "WA1", "WA0", "D0"
        },
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y);
  }


  /**
   * Write port W, a four-word latch file, and a vertical read port R. While nWE is low the word
   * selected by WA follows D; nRE low drives the word selected by RA onto Q, and nRE high releases
   * Q. WA0, WA1 and nWE drop straight into W. The link from W into the file is a bus. R takes the
   * word, nRE and both read-address bits on its left face. Q is a bus of straight lines:
   * pins 9 and 10, a stem from their midpoint, a branch into R, then pins 6 and 7.
   */
  private static void drawStructure(Graphics g, int x, int y) {
    final var saved = g.getFont();
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    // W sits under WA0, WA1 and nWE so each pin drops straight in. The write bus leaves at x = 48,
    // off those three columns. R is taller than it is wide; Q is not on an input's row.
    g.drawRect(x + 32, y + 14, 76, 10);
    g.drawRect(x + 32, y + 28, 32, 12);
    g.drawRect(x + 116, y + 26, 14, 20);
    g.drawLine(x + 40, y + 28, x + 40, y + 40);
    g.drawLine(x + 48, y + 32, x + 48, y + 40);
    g.drawLine(x + 56, y + 28, x + 56, y + 40);
    final String[] words = {"0", "1", "2", "3"};
    for (var i = 0; i < words.length; i++) {
      centeredLabel(g, words[i], x + 36 + i * 8, y + 34);
    }
    centeredLabel(g, "W", x + 70, y + 19);
    centeredLabel(g, "R", x + 123, y + 36);
    g.setFont(saved);

    // nWE on the top of W, nRE on the left of R. Each bubble touches the face.
    g.drawOval(x + 89, y + 12, 2, 2);
    g.drawOval(x + 114, y + 29, 2, 2);

    final var g2 = (Graphics2D) g;
    final var previous = g2.getStroke();
    g2.setStroke(new BasicStroke(1f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    g.drawLine(x + 50, y + 7, x + 50, y + 14);
    g.drawLine(x + 70, y + 7, x + 70, y + 14);
    g.drawLine(x + 90, y + 7, x + 90, y + 12);
    g.drawPolyline(
        new int[] {x + 110, x + 110, x + 114}, new int[] {y + 7, y + 30, y + 30}, 3);
    g.drawLine(x + 10, y + 53, x + 10, y + 48);
    g.drawLine(x + 30, y + 7, x + 30, y + 48);
    g.drawLine(x + 30, y + 53, x + 30, y + 48);
    g.drawLine(x + 50, y + 53, x + 50, y + 48);
    // RA1 turns above RA0, so the two address wires do not cross.
    g.drawPolyline(
        new int[] {x + 70, x + 70, x + 116}, new int[] {y + 53, y + 38, y + 38}, 3);
    g.drawPolyline(
        new int[] {x + 90, x + 90, x + 116}, new int[] {y + 53, y + 42, y + 42}, 3);
    // Pins 10 and 9 meet the top of the Q stem. Pins 6 and 7 meet the bottom.
    g.drawLine(x + 130, y + 7, x + 130, y + 24);
    g.drawLine(x + 150, y + 7, x + 150, y + 24);
    g.drawLine(x + 110, y + 53, x + 110, y + 48);
    g.drawLine(x + 130, y + 53, x + 130, y + 48);

    g2.setStroke(new BasicStroke(2f, BasicStroke.CAP_BUTT, BasicStroke.JOIN_MITER));
    drawBus(g2, x + 48, y + 24, x + 48, y + 28);
    drawBus(g2, x + 9.5f, y + 48, x + 50.5f, y + 48);
    drawBus(g2, x + 40, y + 48, x + 40, y + 40);
    drawBus(g2, x + 64, y + 34, x + 116, y + 34);
    // Q: pins 9–10, stem from their midpoint, branch into R, then down to pins 6–7.
    // A stroke-2 bus stops on the outer face of the bus it enters: one pixel short of that center.
    drawBus(g2, x + 129.5f, y + 24, x + 150.5f, y + 24);
    drawBus(g2, x + 140, y + 25, x + 140, y + 47);
    drawBus(g2, x + 130, y + 36, x + 139, y + 36);
    drawBus(g2, x + 109.5f, y + 48, x + 141, y + 48);
    g2.setStroke(previous);
  }

  private static void drawBus(Graphics2D g, float... xy) {
    var path = new Path2D.Float();
    path.moveTo(xy[0], xy[1]);
    for (var i = 2; i < xy.length; i += 2) {
      path.lineTo(xy[i], xy[i + 1]);
    }
    g.draw(path);
  }

  private static void centeredLabel(Graphics g, String text, int cx, int cy) {
    GraphicsUtil.drawText(g, text, cx, cy, GraphicsUtil.H_CENTER, GraphicsUtil.V_CENTER_OVERALL);
  }

  private record LogicScope(InstanceState state, TtlRegisterData data) {
    LogicScope(InstanceState state) {
      this(state, getData(state));
    }

    /**
     * IC pin indices are datasheet based (1-indexed), but ports are 0-indexed
     *
     * @param dsPinNr datasheet pin number
     * @return port number
     */
    private byte pinNrToPortNr(byte dsPinNr) {
      return (byte) ((dsPinNr <= GND) ? dsPinNr - 1 : dsPinNr - 2);
    }

    /**
     * Gets the current state of the specified pin
     *
     * @param dsPinNr datasheet pin number
     * @return the current state of the specified pin
     */
    private Value getPort(byte dsPinNr) {
      return state.getPortValue(pinNrToPortNr(dsPinNr));
    }

    /**
     * Sets the specified pin to the specified value
     *
     * @param dsPinNr datasheet pin number
     * @param v       the value for the pin
     */
    private void setPort(byte dsPinNr, Value v) {
      state.setPort(pinNrToPortNr(dsPinNr), v, DELAY);
    }

    /**
     * Gets the instance data
     *
     * @return the instance data
     */
    private static TtlRegisterData getData(InstanceState state) {
      var data = (TtlRegisterData) state.getData();

      if (data == null) {
        data = new TtlRegisterData(BitWidth.create(4), 4);
        state.setData(data);
      }

      return data;
    }

    /**
     * Calculate the read/write address
     *
     * @param msbPinNr the pin number of the most significant bit
     * @param lsbPinNr the pin number of the least significant bit
     * @return the address
     */
    private int getAddress(byte msbPinNr, byte lsbPinNr) {
      return ((getPort(msbPinNr) == Value.TRUE) ? 2 : 0)
          + ((getPort(lsbPinNr) == Value.TRUE) ? 1 : 0);
    }

    /**
     * Calculate the read-address based on the levels on RA0 and RA1
     *
     * @return the read-address
     */
    private int getReadAddress() {
      return getAddress(RA1, RA0);
    }

    /**
     * Calculate the write-address based on the levels on WA0 and WA1
     *
     * @return the write-address
     */
    private int getWriteAddress() {
      return getAddress(WA1, WA0);
    }

    /**
     * Predicate which is true when the clock was triggered for the selected address.
     *
     * @return true when the clock was triggered
     */
    private boolean isTriggered() {
      return data.updateClock(getPort(nWE), getWriteAddress(), StdAttr.TRIG_LOW);
    }

    /**
     * Propagate data from the data inputs to internal memory
     */
    private void propagateWritePort() {
      if (isTriggered()) {
        for (var i = 0; i < 4; i++) {
          data.setValue(
              getWriteAddress(),
              Value.create(new Value[]{getPort(D0), getPort(D1), getPort(D2), getPort(D3)}));
        }
      }
    }

    /**
     * Propagate data from internal memory to the data outputs
     */
    private void propagateReadPort() {
      var readEnabled = getPort(nRE) == Value.FALSE;  // nRE is active low
      var readData = data.getValue(getReadAddress());

      for (var i = 0; i < 4; i++) {
        setPort(DATA_OUTPUTS[i], readEnabled ? readData.get(i) : Value.UNKNOWN);
      }
    }

    public void propagate() {
      // Must write before read in order to simulate the internal latches
      propagateWritePort();
      propagateReadPort();
    }
  }

  @Override
  public void propagateTtl(InstanceState state) {
    new LogicScope(state).propagate();
  }
}
