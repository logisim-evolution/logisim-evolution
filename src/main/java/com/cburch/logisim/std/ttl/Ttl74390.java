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
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;

/**
 * TTL 74x390: dual decade ripple counter.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT390.pdf">74HC390 datasheet</a>.
 * Each half contains a divide-by-2 section ({@code nQ0}, falling edge of {@code nCP0}) and a
 * divide-by-5 section ({@code nQ1} through {@code nQ3}, falling edge of {@code nCP1}). A high
 * {@code nMR} asynchronously clears that half. Decade and bi-quinary operation use the external
 * connections described in the data sheet.
 */
public class Ttl74390 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74390";

  public static final int PORT_INDEX_1CP0 = 0;
  public static final int PORT_INDEX_1MR = 1;
  public static final int PORT_INDEX_1Q0 = 2;
  public static final int PORT_INDEX_1CP1 = 3;
  public static final int PORT_INDEX_1Q1 = 4;
  public static final int PORT_INDEX_1Q2 = 5;
  public static final int PORT_INDEX_1Q3 = 6;
  public static final int PORT_INDEX_2Q3 = 7;
  public static final int PORT_INDEX_2Q2 = 8;
  public static final int PORT_INDEX_2Q1 = 9;
  public static final int PORT_INDEX_2CP1 = 10;
  public static final int PORT_INDEX_2Q0 = 11;
  public static final int PORT_INDEX_2MR = 12;
  public static final int PORT_INDEX_2CP0 = 13;

  private static final int DELAY = 4;
  private static final int HALVES = 2;
  /** Datasheet section height. Dialog 8 label bounds are 13 px, so the block matches them. */
  private static final int BLOCK_H = 13;
  private static final int DIV2_W = 28;
  private static final Font SECTION_FONT = new Font(Font.DIALOG, Font.PLAIN, 8);
  private static final BitWidth WIDTH = BitWidth.create(4);
  private static final byte[] OUTPUT_PORTS = {3, 5, 6, 7, 9, 10, 11, 13};
  private static final String[] PORT_NAMES = {
    "1CP0 (Clock divide-by-2)",
    "1MR (Master reset, active HIGH)",
    "1Q0",
    "1CP1 (Clock divide-by-5)",
    "1Q1",
    "1Q2",
    "1Q3",
    "2Q3",
    "2Q2",
    "2Q1",
    "2CP1 (Clock divide-by-5)",
    "2Q0",
    "2MR (Master reset, active HIGH)",
    "2CP0 (Clock divide-by-2)"
  };
  private static final int[] CP0_PORTS = {PORT_INDEX_1CP0, PORT_INDEX_2CP0};
  private static final int[] CP1_PORTS = {PORT_INDEX_1CP1, PORT_INDEX_2CP1};
  private static final int[] MR_PORTS = {PORT_INDEX_1MR, PORT_INDEX_2MR};
  private static final int[] Q0_PORTS = {PORT_INDEX_1Q0, PORT_INDEX_2Q0};
  private static final int[] Q1_PORTS = {PORT_INDEX_1Q1, PORT_INDEX_2Q1};
  private static final int[] Q2_PORTS = {PORT_INDEX_1Q2, PORT_INDEX_2Q2};
  private static final int[] Q3_PORTS = {PORT_INDEX_1Q3, PORT_INDEX_2Q3};

  /** Creates a 74390 dual decade ripple counter. */
  public Ttl74390() {
    super(_ID, (byte) 16, OUTPUT_PORTS, null, PORT_NAMES, null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var previousFont = g.getFont();
    g.setFont(SECTION_FONT);
    drawHalf(g, x, y, height, true);
    drawHalf(g, x, y, height, false);
    g.setFont(previousFont);
  }

  /**
   * One decade as the datasheet draws it: a ÷2 section and a ÷5 section with a shared reset.
   * Outputs leave the long face toward the pins. The falling-edge clock enters the opposite long
   * face. Reset enters the short face toward its pin; the link to ÷5 crosses that half's clock in
   * the gap, because the clock pin stands between the reset pin and the ÷5 block.
   *
   * @param pinBelow true when this half's pins are on the bottom of the package
   */
  private static void drawHalf(Graphics g, int x, int y, int height, boolean pinBelow) {
    final int pinY = pinBelow ? y + height - PIN_HEIGHT : y + PIN_HEIGHT;
    final int blockTop = pinBelow ? pinY - 4 - BLOCK_H : pinY + 4;
    final int midY = blockTop + BLOCK_H / 2;
    final int innerY = pinBelow ? blockTop : blockTop + BLOCK_H;
    final int outerY = pinBelow ? blockTop + BLOCK_H : blockTop;
    final int cpRail = pinBelow ? innerY - 4 : innerY + 4;
    final int bubbleFar = pinBelow ? innerY - 2 : innerY + 2;
    final int bubbleCenter = pinBelow ? innerY - 1 : innerY + 1;
    final int busY = pinBelow ? outerY + 2 : outerY - 2;
    final int q0 = pinBelow ? x + 50 : x + 70;
    final int q1 = pinBelow ? x + 90 : x + 110;
    final int q2 = pinBelow ? x + 110 : x + 130;
    final int q3 = pinBelow ? x + 130 : x + 150;
    final int cp0Pin = pinBelow ? x + 10 : x + 30;
    final int mrPin = pinBelow ? x + 30 : x + 50;
    final int cp1Pin = pinBelow ? x + 70 : x + 90;
    final int div2Left = q0 - DIV2_W / 2;
    final int div2Clock = div2Left + 6;
    final int div5Left = q1 - 7;
    final int div5Width = q3 + 7 - div5Left;
    final int div5Clock = div5Left + div5Width / 3;
    final int riseX = div2Left + DIV2_W + 2;

    drawSection(g, div2Left, blockTop, DIV2_W, "÷2", div2Clock, pinBelow);
    drawSection(g, div5Left, blockTop, div5Width, "÷5", div5Clock, pinBelow);

    wire(g, q0, outerY, q0, pinY);
    wire(g, q1, outerY, q1, pinY);
    wire(g, q2, outerY, q2, pinY);
    wire(g, q3, outerY, q3, pinY);

    wire(g, cp0Pin, pinY, cp0Pin, cpRail);
    wire(g, cp0Pin, cpRail, div2Clock, cpRail);
    wire(g, div2Clock, cpRail, div2Clock, bubbleFar);
    wire(g, cp1Pin, pinY, cp1Pin, cpRail);
    wire(g, cp1Pin, cpRail, div5Clock, cpRail);
    wire(g, div5Clock, cpRail, div5Clock, bubbleFar);
    drawBubble(g, div2Clock, bubbleCenter);
    drawBubble(g, div5Clock, bubbleCenter);

    wire(g, mrPin, pinY, mrPin, midY);
    wire(g, mrPin, midY, div2Left, midY);
    wire(g, mrPin, busY, riseX, busY);
    drawNode(g, mrPin, busY);
    wire(g, riseX, busY, riseX, midY);
    wire(g, riseX, midY, div5Left, midY);
  }

  /** Horizontal section. The clock wedge sits off the label axis, tip inside, base on the face. */
  private static void drawSection(
      Graphics g, int left, int top, int width, String label, int clockX, boolean pinBelow) {
    g.drawRect(left, top, width, BLOCK_H);
    final int face = pinBelow ? top : top + BLOCK_H;
    final int tip = pinBelow ? face + 3 : face - 3;
    g.drawLine(clockX - 2, face, clockX + 2, face);
    g.drawLine(clockX - 2, face, clockX, tip);
    g.drawLine(clockX + 2, face, clockX, tip);
    GraphicsUtil.drawText(
        g,
        label,
        left + width / 2,
        top + BLOCK_H / 2,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER_OVERALL);
  }

  private static void wire(Graphics g, int x0, int y0, int x1, int y1) {
    g.drawLine(x0, y0, x1, y1);
  }

  /** Even 2×2 inversion mark. Centre on the wire axis; the wire stops on the far side. */
  private static void drawBubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  private static void drawNode(Graphics g, int nodeX, int nodeY) {
    g.fillOval(nodeX - 1, nodeY - 1, 2, 2);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, HALVES);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    for (var half = 0; half < HALVES; half++) {
      updateHalf(state, data, half);
    }
  }

  /**
   * Updates one decade. Clock bits are stored as 1CP0, 1CP1, 2CP0, 2CP1.
   *
   * @param state the instance being propagated
   * @param data register and clock state for both halves
   * @param half 0 for the first decade, 1 for the second
   */
  private static void updateHalf(InstanceState state, TtlRegisterData data, int half) {
    final var divideByTwo =
        data.updateClock(state.getPortValue(CP0_PORTS[half]), half * 2, StdAttr.TRIG_FALLING);
    final var divideByFive =
        data.updateClock(
            state.getPortValue(CP1_PORTS[half]), half * 2 + 1, StdAttr.TRIG_FALLING);
    if (state.getPortValue(MR_PORTS[half]) == Value.TRUE) {
      data.setValue(half, Value.createKnown(WIDTH, 0));
    } else {
      final var bits = data.getValue(half).getAll();
      if (divideByTwo) bits[0] = toggle(bits[0]);
      if (divideByFive) advanceDivideByFive(bits);
      data.setValue(half, Value.create(bits));
    }
    final var value = data.getValue(half);
    state.setPort(Q0_PORTS[half], value.get(0), DELAY);
    state.setPort(Q1_PORTS[half], value.get(1), DELAY);
    state.setPort(Q2_PORTS[half], value.get(2), DELAY);
    state.setPort(Q3_PORTS[half], value.get(3), DELAY);
  }

  /**
   * Toggles the divide-by-2 bit. {@link Value#not()} turns unknown into error, so unknown stays
   * unknown and error stays error.
   */
  private static Value toggle(Value bit) {
    if (bit == Value.TRUE) return Value.FALSE;
    if (bit == Value.FALSE) return Value.TRUE;
    if (bit == Value.ERROR) return Value.ERROR;
    return Value.UNKNOWN;
  }

  /**
   * One falling edge of CPn1 through the divide-by-5 JK chain: Q1 has J = not Q3 and K = 1, Q3 has
   * J = Q1 and Q2 with K = 1, and Q2 toggles on the falling edge of Q1. An error among those bits
   * makes all three error. Any undefined bit makes them unknown.
   */
  private static void advanceDivideByFive(Value[] bits) {
    final var q1 = bits[1];
    final var q2 = bits[2];
    final var q3 = bits[3];
    if (q1 == Value.ERROR || q2 == Value.ERROR || q3 == Value.ERROR) {
      setDivideByFive(bits, Value.ERROR);
      return;
    }
    if (!q1.isFullyDefined() || !q2.isFullyDefined() || !q3.isFullyDefined()) {
      setDivideByFive(bits, Value.UNKNOWN);
      return;
    }
    final var nextQ1 = jk(q1, q3 == Value.FALSE, true);
    final var nextQ3 = jk(q3, q1 == Value.TRUE && q2 == Value.TRUE, true);
    final var nextQ2 = q1 == Value.TRUE ? toggle(q2) : q2;
    bits[1] = nextQ1;
    bits[2] = nextQ2;
    bits[3] = nextQ3;
  }

  /** One JK step. K is always driven high in this counter, so J low clears and J high toggles. */
  private static Value jk(Value q, boolean j, boolean k) {
    if (j && k) return toggle(q);
    if (j) return Value.TRUE;
    if (k) return Value.FALSE;
    return q;
  }

  private static void setDivideByFive(Value[] bits, Value value) {
    bits[1] = value;
    bits[2] = value;
    bits[3] = value;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_1CP0, PORT_INDEX_1CP1, PORT_INDEX_2CP1, PORT_INDEX_2CP0};
  }
}
