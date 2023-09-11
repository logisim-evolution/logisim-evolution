/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.tcl;

import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringGetter;
import com.cburch.logisim.util.StringUtil;
import java.awt.Color;
import java.awt.Font;
import java.util.Arrays;
import java.util.WeakHashMap;

/**
 * This is the component one should extend to implement the different TCL components.
 *
 * @author christian.mueller@heig-vd.ch
 */
public abstract class TclComponent extends InstanceFactory {

  public static class PortDescription {

    private final String name;
    private final String type;
    private final BitWidth width;

    public PortDescription(String name, String type, int width) {
      this.name = name;
      this.type = type;
      this.width = BitWidth.create(width);
    }

    public String getName() {
      return this.name;
    }

    public String getType() {
      return this.type;
    }

    public BitWidth getWidth() {
      return this.width;
    }
  }

  protected static <T> T[] concat(T[] first, T[] second) {
    T[] result = Arrays.copyOf(first, first.length + second.length);
    System.arraycopy(second, 0, result, first.length, second.length);
    return result;
  }

  static final int WIDTH = 140;
  static final int HEIGHT = 40;

  static final int PORT_GAP = 10;
  static final int X_PADDING = 5;

  private Port[] inputs;
  private Port[] outputs;

  private final WeakHashMap<Instance, TclComponentListener> contentListeners;

  public TclComponent(String name, StringGetter displayName) {
    super(name, displayName);

    this.contentListeners = new WeakHashMap<>();

    inputs = new Port[0];
    outputs = new Port[0];

    setIcon(new ArithmeticIcon("TCL", 3));
  }

  //
  // methods for instances
  //
  @Override
  protected void configureNewInstance(Instance instance) {
    TclComponentListener listener = new TclComponentListener(instance);

    contentListeners.put(instance, listener);

    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new TclComponentAttributes();
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    int nbInputs = inputs.length;
    int nbOutputs = outputs.length;

    return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs) * PORT_GAP + HEIGHT);
  }


  /**
   * This was taken from VHDL component
   *
   * @param painter
   */
  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    var metric = g.getFontMetrics();
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));

    final var bds = painter.getBounds();
    final var x0 = bds.getX() + (bds.getWidth() / 2);
    final var y0 = bds.getY() + metric.getHeight() + 12;
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(getDisplayName(), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    final var glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      Font font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(
          g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    int i = 0;
    for (Port p : inputs) {
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(p.getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i++ * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    }

    i = 0;
    for (Port p : outputs) {
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(p.getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + WIDTH - 5,
          bds.getY() + HEIGHT - 2 + (i++ * PORT_GAP),
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_CENTER);
    }

    painter.drawBounds();
    painter.drawPorts();
  }

  /**
   * This creates a new TCL process executing the TCL content file. Communication is done through a
   * socket
   *
   * @param state
   */
  @Override
  public void propagate(InstanceState state) {

    /*
     * The ComponentData is the persistent thing through logisim usage. It
     * doesn't change when you move the component when InstanceComponent
     * does.
     */
    final var tclComponentData = TclComponentData.get(state);
    tclComponentData.getTclWrapper().start();

    /*
     * Here we may miss the first clock if the TCL process is not soon fast
     * enought You may change this behavior, but blocking here seemed bad to
     * me
     */
    if (tclComponentData.isConnected()) {

      /* Send port values to the TCL wrapper */
      for (final var p : state.getInstance().getPorts()) {
        final var index = state.getPortIndex(p);
        final var val = state.getPortValue(index);
        final var message = p.getType() + ":" + p.getToolTip() + ":" + val.toBinaryString() + ":" + index;

        tclComponentData.send(message);
      }

      /*
       * If it is a new tick, ask the console to force the sti in the
       * console and set them in Logisim in return. If it is not a new
       * tick, simply send the updated obs to the console.
       */
      if (tclComponentData.isNewTick()) {
        tclComponentData.send("sync_force");
        getPortsFromServer(state, tclComponentData);
      } else {
        tclComponentData.send("sync_examine");
        String serverResponse;

        /* Ignore all messages until "sync" is recieved */
        while ((serverResponse = tclComponentData.receive()) != null
            && serverResponse.length() > 0
            && !serverResponse.equals("sync")) ;
      }
    }
  }

  void getPortsFromServer(InstanceState state, TclComponentData tclComponentData) {
    String serverResponse;
    while ((serverResponse = tclComponentData.receive()) != null
        && serverResponse.length() > 0
        && !serverResponse.equals("sync")) {

      final var parameters = serverResponse.split(":");

      /* Skip if we receive crap, still better than an out of range */
      if (parameters.length < 2) continue;

      var busValue = parameters[1];
      final var portId = Integer.parseInt(parameters[2]);

      // Expected response width
      final var width = state.getFactory().getPorts().get(portId).getFixedBitWidth().getWidth();

      /*
       * If the received string is too long, cut the leftmost part to
       * match the expected length
       */
      if (busValue.length() > width) busValue = busValue.substring(busValue.length() - width);

      /*
       * If the received value is not wide enough, complete with X on
       * the MSB
       */
      final var vectorValues = new Value[width];
      for (var i = width - 1; i >= busValue.length(); i--) {
        vectorValues[i] = Value.UNKNOWN;
      }

      /* Transform char to Logisim Value */
      var idx = busValue.length() - 1;
      for (final var bit : busValue.toCharArray()) {

        try {
          vectorValues[idx] = switch (Character.getNumericValue(bit)) {
            case 0 -> Value.FALSE;
            case 1 -> Value.TRUE;
            default -> Value.UNKNOWN;
          };
        } catch (NumberFormatException e) {
          vectorValues[idx] = Value.ERROR;
        }
        idx--;
      }

      /* Affect the value to the port */
      state.setPort(portId, Value.create(vectorValues), 1);
    }
  }

  /**
   * When setting ports we also set some local attributes so we can manage inputs and outputs
   * separately
   *
   * @param inputs
   * @param outputs
   */
  void setPorts(Port[] inputs, Port[] outputs) {
    this.inputs = inputs;
    this.outputs = outputs;
    setPorts(concat(inputs, outputs));
  }

  void updatePorts(Instance instance) {
    // Ports are static so they are never updated (only values will but
    // that's not this handler
  }
}
