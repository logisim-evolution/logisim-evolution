/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import static com.cburch.logisim.data.Value.falseColor;
import static com.cburch.logisim.data.Value.trueColor;

import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public class Ttl74192 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74192";

  public static final int PORT_INDEX_B = 0;
  public static final int PORT_INDEX_QB = 1;
  public static final int PORT_INDEX_QA = 2;
  public static final int PORT_INDEX_CntD = 3;
  public static final int PORT_INDEX_CntU = 4;
  public static final int PORT_INDEX_QC = 5;
  public static final int PORT_INDEX_QD = 6;
  public static final int PORT_INDEX_D = 7;
  public static final int PORT_INDEX_C = 8;
  public static final int PORT_INDEX_LOAD = 9;
  public static final int PORT_INDEX_CARRY = 10;
  public static final int PORT_INDEX_BORROW = 11;
  public static final int PORT_INDEX_CLEAR = 12;
  public static final int PORT_INDEX_A = 13;
  private static final String[] PORT_NAMES = {
      "Data Input A",
      "Data Output B",
      "Data Output A",
      "Count Down",
      "Count Up",
      "Data Output C",
      "Data Output D",
      "Data Input D",
      "Data Input C",
      "Load",
      "Carry",
      "Borrow",
      "Clear",
      "Data Input A"
  };
  private static final byte[] OUTPUT_PORTS = {2, 3, 6, 7, 12, 13};

  private final int maxVal;

  private Value cntD_prev = Value.NIL;
  private Value cntU_prev = Value.NIL;

  public Ttl74192() {
    super(_ID, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
    maxVal = 9;
  }

  public Ttl74192(String name, int maxVal) {
    super(name, (byte) 16, OUTPUT_PORTS, PORT_NAMES, null);
    super.setInstancePoker(Poker.class);
    this.maxVal = maxVal;
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var inside = false;
      for (var i = 0; i < 4; i++) {
        final var dx = p.x - (56 + i * 10);
        final var dy = p.y - 30;
        final var d2 = dx * dx + dy * dy;
        inside |= (d2 < 4 * 4);
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      for (var i = 0; i < 4; i++) {
        int dx = p.x - (56 + i * 10);
        int dy = p.y - 30;
        int d2 = dx * dx + dy * dy;
        if (d2 < 4 * 4) return 3 - i;
      }
      return 0;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE).booleanValue()) return;
      if (isPressed && isInside(state, e)) {
        var index = getIndex(state, e);
        final var data = (TtlRegisterData) state.getData();
        if (data == null) return;
        var current = data.getValue().toLongValue();
        final long bitValue = 1 << index;
        current ^= bitValue;
        updateState(state, current, Value.FALSE, Value.FALSE);
      }
      isPressed = false;
    }
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var gfx = (Graphics2D) painter.getGraphics();
    super.paintBase(painter, false, false);
    Drawgates.paintPortNames(
        painter,
        x,
        y,
        height,
        new String[] {
          "B", "QB", "QA", "CntD", "CntU", "QC", "QD", "D", "C", "LOAD", "CAR", "BOR", "CLR", "A"
        });
    TtlRegisterData data = (TtlRegisterData) painter.getData();
    drawState(gfx, x, y, height, data);
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, TtlRegisterData state) {
    if (state != null) {
      long value = state.getValue().toLongValue();
      for (var i = 0; i < 4; i++) {
        final var isSetBitValue = (value & (1 << (3 - i))) != 0;
        gfx.setColor(isSetBitValue ? trueColor : falseColor);
        gfx.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
        gfx.setColor(Color.WHITE);
        GraphicsUtil.drawCenteredText(gfx, isSetBitValue ? "1" : "0", x + 56 + i * 10, y + height / 2);
      }
      gfx.setColor(Color.BLACK);
    }
  }

  public static void updateState(InstanceState state, Long value, Value carry, Value borrow) {
    var data = getStateData(state);

    data.setValue(Value.createKnown(BitWidth.create(6), value + (carry.toLongValue() << 4) + (borrow.toLongValue() << 5)));
    final var vA = data.getValue().get(0);
    final var vB = data.getValue().get(1);
    final var vC = data.getValue().get(2);
    final var vD = data.getValue().get(3);
    final var vCar = data.getValue().get(4);
    final var vBor = data.getValue().get(5);

    state.setPort(PORT_INDEX_QA, vA, 4);
    state.setPort(PORT_INDEX_QB, vB, 4);
    state.setPort(PORT_INDEX_QC, vC, 4);
    state.setPort(PORT_INDEX_QD, vD, 4);
    state.setPort(PORT_INDEX_CARRY, vCar, 4);
    state.setPort(PORT_INDEX_BORROW, vBor, 4);
  }

  public static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(6)); // 4 Data + carry + borrow
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = getStateData(state);

    var carry = Value.TRUE;
    var borrow = Value.TRUE;
    var counter = data.getValue().get(0).toLongValue();
    counter += data.getValue().get(1).toLongValue() << 1;
    counter += data.getValue().get(2).toLongValue() << 2;
    counter += data.getValue().get(3).toLongValue() << 3;

    var cntD_cur = state.getPortValue(PORT_INDEX_CntD);
    var cntU_cur = state.getPortValue(PORT_INDEX_CntU);
    var cntD_falling = cntD_prev == Value.TRUE && cntD_cur == Value.FALSE;
    var cntD_rising = cntD_prev == Value.FALSE && cntD_cur == Value.TRUE;
    var cntU_falling = cntU_prev == Value.TRUE && cntU_cur == Value.FALSE;
    var cntU_rising = cntU_prev == Value.FALSE && cntU_cur == Value.TRUE;
    var cntD_unchanged_high = cntD_prev == Value.TRUE && cntD_cur == Value.TRUE;
    var cntU_unchanged_high = cntU_prev == Value.TRUE && cntU_cur == Value.TRUE;
    cntD_prev = cntD_cur;
    cntU_prev = cntU_cur;

    if (state.getPortValue(PORT_INDEX_CLEAR) == Value.TRUE) { // reset
      counter = 0;
    } else if (state.getPortValue(PORT_INDEX_LOAD) == Value.FALSE) { // load value
      var inputValue = state.getPortValue(PORT_INDEX_A).toLongValue();
      inputValue += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
      inputValue += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
      inputValue += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      counter = inputValue > maxVal ? 0 : inputValue; // TODO: not sure
    } else if (cntD_rising && cntU_unchanged_high) { // count down
      counter--;
      if (counter < 0) {
        counter = maxVal;
      }
    }else if (cntU_rising && cntD_unchanged_high) { // count up
      counter++;
      if (counter > maxVal) {
        counter = 0;
      }
    } else if (cntU_falling && cntD_unchanged_high) { // carry
      if (counter == maxVal) {
        carry = Value.FALSE;
      }
    } else if (cntD_falling && cntU_unchanged_high) { // borrow
      if (counter == 0) {
        borrow = Value.FALSE;
      }
    } else {  // state does not change
      carry = data.getValue().get(4);
      borrow = data.getValue().get(5);
    }
    updateState(state, counter, carry, borrow);
  }
}
