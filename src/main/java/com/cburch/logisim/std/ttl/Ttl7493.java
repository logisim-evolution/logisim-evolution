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

public class Ttl7493 extends AbstractTtlGate {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "7493";

  public static final int PORT_INDEX_CKB = 0;
  public static final int PORT_INDEX_R0_1 = 1;
  public static final int PORT_INDEX_R0_2 = 2;
  public static final int PORT_INDEX_QC = 3;
  public static final int PORT_INDEX_QB = 4;
  public static final int PORT_INDEX_QD = 5;
  public static final int PORT_INDEX_QA = 6;
  public static final int PORT_INDEX_CKA = 7;

  private static final byte[] OUTPUT_PORTS = {8, 9, 11, 12};
  private static final byte[] UNUSED_PINS = {4, 6, 7, 13};
  private static final String[] PORT_NAMES = {
    "CKB (Clock B)",
    "R0(1) (Reset, active HIGH)",
    "R0(2) (Reset, active HIGH)",
    "QC",
    "QB",
    "QD",
    "QA",
    "CKA (Clock A)"
  };
  private static final BitWidth WIDTH = BitWidth.create(4);

  public Ttl7493() {
    super(
        _ID,
        (byte) 14,
        OUTPUT_PORTS,
        UNUSED_PINS,
        PORT_NAMES,
        (byte) 5,
        (byte) 10,
        null);
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    final var g = painter.getGraphics();
    super.paintBase(painter, false, false);
    g.setColor(Color.BLACK);
    drawStructure(g, x, y, height);
  }

  /**
   * Datasheet logic: stage A is a divide-by-two clocked by the falling edge of CKA. Stages B, C
   * and D are a divide-by-eight; CKB clocks B, QB clocks C and QC clocks D. QA is not tied to CKB.
   * R0(1) and R0(2) meet in an AND gate whose high output clears every stage.
   */
  private static void drawStructure(Graphics g, int x, int y, int height) {
    final var pinTop = y + PIN_HEIGHT;
    final var pinBottom = y + height - PIN_HEIGHT;
    final var previousFont = g.getFont();
    // Dialog 5 keeps each name inside its block and clear of the clock wedge.
    g.setFont(new Font(Font.DIALOG, Font.PLAIN, 5));
    stage(g, x + 20, y + 22, 8, "A");
    stage(g, x + 40, y + 22, 10, "D");
    stage(g, x + 98, y + 22, 8, "B");
    stage(g, x + 116, y + 22, 8, "C");
    Drawgates.paintAnd(g, x + 88, y + 34, 8, 8, false);
    g.setFont(previousFont);

    // Each clock enters the left edge. The wire stops on the far side of the bubble.
    bubble(g, x + 19, y + 26);
    bubble(g, x + 39, y + 26);
    bubble(g, x + 97, y + 26);
    bubble(g, x + 115, y + 26);
    wire(g, x + 10, pinTop, x + 10, y + 26, x + 18, y + 26);
    // CKB shares the pin column with CKA and keeps a gap before it.
    wire(g, x + 10, pinBottom, x + 10, y + 31, x + 14, y + 31, x + 14, y + 15);
    wire(g, x + 14, y + 15, x + 78, y + 15, x + 78, y + 26, x + 96, y + 26);

    // Q leaves the right edge and steps aside before rising, so it does not ride the outline.
    wire(g, x + 28, y + 30, x + 32, y + 30, x + 32, y + 11, x + 50, y + 11, x + 50, pinTop);
    wire(g, x + 50, y + 30, x + 56, y + 30, x + 56, y + 11, x + 70, y + 11, x + 70, pinTop);
    wire(g, x + 106, y + 30, x + 110, y + 30, x + 110, pinTop);
    wire(g, x + 110, y + 26, x + 114, y + 26);
    node(g, x + 110, y + 26);
    wire(g, x + 124, y + 30, x + 130, y + 30, x + 130, pinTop);
    // QC returns above the stages to D's left clock.
    wire(g, x + 36, y + 19, x + 130, y + 19);
    wire(g, x + 36, y + 19, x + 36, y + 26, x + 38, y + 26);
    node(g, x + 130, y + 19);

    // R0(1) is the farther pin, so its vertical stays left of R0(2).
    wire(g, x + 30, pinBottom, x + 30, y + 45, x + 70, y + 45, x + 70, y + 32, x + 80, y + 32);
    wire(g, x + 50, pinBottom, x + 50, y + 49, x + 74, y + 49, x + 74, y + 36, x + 80, y + 36);
    // One straight reset from the AND output into B. The bus dot sits on that line.
    wire(g, x + 88, y + 34, x + 98, y + 34);
    wire(g, x + 94, y + 34, x + 94, y + 41);
    wire(g, x + 16, y + 41, x + 113, y + 41);
    wire(g, x + 16, y + 41, x + 16, y + 34, x + 20, y + 34);
    wire(g, x + 36, y + 41, x + 36, y + 34, x + 40, y + 34);
    wire(g, x + 113, y + 41, x + 113, y + 34, x + 116, y + 34);
    node(g, x + 36, y + 41);
    node(g, x + 94, y + 34);
    node(g, x + 94, y + 41);
  }

