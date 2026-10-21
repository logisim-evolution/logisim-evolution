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
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;

/**
 * TTL 74HC374: octal positive-edge D flip-flop with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc374.pdf">TI SN74HC374</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT374.pdf">Nexperia 74HC374</a>
 * data sheets. Each flip-flop stores its D input on the rising edge of CLK. A low nOE drives the
 * stored bit onto Q; a high nOE, or any level that is not a solid low, releases every Q. Output
 * enable does not change the register, and the register still loads while the outputs are
 * released. High impedance is reported as unknown. Pin names follow the TI numbering used by the
 * 74273 and 74377 in this library, where Q1 is Nexperia's Q0 and D1 is Nexperia's D0.
 */
public class Ttl74374 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value MUST be unique string among all tools.
   */
  public static final String _ID = "74374";

  public static final int PORT_INDEX_nOE = 0;
  public static final int PORT_INDEX_Q1 = 1;
  public static final int PORT_INDEX_D1 = 2;
  public static final int PORT_INDEX_D2 = 3;
  public static final int PORT_INDEX_Q2 = 4;
  public static final int PORT_INDEX_Q3 = 5;
  public static final int PORT_INDEX_D3 = 6;
  public static final int PORT_INDEX_D4 = 7;
  public static final int PORT_INDEX_Q4 = 8;
  public static final int PORT_INDEX_CLK = 9;
  public static final int PORT_INDEX_Q5 = 10;
  public static final int PORT_INDEX_D5 = 11;
  public static final int PORT_INDEX_D6 = 12;
  public static final int PORT_INDEX_Q6 = 13;
  public static final int PORT_INDEX_Q7 = 14;
  public static final int PORT_INDEX_D7 = 15;
  public static final int PORT_INDEX_D8 = 16;
  public static final int PORT_INDEX_Q8 = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** Data port of each bit. Bit 0 is D1, which is Nexperia's D0. */
  private static final int[] DATA = {
    PORT_INDEX_D1,
    PORT_INDEX_D2,
    PORT_INDEX_D3,
    PORT_INDEX_D4,
    PORT_INDEX_D5,
    PORT_INDEX_D6,
    PORT_INDEX_D7,
    PORT_INDEX_D8
  };
  /** Output port of each bit. Bit 0 is Q1, which is Nexperia's Q0. */
  private static final int[] OUTPUTS = {
    PORT_INDEX_Q1,
    PORT_INDEX_Q2,
    PORT_INDEX_Q3,
    PORT_INDEX_Q4,
    PORT_INDEX_Q5,
    PORT_INDEX_Q6,
    PORT_INDEX_Q7,
    PORT_INDEX_Q8
  };
  private static final byte[] OUTPUT_PINS = {2, 5, 6, 9, 12, 15, 16, 19};
  private static final String[] PORT_NAMES = {
    "nOE (output enable, active low)",
    "Q1",
    "D1",
    "D2",
    "Q2",
    "Q3",
    "D3",
    "D4",
    "Q4",
    "CLK",
    "Q5",
    "D5",
    "D6",
    "Q6",
    "Q7",
    "D7",
    "D8",
    "Q8"
  };

  /** Creates a 74374 octal positive-edge D flip-flop with three-state outputs. */
  public Ttl74374() {
    super(_ID, (byte) 20, OUTPUT_PINS, PORT_NAMES, new Ttl74374HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    final var rawState = painter.getData();
    final TtlRegisterData stored = rawState instanceof TtlRegisterData ? (TtlRegisterData) rawState : null;
    g.drawRect((x + 24), (y + height - 28), 12, 14);
    g.drawLine((x + 30), ((y + height - 28)) + 14, (x + 30), (y + height - 7));
    g.drawLine((x + 50), (y + height - 7), (x + 50), (y + height - 13));
    g.drawLine((x + 50), (y + height - 13), (x + 30), (y + height - 13));
    g.drawLine((x + 30), (y + height - 13), (x + 30), (y + height - 28));
    g.drawLine((x + 24), (((y + height - 28)) + 7) - 3, ((x + 24)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 24), (((y + height - 28)) + 7) + 3, ((x + 24)) + 4, ((y + height - 28)) + 7);
    structureDot(g, (x + 30), ((y + height - 28)) + 7, stored, 0, 0);
    g.drawRect((x + 84), (y + height - 28), 12, 14);
    g.drawLine((x + 90), ((y + height - 28)) + 14, (x + 90), (y + height - 7));
    g.drawLine((x + 70), (y + height - 7), (x + 70), (y + height - 13));
    g.drawLine((x + 70), (y + height - 13), (x + 90), (y + height - 13));
    g.drawLine((x + 90), (y + height - 13), (x + 90), (y + height - 28));
    g.drawLine((x + 84), (((y + height - 28)) + 7) - 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 84), (((y + height - 28)) + 7) + 3, ((x + 84)) + 4, ((y + height - 28)) + 7);
    g.drawRect((x + 104), (y + height - 28), 12, 14);
    g.drawLine((x + 110), ((y + height - 28)) + 14, (x + 110), (y + height - 7));
    g.drawLine((x + 130), (y + height - 7), (x + 130), (y + height - 13));
    g.drawLine((x + 130), (y + height - 13), (x + 110), (y + height - 13));
    g.drawLine((x + 110), (y + height - 13), (x + 110), (y + height - 28));
    g.drawLine((x + 104), (((y + height - 28)) + 7) - 3, ((x + 104)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 104), (((y + height - 28)) + 7) + 3, ((x + 104)) + 4, ((y + height - 28)) + 7);
    g.drawRect((x + 164), (y + height - 28), 12, 14);
    g.drawLine((x + 170), ((y + height - 28)) + 14, (x + 170), (y + height - 7));
    g.drawLine((x + 150), (y + height - 7), (x + 150), (y + height - 13));
    g.drawLine((x + 150), (y + height - 13), (x + 170), (y + height - 13));
    g.drawLine((x + 170), (y + height - 13), (x + 170), (y + height - 28));
    g.drawLine((x + 164), (((y + height - 28)) + 7) - 3, ((x + 164)) + 4, ((y + height - 28)) + 7);
    g.drawLine((x + 164), (((y + height - 28)) + 7) + 3, ((x + 164)) + 4, ((y + height - 28)) + 7);
    g.drawRect((x + 164), (y + 12), 12, 14);
    g.drawLine((x + 170), (y + 12), (x + 170), (y + 7));
    g.drawLine((x + 150), (y + 7), (x + 150), (y + 13));
    g.drawLine((x + 150), (y + 13), (x + 170), (y + 13));
    g.drawLine((x + 170), (y + 13), (x + 170), ((y + 12)) + 14);
    g.drawLine((x + 164), (((y + 12)) + 7) - 3, ((x + 164)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 164), (((y + 12)) + 7) + 3, ((x + 164)) + 4, ((y + 12)) + 7);
    g.drawRect((x + 104), (y + 12), 12, 14);
    g.drawLine((x + 110), (y + 12), (x + 110), (y + 7));
    g.drawLine((x + 130), (y + 7), (x + 130), (y + 13));
    g.drawLine((x + 130), (y + 13), (x + 110), (y + 13));
    g.drawLine((x + 110), (y + 13), (x + 110), ((y + 12)) + 14);
    g.drawLine((x + 104), (((y + 12)) + 7) - 3, ((x + 104)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 104), (((y + 12)) + 7) + 3, ((x + 104)) + 4, ((y + 12)) + 7);
    g.drawRect((x + 84), (y + 12), 12, 14);
    g.drawLine((x + 90), (y + 12), (x + 90), (y + 7));
    g.drawLine((x + 70), (y + 7), (x + 70), (y + 13));
    g.drawLine((x + 70), (y + 13), (x + 90), (y + 13));
    g.drawLine((x + 90), (y + 13), (x + 90), ((y + 12)) + 14);
    g.drawLine((x + 84), (((y + 12)) + 7) - 3, ((x + 84)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 84), (((y + 12)) + 7) + 3, ((x + 84)) + 4, ((y + 12)) + 7);
    g.drawRect((x + 24), (y + 12), 12, 14);
    g.drawLine((x + 30), (y + 12), (x + 30), (y + 7));
    g.drawLine((x + 50), (y + 7), (x + 50), (y + 13));
    g.drawLine((x + 50), (y + 13), (x + 30), (y + 13));
    g.drawLine((x + 30), (y + 13), (x + 30), ((y + 12)) + 14);
    g.drawLine((x + 24), (((y + 12)) + 7) - 3, ((x + 24)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 24), (((y + 12)) + 7) + 3, ((x + 24)) + 4, ((y + 12)) + 7);
    g.drawLine((x + 24), (y + 28), (x + 176), (y + 28));
    g.drawLine((x + 30), (y + 28), (x + 30), (y + 26));
    g.drawLine((x + 90), (y + 28), (x + 90), (y + 26));
    g.drawLine((x + 110), (y + 28), (x + 110), (y + 26));
    g.drawLine((x + 170), (y + 28), (x + 170), (y + 26));
    g.drawLine((x + 170), (y + 28), (x + 170), (y + 26));
    g.drawLine((x + 110), (y + 28), (x + 110), (y + 26));
    g.drawLine((x + 90), (y + 28), (x + 90), (y + 26));
    g.drawLine((x + 30), (y + 28), (x + 30), (y + 26));
    g.drawLine((x + 190), (y + 7), (x + 190), (y + 13));
    g.drawLine((x + 190), (y + 13), (x + 24), (y + 13));
    g.drawLine((x + 24), (y + 13), (x + 24), (y + 28));
    g.drawLine((x + 10), (y + height - 7), (x + 10), (y + 30));
    structureBubble(g, (x + 10), (y + height - 7));
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.ONE, BITS);
      state.setData(data);
    }
    if (data.updateClock(state.getPortValue(PORT_INDEX_CLK), StdAttr.TRIG_RISING)) {
      for (var bit = 0; bit < BITS; bit++) {
        data.setValue(bit, state.getPortValue(DATA[bit]));
      }
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(OUTPUTS[bit], enabled ? data.getValue(bit) : Value.UNKNOWN, DELAY);
    }
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CLK};
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
