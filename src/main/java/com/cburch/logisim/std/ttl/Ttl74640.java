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
 * TTL 74HC640: octal inverting bus transceiver with three-state outputs.
 *
 * <p>Simulation follows the
 * <a href="https://www.ti.com/lit/ds/symlink/sn74hc640.pdf">TI SN74HC640</a> and
 * <a href="https://assets.nexperia.com/documents/data-sheet/74HC_HCT640.pdf">Nexperia 74HC640</a>
 * data sheets. A low {@code nOE} and a high {@code DIR} drive each B pin with the complement of
 * the paired A pin. A low {@code nOE} and a low {@code DIR} drive each A pin with the complement of
 * the paired B pin. A high {@code nOE}, or a control that is neither high nor low, releases both
 * buses. High impedance is reported as unknown. The part is the inverting counterpart of the
 * 74HC245. Pin names follow the TI numbering, where A1 is Nexperia's A0.
 */
public class Ttl74640 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "74640";

  public static final int PORT_INDEX_DIR = 0;
  public static final int PORT_INDEX_A1 = 1;
  public static final int PORT_INDEX_A2 = 2;
  public static final int PORT_INDEX_A3 = 3;
  public static final int PORT_INDEX_A4 = 4;
  public static final int PORT_INDEX_A5 = 5;
  public static final int PORT_INDEX_A6 = 6;
  public static final int PORT_INDEX_A7 = 7;
  public static final int PORT_INDEX_A8 = 8;
  public static final int PORT_INDEX_B8 = 9;
  public static final int PORT_INDEX_B7 = 10;
  public static final int PORT_INDEX_B6 = 11;
  public static final int PORT_INDEX_B5 = 12;
  public static final int PORT_INDEX_B4 = 13;
  public static final int PORT_INDEX_B3 = 14;
  public static final int PORT_INDEX_B2 = 15;
  public static final int PORT_INDEX_B1 = 16;
  public static final int PORT_INDEX_nOE = 17;
  /** Port index of GND when the explicit power pins are shown. */
  public static final int PORT_INDEX_GND = 18;
  /** Port index of VCC when the explicit power pins are shown. */
  public static final int PORT_INDEX_VCC = 19;

  private static final int DELAY = 1;
  private static final int BITS = 8;
  /** A-bus port of each bit. Bit 0 is A1, which shares a channel with B1. */
  private static final int[] BUS_A = {
    PORT_INDEX_A1,
    PORT_INDEX_A2,
    PORT_INDEX_A3,
    PORT_INDEX_A4,
    PORT_INDEX_A5,
    PORT_INDEX_A6,
    PORT_INDEX_A7,
    PORT_INDEX_A8
  };
  /** B-bus port of each bit. Bit 0 is B1, on physical pin 18. */
  private static final int[] BUS_B = {
    PORT_INDEX_B1,
    PORT_INDEX_B2,
    PORT_INDEX_B3,
    PORT_INDEX_B4,
    PORT_INDEX_B5,
    PORT_INDEX_B6,
    PORT_INDEX_B7,
    PORT_INDEX_B8
  };
  private static final byte[] INOUT_PINS = {
    2, 3, 4, 5, 6, 7, 8, 9, 11, 12, 13, 14, 15, 16, 17, 18
  };
  private static final String[] PORT_NAMES = {
    "DIR (direction, high sends A to B)",
    "A1",
    "A2",
    "A3",
    "A4",
    "A5",
    "A6",
    "A7",
    "A8",
    "B8",
    "B7",
    "B6",
    "B5",
    "B4",
    "B3",
    "B2",
    "B1",
    "nOE (output enable, active low)"
  };

  /** Creates a 74640 octal inverting bus transceiver. */
  public Ttl74640() {
    super(_ID, (byte) 20, new byte[] {}, new byte[] {}, INOUT_PINS, PORT_NAMES,
        new Ttl74640HdlGenerator());
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawBuffers(g, x, y, height);
  }

  /**
   * Two rows of three-state inverters, one each way, as in the 74HC640 logic diagram. Each output
   * wire stops on its bubble. Enable taps use one node size, and the bus has no node at its end.
   */
  private static void drawBuffers(Graphics g, int x, int y, int height) {
    final var pin = AbstractTtlGate.PIN_HEIGHT;
    g.drawPolyline(
        new int[] {x + 10, x + 10, x + 17, x + 17},
        new int[] {y + height - pin, y + 10, y + 10, y + 13},
        4);
    g.drawOval(x + 16, y + 13, 2, 2);
    drawNode(g, x + 10, y + height - 10);
    g.drawPolyline(
        new int[] {x + 10, x + 17, x + 17},
        new int[] {y + height - 10, y + height - 10, y + height - 15},
        3);

    g.drawPolyline(
        new int[] {x + 30, x + 30, x + 27, x + 23, x + 23},
        new int[] {y + pin, y + height - 13, y + height - 10, y + height - 10, y + height - 13},
        5);
    g.drawOval(x + 22, y + height - 15, 2, 2);
    drawNode(g, x + 30, y + 10);
    g.drawPolyline(
        new int[] {x + 30, x + 23, x + 23},
        new int[] {y + 10, y + 10, y + 13},
        3);
    g.drawOval(x + 22, y + 13, 2, 2);

    g.drawPolyline(
        new int[] {x + 15, x + 15, x + 25, x + 25},
        new int[] {y + height - 20, y + height - 15, y + height - 15, y + height - 20},
        4);
    GraphicsUtil.drawCenteredArc(g, x + 20, y + height - 20, 5, 0, 180);
    g.drawPolyline(
        new int[] {x + 20, x + 20, x + 175},
        new int[] {y + height - 25, y + height - 28, y + height - 28},
        3);

    for (var i = x + 30; i < x + 190; i += 20) {
      g.drawPolyline(
          new int[] {i, i + 3, i + 10, i + 10},
          new int[] {y + height - pin, y + height - 10, y + height - 10, y + height - 14},
          4);
      g.drawPolygon(
          new int[] {i + 6, i + 10, i + 14},
          new int[] {y + height - 14, y + height - 20, y + height - 14},
          3);
      // Two-pixel bubble on the tip, clear of the enable rail above it.
      g.drawOval(i + 9, y + height - 22, 2, 2);
      if (i < x + 170) {
        drawNode(g, i + 5, y + height - 28);
      }
      // Stop on the slanted side. One pixel further would enter the triangle.
      g.drawPolyline(
          new int[] {i + 5, i + 5, i + 7},
          new int[] {y + height - 28, y + height - 17, y + height - 17},
          3);
      g.drawPolyline(
          new int[] {i + 10, i + 10, i + 16, i + 20},
          new int[] {y + height - 22, y + 16, y + 10, y + 10},
          4);
      drawNode(g, i + 20, y + 10);
    }

    g.drawPolyline(
        new int[] {x + 15, x + 15, x + 25, x + 25},
        new int[] {y + 20, y + 15, y + 15, y + 20},
        4);
    GraphicsUtil.drawCenteredArc(g, x + 20, y + 20, 5, 180, 180);
    g.drawPolyline(
        new int[] {x + 20, x + 20, x + 185},
        new int[] {y + 25, y + 28, y + 28},
        3);

    for (var i = x + 50; i < x + 210; i += 20) {
      g.drawLine(i, y + pin, i, y + 14);
      g.drawPolygon(
          new int[] {i + 4, i, i - 4},
          new int[] {y + 14, y + 20, y + 14},
          3);
      // Two-pixel bubble on the tip, clear of the enable rail below it.
      g.drawOval(i - 1, y + 20, 2, 2);
      if (i < x + 190) {
        drawNode(g, i - 5, y + 28);
      }
      // Stop on the slanted side. One pixel further would enter the triangle.
      g.drawPolyline(
          new int[] {i - 5, i - 5, i - 3},
          new int[] {y + 28, y + 16, y + 16},
          3);
      g.drawPolyline(
          new int[] {i, i, i - 6, i - 10},
          new int[] {y + 22, y + height - 16, y + height - 10, y + height - 10},
          4);
      drawNode(g, i - 10, y + height - 10);
    }
  }

  /** Junction dot centered on a wire crossing. Every node is this size. */
  private static void drawNode(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  /**
   * Releases both buses when the exposed supply pins are not a valid ground and positive rail.
   * The base implementation only clears output ports, and these data pins are bidirectional.
   */
  @Override
  public void propagate(InstanceState state) {
    if (exposedPowerIsInvalid(state)) {
      releaseBothBuses(state);
      return;
    }
    propagateTtl(state);
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var fromA = new Value[BITS];
    final var fromB = new Value[BITS];
    for (var bit = 0; bit < BITS; bit++) {
      fromA[bit] = state.getPortValue(BUS_A[bit]);
      fromB[bit] = state.getPortValue(BUS_B[bit]);
    }
    final var enabled = state.getPortValue(PORT_INDEX_nOE) == Value.FALSE;
    final var direction = state.getPortValue(PORT_INDEX_DIR);
    if (enabled && direction == Value.TRUE) {
      drive(state, BUS_B, fromA);
      release(state, BUS_A);
    } else if (enabled && direction == Value.FALSE) {
      drive(state, BUS_A, fromB);
      release(state, BUS_B);
    } else {
      releaseBothBuses(state);
    }
  }

  private static boolean exposedPowerIsInvalid(InstanceState state) {
    if (!state.getAttributeValue(TtlLibrary.VCC_GND)) return false;
    return state.getPortValue(PORT_INDEX_GND) != Value.FALSE
        || state.getPortValue(PORT_INDEX_VCC) != Value.TRUE;
  }

  /** Drives {@code ports} with the complement of {@code sources}. An invalid source becomes error. */
  private static void drive(InstanceState state, int[] ports, Value[] sources) {
    for (var bit = 0; bit < BITS; bit++) {
      state.setPort(ports[bit], sources[bit].not(), DELAY);
    }
  }

  private static void releaseBothBuses(InstanceState state) {
    release(state, BUS_A);
    release(state, BUS_B);
  }

  private static void release(InstanceState state, int[] ports) {
    for (final var port : ports) {
      state.setPort(port, Value.UNKNOWN, DELAY);
    }
  }

}
