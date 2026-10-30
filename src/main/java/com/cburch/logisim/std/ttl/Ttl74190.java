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
 * TTL 74x190: synchronous presettable BCD up/down counter.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/cd74hc190.pdf">CD74HC190</a> and Philips 74HC/HCT190
 * data sheets. Parallel load is transparent and overrides the clock. The counter advances on the
 * rising edge of the clock when load is high and count enable is low. A low up/down input counts
 * up. Terminal count is the gate term measured on a 74HC190: while counting up it is QA and QD, so
 * codes 9, 11, 13 and 15 all assert it, and while counting down it is the all-zero word. Count
 * enable does not clear that output. The ripple clock is low only while terminal count is high,
 * count enable is low and the clock is low. The next-state table, including codes 10 to 15, is
 * Figure 3 of the TI
 * data sheet. Nanosecond delays are not modeled.
 *
 * <p>An unknown or error input changes an output only when the two substitutions disagree. An error
 * on such an input makes the disagreed output an error; an unknown input makes it unknown.
 */
public class Ttl74190 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74190";

  public static final int DELAY = 1;

  public static final byte D1 = 1;
  public static final byte Q1 = 2;
  public static final byte Q0 = 3;
  public static final byte CE = 4;
  public static final byte DU = 5;
  public static final byte Q2 = 6;
  public static final byte Q3 = 7;
  public static final byte GND = 8;
  public static final byte D3 = 9;
  public static final byte D2 = 10;
  public static final byte PL = 11;
  public static final byte TC = 12;
  public static final byte RC = 13;
  public static final byte CP = 14;
  public static final byte D0 = 15;
  public static final byte VCC = 16;

  /** Next code while counting up. Index 10 to 15 are the illegal codes on TI Figure 3. */
  static final int[] COUNT_UP = {1, 2, 3, 4, 5, 6, 7, 8, 9, 0, 9, 4, 9, 0, 9, 0};

  /** Next code while counting down. State 11 needs a second clock to reach the decade cycle. */
  static final int[] COUNT_DOWN = {9, 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 3, 4, 5, 6};

  private static final int WIDTH = 4;
  private static final byte[] OUTPUT_PINS = {Q1, Q0, Q2, Q3, TC, RC};
  private static final String[] PORT_NAMES = {
    "D1/B",
    "Q1/QB",
    "Q0/QA",
    "nCE/CTEN (Count enable, active low)",
    "D/U (Down/up, high counts down)",
    "Q2/QC",
    "Q3/QD",
    "D3/D",
    "D2/C",
    "nPL/LOAD (Parallel load, active low)",
    "TC/MAXMIN (Terminal count)",
    "nRC/RCO (Ripple clock, active low)",
    "CP/CLK (Clock)",
    "D0/A"
  };
  private static final String[] PIN_NAMES = {
    "D1", "Q1", "Q0", "nCE", "D/U", "Q2", "Q3", null,
    "D3", "D2", "nPL", "TC", "nRC", "CP", "D0", null
  };

  private static final int BIT_LOAD_HIGH = 0;
  private static final int BIT_ENABLE_HIGH = 1;
  private static final int BIT_DOWN = 2;
  private static final int BIT_DATA = 3;
  private static final int BIT_STORED = 7;
  private static final int COUNT_BITS = 11;

  /** Creates a 74190 synchronous presettable BCD up/down counter. */
  public Ttl74190() {
    super(_ID, (byte) 16, OUTPUT_PINS, PORT_NAMES, new Ttl74190HdlGenerator());
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

  /** Next state from TI Figure 3. {@code down} selects the count-down diagram. */
  static int nextCount(int code, boolean down) {
    return (down ? COUNT_DOWN : COUNT_UP)[code & 0xF];
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawCounter(g, x, y, height);
  }

  /**
   * Stages sit on the Q pins. Clock, load and direction stay on rails above the boxes. Terminal
   * count and the ripple clock leave a separate gate in the gap between Q0 and Q2.
   */
  private static void drawCounter(Graphics g, int x, int y, int height) {
    final var pinTop = y + AbstractTtlGate.PIN_HEIGHT;
    final var pinBottom = y + height - AbstractTtlGate.PIN_HEIGHT;
    final int[] bits = {1, 0, 2, 3};
    final int[] outputs = {Q1, Q0, Q2, Q3};
    final var stageTop = y + 34;
    final var stageBottom = y + 46;
    final var stageMid = stageTop + (stageBottom - stageTop) / 2;
    final var half = 5;
    final var loadRail = y + 12;
    final var directionRail = y + 20;
    final var clockRail = y + 26;
    final var dataLane = y + 9;
    final int[] center = new int[outputs.length];
    for (var i = 0; i < outputs.length; i++) {
      center[i] = pinX(x, outputs[i]);
    }
    for (var i = 0; i < center.length; i++) {
      g.drawRect(center[i] - half, stageTop, half * 2, stageBottom - stageTop);
      g.drawLine(center[i] - 3, clockRail, center[i], stageTop);
      g.drawLine(center[i] + 3, clockRail, center[i], stageTop);
      final var previousFont = g.getFont();
      g.setFont(new Font(Font.DIALOG, Font.PLAIN, 8));
      GraphicsUtil.drawCenteredText(g, Integer.toString(bits[i]), center[i], stageMid + 1);
      g.setFont(previousFont);
      g.drawLine(center[i], stageBottom, center[i], pinBottom);
    }

    g.drawLine(center[0], clockRail, center[center.length - 1], clockRail);
    final var clockX = pinX(x, CP);
    g.drawLine(clockX, pinTop, clockX, clockRail);
    drawNode(g, clockX, clockRail);
    drawNode(g, center[2], clockRail);

    final var loadLeft = center[0] - half;
    final var loadRight = center[center.length - 1] - half;
    g.drawLine(loadLeft, loadRail, loadRight, loadRail);
    for (var i = 0; i < center.length; i++) {
      final var edge = center[i] - half;
      g.drawLine(edge, loadRail, edge, stageTop);
      if (i > 0 && i < center.length - 1) {
        drawNode(g, edge, loadRail);
      }
    }
    final var loadX = pinX(x, PL);
    g.drawLine(loadX, pinTop, loadX, loadRail - 4);
    g.drawOval(loadX - 2, loadRail - 4, 4, 4);
    drawNode(g, loadX, loadRail);

    final var directionLeft = center[0] + half;
    final var directionRight = center[center.length - 1] + half;
    g.drawLine(directionLeft, directionRail, directionRight, directionRail);
    for (var i = 0; i < center.length; i++) {
      final var edge = center[i] + half;
      g.drawLine(edge, directionRail, edge, stageTop);
      if (i > 0 && i < center.length - 1) {
        drawNode(g, edge, directionRail);
      }
    }
    final var directionX = pinX(x, DU);
    g.drawLine(directionX, pinBottom, directionX, directionRail);
    drawNode(g, directionX, directionRail);

    final var enableX = pinX(x, CE);
    g.drawLine(enableX, pinBottom, enableX, clockRail + 4);
    g.drawOval(enableX - 2, clockRail, 4, 4);

    wireData(g, pinX(x, D1), pinBottom, center[0] - half, stageMid);
    wireData(g, pinX(x, D0), pinTop, center[1] - half, dataLane, stageTop);
    wireData(g, pinX(x, D2), pinTop, center[2] - half, dataLane, stageTop);
    wireData(g, pinX(x, D3), pinTop, center[3] - half, dataLane, stageTop);

    final var termLeft = x + 74;
    final var termTop = stageTop;
    final var termWidth = 12;
    final var termHeight = stageBottom - stageTop;
    g.drawRect(termLeft, termTop, termWidth, termHeight);
    final var busY = stageBottom + 3;
    g.drawLine(center[0], busY, center[center.length - 1], busY);
    for (var stage : center) {
      drawNode(g, stage, busY);
    }
    final var busUp = termLeft + termWidth / 2;
    g.drawLine(busUp, busY, busUp, stageBottom);
    drawNode(g, busUp, busY);
    final var termX = pinX(x, TC);
    final var rippleX = pinX(x, RC);
    final var outLane = y + 16;
    g.drawLine(termLeft + 3, termTop, termLeft + 3, outLane);
    g.drawLine(termLeft + 3, outLane, termX, outLane);
    g.drawLine(termX, outLane, termX, pinTop);
    g.drawOval(termLeft + 7, termTop - 4, 4, 4);
    g.drawLine(termLeft + 9, termTop - 4, termLeft + 9, outLane);
    g.drawLine(termLeft + 9, outLane, rippleX, outLane);
    g.drawLine(rippleX, outLane, rippleX, pinTop);
  }

  private static int pinX(int x, int pin) {
    return pin <= GND ? x + (pin - 1) * 20 + 10 : x + (VCC - pin) * 20 + 10;
  }

  /** Bottom data pin into the left side of its stage. */
  private static void wireData(Graphics g, int pinX, int pinY, int edgeX, int laneY) {
    g.drawLine(pinX, pinY, pinX, laneY);
    g.drawLine(pinX, laneY, edgeX, laneY);
  }

  /** Top data pin along a lane above the rails, then down the left edge to the stage corner. */
  private static void wireData(Graphics g, int pinX, int pinY, int edgeX, int laneY, int edgeY) {
    g.drawLine(pinX, pinY, pinX, laneY);
    g.drawLine(pinX, laneY, edgeX, laneY);
    g.drawLine(edgeX, laneY, edgeX, edgeY);
  }

  private static void drawNode(Graphics g, int x, int y) {
    g.fillOval(x - 2, y - 2, 4, 4);
  }





  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getState(state);
    final var clock = input(state, CP);
    final var triggered = data.updateClock(clock, StdAttr.TRIG_RISING);
    final var count =
        resolveCount(
            data.getValue(),
            triggered,
            input(state, PL),
            input(state, CE),
            input(state, DU),
            input(state, D0),
            input(state, D1),
            input(state, D2),
            input(state, D3));
    data.setValue(count);
    final var flags = resolveFlags(count, input(state, CE), input(state, DU), clock);
    state.setPort(pinNrToPortNr(Q0), count.get(0), DELAY);
    state.setPort(pinNrToPortNr(Q1), count.get(1), DELAY);
    state.setPort(pinNrToPortNr(Q2), count.get(2), DELAY);
    state.setPort(pinNrToPortNr(Q3), count.get(3), DELAY);
    state.setPort(pinNrToPortNr(TC), flags.get(0), DELAY);
    state.setPort(pinNrToPortNr(RC), flags.get(1), DELAY);
  }

  private static TtlRegisterData getState(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(WIDTH));
      state.setData(data);
    }
    return data;
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
  }

  /**
   * Parallel load wins over a rising edge. Counting uses the stored word and the up/down input.
   * A high count enable, or no rising edge, holds the stored word.
   */
  private static Value resolveCount(
      Value stored,
      boolean triggered,
      Value loadPin,
      Value enablePin,
      Value direction,
      Value data0,
      Value data1,
      Value data2,
      Value data3) {
    final var sources =
        new Value[] {
          loadPin,
          enablePin,
          direction,
          data0,
          data1,
          data2,
          data3,
          stored.get(0),
          stored.get(1),
          stored.get(2),
          stored.get(3)
        };
    final var choice = new Choice(WIDTH);
    for (var mask = 0; mask < (1 << COUNT_BITS); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      final var loadHigh = bitHigh(mask, BIT_LOAD_HIGH);
      final var counting = triggered && !bitHigh(mask, BIT_ENABLE_HIGH);
      final var down = bitHigh(mask, BIT_DOWN);
      final var data = (mask >> BIT_DATA) & 0xF;
      final var current = (mask >> BIT_STORED) & 0xF;
      final int next;
      if (!loadHigh) {
        next = data;
      } else if (counting) {
        next = nextCount(current, down);
      } else {
        next = current;
      }
      choice.accept(next, errorIn(sources));
    }
    return choice.value();
  }

  /** Terminal count and ripple clock are combinational functions of the current word. */
  private static Value resolveFlags(Value count, Value enablePin, Value direction, Value clock) {
    final var sources =
        new Value[] {
          enablePin, direction, clock, count.get(0), count.get(1), count.get(2), count.get(3)
        };
    final var choice = new Choice(2);
    for (var mask = 0; mask < (1 << sources.length); mask++) {
      if (!accepts(sources, mask)) {
        continue;
      }
      final var enabled = !bitHigh(mask, 0);
      final var down = bitHigh(mask, 1);
      final var clockHigh = bitHigh(mask, 2);
      final var code = (mask >> 3) & 0xF;
      final var terminal = down ? code == 0 : (code & 0x9) == 0x9;
      final var rippleHigh = !(terminal && enabled && !clockHigh);
      choice.accept((terminal ? 1 : 0) | (rippleHigh ? 2 : 0), errorIn(sources));
    }
    return choice.value();
  }

  private static boolean accepts(Value[] sources, int mask) {
    for (var index = 0; index < sources.length; index++) {
      final var actual = sources[index];
      final var high = bitHigh(mask, index);
      if (actual == Value.TRUE && !high) {
        return false;
      }
      if (actual == Value.FALSE && high) {
        return false;
      }
    }
    return true;
  }

  private static boolean errorIn(Value[] sources) {
    for (final var source : sources) {
      if (source == Value.ERROR) {
        return true;
      }
    }
    return false;
  }

  private static boolean bitHigh(int mask, int bit) {
    return (mask & (1 << bit)) != 0;
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {pinNrToPortNr(CP)};
  }

  /** Merges every accepted substitution. Disagreements become unknown, or error if one was seen. */
  private static final class Choice {
    private final Value[] bits;
    private final boolean[] conflict;
    private boolean sawError;
    private boolean any;

    private Choice(int width) {
      bits = new Value[width];
      conflict = new boolean[width];
    }

    private void accept(int value, boolean error) {
      sawError |= error;
      if (!any) {
        any = true;
        for (var index = 0; index < bits.length; index++) {
          bits[index] = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        }
        return;
      }
      for (var index = 0; index < bits.length; index++) {
        final var bit = ((value >> index) & 1) == 0 ? Value.FALSE : Value.TRUE;
        if (bits[index] != bit) {
          conflict[index] = true;
        }
      }
    }

    private Value value() {
      if (!any) {
        return Value.createUnknown(BitWidth.create(bits.length));
      }
      for (var index = 0; index < bits.length; index++) {
        if (conflict[index]) {
          bits[index] = sawError ? Value.ERROR : Value.UNKNOWN;
        }
      }
      return Value.create(bits);
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
