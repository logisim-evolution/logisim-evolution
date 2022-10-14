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
import com.cburch.logisim.fpga.hdlgenerator.HdlGeneratorFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstancePoker;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.event.MouseEvent;

public class AbstractOctalFlops extends AbstractTtlGate {

  private boolean hasWe;

  protected AbstractOctalFlops(
      String name,
      byte pins,
      byte[] outputPorts,
      String[] ttlPortNames,
      HdlGeneratorFactory generator) {
    super(name, pins, outputPorts, ttlPortNames, 80, generator);
    super.setInstancePoker(Poker.class);
  }

  public static class Poker extends InstancePoker {
    boolean isPressed = true;

    private boolean isInside(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      var inside = false;
      for (var i = 0; i < 8; i++) {
        final var dx = p.x - (95 + i * 10);
        final var dy = p.y - 40;
        final var d2 = dx * dx + dy * dy;
        inside |= (d2 < 4 * 4);
      }
      return inside;
    }

    private int getIndex(InstanceState state, MouseEvent e) {
      final var p = getTranslatedTtlXY(state, e);
      for (var i = 0; i < 8; i++) {
        final var dx = p.x - (95 + i * 10);
        final var dy = p.y - 40;
        final var d2 = dx * dx + dy * dy;
        if (d2 < 4 * 4) return i;
      }
      return 0;
    }

    @Override
    public void mousePressed(InstanceState state, MouseEvent e) {
      isPressed = isInside(state, e);
    }

    @Override
    public void mouseReleased(InstanceState state, MouseEvent e) {
      if (!state.getAttributeValue(TtlLibrary.DRAW_INTERNAL_STRUCTURE)) return;
      if (isPressed && isInside(state, e)) {
        final var index = getIndex(state, e);
        TtlRegisterData myState = (TtlRegisterData) state.getData();
        if (myState == null) return;
        final var values = myState.getValue().getAll();
        if (values[index].isFullyDefined()) values[index] = values[index].not();
        else values[index] = Value.createKnown(1, 0);
        myState.setValue(Value.create(values));
        state.fireInvalidated();
      }
      isPressed = false;
    }
  }

  public void setWe(boolean haswe) {
    hasWe = haswe;
  }

  @Override
  public void paintInternal(InstancePainter painter, int x, int y, int height, boolean up) {
    super.paintBase(painter, false, false);
    final var g = (Graphics2D) painter.getGraphics();
    for (var i = 0; i < 8; i++) {
      g.drawRect(x + 90 + i * 10, y + 25, 10, 30);
    }
    g.drawLine(x + 85, y + 30, x + 90, y + 30);
    g.drawLine(x + 85, y + 50, x + 90, y + 50);
    g.drawLine(x + 85, y + 25, x + 85, y + 30);
    g.drawLine(x + 85, y + 50, x + 85, y + 55);
    g.drawLine(x + 65, y + 55, x + 85, y + 55);
    g.drawLine(x + 65, y + 25, x + 85, y + 25);
    g.drawLine(x + 65, y + 25, x + 65, y + 55);
    g.drawOval(x + 68, y + 55, 4, 4);

    g.drawLine(x + 78, y + 55, x + 80, y + 50);
    g.drawLine(x + 82, y + 55, x + 80, y + 50);
    g.drawLine(x + 190, y + AbstractTtlGate.PIN_HEIGHT, x + 190, y + 60);
    g.drawLine(x + 180, y + 60, x + 190, y + 60);
    g.drawLine(x + 180, y + 60, x + 180, y + 70);
    g.drawLine(x + 80, y + 70, x + 180, y + 70);
    g.drawLine(x + 80, y + 55, x + 80, y + 70);
    g.drawLine(x + 10, y + height - AbstractTtlGate.PIN_HEIGHT, x + 10, y + 60);
    g.drawLine(x + 10, y + 60, x + 70, y + 60);
    g.drawLine(x + 70, y + 59, x + 70, y + 60);

    g.rotate(-Math.PI / 2, x, y);
    if (hasWe) {
      g.drawString("1C2", x - 49, y + 83);
      g.drawString("G1", x - 54, y + 73);
      g.drawString("2D", x - 54, y + 98);
    } else {
      g.drawString("C1", x - 49, y + 83);
      g.drawString("R", x - 54, y + 73);
      g.drawString("1D", x - 54, y + 98);
    }
    g.rotate(Math.PI / 2, x, y);
    for (int i = 0; i < 8; i++) {
      g.drawLine(x + 95 + i * 10, y + 20, x + 95 + i * 10, y + 25);
      g.drawLine(x + 95 + i * 10, y + 20, x + 95 + i * 10 + 3, y + 17);
      g.drawLine(x + 95 + i * 10, y + 55, x + 95 + i * 10, y + 60);
      g.drawLine(x + 95 + i * 10, y + 60, x + 95 + i * 10 + 3, y + 63);
    }
    final var dincr = new int[] {20, 60, 20, 0};
    var dpos1 = 50;
    var dpos2 = 150;
    final var qincr = new int[] {60, 20, 60, 0};
    var qpos1 = 30;
    var qpos2 = 170;
    for (var i = 0; i < 4; i++) {
      g.drawLine(x + dpos1, y + height - AbstractTtlGate.PIN_HEIGHT, x + dpos1, y + 66);
      g.drawLine(x + dpos1, y + 66, x + dpos1 + 3, y + 63);
      dpos1 += dincr[i];
      g.drawLine(x + dpos2, y + AbstractTtlGate.PIN_HEIGHT, x + dpos2, y + 10);
      g.drawLine(x + dpos2, y + 10, x + dpos2 + 3, y + 13);
      dpos2 -= dincr[i];
      g.drawLine(x + qpos1, y + height - AbstractTtlGate.PIN_HEIGHT, x + qpos1, y + 70);
      g.drawLine(x + qpos1, y + 70, x + qpos1 + 3, y + 67);
      qpos1 += qincr[i];
      g.drawLine(x + qpos2, y + AbstractTtlGate.PIN_HEIGHT, x + qpos2, y + 14);
      g.drawLine(x + qpos2, y + 14, x + qpos2 + 3, y + 17);
      qpos2 -= qincr[i];
    }
    g.setStroke(new BasicStroke(2));
    g.drawLine(x + 33, y + 17, x + 173, y + 17);
    g.drawLine(x + 33, y + 67, x + 173, y + 67);
    g.drawLine(x + 30, y + 20, x + 33, y + 17);
    g.drawLine(x + 30, y + 64, x + 33, y + 67);
    g.drawLine(x + 30, y + 20, x + 30, y + 64);
    g.drawLine(x + 53, y + 13, x + 153, y + 13);
    g.drawLine(x + 53, y + 63, x + 168, y + 63);
    g.drawLine(x + 46, y + 20, x + 53, y + 13);
    g.drawLine(x + 46, y + 57, x + 53, y + 63);
    g.drawLine(x + 46, y + 20, x + 46, y + 57);
    g.setStroke(new BasicStroke(1));
    drawState(g, x, y, (TtlRegisterData) painter.getData());
  }

