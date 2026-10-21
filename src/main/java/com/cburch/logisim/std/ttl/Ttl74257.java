/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.ttl;

import java.awt.Color;
import java.awt.Graphics;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;

/**
 * TTL 74x257: quad 2-line to 1-line data selector with 3-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT257.pdf">Nexperia 74HC257</a>
 * and <a href="https://www.ti.com/lit/ds/symlink/sn74hc257.pdf">TI SN74HC257</a> function tables.
 * A low output enable passes the selected source without inversion. A high output enable releases
 * every output. The common select input chooses source 1 when high and source 0 when low. Pin
 * names follow Nexperia; TI's SN74HC257 calls the same pins A/B, A, B, Y and G. Nanosecond delays
 * are not modeled.
 *
 * <p>An unknown or error input changes an output only when the accepted substitutions disagree. An
 * error on such an input makes the disagreed output an error; an unknown input makes it unknown. A
 * high output enable releases the outputs even if a data input is an error. A released output is
 * {@link Value#UNKNOWN}, which is how this library represents high impedance.
 */
public class Ttl74257 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74257";

  public static final int DELAY = 1;

  public static final byte SELECT = 1;
  public static final byte L1_I0 = 2;
  public static final byte L1_I1 = 3;
  public static final byte L1_Y = 4;
  public static final byte L2_I0 = 5;
  public static final byte L2_I1 = 6;
  public static final byte L2_Y = 7;
  public static final byte GND = 8;
  public static final byte L3_Y = 9;
  public static final byte L3_I1 = 10;
  public static final byte L3_I0 = 11;
  public static final byte L4_Y = 12;
  public static final byte L4_I1 = 13;
  public static final byte L4_I0 = 14;
  public static final byte OE = 15;
  public static final byte VCC = 16;

  /** Each row is source 0, source 1, and the output of one multiplexer. */
  private static final byte[][] CHANNELS = {
    {L1_I0, L1_I1, L1_Y},
    {L2_I0, L2_I1, L2_Y},
    {L3_I0, L3_I1, L3_Y},
    {L4_I0, L4_I1, L4_Y}
  };

  private static final byte[] OUTPUTS = {L1_Y, L2_Y, L3_Y, L4_Y};

  private static final String[] PORT_NAMES = {
    "S (Select)",
    "1I0",
    "1I1",
    "1Y",
    "2I0",
    "2I1",
    "2Y",
    "3Y",
    "3I1",
    "3I0",
    "4Y",
    "4I1",
    "4I0",
    "nOE (Output enable, active low)"
  };

  /** Creates a 74257 quad 2-line to 1-line data selector with 3-state outputs. */
  public Ttl74257() {
    super(_ID, (byte) 16, OUTPUTS, PORT_NAMES, new Ttl74257HdlGenerator());
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
    g.drawLine((x + 30), (y + 34), (x + 50), (y + 18));
    g.drawLine((x + 70), (y + 34), (x + 50), (y + 18));
    g.drawLine((x + 30), (y + 34), (x + 70), (y + 34));
    g.drawLine((x + 50), (y + 18), (x + 70), (y + height - 7));
    g.drawLine((x + 30), (y + height - 7), (x + 30), (y + 34));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + 34));
    g.drawLine((x + 90), (y + 34), (x + 110), (y + 18));
    g.drawLine((x + 130), (y + 34), (x + 110), (y + 18));
    g.drawLine((x + 90), (y + 34), (x + 130), (y + 34));
    g.drawLine((x + 110), (y + 18), (x + 130), (y + height - 7));
    g.drawLine((x + 90), (y + height - 7), (x + 90), (y + 34));
    g.drawLine((x + 110), (y + height - 7), (x + 110), (y + 34));
    g.drawLine((x + 110), (y + 34), (x + 130), (y + 18));
    g.drawLine((x + 150), (y + 34), (x + 130), (y + 18));
    g.drawLine((x + 110), (y + 34), (x + 150), (y + 34));
    g.drawLine((x + 130), (y + 18), (x + 150), (y + 7));
    g.drawLine((x + 110), (y + 7), (x + 110), (y + 34));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 34));
    g.drawLine((x + 50), (y + 34), (x + 70), (y + 18));
    g.drawLine((x + 90), (y + 34), (x + 70), (y + 18));
    g.drawLine((x + 50), (y + 34), (x + 90), (y + 34));
    g.drawLine((x + 70), (y + 18), (x + 90), (y + 7));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 34));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 34));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 16));
    GraphicsUtil.drawCenteredText(g, "S", (x + 10), (y + 16));
    g.drawLine((x + 30), (y + 7), (x + 30), (y + 40));
    structureBubble(g, (x + 30), (y + 7));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var outputEnable = state.getPortValue(pinNrToPortNr(OE));
    final var select = state.getPortValue(pinNrToPortNr(SELECT));
    for (final var channel : CHANNELS) {
      final var source0 = state.getPortValue(pinNrToPortNr(channel[0]));
      final var source1 = state.getPortValue(pinNrToPortNr(channel[1]));
      state.setPort(
          pinNrToPortNr(channel[2]), channelOutput(outputEnable, select, source0, source1), DELAY);
    }
  }

  /**
   * Resolves one multiplexer output, including unknown and error substitutions.
   *
   * <p>A definitely high output enable releases the pin and does not consult the data inputs.
   */
  private static Value channelOutput(
      Value outputEnable, Value select, Value source0, Value source1) {
    if (outputEnable == Value.TRUE) {
      return Value.UNKNOWN;
    }
    final var driven = selected(select, source0, source1);
    if (outputEnable == Value.FALSE) {
      return driven;
    }
    if (driven == Value.UNKNOWN && outputEnable == Value.UNKNOWN) {
      return Value.UNKNOWN;
    }
    if (outputEnable == Value.ERROR || driven == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
  }

  /** Returns the source chosen by select, or the value both sources share when select is not binary. */
  private static Value selected(Value select, Value source0, Value source1) {
    if (select == Value.TRUE) {
      return source1;
    }
    if (select == Value.FALSE) {
      return source0;
    }
    if (source0 == source1) {
      return source0;
    }
    if (select == Value.ERROR || source0 == Value.ERROR || source1 == Value.ERROR) {
      return Value.ERROR;
    }
    return Value.UNKNOWN;
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
