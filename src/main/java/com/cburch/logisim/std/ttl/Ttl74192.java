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
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
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
  public static final int PORT_INDEX_DOWN = 3;
  public static final int PORT_INDEX_UP = 4;
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

  private static final BitWidth WIDTH = BitWidth.create(4);
  private final int maxVal;

  public Ttl74192() {
    this(_ID, 9);
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
        final var data = (UpDownCounterData) state.getData();
        if (data == null) return;
        var current = data.getValue().toLongValue();
        final var bitValue = 1L << index;
        current ^= bitValue;
        updateState(state, Value.createKnown(WIDTH, current), Value.FALSE, Value.FALSE, Value.FALSE, Value.FALSE);
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
    UpDownCounterData data = (UpDownCounterData) painter.getData();
    drawState(gfx, x, y, height, data);
  }

  private void drawState(Graphics2D gfx, int x, int y, int height, UpDownCounterData state) {
    if (state == null) return;
    var value = state.getValue();
    for (var i = 0; i < 4; i++) {
      final var bitValue = value.get(3 - i);
      gfx.setColor(bitValue.getColor());
      gfx.fillOval(x + 52 + i * 10, y + height / 2 - 4, 8, 8);
      gfx.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(gfx, bitValue == Value.TRUE ? "1" : "0", x + 56 + i * 10, y + height / 2);
    }
    gfx.setColor(Color.BLACK);
  }

  public static void updateState(InstanceState state, Value value, Value carry, Value borrow, Value down, Value up) {
    final var data = getStateData(state);

    data.setAll(value, carry, borrow, down, up);
    final var vA = data.getValue().get(0);
    final var vB = data.getValue().get(1);
    final var vC = data.getValue().get(2);
    final var vD = data.getValue().get(3);
    final var vCar = data.getCarry();
    final var vBor = data.getBorrow();

    state.setPort(PORT_INDEX_QA, vA, 4);
    state.setPort(PORT_INDEX_QB, vB, 4);
    state.setPort(PORT_INDEX_QC, vC, 4);
    state.setPort(PORT_INDEX_QD, vD, 4);
    state.setPort(PORT_INDEX_CARRY, vCar, 4);
    state.setPort(PORT_INDEX_BORROW, vBor, 4);
  }

  public static UpDownCounterData getStateData(InstanceState state) {
    var data = (UpDownCounterData) state.getData();
    if (data == null) {
      data = new UpDownCounterData();
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);

    var carry = Value.TRUE;
    var borrow = Value.TRUE;
    var counter = data.getValue().toLongValue();

    final var downPrev = data.getDownPrev();
    final var upPrev = data.getUpPrev();
    final var downCur = state.getPortValue(PORT_INDEX_DOWN);
    final var upCur = state.getPortValue(PORT_INDEX_UP);
    final var downFalling = downPrev == Value.TRUE && downCur == Value.FALSE;
    final var downRising = downPrev == Value.FALSE && downCur == Value.TRUE;
    final var upFalling = upPrev == Value.TRUE && upCur == Value.FALSE;
    final var upRising = upPrev == Value.FALSE && upCur == Value.TRUE;
    final var downUnchangedHigh = downPrev == Value.TRUE && downCur == Value.TRUE;
    final var upUnchangedHigh = upPrev == Value.TRUE && upCur == Value.TRUE;

    if (state.getPortValue(PORT_INDEX_CLEAR) == Value.TRUE) { // reset
      counter = 0;
    } else if (state.getPortValue(PORT_INDEX_LOAD) == Value.FALSE) { // load value
      var inputValue = state.getPortValue(PORT_INDEX_A).toLongValue();
      inputValue += state.getPortValue(PORT_INDEX_B).toLongValue() << 1;
      inputValue += state.getPortValue(PORT_INDEX_C).toLongValue() << 2;
      inputValue += state.getPortValue(PORT_INDEX_D).toLongValue() << 3;
      counter = inputValue > maxVal ? 0 : inputValue; // TODO: not sure
    } else if (downRising && upUnchangedHigh) { // count down
      counter--;
      if (counter < 0) {
        counter = maxVal;
      }
    } else if (upRising && downUnchangedHigh) { // count up
      counter++;
      if (counter > maxVal) {
        counter = 0;
      }
    } else if (upFalling && downUnchangedHigh) { // carry
      if (counter == maxVal) {
        carry = Value.FALSE;
      }
    } else if (downFalling && upUnchangedHigh) { // borrow
      if (counter == 0) {
        borrow = Value.FALSE;
      }
    } else {  // state does not change
      carry = data.getCarry();
      borrow = data.getBorrow();
    }
    updateState(state, Value.createKnown(WIDTH, counter), carry, borrow, downCur, upCur);
  }
}