  @Override
  public void propagateTtl(InstanceState state) {
    var data = (TtlRegisterData) state.getData();
    if (data == null) {
      data = new TtlRegisterData(BitWidth.create(8));
      state.setData(data);
    }
    var changed = false;
    final var triggered = data.updateClock(state.getPortValue(9));
    var values = data.getValue().getAll();
    if (hasWe) {
      if (triggered && (state.getPortValue(0).equals(Value.FALSE))) {
        changed = true;
        values[0] = state.getPortValue(2);
        values[1] = state.getPortValue(3);
        values[2] = state.getPortValue(6);
        values[3] = state.getPortValue(7);
        values[4] = state.getPortValue(11);
        values[5] = state.getPortValue(12);
        values[6] = state.getPortValue(15);
        values[7] = state.getPortValue(16);
      }
    } else {
      if (state.getPortValue(0).equals(Value.FALSE)) {
        values = Value.createKnown(8, 0).getAll();
        changed = true;
      } else if (triggered) {
        changed = true;
        values[0] = state.getPortValue(2);
        values[1] = state.getPortValue(3);
        values[2] = state.getPortValue(6);
        values[3] = state.getPortValue(7);
        values[4] = state.getPortValue(11);
        values[5] = state.getPortValue(12);
        values[6] = state.getPortValue(15);
        values[7] = state.getPortValue(16);
      }
    }
    if (changed) {
      data.setValue(Value.create(values));
    }
    state.setPort(1, data.getValue().get(0), 8);
    state.setPort(4, data.getValue().get(1), 8);
    state.setPort(5, data.getValue().get(2), 8);
    state.setPort(8, data.getValue().get(3), 8);
    state.setPort(10, data.getValue().get(4), 8);
    state.setPort(13, data.getValue().get(5), 8);
    state.setPort(14, data.getValue().get(6), 8);
    state.setPort(17, data.getValue().get(7), 8);
  }

  private void drawState(Graphics2D g, int x, int y, TtlRegisterData state) {
    if (state == null) return;
    g.rotate(-Math.PI / 2, x, y);
    for (var i = 0; i < 8; i++) {
      g.setColor(state.getValue().get(i).getColor());
      g.fillOval(x - 44, y + 91 + i * 10, 8, 8);
      g.setColor(Color.WHITE);
      GraphicsUtil.drawCenteredText(g, state.getValue().get(i).toDisplayString(), x - 41, y + 94 + i * 10);
    }
    g.rotate(-Math.PI / 2, x, y);
    g.setColor(Color.BLACK);
  }

  @Override
  public boolean checkForGatedClocks(netlistComponent comp) {
    return true;
  }

  @Override
  public int[] clockPinIndex(netlistComponent comp) {
    return new int[] {9};
  }
}
