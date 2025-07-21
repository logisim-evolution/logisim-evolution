package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.std.io.TelnetServer.ServerHolder;
import com.cburch.logisim.tools.key.BitWidthConfigurator;
import com.cburch.logisim.tools.key.DirectionConfigurator;
import com.cburch.logisim.tools.key.JoinedConfigurator;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.event.KeyEvent;
import java.io.IOException;

public class Telnet extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files. Do NOT change as it will
   * prevent project files from loading.
   *
   * <p>Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Telnet";

  private static final int IN = 0;
  private static final int OUT = 1;

  private static final int CLK = 2;

  private static final int WR = 3;
  private static final int RD = 4;

  private static final int AVAIL = 5;

  private static final Attribute<Boolean> ATTR_TELNET_MODE =
      Attributes.forBoolean("telnetMode", S.getter("telnetModeAttr"));

  private static final Attribute<Integer> ATTR_PORT =
      Attributes.forIntegerRange("port", S.getter("telnetPortAttr"), 1, 65535);


  /**
   * @see <a href="https://github.com/logisim-evolution/logisim-evolution/issues/2284">Increase buffer size for Telnet component. #2284</a>
   * @see <a href="https://github.com/logisim-evolution/logisim-evolution/pull/2285">Update Telnet.java (Increase buffer size draft) #2285</a>
   * @todo n should be the only selectable value for buflen, which is preferable to entering a raw number. For example, provide a dropdown menu with values from 1 to 1024 (in powers of two), each multiplied by 16 × 1024, up to a maximum of 16 MB.
   */
  private static final Attribute<Integer> ATTR_BUFFER =
      Attributes.forIntegerRange("buflen", S.getter("keybBufferLengthAttr"), 1, 16*/*n=*/1024 *1024); // One 1024 should be a variable n going from 2 to 1024.

  public Telnet() {
    super(_ID, S.getter("telnetComponent"));
    setAttributes(
        new Attribute[]{
            ATTR_TELNET_MODE,
            ATTR_PORT,
            ATTR_BUFFER,
            StdAttr.EDGE_TRIGGER,
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT,
        }, new Object[]{
            false,
            8000,
            1024,
            StdAttr.TRIG_RISING,
            "",
            Direction.NORTH,
            StdAttr.DEFAULT_LABEL_FONT,
        }
    );
    setOffsetBounds(Bounds.create(-30, -20, 40, 60));
    setIconName("telnet.gif");
    setKeyConfigurator(
        JoinedConfigurator.create(
            new BitWidthConfigurator(StdAttr.WIDTH),
            new DirectionConfigurator(StdAttr.LABEL_LOC, KeyEvent.ALT_DOWN_MASK)));

    final var ps = new Port[6];
    ps[IN] = new Port(-30, 10, Port.INPUT, 8);
    ps[OUT] = new Port(10, 10, Port.OUTPUT, 8);
    ps[CLK] = new Port(-30, 20, Port.INPUT, 1);
    ps[WR] = new Port(-30, 30, Port.INPUT, 1);
    ps[RD] = new Port(10, 30, Port.INPUT, 1);
    ps[AVAIL] = new Port(10, 20, Port.OUTPUT, 1);
    ps[IN].setToolTip(S.getter("telnetInTip"));
    ps[OUT].setToolTip(S.getter("telnetOutTip"));
    ps[CLK].setToolTip(S.getter("telnetClkTip"));
    ps[WR].setToolTip(S.getter("telnetWriteTip"));
    ps[RD].setToolTip(S.getter("telnetReadTip"));
    ps[AVAIL].setToolTip(S.getter("telnetAvailableTip"));
    setPorts(ps);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();

    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    painter.drawBounds();
    painter.drawPort(IN, "in", Direction.EAST);
    painter.drawPort(OUT, "out", Direction.WEST);
    painter.drawClock(CLK, Direction.EAST);
    painter.drawPort(WR, "wr", Direction.EAST);
    painter.drawPort(RD, "rd", Direction.WEST);
    painter.drawPort(AVAIL, "av", Direction.WEST);

    var metric = g.getFontMetrics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    final var bds = painter.getBounds();
    final var x0 = bds.getX() + (bds.getWidth() / 2);
    final var y0 = bds.getY() + metric.getHeight() + 2;
    GraphicsUtil.drawText(
        g,
        "Telnet",
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);
    g.setFont(g.getFont().deriveFont(g.getFont().getSize2D() * 0.8f));
    GraphicsUtil.drawText(
        g,
        painter.getAttributeValue(ATTR_PORT).toString(),
        x0,
        y0 + metric.getHeight() - 4,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    g.setColor(painter.getAttributeValue(StdAttr.LABEL_COLOR));
    painter.drawLabel();
  }

  private TelnetServer getData(InstanceState state) {
    var data = (TelnetServer) state.getData();
    var port = state.getAttributeValue(ATTR_PORT);
    if (data == null || data.getPort() != port) {
      try {
        data = ServerHolder.INSTANCE.getServer(port, state.getAttributeValue(ATTR_BUFFER));
        data.setInstanceState(state);
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
      state.setData(data);
    } else {
      var buffer = state.getAttributeValue(ATTR_BUFFER);
      if (data.getBufferSize() != buffer) {
        data.setBufferSize(buffer);
      }
    }
    return data;
  }

  @Override
  public void propagate(InstanceState circState) {
    final var state = getData(circState);
    final var telnetMode = circState.getAttributeValue(ATTR_TELNET_MODE);
    state.setTelnetEscape(telnetMode);
    final var triggerType = circState.getAttributeValue(StdAttr.TRIGGER);
    final var clock = circState.getPortValue(CLK);
    final var write = circState.getPortValue(WR);
    final var read = circState.getPortValue(RD);
    final var in = circState.getPortValue(IN);

    synchronized (state) {
      final var lastClock = state.setLastClock(clock);
      final var go = (triggerType == StdAttr.TRIG_FALLING)
          ? lastClock == Value.TRUE && clock == Value.FALSE
          : lastClock == Value.FALSE && clock == Value.TRUE;

      circState.setPort(AVAIL, state.hasData() ? Value.TRUE : Value.FALSE, 0);

      if (read == Value.TRUE) {
        circState.setPort(OUT, Value.createKnown(8, state.getData()), 0);
      } else {
        circState.setPort(OUT, Value.UNKNOWN, 0);
      }

      if (go) {
        if (write == Value.TRUE) {
          state.send((int) in.toLongValue());
        }
        if (read == Value.TRUE) {
          state.deleteOldest();
        }
      }
    }
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    instance.addAttributeListener();
    instance.computeLabelTextField(Instance.AVOID_SIDES);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.LABEL_LOC) {
      instance.computeLabelTextField(Instance.AVOID_SIDES);
    }
  }
}
