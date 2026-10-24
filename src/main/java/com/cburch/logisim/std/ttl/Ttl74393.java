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
import java.awt.Color;
import java.awt.Graphics;

/**
 * TTL 74x393: dual 4-bit binary ripple counter.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT393.pdf">Nexperia 74HC393</a>
 * data sheet. Each half advances on the high-to-low transition of {@code nCP}. {@code nQ0} is the
 * least significant bit, and the count wraps from 15 to 0. A high {@code nMR} asynchronously
 * clears that half and overrides its clock. The two counters are independent. Ripple delays are
 * not modeled: a falling edge updates the whole nibble in one step.
 */
public class Ttl74393 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74393";

  public static final int PORT_INDEX_1CP = 0;
  public static final int PORT_INDEX_1MR = 1;
  public static final int PORT_INDEX_1Q0 = 2;
  public static final int PORT_INDEX_1Q1 = 3;
  public static final int PORT_INDEX_1Q2 = 4;
  public static final int PORT_INDEX_1Q3 = 5;
  public static final int PORT_INDEX_2Q3 = 6;
  public static final int PORT_INDEX_2Q2 = 7;
  public static final int PORT_INDEX_2Q1 = 8;
  public static final int PORT_INDEX_2Q0 = 9;
  public static final int PORT_INDEX_2MR = 10;
  public static final int PORT_INDEX_2CP = 11;

  private static final int COUNTER_1 = 0;
  private static final int COUNTER_2 = 1;
  private static final int BITS = 4;
  private static final int STAGE_MASK = (1 << BITS) - 1;
  private static final int DELAY = 4;
  private static final int BUBBLE_SIZE = 4;
  private static final BitWidth WIDTH = BitWidth.create(BITS);
  private static final byte[] OUTPUT_PINS = {3, 4, 5, 6, 8, 9, 10, 11};
  private static final String[] PORT_NAMES = {
    "1CP (Clock input, high-to-low)",
    "1MR (Master reset, active high)",
    "1Q0",
    "1Q1",
    "1Q2",
    "1Q3",
    "2Q3",
    "2Q2",
    "2Q1",
    "2Q0",
    "2MR (Master reset, active high)",
    "2CP (Clock input, high-to-low)"
  };
  private static final int[] OUTPUT_PORT_INDEXES = {
    PORT_INDEX_1Q0,
    PORT_INDEX_1Q1,
    PORT_INDEX_1Q2,
    PORT_INDEX_1Q3,
    PORT_INDEX_2Q0,
    PORT_INDEX_2Q1,
    PORT_INDEX_2Q2,
    PORT_INDEX_2Q3
  };
  private static final int[] OUTPUT_COUNTERS = {
    COUNTER_1, COUNTER_1, COUNTER_1, COUNTER_1, COUNTER_2, COUNTER_2, COUNTER_2, COUNTER_2
  };
  private static final int[] OUTPUT_BITS = {0, 1, 2, 3, 0, 1, 2, 3};

  /** Creates a 74393 dual 4-bit binary ripple counter. */
  public Ttl74393() {
    super(_ID, (byte) 14, OUTPUT_PINS, PORT_NAMES, new Ttl74393HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored =
        rawState instanceof TtlRegisterData register ? register : null;
    // Counter 1 sits on the lower pins. Q0..Q3 are pins 3..6.
    drawRipple(g, x, y, height, false, new int[] {50, 70, 90, 110}, 10, 30, stored, COUNTER_1);
    // Counter 2 sits on the upper pins. Q0..Q3 are pins 11, 10, 9, 8.
    drawRipple(g, x, y, height, true, new int[] {70, 90, 110, 130}, 30, 50, stored, COUNTER_2);
  }

  /**
   * One ripple chain, following the 74HC393 logic diagram. Every clock wire stops on the edge of
   * its falling-edge bubble. A Q node is drawn only where that output also clocks the next stage.
   */
  private static void drawRipple(
      Graphics g,
      int x,
      int y,
      int height,
      boolean alongTop,
      int[] outputs,
      int clockOffset,
      int resetOffset,
      TtlRegisterData stored,
      int word) {
    final var boxWidth = 10;
    final var boxHeight = 10;
    final var top = alongTop ? y + 12 : y + height - 22;
    final var mid = top + boxHeight / 2;
    final var pinY = alongTop ? y + PIN_HEIGHT : y + height - PIN_HEIGHT;
    final var towardPins = alongTop ? pinY + 2 : pinY - 2;
    for (var stage = 0; stage < outputs.length; stage++) {
      final var center = x + outputs[stage];
      final var left = center - boxWidth / 2;
      final var right = left + boxWidth;
      g.drawRect(left, top, boxWidth, boxHeight);
      g.drawLine(left + 1, mid - 2, left + 4, mid);
      g.drawLine(left + 1, mid + 2, left + 4, mid);
      drawBit(g, left + 7, mid, stored, word, stage);
      // The tap sits just below the lead and clear of the box, so the dot does not cover the body.
      final var nodeX = right + 3;
      final var nodeY = mid + 2;
      g.drawLine(right, mid, nodeX, mid);
      g.drawLine(nodeX, mid, nodeX, towardPins);
      g.drawLine(nodeX, towardPins, center, towardPins);
      g.drawLine(center, towardPins, center, pinY);
      if (stage + 1 < outputs.length) {
        final var nextLeft = x + outputs[stage + 1] - boxWidth / 2;
        final var bubbleEdge = nextLeft - BUBBLE_SIZE;
        final var riseX = bubbleEdge - 2;
        g.drawLine(nodeX, mid, nodeX, nodeY);
        g.drawLine(nodeX, nodeY, riseX, nodeY);
        g.drawLine(riseX, nodeY, riseX, mid);
        g.drawLine(riseX, mid, bubbleEdge, mid);
        drawClockBubble(g, nextLeft, mid);
        drawTap(g, nodeX, nodeY);
      }
    }

    final var firstLeft = x + outputs[0] - boxWidth / 2;
    final var clockX = x + clockOffset;
    g.drawLine(clockX, pinY, clockX, mid);
    g.drawLine(clockX, mid, firstLeft - BUBBLE_SIZE, mid);
    drawClockBubble(g, firstLeft, mid);

    final var rail = alongTop ? top + boxHeight + 4 : top - 4;
    final var resetX = x + resetOffset;
    final var resetEdge = alongTop ? top + boxHeight : top;
    g.drawLine(resetX, pinY, resetX, rail);
    g.drawLine(resetX, rail, x + outputs[outputs.length - 1], rail);
    for (var output : outputs) {
      g.drawLine(x + output, rail, x + output, resetEdge);
      drawNode(g, x + output, rail);
    }
  }

  /** Bubble whose right side touches the clock input. The wire stops at its left side. */
  private static void drawClockBubble(Graphics g, int inputX, int mid) {
    g.drawOval(inputX - BUBBLE_SIZE, mid - BUBBLE_SIZE / 2, BUBBLE_SIZE, BUBBLE_SIZE);
  }

  /** Junction dot centered on the crossing of two wires. */
  private static void drawNode(Graphics g, int x, int y) {
    g.fillOval(x - 2, y - 2, 4, 4);
  }

  /** Small tap dot. A 2×2 oval is centered on the integer crossing. */
  private static void drawTap(Graphics g, int x, int y) {
    g.fillOval(x - 1, y - 1, 2, 2);
  }

  private static void drawBit(
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
    g.fillOval(centerX - 1, centerY - 1, 3, 3);
    g.setColor(Color.BLACK);
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH, 2);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var clock1 =
        data.updateClock(state.getPortValue(PORT_INDEX_1CP), COUNTER_1, StdAttr.TRIG_FALLING);
    final var clock2 =
        data.updateClock(state.getPortValue(PORT_INDEX_2CP), COUNTER_2, StdAttr.TRIG_FALLING);
    advance(data, COUNTER_1, state.getPortValue(PORT_INDEX_1MR) == Value.TRUE, clock1);
    advance(data, COUNTER_2, state.getPortValue(PORT_INDEX_2MR) == Value.TRUE, clock2);
    for (var i = 0; i < OUTPUT_PORT_INDEXES.length; i++) {
      final var bit = data.getValue(OUTPUT_COUNTERS[i]).get(OUTPUT_BITS[i]);
      state.setPort(OUTPUT_PORT_INDEXES[i], bit, DELAY);
    }
  }

  private static void advance(TtlRegisterData data, int counter, boolean reset, boolean clocked) {
    if (reset) {
      data.setValue(counter, Value.createKnown(WIDTH, 0));
    } else if (clocked) {
      data.setValue(counter, nextCount(data.getValue(counter)));
    }
  }

  /**
   * Advances one binary count. An error anywhere makes the next word entirely error. A word that is
   * only unknown stays unknown. A known word increments modulo 16.
   */
  private static Value nextCount(Value current) {
    if (containsBit(current, Value.ERROR)) return Value.createError(WIDTH);
    if (!current.isFullyDefined()) return Value.createUnknown(WIDTH);
    return Value.createKnown(WIDTH, (current.toLongValue() + 1) & STAGE_MASK);
  }

  private static boolean containsBit(Value word, Value bit) {
    for (var i = 0; i < word.getWidth(); i++) {
      if (word.get(i) == bit) return true;
    }
    return false;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_1CP, PORT_INDEX_2CP};
  }

}
