/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.dma;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.InstanceStateImpl;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics2D;

/**
 * SoC DMA engine component.
 *
 * <p>A simple linear memory-copy DMA engine driven by a clock input.
 * It is a bus slave (MMIO control registers) and a bus master (data transfers).
 * Three independent bus connections allow routing control, source reads, and
 * destination writes to different SoC buses.
 *
 * <p>Ports:
 * <ul>
 *   <li>0 – Reset (active high)</li>
 *   <li>1 – Clock</li>
 *   <li>2 – IRQ output (active high, directly driven)</li>
 * </ul>
 */
public class SocDma extends SocInstanceFactory {

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   */
  public static final String _ID = "SocDma";

  public static final int RESET_INDEX = 0;
  public static final int CLOCK_INDEX = 1;
  public static final int IRQ_INDEX = 2;

  private static final int WIDTH = 320;
  private static final int HEIGHT = 120;

  public SocDma() {
    super(_ID, S.getter("SocDmaComponent"), SOC_SLAVE | SOC_MASTER);
    setIcon(new ArithmeticIcon("DMA", 3));
    setOffsetBounds(Bounds.create(0, 0, WIDTH, HEIGHT));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new DmaAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    Port[] ps = new Port[3];
    ps[RESET_INDEX] = new Port(0, 100, Port.INPUT, 1);
    ps[RESET_INDEX].setToolTip(S.getter("SocDmaResetInput"));
    ps[CLOCK_INDEX] = new Port(0, 110, Port.INPUT, 1);
    ps[CLOCK_INDEX].setToolTip(S.getter("SocDmaClockInput"));
    ps[IRQ_INDEX] = new Port(WIDTH, 100, Port.OUTPUT, 1);
    ps[IRQ_INDEX].setToolTip(S.getter("SocDmaIrqOutput"));
    instance.setPorts(ps);

    Bounds bds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() / 2,
        bds.getY() - 3,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BASELINE);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT
        || attr == DmaAttributes.DMA_SRC_BUS
        || attr == DmaAttributes.DMA_DST_BUS
        || attr == DmaAttributes.START_ADDRESS
        || attr == DmaAttributes.BURST_SIZE) {
      instance.fireInvalidated();
    }
    super.instanceAttributeChanged(instance, attr);
  }

  @Override
  public void propagate(InstanceState state) {
    DmaState.DmaRegState data = (DmaState.DmaRegState) state.getData();
    if (data == null) {
      data = state.getAttributeValue(DmaAttributes.DMA_STATE).getNewState();
      state.setData(data);
    }

    // Reset handling
    if (state.getPortValue(RESET_INDEX) == Value.TRUE) {
      data.reset();
      state.setPort(IRQ_INDEX, Value.FALSE, 1);
      return;
    }

    // Clock edge detection
    Value clock = state.getPortValue(CLOCK_INDEX);
    if (data.lastClock == Value.FALSE && clock == Value.TRUE) {
      DmaState dmaState = state.getAttributeValue(DmaAttributes.DMA_STATE);
      dmaState.executeBurst(data, ((InstanceStateImpl) state).getCircuitState());
    }
    data.lastClock = clock;

    // Drive IRQ output
    state.setPort(IRQ_INDEX, data.irqAsserted ? Value.TRUE : Value.FALSE, 1);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    Graphics2D g2 = (Graphics2D) painter.getGraphics();
    Location loc = painter.getLocation();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    painter.drawClock(CLOCK_INDEX, Direction.EAST);
    painter.drawPort(RESET_INDEX, "Reset", Direction.EAST);
    painter.drawPort(IRQ_INDEX, "IRQ", Direction.WEST);

    int x = loc.getX();
    int y = loc.getY();

    Font f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, "SOC DMA Engine", x + WIDTH / 2, y + 10);
    g2.setFont(f);

    // Show base address and burst size
    int baseAddr = painter.getAttributeValue(DmaAttributes.START_ADDRESS);
    int burst = painter.getAttributeValue(DmaAttributes.BURST_SIZE);
    GraphicsUtil.drawCenteredText(g2,
        S.get("SocDmaBaseLabel") + String.format("0x%08X", baseAddr),
        x + WIDTH / 4, y + 30);
    GraphicsUtil.drawCenteredText(g2,
        S.get("SocDmaBurstLabel") + burst + " words/tick",
        x + 3 * WIDTH / 4, y + 30);

    // Show runtime status if available
    DmaState.DmaRegState data = (DmaState.DmaRegState) painter.getData();
    if (data != null) {
      String statusStr;
      if (data.busy) {
        int pct = data.length > 0 ? (data.bytesDone * 100 / data.length) : 0;
        statusStr = S.get("SocDmaStatusBusy") + " " + pct + "%"
            + " (" + data.bytesDone + "/" + data.length + " bytes)";
      } else if (data.done) {
        statusStr = S.get("SocDmaStatusDone");
      } else {
        statusStr = S.get("SocDmaStatusIdle");
      }
      GraphicsUtil.drawCenteredText(g2, statusStr, x + WIDTH / 2, y + 50);
    }

    if (painter.isPrintView()) return;

    // Paint control bus connection
    painter.getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(g2, Bounds.create(x + 5, y + 65, 100, 16));
    // Paint source bus connection
    painter.getAttributeValue(DmaAttributes.DMA_SRC_BUS)
        .paint(g2, Bounds.create(x + 110, y + 65, 100, 16));
    // Paint destination bus connection
    painter.getAttributeValue(DmaAttributes.DMA_DST_BUS)
        .paint(g2, Bounds.create(x + 215, y + 65, 100, 16));

    // Bus connection labels
    g2.setColor(Color.BLACK);
    GraphicsUtil.drawCenteredText(g2, "CTRL", x + 55, y + 85);
    GraphicsUtil.drawCenteredText(g2, "SRC", x + 160, y + 85);
    GraphicsUtil.drawCenteredText(g2, "DST", x + 265, y + 85);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(DmaAttributes.DMA_STATE);
  }

  @Override
  public SocBusSnifferInterface getSnifferInterface(AttributeSet attrs) {
    return null;
  }

  @Override
  public SocProcessorInterface getProcessorInterface(AttributeSet attrs) {
    return null;
  }
}
