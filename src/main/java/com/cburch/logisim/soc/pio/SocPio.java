/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.pio;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.data.SocBusSlaveInterface;
import com.cburch.logisim.soc.data.SocBusSnifferInterface;
import com.cburch.logisim.soc.data.SocInstanceFactory;
import com.cburch.logisim.soc.data.SocProcessorInterface;
import com.cburch.logisim.soc.data.SocSimulationManager;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;

import java.awt.Color;
import java.awt.Graphics2D;

public class SocPio extends SocInstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "SocPio";

  public static final int RESET_INDEX = 0;
  public static final int IRQ_INDEX = 1;

  public SocPio() {
    super(_ID, S.getter("SocPioComponent"), SOC_SLAVE);
    setIcon(new ArithmeticIcon("SocPIO", 3));
    setOffsetBounds(Bounds.create(0, 0, 380, 120));
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new PioAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    updatePorts(instance);
    final var bds = instance.getBounds();
    instance.setTextField(
        StdAttr.LABEL,
        StdAttr.LABEL_FONT,
        bds.getX() + bds.getWidth() + 2,
        bds.getY() + bds.getHeight() / 2,
        GraphicsUtil.H_LEFT,
        GraphicsUtil.V_CENTER);
  }

  private void updatePorts(Instance instance) {
    final var nrBits = instance.getAttributeValue(StdAttr.WIDTH).getWidth();
    var nrOfPorts = nrBits;
    final var dir = instance.getAttributeValue(PioAttributes.PIO_DIRECTION);
    final var hasIrq = hasIrqPin(instance.getAttributeSet());
    if (dir == PioAttributes.PORT_INOUT) nrOfPorts *= 2;
    var index = hasIrq ? 2 : 1;
    nrOfPorts += index;
    final var ps = new Port[nrOfPorts];
    if (hasIrq) {
      ps[IRQ_INDEX] = new Port(20, 0, Port.OUTPUT, 1);
      ps[IRQ_INDEX].setToolTip(S.getter("SocPioIrqOutput"));
    }
    ps[RESET_INDEX] = new Port(0, 110, Port.INPUT, 1);
    ps[RESET_INDEX].setToolTip(S.getter("SocPioResetInput"));
    if (dir == PioAttributes.PORT_INPUT || dir == PioAttributes.PORT_INOUT) {
      for (var b = 0; b < nrBits; b++) {
        ps[index + b] = new Port(370 - b * 10, 120, Port.INPUT, 1);
        ps[index + b].setToolTip(S.getter("SocPioInputPinx", Integer.toString(b)));
      }
      index += nrBits;
    }
    if (dir == PioAttributes.PORT_INOUT
        || dir == PioAttributes.PORT_OUTPUT
        || dir == PioAttributes.PORT_BIDIR) {
      final var portType = (dir == PioAttributes.PORT_BIDIR) ? Port.INOUT : Port.OUTPUT;
      for (var b = 0; b < nrBits; b++) {
        ps[index + b] = new Port(370 - b * 10, 0, portType, 1);
        ps[index + b].setToolTip(
            (dir == PioAttributes.PORT_BIDIR)
                ? S.getter("SocPioBidirPinx", Integer.toString(b))
                : S.getter("SocPioOutputPinx", Integer.toString(b)));
      }
    }
    instance.setPorts(ps);
  }

  private boolean hasIrqPin(AttributeSet attrs) {
    if (!attrs.containsAttribute(PioAttributes.PIO_GEN_IRQ)) return false;
    return attrs.getValue(PioAttributes.PIO_GEN_IRQ);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == SocSimulationManager.SOC_BUS_SELECT) {
      instance.fireInvalidated();
    } else if (attr == StdAttr.WIDTH
        || attr == PioAttributes.PIO_DIRECTION
        || attr == PioAttributes.PIO_GEN_IRQ) {
      updatePorts(instance);
    }
    super.instanceAttributeChanged(instance, attr);
  }

  private void paintPins(InstancePainter painter, Graphics2D g2, Location loc) {
    painter.drawPort(RESET_INDEX, "Reset", Direction.EAST);
    int nrBits = painter.getAttributeValue(StdAttr.WIDTH).getWidth();
    final var dir = painter.getAttributeValue(PioAttributes.PIO_DIRECTION);
    int index = 1;
    if (hasIrqPin(painter.getAttributeSet())) {
      index++;
      painter.drawPort(IRQ_INDEX, "IRQ", Direction.NORTH);
    }
    if (dir == PioAttributes.PORT_INPUT || dir == PioAttributes.PORT_INOUT) {
      for (int b = 0; b < nrBits; b++) painter.drawPort(index + b);
      if (!painter.isPrintView()) {
        index += nrBits;
        g2.drawRect(loc.getX() + 40, loc.getY() + 80, 340, 40);
        GraphicsUtil.drawCenteredText(g2, S.get("SocPioInputs"), loc.getX() + 210, loc.getY() + 95);
        GraphicsUtil.drawCenteredText(g2, "0", loc.getX() + 370, loc.getY() + 110);
        if (nrBits > 9) {
          GraphicsUtil.drawCenteredText(
              g2, Integer.toString(nrBits - 1), loc.getX() + 380 - nrBits * 10, loc.getY() + 110);
        } else {
          for (int b = 1; b < nrBits; b++)
            GraphicsUtil.drawCenteredText(
                g2, Integer.toString(b), loc.getX() + 370 - b * 10, loc.getY() + 110);
        }
      }
    }
    if (dir == PioAttributes.PORT_INOUT
        || dir == PioAttributes.PORT_OUTPUT
        || dir == PioAttributes.PORT_BIDIR) {
      for (int b = 0; b < nrBits; b++) painter.drawPort(index + b);
      if (!painter.isPrintView()) {
        final var name =
            (dir == PioAttributes.PORT_BIDIR) ? S.get("SocPioBidirs") : S.get("SocPioOutputs");
        g2.drawRect(loc.getX() + 40, loc.getY(), 340, 40);
        GraphicsUtil.drawCenteredText(g2, name, loc.getX() + 210, loc.getY() + 25);
        GraphicsUtil.drawCenteredText(g2, "0", loc.getX() + 370, loc.getY() + 10);
        if (nrBits > 9) {
          GraphicsUtil.drawCenteredText(
              g2, Integer.toString(nrBits - 1), loc.getX() + 380 - nrBits * 10, loc.getY() + 10);
        } else {
          for (int b = 1; b < nrBits; b++)
            GraphicsUtil.drawCenteredText(
                g2, Integer.toString(b), loc.getX() + 370 - b * 10, loc.getY() + 10);
        }
      }
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var myState = state.getAttributeValue(PioAttributes.PIO_STATE);
    myState.handleOperations(state, false);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g2 = (Graphics2D) painter.getGraphics();
    final var loc = painter.getLocation();
    g2.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    painter.drawBounds();
    painter.drawLabel();
    paintPins(painter, g2, loc);
    final var f = g2.getFont();
    g2.setFont(StdAttr.DEFAULT_LABEL_FONT);
    GraphicsUtil.drawCenteredText(g2, S.get("SocPioComponent"), loc.getX() + 210, loc.getY() + 50);
    g2.setFont(f);
    if (painter.isPrintView()) return;
    painter
        .getAttributeValue(SocSimulationManager.SOC_BUS_SELECT)
        .paint(g2, Bounds.create(loc.getX() + 45, loc.getY() + 61, 330, 18));
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    return (key == MenuExtender.class)
        ? new PioMenu(instance)
        : super.getInstanceFeature(instance, key);
  }

  @Override
  public SocBusSlaveInterface getSlaveInterface(AttributeSet attrs) {
    return attrs.getValue(PioAttributes.PIO_STATE);
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
