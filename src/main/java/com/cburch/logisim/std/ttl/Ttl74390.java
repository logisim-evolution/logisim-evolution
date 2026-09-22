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
import java.awt.Graphics2D;

/**
 * TTL 74x390: dual decade ripple counter.
 *
 * <p>Model based on the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT390.pdf">74HC390 datasheet</a>.
 * Each half contains a divide-by-2 section ({@code nQ0}, falling edge of {@code nCP0}) and a
 * divide-by-5 section ({@code nQ1} through {@code nQ3}, falling edge of {@code nCP1}). A HIGH
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
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNamesByPin(
        painter,
        x,
        y,
        height,
        new String[] {
          "1CP0", "1MR", "1Q0", "1CP1", "1Q1", "1Q2", "1Q3", null,
          "2Q3", "2Q2", "2Q1", "2CP1", "2Q0", "2MR", "2CP0", null
        });
    drawState(gfx, x, y, height, (TtlRegisterData) painter.getData());
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state == null) return;
    for (var half = 0; half < HALVES; half++) {
      final var originX = x + 32 + half * 56;
      for (var i = 0; i < 4; i++) {
        final var bit = state.getValue(half).get(3 - i);
        gfx.setColor(bit.getColor());
        gfx.fillOval(originX + i * 10, y + height / 2 - 4, 8, 8);
        gfx.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(
            gfx, bit.toDisplayString(), originX + 4 + i * 10, y + height / 2);
      }
    }
    gfx.setColor(Color.BLACK);
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
   * Advances Q1..Q3 through the documented divide-by-5 sequence 0, 1, 2, 3, 4. An error among
   * those bits makes all three error. Any other undefined code, including the unspecified codes
   * 5..7, makes them unknown.
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
    final var code =
        (q1 == Value.TRUE ? 1 : 0) + (q2 == Value.TRUE ? 2 : 0) + (q3 == Value.TRUE ? 4 : 0);
    if (code > 4) {
      setDivideByFive(bits, Value.UNKNOWN);
      return;
    }
    final var next = code == 4 ? 0 : code + 1;
    bits[1] = bit(next, 1);
    bits[2] = bit(next, 2);
    bits[3] = bit(next, 4);
  }

  private static void setDivideByFive(Value[] bits, Value value) {
    bits[1] = value;
    bits[2] = value;
    bits[3] = value;
  }

  private static Value bit(int code, int mask) {
    return (code & mask) == 0 ? Value.FALSE : Value.TRUE;
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
