/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Graphics;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74HC259: 8-bit addressable latch.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT259.pdf">Nexperia 74HC259</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc259.pdf">TI SN74HC259</a> function tables.
 * Nexperia names the controls {@code LE} and {@code MR}; TI names the same pins {@code G} and
 * {@code CLR}. Both are active low. Nanosecond delays are not modeled.
 *
 * <p>{@code MR} low and {@code LE} high clears every latch. Both low make the addressed output
 * follow {@code D} and clear every other latch. {@code MR} high and {@code LE} low make only the
 * addressed latch follow {@code D}. Both high hold the latches. A control that is not exactly high
 * or low does not write. An address bit that is not exactly high or low makes each latch that
 * might still be selected combine the selected and unselected results: a 0/1 conflict is an error,
 * and any other disagreement is unknown.
 */
public class Ttl74259 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74259";

  public static final int DELAY = 1;

  public static final byte A0 = 1;
  public static final byte A1 = 2;
  public static final byte A2 = 3;
  public static final byte Q0 = 4;
  public static final byte Q1 = 5;
  public static final byte Q2 = 6;
  public static final byte Q3 = 7;
  public static final byte GND = 8;
  public static final byte Q4 = 9;
  public static final byte Q5 = 10;
  public static final byte Q6 = 11;
  public static final byte Q7 = 12;
  public static final byte D = 13;
  /** Latch enable, active low. TI calls this pin G. */
  public static final byte LE = 14;
  /** Conditional reset, active low. TI calls this pin CLR. */
  public static final byte MR = 15;
  public static final byte VCC = 16;

  private static final int BITS = 8;
  private static final byte[] ADDRESS = {A0, A1, A2};
  private static final byte[] OUTPUTS = {Q0, Q1, Q2, Q3, Q4, Q5, Q6, Q7};
  private static final String[] PORT_NAMES = {
    "A0 address",
    "A1 address",
    "A2 address",
    "Q0",
    "Q1",
    "Q2",
    "Q3",
    "Q4",
    "Q5",
    "Q6",
    "Q7",
    "D data",
    "LE / G (latch enable, active low)",
    "MR / CLR (conditional reset, active low)"
  };

  /** Creates a 74259 8-bit addressable latch. */
  public Ttl74259() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74259HdlGenerator());
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

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 64), (y + height - 28), 12, 14);
    g.drawLine((x + 70), ((y + height - 28)) + 14, (x + 70), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 70), ((y + height - 28)) + 8);
    structureDot(g, (x + 70), ((y + height - 28)) + 7, stored, 0, 0);
    g.drawRect((x + 84), (y + height - 28), 12, 14);
    g.drawLine((x + 90), ((y + height - 28)) + 14, (x + 90), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 90), ((y + height - 28)) + 8);
    g.drawRect((x + 104), (y + height - 28), 12, 14);
    g.drawLine((x + 110), ((y + height - 28)) + 14, (x + 110), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 110), ((y + height - 28)) + 8);
    g.drawRect((x + 124), (y + height - 28), 12, 14);
    g.drawLine((x + 130), ((y + height - 28)) + 14, (x + 130), (y + height - 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 130), ((y + height - 28)) + 8);
    g.drawRect((x + 144), (y + 12), 12, 14);
    g.drawLine((x + 150), (y + 12), (x + 150), (y + 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 150), ((y + 12)) + 8);
    g.drawRect((x + 124), (y + 12), 12, 14);
    g.drawLine((x + 130), (y + 12), (x + 130), (y + 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 130), ((y + 12)) + 8);
    g.drawRect((x + 104), (y + 12), 12, 14);
    g.drawLine((x + 110), (y + 12), (x + 110), (y + 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 110), ((y + 12)) + 8);
    g.drawRect((x + 84), (y + 12), 12, 14);
    g.drawLine((x + 90), (y + 12), (x + 90), (y + 7));
    GraphicsUtil.drawCenteredText(g, "L", (x + 90), ((y + 12)) + 8);
    g.drawLine((x + 64), (y + 28), (x + 156), (y + 28));
    g.drawLine((x + 70), (y + 28), (x + 70), (y + 26));
    g.drawLine((x + 90), (y + 28), (x + 90), (y + 26));
    g.drawLine((x + 110), (y + 28), (x + 110), (y + 26));
    g.drawLine((x + 130), (y + 28), (x + 130), (y + 26));
    g.drawLine((x + 150), (y + 28), (x + 150), (y + 26));
    g.drawLine((x + 130), (y + 28), (x + 130), (y + 26));
    g.drawLine((x + 110), (y + 28), (x + 110), (y + 26));
    g.drawLine((x + 90), (y + 28), (x + 90), (y + 26));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 64), (y + 13));
    g.drawLine((x + 64), (y + 13), (x + 64), (y + 28));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 30));
    structureBubble(g, (x + 30), (y + 7));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = stateData(state);
    final var masterReset = input(state, MR);
    final var latchEnable = input(state, LE);
    if (masterReset == Value.FALSE && latchEnable == Value.TRUE) {
      clearAll(data);
    } else if (masterReset == Value.FALSE && latchEnable == Value.FALSE) {
      writeTransparent(state, data, true);
    } else if (masterReset == Value.TRUE && latchEnable == Value.FALSE) {
      writeTransparent(state, data, false);
    }
    publish(state, data);
  }

  private static TtlRegisterData stateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    return data;
  }

  private static void clearAll(TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      data.setValue(index, Value.FALSE);
    }
  }

  /**
   * Writes the transparent mode. In demultiplexer mode every unselected latch is cleared. In
   * addressable-latch mode an unselected latch keeps the value it already stores.
   */
  private static void writeTransparent(InstanceState state, TtlRegisterData data, boolean clearUnselected) {
    final var dataIn = input(state, D);
    for (var index = 0; index < BITS; index++) {
      final var idle = clearUnselected ? Value.FALSE : data.getValue(index);
      data.setValue(index, levelFor(state, index, dataIn, idle));
    }
  }

  private static Value levelFor(InstanceState state, int index, Value dataIn, Value idle) {
    if (addressExcludes(state, index)) return idle;
    if (addressSelects(state, index)) return dataIn;
    return combine(dataIn, idle);
  }

  private static boolean addressExcludes(InstanceState state, int index) {
    for (var place = 0; place < ADDRESS.length; place++) {
      final var bit = input(state, ADDRESS[place]);
      final var expectedHigh = ((index >> place) & 1) == 1;
      if (expectedHigh && bit == Value.FALSE) return true;
      if (!expectedHigh && bit == Value.TRUE) return true;
    }
    return false;
  }

  private static boolean addressSelects(InstanceState state, int index) {
    if (addressExcludes(state, index)) return false;
    for (final var pin : ADDRESS) {
      final var bit = input(state, pin);
      if (bit != Value.TRUE && bit != Value.FALSE) return false;
    }
    return true;
  }

  private static Value combine(Value whenSelected, Value whenIdle) {
    if (whenSelected == whenIdle) return whenSelected;
    if (whenSelected == Value.ERROR || whenIdle == Value.ERROR) return Value.ERROR;
    if (whenSelected.isFullyDefined() && whenIdle.isFullyDefined()) return Value.ERROR;
    return Value.UNKNOWN;
  }

  private static void publish(InstanceState state, TtlRegisterData data) {
    for (var index = 0; index < BITS; index++) {
      state.setPort(pinNrToPortNr(OUTPUTS[index]), data.getValue(index), DELAY);
    }
  }

  private static Value input(InstanceState state, byte pin) {
    return state.getPortValue(pinNrToPortNr(pin));
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