  /** One toggle stage. Clock and reset are on the left; the name is the block center. */
  private static void stage(Graphics g, int left, int top, int width, String name) {
    g.drawRect(left, top, width, 16);
    clockLeft(g, left, top + 4);
    GraphicsUtil.drawText(
        g,
        name,
        left + width / 2,
        top + 8,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_CENTER_OVERALL);
  }

  /** Falling-edge mark on the left side. The point is inside; the bubble sits outside. */
  private static void clockLeft(Graphics g, int edgeX, int midY) {
    g.drawLine(edgeX, midY - 3, edgeX + 2, midY);
    g.drawLine(edgeX, midY + 3, edgeX + 2, midY);
  }

  /** Active-low clock mark. Even 2×2 oval, center on the wire axis, touching the stage. */
  private static void bubble(Graphics g, int centerX, int centerY) {
    g.drawOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Junction of three branches. The oval center is the common point. */
  private static void node(Graphics g, int centerX, int centerY) {
    g.fillOval(centerX - 1, centerY - 1, 2, 2);
  }

  /** Orthogonal wire through the given points. */
  private static void wire(Graphics g, int... points) {
    for (var i = 2; i < points.length; i += 2) {
      g.drawLine(points[i - 2], points[i - 1], points[i], points[i + 1]);
    }
  }

  private static TtlRegisterData getStateData(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(WIDTH);
      state.setData(data);
    }
    return data;
  }

  @Override
  public void propagateTtl(InstanceState state) {
    final var data = getStateData(state);
    final var clockATriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_CKA), 0, StdAttr.TRIG_FALLING);
    final var clockBTriggered =
        data.updateClock(state.getPortValue(PORT_INDEX_CKB), 1, StdAttr.TRIG_FALLING);
    final var values = data.getValue().getAll();

    if (state.getPortValue(PORT_INDEX_R0_1) == Value.TRUE
        && state.getPortValue(PORT_INDEX_R0_2) == Value.TRUE) {
      data.setValue(Value.createKnown(WIDTH, 0));
    } else {
      if (clockATriggered) values[0] = values[0].not();
      if (clockBTriggered) {
        var carry = Value.TRUE;
        for (var i = 1; i < values.length; i++) {
          final var oldValue = values[i];
          values[i] = oldValue.xor(carry);
          carry = oldValue.and(carry);
        }
      }
      data.setValue(Value.create(values));
    }

    state.setPort(PORT_INDEX_QA, data.getValue().get(0), 4);
    state.setPort(PORT_INDEX_QB, data.getValue().get(1), 4);
    state.setPort(PORT_INDEX_QC, data.getValue().get(2), 4);
    state.setPort(PORT_INDEX_QD, data.getValue().get(3), 4);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {PORT_INDEX_CKB, PORT_INDEX_CKA};
  }
}
