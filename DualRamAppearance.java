/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 * Modified and converted to Dual Port RAM by: abdelrhman alaa
 * GitHub: https://github.com/abdelrhman1040
 * Date: February 2026
 *
 * https://github.com/logisim-evolution/
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;

public class DualRamAppearance {
  public static int getClassicPortBoffset(AttributeSet attrs) {
    int totalHeight = getControlHeight(attrs) + getExtraHeight(attrs);
    int rawOffset = totalHeight / 2;
    return ((rawOffset + 5) / 10) * 10;
  }

  public static int getNrAddrPorts(AttributeSet attrs) {
    return 2;
  }

  public static int getNrDataInPorts(AttributeSet attrs) {
    if (!seperatedBus(attrs))
      return 0;
    return getNrDataOutPorts(attrs);
  }

  public static int getNrDataOutPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR) || attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL))
        return 4;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD))
        return 8;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO))
        return 16;
    }
    return 2;
  }

  public static int getNrDataPorts(AttributeSet attrs) {
    return getNrDataInPorts(attrs) + getNrDataOutPorts(attrs);
  }

  public static int getNrOEPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR))
      return 0;
    if (!seperatedBus(attrs) || (seperatedBus(attrs) && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES)))
      return 2;
    else
      return 0;
  }

  public static int getNrWEPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR))
      return 0;
    return 2;
  }

  public static int getNrClkPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR))
      return 0;
    boolean async = !synchronous(attrs);
    if (async && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES))
      return 0;
    else
      return 2;
  }

  public static int getNrLEPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR))
      return 0;
    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL))
        return 4;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD))
        return 8;
      if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO))
        return 16;
    }
    return 0;
  }

  public static int getNrBEPorts(AttributeSet attrs) {
    if (!attrs.containsAttribute(Mem.ENABLES_ATTR))
      return 0;
    final var async = !synchronous(attrs);

    if (attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES)
        && attrs.containsAttribute(DualRamAttributes.ATTR_ByteEnables)
        && attrs.getValue(DualRamAttributes.ATTR_ByteEnables).equals(DualRamAttributes.BUS_WITH_BYTEENABLES)
        && !async) {

      final var nrBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
      int countPerPort = (nrBits < 9) ? 0 : (nrBits + 7) >> 3;
      return countPerPort * 2;
    }
    return 0;
  }

  public static int getNrClrPorts(AttributeSet attrs) {
    return (attrs.containsAttribute(DualRamAttributes.CLEAR_PIN) && attrs.getValue(DualRamAttributes.CLEAR_PIN))
        ? 1
        : 0;
  }

  public static int getNrOfPorts(AttributeSet attrs) {
    return getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs) + getNrWEPorts(attrs)
        + getNrClkPorts(attrs) + getNrLEPorts(attrs) + getNrBEPorts(attrs) + getNrClrPorts(attrs);
  }

  public static int getAddrIndex(int portIndex, AttributeSet attrs) {
    if (portIndex < 2)
      return portIndex;
    return -1;
  }

  public static int getDataInIndex(int portIndex, AttributeSet attrs) {
    if (!seperatedBus(attrs))
      return getDataOutIndex(portIndex, attrs);
    int portOffset = getNrAddrPorts(attrs) + getNrDataOutPorts(attrs);
    return getDataOffset(portOffset, portIndex, attrs);
  }

  public static int getDataOutIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs);
    return getDataOffset(portOffset, portIndex, attrs);
  }

  public static int getOEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs);
    int nrOEs = getNrOEPorts(attrs);
    if (nrOEs == 0 || portIndex < 0)
      return -1;
    if (portIndex < nrOEs)
      return portOffset + portIndex;
    return -1;
  }

  public static int getWEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs);
    int nrWEs = getNrWEPorts(attrs);
    if (nrWEs == 0 || portIndex < 0)
      return -1;
    if (portIndex < nrWEs)
      return portOffset + portIndex;
    return -1;
  }

  public static int getClkIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs) + getNrWEPorts(attrs);
    if (getNrClkPorts(attrs) == 0 || portIndex < 0 || portIndex >= getNrClkPorts(attrs)) {
      return -1;
    }
    return portOffset + portIndex;
  }

  public static int getLEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs) + getNrWEPorts(attrs)
        + getNrClkPorts(attrs);
    int nrLEs = getNrLEPorts(attrs);
    if (nrLEs == 0 || portIndex < 0)
      return -1;
    if (portIndex < nrLEs)
      return portOffset + portIndex;
    return -1;
  }

  public static int getBEIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs) + getNrWEPorts(attrs)
        + getNrClkPorts(attrs) + getNrLEPorts(attrs);
    int nrBEs = getNrBEPorts(attrs);
    if (nrBEs == 0 || portIndex < 0)
      return -1;
    if (portIndex < nrBEs)
      return portOffset + portIndex;
    return -1;
  }

  public static int getClrIndex(int portIndex, AttributeSet attrs) {
    int portOffset = getNrAddrPorts(attrs) + getNrDataPorts(attrs) + getNrOEPorts(attrs) + getNrWEPorts(attrs)
        + getNrClkPorts(attrs) + getNrLEPorts(attrs) + getNrBEPorts(attrs);
    int nrClrs = getNrClrPorts(attrs);
    if (nrClrs == 0 || portIndex < 0)
      return -1;
    if (portIndex < nrClrs)
      return portOffset + portIndex;
    return -1;
  }

  public static void configurePorts(Instance instance) {
    final var attrs = instance.getAttributeSet();
    final var ps = new Port[getNrOfPorts(attrs)];
    for (var i = 0; i < getNrAddrPorts(attrs); i++)
      ps[getAddrIndex(i, attrs)] = getAddrPort(i, attrs);
    for (var i = 0; i < getNrDataInPorts(attrs); i++)
      ps[getDataInIndex(i, attrs)] = getDataInPort(i, attrs);
    for (var i = 0; i < getNrDataOutPorts(attrs); i++)
      ps[getDataOutIndex(i, attrs)] = getDataOutPort(i, attrs, instance.getBounds().getWidth());
    for (var i = 0; i < getNrOEPorts(attrs); i++)
      ps[getOEIndex(i, attrs)] = getOEPort(i, attrs);
    for (var i = 0; i < getNrWEPorts(attrs); i++)
      ps[getWEIndex(i, attrs)] = getWEPort(i, attrs);
    for (var i = 0; i < getNrClkPorts(attrs); i++)
      ps[getClkIndex(i, attrs)] = getClkPort(i, attrs);
    for (var i = 0; i < getNrLEPorts(attrs); i++)
      ps[getLEIndex(i, attrs)] = getLEPort(i, attrs);
    for (var i = 0; i < getNrBEPorts(attrs); i++)
      ps[getBEIndex(i, attrs)] = getBEPort(i, attrs);
    for (var i = 0; i < getNrClrPorts(attrs); i++)
      ps[getClrIndex(i, attrs)] = getClrPort(i, attrs);
    instance.setPorts(ps);
  }

  private static int getExtraHeight(AttributeSet attrs) {
    int portsLen = (getNrLEPorts(attrs) + 1) * 10;
    if (classicAppearance(attrs)) {
      return Math.max(70, portsLen);
    } else {
      int dataLen = attrs.getValue(Mem.DATA_ATTR).getWidth() * 40;
      return Math.max(dataLen, portsLen);
    }
  }

  public static Bounds getBounds(AttributeSet attrs) {
    int xoffset = (seperatedBus(attrs)) ? 40 : 50;
    int widthOffset = classicAppearance(attrs) ? 40 : xoffset;
    int len = getExtraHeight(attrs);
    int totalHeight = getControlHeight(attrs) + len;
    return Bounds.create(0, 0, Mem.SymbolWidth + widthOffset, totalHeight);
  }

  public static boolean classicAppearance(AttributeSet attrs) {
    return attrs.getValue(StdAttr.APPEARANCE).equals(StdAttr.APPEAR_CLASSIC);
  }

  public static void drawRamClassic(InstancePainter painter) {
    final var attrs = painter.getAttributeSet();
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var inst = painter.getInstance();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    /* draw label */
    final var Label = painter.getAttributeValue(StdAttr.LABEL);
    if (Label != null && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      final var font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, Label, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    /* draw body */
    painter.drawBounds();

    /* draw connections */
    drawConnections(inst, attrs, painter);

    /* draw the size */
    final var type = "Dual Port RAM ";
    GraphicsUtil.drawCenteredText(g,
        type + Mem.getSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR).getWidth())
            + " x " + painter.getAttributeValue(Mem.DATA_ATTR).getWidth(),
        bds.getX() + (Mem.SymbolWidth / 2) + 20,
        bds.getY() + 6);
    /* draw the contents */
    if (painter.getShowState()) {
      MemState memState = (MemState) inst.getData(painter.getCircuitState());
      if (memState instanceof DualRamState) {
        DualRamState state = (DualRamState) memState;
        int highlightCount = getNrToHighlight(attrs);

        int totalHeight = bds.getHeight() - 20;
        int halfHeight = totalHeight / 2;

        long addrA = state.getCurrent(0);
        state.setCurrent(0, addrA);
        state.scrollToShow(addrA);
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            30,
            15,
            bds.getWidth() - 60,
            halfHeight,
            highlightCount);

        long addrB = state.getCurrent(1);
        state.setCurrent(0, addrB);
        state.scrollToShow(addrB);

        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            30,
            15 + halfHeight,
            bds.getWidth() - 60,
            halfHeight,
            highlightCount);

        state.setCurrent(0, addrA);
        state.scrollToShow(addrA);
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            30,
            15,
            bds.getWidth() - 60,
            halfHeight,
            highlightCount);
      }
    }
  }

  public static void drawRamEvolution(InstancePainter painter) {
    final var attrs = painter.getAttributeSet();
    final var g = painter.getGraphics();
    final var bds = painter.getBounds();
    final var inst = painter.getInstance();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    /* draw label */
    final var Label = painter.getAttributeValue(StdAttr.LABEL);
    if (Label != null && painter.getAttributeValue(StdAttr.LABEL_VISIBILITY)) {
      final var font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(g, Label, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }
    /* draw shape */
    drawControlBlock(inst, attrs, painter);
    drawDataBlocks(inst, attrs, painter);
    /* draw connections */
    drawConnections(inst, attrs, painter);
    /* draw the size */
    final var type = "Dual Port RAM ";
    GraphicsUtil.drawCenteredText(g,
        type + Mem.getSizeLabel(painter.getAttributeValue(Mem.ADDR_ATTR).getWidth())
            + " x " + painter.getAttributeValue(Mem.DATA_ATTR).getWidth(),
        bds.getX() + (Mem.SymbolWidth / 2) + 20, bds.getY() + 6);
    /* draw the contents */
    if (painter.getShowState()) {
      final var memState = inst.getData(painter.getCircuitState());

      if (memState instanceof DualRamState) {
        DualRamState state = (DualRamState) memState;
        int highlightCount = getNrToHighlight(attrs);

        int totalAvailHeight = bds.getHeight() - 10 - getControlHeight(attrs);
        int boxHeight = (totalAvailHeight / 2) - 5;

        long addrA = state.getCurrent(0);

        state.setCurrent(0, addrA);
        state.scrollToShow(addrA);
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            50,
            getControlHeight(attrs) + 5,
            bds.getWidth() - 100,
            boxHeight,
            highlightCount);

        long addrB = state.getCurrent(1);
        state.setCurrent(0, addrB);
        state.scrollToShow(addrB);
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            50,
            getControlHeight(attrs) + 5 + boxHeight + 10,
            bds.getWidth() - 100,
            boxHeight,
            highlightCount);

        state.setCurrent(0, addrA);
        state.scrollToShow(addrA);
        state.paint(
            painter.getGraphics(),
            bds.getX(),
            bds.getY(),
            50,
            getControlHeight(attrs) + 5,
            bds.getWidth() - 100,
            boxHeight,
            highlightCount);
      }
    }
  }

  public static int getControlHeight(AttributeSet attrs) {
    var result = 60;
    if (attrs.containsAttribute(Mem.ENABLES_ATTR) && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      if (!classicAppearance(attrs))
        result += 30;
      result += (getNrLEPorts(attrs) / 2) * 10;
    } else if (attrs.containsAttribute(StdAttr.TRIGGER)) {
      final var async = !synchronous(attrs);
      result += 20;
      if (!async)
        result += 10;
      result += (getNrLEPorts(attrs) / 2) * 10;
    }
    return result * 2;
  }

  /* here all private handles are defined */
  private static int getNrToHighlight(AttributeSet attrs) {
    if (attrs.containsAttribute(Mem.ENABLES_ATTR) && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USEBYTEENABLES))
      return 1;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.DUAL))
      return 2;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.QUAD))
      return 4;
    if (attrs.getValue(Mem.LINE_ATTR).equals(Mem.OCTO))
      return 8;
    return 1;
  }

  private static int getDataOffset(int portOffset, int portIndex, AttributeSet attrs) {
    boolean checkLines = !attrs.containsAttribute(Mem.ENABLES_ATTR)
        || attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES);

    if (checkLines && portIndex < getNrDataOutPorts(attrs)) {
      return portOffset + portIndex;
    }

    if (portIndex == 0)
      return portOffset;
    if (portIndex == 1)
      return portOffset + 1;

    return -1;
  }

  private static Port getAddrPort(int portIndex, AttributeSet attrs) {
    final var nrAddrs = getNrAddrPorts(attrs);
    if (nrAddrs == 0 || portIndex < 0 || portIndex >= nrAddrs)
      return null;

    var totalHeight = getControlHeight(attrs);
    var singleRamHeight = totalHeight / 2;

    var ypos = 0;
    final var classic = classicAppearance(attrs);

    if (!classic) {
      int offsetB = singleRamHeight - 10;
      ypos = (portIndex == 0) ? 20 : 20 + offsetB;
    } else {
      int splitIndex = nrAddrs / 2;
      ypos = 10;
      if (portIndex >= splitIndex)
        ypos += getClassicPortBoffset(attrs);
    }

    final var result = new Port(0, ypos, Port.INPUT, attrs.getValue(Mem.ADDR_ATTR));
    result.setToolTip(S.getter("memAddrTip"));
    return result;
  }

  private static Port getDataInPort(int portIndex, AttributeSet attrs) {
    final var nrDins = getNrDataInPorts(attrs);
    if (nrDins == 0 || portIndex < 0 || portIndex >= nrDins)
      return null;

    var totalHeight = getControlHeight(attrs);
    int split = nrDins / 2;
    boolean isClassic = classicAppearance(attrs);

    var ypos = isClassic ? (totalHeight / 2) : totalHeight;

    final var bits = attrs.getValue(Mem.DATA_ATTR);
    if (!isClassic && bits.getWidth() == 1)
      ypos += 10;

    ypos += (portIndex % split) * 10;

    if (portIndex >= split) {
      ypos += isClassic ? getClassicPortBoffset(attrs) : (bits.getWidth() * 20);
    }

    final var result = new Port(0, ypos, Port.INPUT, bits);

    switch (portIndex) {
      case 0:
        if (nrDins == 1)
          result.setToolTip(S.getter("ramInTip"));
        else
          result.setToolTip(S.getter("ramInTip0"));
        break;
      case 1:
        result.setToolTip(S.getter("ramInTip1"));
        break;
      case 2:
        result.setToolTip(S.getter("ramInTip2"));
        break;
      case 3:
        result.setToolTip(S.getter("ramInTip3"));
        break;
    }
    return result;
  }

  private static Port getDataOutPort(int portIndex, AttributeSet attrs, int xpos) {
    final var nrDouts = getNrDataOutPorts(attrs);
    if (nrDouts == 0 || portIndex < 0 || portIndex >= nrDouts)
      return null;
    final var totalHeight = getControlHeight(attrs);
    final int splitIndex = nrDouts / 2;
    final boolean isClassic = classicAppearance(attrs);
    final var bits = attrs.getValue(Mem.DATA_ATTR);

    var portType = (!seperatedBus(attrs) && attrs.containsAttribute(Mem.ENABLES_ATTR))
        ? Port.INOUT
        : Port.OUTPUT;

    var ypos = isClassic ? (totalHeight / 2) : totalHeight;
    if (!isClassic && bits.getWidth() == 1)
      ypos += 10;
    ypos += (portIndex % splitIndex) * 10;
    if (portIndex >= splitIndex) {
      ypos += isClassic ? getClassicPortBoffset(attrs) : (bits.getWidth() * 20);
    }

    final var result = new Port(xpos, ypos, portType, bits);
    switch (portIndex) {
      case 0:
        if (nrDouts == 1)
          result.setToolTip(S.getter("memDataTip"));
        else
          result.setToolTip(S.getter("memDataTip0"));
        break;
      case 1:
        result.setToolTip(S.getter("memDataTip1"));
        break;
      case 2:
        result.setToolTip(S.getter("memDataTip2"));
        break;
      case 3:
        result.setToolTip(S.getter("memDataTip3"));
        break;
      default:
        // none
    }
    return result;
  }

  private static Port getOEPort(int portIndex, AttributeSet attrs) {
    final var nrOEs = getNrOEPorts(attrs);
    if (nrOEs == 0 || portIndex < 0 || portIndex >= nrOEs)
      return null;

    final var totalHeight = getControlHeight(attrs);
    final int splitIndex = nrOEs / 2;
    final boolean isClassic = classicAppearance(attrs);

    var ypos = isClassic ? 60 : 70;
    if (isClassic && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      ypos = 20;
    }
    if (!isClassic) {
      if (portIndex > 0)
        ypos += (totalHeight / 2) - 10;
    } else if (portIndex >= splitIndex) {
      ypos = getClassicPortBoffset(attrs) + 60;
    }

    final var result = new Port(0, ypos, Port.INPUT, 1);
    result.setToolTip(S.getter("ramOETip"));
    return result;
  }

  private static Port getWEPort(int portIndex, AttributeSet attrs) {
    final var nrWEs = getNrWEPorts(attrs);
    if (nrWEs == 0 || portIndex < 0 || portIndex >= nrWEs)
      return null;

    final var totalHeight = getControlHeight(attrs);
    final int splitIndex = nrWEs / 2;
    final boolean isClassic = classicAppearance(attrs);

    var ypos = isClassic ? 50 : 60;
    if (isClassic && attrs.getValue(Mem.ENABLES_ATTR).equals(Mem.USELINEENABLES)) {
      ypos = 30;
    }

    if (!isClassic) {
      if (portIndex > 0)
        ypos += (totalHeight / 2) - 10;
    } else if (portIndex >= splitIndex) {
      ypos += getClassicPortBoffset(attrs);
    }

    final var result = new Port(0, ypos, Port.INPUT, 1);
    result.setToolTip(S.getter("ramWETip"));
    return result;
  }

  private static Port getClkPort(int portIndex, AttributeSet attrs) {
    final var nrClks = getNrClkPorts(attrs);
    if (nrClks <= 0 || portIndex < 0 || portIndex >= nrClks)
      return null;

    final var totalHeight = getControlHeight(attrs);
    final int splitIndex = nrClks / 2;
    final boolean isClassic = classicAppearance(attrs);
    final boolean useLineEnables = Mem.USELINEENABLES.equals(attrs.getValue(Mem.ENABLES_ATTR));

    final int nrLEs = getNrLEPorts(attrs);
    final int nrBEs = getNrBEPorts(attrs);

    var ypos = 0;
    if (!isClassic) {
      ypos = 80 + (nrLEs / 2 * 10) + (nrBEs / 2 * 10);
    } else {
      ypos = useLineEnables ? 40 + (nrLEs * 5) + (nrBEs * 5) : 70;
    }

    if (!isClassic) {
      if (portIndex >= splitIndex)
        ypos += (totalHeight / 2) - 10;
    } else if (portIndex >= splitIndex) {
      ypos += getClassicPortBoffset(attrs);
    }

    final var result = new Port(0, ypos, Port.INPUT, 1);
    result.setToolTip(S.getter("ramClkTip"));
    return result;
  }

  private static Port getLEPort(int portIndex, AttributeSet attrs) {
    final var nrLEs = getNrLEPorts(attrs);
    if (nrLEs == 0 || portIndex < 0 || portIndex >= nrLEs)
      return null;

    final var totalHeight = getControlHeight(attrs);
    final int splitIndex = nrLEs / 2;
    final boolean isClassic = classicAppearance(attrs);
    final boolean useLineEnables = Mem.USELINEENABLES.equals(attrs.getValue(Mem.ENABLES_ATTR));

    var ypos = 80;
    if (isClassic && useLineEnables) {
      ypos = 40;
    }

    if (splitIndex > 0) {
      ypos += (portIndex % splitIndex) * 10;
    }

    if (!isClassic) {
      if (portIndex >= splitIndex)
        ypos += (totalHeight / 2) - 10;
    } else if (portIndex >= splitIndex && useLineEnables) {
      ypos += getClassicPortBoffset(attrs);
    }

    final var result = new Port(0, ypos, Port.INPUT, 1);

    switch (portIndex % 4) {
      case 0 -> result.setToolTip(S.getter("ramLETip0"));
      case 1 -> result.setToolTip(S.getter("ramLETip1"));
      case 2 -> result.setToolTip(S.getter("ramLETip2"));
      case 3 -> result.setToolTip(S.getter("ramLETip3"));
      default -> {
      } // none
    }
    return result;
  }

  private static Port getBEPort(int portIndex, AttributeSet attrs) {
    final var nrBEs = getNrBEPorts(attrs);
    if (nrBEs == 0 || portIndex < 0 || portIndex >= nrBEs)
      return null;

    int offsetB = getControlHeight(attrs) / 2 - 10;
    int splitIndex = nrBEs / 2;

    int leOffset = (getNrLEPorts(attrs) / 2) * 10;

    int relativeIndex = portIndex % splitIndex;
    var ypos = 60 + leOffset + (splitIndex - relativeIndex - 1) * 10;

    if (portIndex >= splitIndex)
      ypos += offsetB;

    final var result = new Port(0, ypos, Port.INPUT, 1);
    switch (portIndex % 4) {
      case 0 -> result.setToolTip(S.getter("ramByteEnableTip0"));
      case 1 -> result.setToolTip(S.getter("ramByteEnableTip1"));
      case 2 -> result.setToolTip(S.getter("ramByteEnableTip2"));
      case 3 -> result.setToolTip(S.getter("ramByteEnableTip3"));
      default -> {
      }
    }
    return result;
  }

  private static Port getClrPort(int portIndex, AttributeSet attrs) {
    if (getNrClrPorts(attrs) == 0 || portIndex != 0)
      return null;
    final var result = new Port(40, 0, Port.INPUT, 1);
    result.setToolTip(S.getter("ramClrPin"));
    return result;
  }

  private static boolean seperatedBus(AttributeSet attrs) {
    Object bus = attrs.getValue(DualRamAttributes.ATTR_DBUS);
    return (bus == null || bus.equals(DualRamAttributes.BUS_SEP));
  }

  private static boolean synchronous(AttributeSet attrs) {
    return attrs.containsAttribute(StdAttr.TRIGGER)
        && (attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_RISING)
            || attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_FALLING));
  }

  private static void drawConnections(Instance inst, AttributeSet attrs, InstancePainter painter) {
    boolean classic = classicAppearance(attrs);
    final var g = (Graphics2D) painter.getGraphics().create();
    final var font = g.getFont();
    g.setStroke(new BasicStroke(4));
    String label;
    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var nrOfDataPorts = Math.max(getNrDataInPorts(attrs), getNrDataOutPorts(attrs));
    int inputSplit = getNrDataInPorts(attrs) / 2;

    /* Draw data input connections */
    for (var i = 0; i < getNrDataInPorts(attrs); i++) {
      label = !classic ? "" : getNrDataInPorts(attrs) == 1 ? "D" : "D" + i;
      final var idx = getDataInIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        final var x = loc.getX();
        final var y = loc.getY();
        if (nrOfBits == 1) {
          g.setStroke(new BasicStroke(2));
          if (nrOfDataPorts > 1) {
            final var xpos = new int[4];
            final var ypos = new int[4];
            xpos[0] = x;
            xpos[1] = xpos[2] = x + 4 + i * 4;
            xpos[3] = x + 20;
            ypos[0] = ypos[1] = y;
            ypos[2] = ypos[3] = y - (i + 1) * 6;
            g.drawPolyline(xpos, ypos, 4);
          } else {
            g.drawLine(x, y, x + 20, y);
          }
          g.setStroke(new BasicStroke(4));
        } else {
          if (i != 0 && i != inputSplit) {
            if (i == 3 && nrOfBits == 2)
              g.drawLine(loc.getX(), loc.getY(), loc.getX() + 4, loc.getY() - 4);
            else
              g.drawLine(loc.getX(), loc.getY(), loc.getX() + 4, loc.getY() + 4);
          } else {
            final var xpos = new int[3];
            final var ypos = new int[3];
            g.setStroke(new BasicStroke(2));
            xpos[0] = x + 5;
            xpos[1] = x + 10;
            xpos[2] = x + 20;
            ypos[0] = y + 5;
            ypos[1] = ypos[2] = y + 10;
            g.setFont(font.deriveFont(7.0f));
            g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
            for (var j = 0; j < nrOfBits; j++) {
              g.drawPolyline(xpos, ypos, 3);
              GraphicsUtil.drawText(g, Integer.toString(j), xpos[2] - 3, ypos[2] - 3, GraphicsUtil.H_RIGHT,
                  GraphicsUtil.V_BASELINE);
              ypos[0] += 20;
              ypos[1] += 20;
              ypos[2] += 20;
            }
            g.setColor(Value.multiColor);
            g.setStroke(new BasicStroke(4));
            xpos[0] = x;
            xpos[1] = xpos[2] = x + 5;
            ypos[0] = y;
            ypos[1] = y + 5;
            ypos[2] = y + 5 + (nrOfBits - 1) * 20;
            g.drawPolyline(xpos, ypos, 3);
          }
        }
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    /* Draw data output connections (& in/out) */
    int outputSplit = getNrDataOutPorts(attrs) / 2;

    /* Draw data output connections */
    for (var i = 0; i < getNrDataOutPorts(attrs); i++) {
      label = !classic ? "" : getNrDataOutPorts(attrs) == 1 ? "D" : "D" + i;
      int idx = getDataOutIndex(i, attrs);
      if (!classic) {
        final var seperate = seperatedBus(attrs) || !attrs.containsAttribute(DualRamAttributes.ATTR_DBUS);
        final var loc = inst.getPortLocation(idx);
        final var x = loc.getX();
        final var y = loc.getY();
        if (nrOfBits == 1) {
          g.setStroke(new BasicStroke(2));
          if (nrOfDataPorts > 1) {
            final var xpos = new int[4];
            final var ypos = new int[4];
            xpos[0] = x;
            xpos[1] = xpos[2] = x - (i + 1) * 4;
            xpos[3] = x - 20;
            ypos[0] = ypos[1] = y;
            ypos[2] = ypos[3] = y - (i + 1) * 6;
            g.drawPolyline(xpos, ypos, 4);
          } else {
            g.drawLine(x, y, x - 20, y);
          }
          if (!seperate && i == 0)
            drawBidir(g, x - 20, y);
          g.setStroke(new BasicStroke(4));
        } else {
          if (i != 0 && i != outputSplit) {
            if (i == 3 && nrOfBits == 2)
              g.drawLine(loc.getX(), loc.getY(), loc.getX() - 4, loc.getY() - 4);
            else
              g.drawLine(loc.getX(), loc.getY(), loc.getX() - 4, loc.getY() + 4);
          } else {
            final var xpos = new int[3];
            final var ypos = new int[3];
            g.setStroke(new BasicStroke(2));
            xpos[0] = x - 5;
            xpos[1] = x - 10;
            xpos[2] = x - 20;
            ypos[0] = y + 5;
            ypos[1] = ypos[2] = y + 10;
            g.setFont(font.deriveFont(7.0f));
            g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
            for (var j = 0; j < nrOfBits; j++) {
              g.drawPolyline(xpos, ypos, 3);
              GraphicsUtil.drawText(g, Integer.toString(j), xpos[2] + 3, ypos[2] - 3, GraphicsUtil.H_LEFT,
                  GraphicsUtil.V_BASELINE);
              if (!seperate)
                drawBidir(g, xpos[2], ypos[2]);
              ypos[0] += 20;
              ypos[1] += 20;
              ypos[2] += 20;
            }
            g.setStroke(new BasicStroke(4));
            g.setColor(Value.multiColor);
            xpos[0] = x;
            xpos[1] = xpos[2] = x - 5;
            ypos[0] = y;
            ypos[1] = y + 5;
            ypos[2] = y + 5 + (nrOfBits - 1) * 20;
            g.drawPolyline(xpos, ypos, 3);
          }
        }
      }
      painter.drawPort(idx, label, Direction.WEST);
    }
    // ... (rest of the draw connections: Addr, OE, WE, Clk, LE, BE, Clr)
    // Same as original, no changes needed below this point for drawing logic
    // EXCEPT I will just paste the rest to ensure you have the full file.

    for (var i = 0; i < getNrAddrPorts(attrs); i++) {
      label = !classic ? "" : getNrAddrPorts(attrs) == 1 ? "A" : "A" + i;
      int idx = getAddrIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        int x = loc.getX();
        int y = loc.getY();
        final var xpos = new int[3];
        final var ypos = new int[3];
        g.setStroke(new BasicStroke(2));
        g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
        xpos[0] = x + 5;
        xpos[1] = x + 10;
        xpos[2] = x + 20;
        ypos[0] = y + 5;
        ypos[1] = ypos[2] = y + 10;
        g.drawPolyline(xpos, ypos, 3);
        for (var j = 0; j < 3; j++) {
          ypos[j] += 20;
          if (attrs.getValue(Mem.ADDR_ATTR).getWidth() > 2)
            g.drawLine(x + 15, y + 13 + j * 6, x + 15, y + 15 + j * 6);
        }
        g.drawPolyline(xpos, ypos, 3);
        g.setColor(Value.multiColor);
        g.setStroke(new BasicStroke(4));
        xpos[0] = x;
        xpos[1] = xpos[2] = x + 5;
        ypos[0] = y;
        ypos[1] = y + 5;
        ypos[2] = y + 25;
        g.drawPolyline(xpos, ypos, 3);
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.setStroke(new BasicStroke(2));

    for (var i = 0; i < getNrOEPorts(attrs); i++) {
      label = !classic ? "" : getNrOEPorts(attrs) == 1 ? "OE" : "OE" + i;
      final var idx = getOEIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX() + 20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    for (var i = 0; i < getNrWEPorts(attrs); i++) {
      label = !classic ? "" : getNrWEPorts(attrs) == 1 ? "WE" : "WE" + i;
      final var idx = getWEIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX() + 20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    for (var i = 0; i < getNrClkPorts(attrs); i++) {
      final var idx = getClkIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        var xend = 20;
        if (attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_FALLING)
            || attrs.getValue(StdAttr.TRIGGER).equals(StdAttr.TRIG_LOW)) {
          xend -= 8;
          g.drawOval(loc.getX() + 12, loc.getY() - 4, 8, 8);
        }
        g.drawLine(loc.getX(), loc.getY(), loc.getX() + xend, loc.getY());
        if (synchronous(attrs))
          painter.drawClockSymbol(loc.getX() + 20, loc.getY());
        painter.drawPort(idx);
      } else {
        if (synchronous(attrs))
          painter.drawClock(idx, Direction.EAST);
        else
          painter.drawPort(idx, getNrClkPorts(attrs) == 1 ? "E" : "E" + i, Direction.EAST);
      }
    }

    for (var i = 0; i < getNrLEPorts(attrs); i++) {
      label = !classic ? "" : getNrLEPorts(attrs) == 1 ? "LE" : "LE" + i;
      final var idx = getLEIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX() + 20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    for (var i = 0; i < getNrBEPorts(attrs); i++) {
      label = !classic ? "" : getNrBEPorts(attrs) == 1 ? "BE" : "BE" + i;
      int idx = getBEIndex(i, attrs);
      if (!classic) {
        final var loc = inst.getPortLocation(idx);
        g.drawLine(loc.getX(), loc.getY(), loc.getX() + 20, loc.getY());
      }
      painter.drawPort(idx, label, Direction.EAST);
    }

    for (var i = 0; i < getNrClrPorts(attrs); i++) {
      final var idx = getClrIndex(i, attrs);
      painter.drawPort(idx);
    }
    g.dispose();
  }

  private static void drawControlBlock(Instance inst, AttributeSet attrs, InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics().create();
    final var x = painter.getBounds().getX();
    final var y = painter.getBounds().getY();
    final var xpos = new int[8];
    final var ypos = new int[8];
    xpos[0] = xpos[1] = x + 30;
    xpos[2] = xpos[3] = x + 20;
    xpos[4] = xpos[5] = x + Mem.SymbolWidth + 20;
    xpos[6] = xpos[7] = x + Mem.SymbolWidth + 10;
    ypos[0] = ypos[7] = y + getControlHeight(attrs);
    ypos[1] = ypos[2] = ypos[5] = ypos[6] = ypos[0] - 10;
    ypos[3] = ypos[4] = y;
    GraphicsUtil.switchToWidth(g, 2);
    g.drawPolyline(xpos, ypos, 8);

    /* draw address text */
    for (var i = 0; i < getNrAddrPorts(attrs); i++) {
      final var idx = getAddrIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      drawAddress(g, loc.getX(), loc.getY(), attrs.getValue(Mem.ADDR_ATTR).getWidth(), i);
    }

    var cidx = 1;
    /* draw clock port text */
    for (var i = 0; i < getNrClkPorts(attrs); i++) {
      final var idx = getClkIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      final var label = synchronous(attrs) ? "C" + cidx : "E" + cidx;
      cidx++;
      g.drawString(label, loc.getX() + 33, loc.getY() + 5);
    }

    /* draw output enable text */
    for (var i = 0; i < getNrOEPorts(attrs); i++) {
      final var idx = getOEIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      final var label = "M" + cidx + " [Output enable]";
      cidx++;
      g.drawString(label, loc.getX() + 33, loc.getY() + 5);
    }

    /* draw write enable text */
    for (var i = 0; i < getNrWEPorts(attrs); i++) {
      final var idx = getWEIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      final var label = "M" + cidx + " [Write enable]";
      cidx++;
      g.drawString(label, loc.getX() + 33, loc.getY() + 5);
    }

    /* draw line enable text */
    for (var i = 0; i < getNrLEPorts(attrs); i++) {
      final var idx = getLEIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      final var label = "M" + cidx + " [Line enable " + i + "]";
      cidx++;
      g.drawString(label, loc.getX() + 33, loc.getY() + 5);
    }

    /* draw byte enable text */
    for (var i = 0; i < getNrBEPorts(attrs); i++) {
      final var idx = getBEIndex(i, attrs);
      final var loc = inst.getPortLocation(idx);
      final var label = "M" + cidx + " [Byte enable " + i + "]";
      cidx++;
      g.drawString(label, loc.getX() + 33, loc.getY() + 5);
    }
    g.dispose();
  }

  private static void drawDataBlocks(Instance inst, AttributeSet attrs, InstancePainter painter) {
    final var g = (Graphics2D) painter.getGraphics().create();
    final var x = painter.getBounds().getX() + 20;

    final var base_y = painter.getBounds().getY() + getControlHeight(attrs);

    final var width = Mem.SymbolWidth;
    final var height = 20;
    g.setFont(g.getFont().deriveFont(9.0f));

    final var nrOfBits = attrs.getValue(Mem.DATA_ATTR).getWidth();
    final var async = !synchronous(attrs)
        || (attrs.containsAttribute(Mem.ASYNC_READ) && attrs.getValue(Mem.ASYNC_READ));
    final var drawDin = attrs.containsAttribute(DualRamAttributes.ATTR_DBUS);
    final var seperate = seperatedBus(attrs) || !drawDin;

    for (int p = 0; p < 2; p++) {
      var y = base_y;
      if (p == 1)
        y += (nrOfBits * 20);
      String label = "A" + (p + 1);
      final var doutLabel = new StringBuilder();
      final var dinLabel = new StringBuilder();
      doutLabel.append(label);
      dinLabel.append(label);

      var cidx = 1;

      // Clocks Loop
      for (var i = 0; i < getNrClkPorts(attrs); i++) {
        if (i == p) {
          if (!async)
            doutLabel.append(",").append(cidx);
          dinLabel.append(",").append(cidx);
        }
        cidx++;
      }

      // OE Loop
      for (var i = 0; i < getNrOEPorts(attrs); i++) {
        if (i == p)
          doutLabel.append(",").append(cidx);
        cidx++;
      }

      // WE Loop
      for (var i = 0; i < getNrWEPorts(attrs); i++) {
        if (i == p)
          dinLabel.append(",").append(cidx);
        cidx++;
      }

      // LE Loop
      final var totalLE = getNrLEPorts(attrs);
      for (var i = 0; i < totalLE; i++) {
        boolean belongs = (p == 0 && i < totalLE / 2) || (p == 1 && i >= totalLE / 2);
        if (belongs)
          dinLabel.append(",").append(cidx);
        cidx++;
      }

      final var appendBE = getNrBEPorts(attrs) > 0;
      int beBaseIdx = cidx;
      cidx += getNrBEPorts(attrs);

      final var DLabel = seperate ? "" : "D";

      for (var i = 0; i < nrOfBits; i++) {
        g.setStroke(new BasicStroke(2));
        g.drawRect(x, y, width, height);
        g.setStroke(new BasicStroke(1));

        GraphicsUtil.drawText(g, doutLabel.toString(), x - (seperate ? 3 : 10) + Mem.SymbolWidth,
            y + (seperate ? 10 : 5), GraphicsUtil.H_RIGHT, GraphicsUtil.V_CENTER);

        if (!seperate) {
          final var xpos = new int[3];
          final var ypos = new int[3];
          xpos[0] = x - 8 + Mem.SymbolWidth;
          xpos[1] = x - 5 + Mem.SymbolWidth;
          xpos[2] = x - 2 + Mem.SymbolWidth;
          ypos[0] = ypos[2] = y + 5;
          ypos[1] = y + 8;
          g.drawPolygon(xpos, ypos, 3);
        }

        var BEIndex = "";
        if (appendBE) {
          int localBE = i >> 3;
          int globalBE = (p == 0) ? localBE : (getNrBEPorts(attrs) / 2 + localBE);
          BEIndex = "," + (beBaseIdx + globalBE);
        }

        if (drawDin)
          GraphicsUtil.drawText(
              g,
              dinLabel + BEIndex + DLabel,
              x + (seperate ? 3 : Mem.SymbolWidth - 3),
              y + (seperate ? 10 : 13),
              seperate ? GraphicsUtil.H_LEFT : GraphicsUtil.H_RIGHT,
              GraphicsUtil.V_CENTER);
        y += 20;
      }
    }
    g.dispose();
  }

  private static void drawBidir(Graphics2D g, int x, int y) {
    final var xpos = new int[4];
    final var ypos = new int[4];
    xpos[0] = xpos[3] = x - 10;
    xpos[1] = xpos[2] = x;
    ypos[0] = ypos[1] = y - 5;
    ypos[2] = ypos[3] = y + 5;
    g.drawPolyline(xpos, ypos, 4);
    xpos[0] = xpos[2] = x - 4;
    xpos[1] = x - 8;
    ypos[0] = y + 2;
    ypos[1] = y + 5;
    ypos[2] = y + 8;
    g.drawPolyline(xpos, ypos, 3);
    xpos[0] = xpos[2] = x - 6;
    xpos[1] = x - 2;
    ypos[0] = y - 8;
    ypos[1] = y - 5;
    ypos[2] = y - 2;
    g.drawPolyline(xpos, ypos, 3);
  }

  private static void drawAddress(Graphics2D g, int xpos, int ypos, int nrAddressBits, int portIndex) {
    GraphicsUtil.switchToWidth(g, 1);
    GraphicsUtil.drawText(g, "0", xpos + 22, ypos + 10, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    GraphicsUtil.drawText(
        g,
        Integer.toString(nrAddressBits - 1),
        xpos + 22,
        ypos + 30,
        GraphicsUtil.H_LEFT,
        GraphicsUtil.V_CENTER);
    String label = (portIndex == 0) ? "1" : "2";
    GraphicsUtil.drawText(g, "A" + label, xpos + 50, ypos + 20, GraphicsUtil.H_LEFT, GraphicsUtil.V_CENTER);
    g.drawLine(xpos + 40, ypos + 5, xpos + 45, ypos + 10);
    g.drawLine(xpos + 45, ypos + 10, xpos + 45, ypos + 17);
    g.drawLine(xpos + 45, ypos + 17, xpos + 48, ypos + 20);
    g.drawLine(xpos + 48, ypos + 20, xpos + 45, ypos + 23);
    g.drawLine(xpos + 45, ypos + 23, xpos + 45, ypos + 30);
    g.drawLine(xpos + 40, ypos + 35, xpos + 45, ypos + 30);
    final var size = Long.toString((1 << nrAddressBits) - 1);
    final var font = g.getFont();
    final var fm = g.getFontMetrics(font);
    final var StrSize = fm.stringWidth(size);
    g.drawLine(xpos + 70, ypos + 20, xpos + 70 + StrSize, ypos + 20);
    GraphicsUtil.drawText(g, "0", xpos + 70 + (StrSize / 2), ypos + 19, GraphicsUtil.H_CENTER, GraphicsUtil.V_BOTTOM);
    GraphicsUtil.drawText(g, size, xpos + 70 + (StrSize / 2), ypos + 21, GraphicsUtil.H_CENTER, GraphicsUtil.V_TOP);
  }
}