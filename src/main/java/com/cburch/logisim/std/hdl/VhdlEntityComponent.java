/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.hdl;

import static com.cburch.logisim.vhdl.Strings.S;

import com.cburch.hdl.HdlModel;
import com.cburch.hdl.HdlModelListener;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.awt.Color;
import java.awt.Window;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VhdlEntityComponent extends InstanceFactory {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "VHDL Entity";

  static class ContentAttribute extends Attribute<VhdlContentComponent> {

    public ContentAttribute() {
      super("content", S.getter("vhdlContentAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, VhdlContentComponent value) {
      final var proj = (source instanceof Frame frame) ? frame.getProject() : null;
      return VhdlEntityAttributes.getContentEditor(source, value, proj);
    }

    @Override
    public VhdlContentComponent parse(String value) {
      VhdlContentComponent content = VhdlContentComponent.create();
      if (!content.compare(value)) content.setContent(value);
      return content;
    }

    @Override
    public String toDisplayString(VhdlContentComponent value) {
      return S.get("vhdlContentValue");
    }

    @Override
    public String toStandardString(VhdlContentComponent value) {
      return value.getContent();
    }
  }

  static class VhdlEntityListener implements HdlModelListener {

    final Instance instance;

    VhdlEntityListener(Instance instance) {
      this.instance = instance;
    }

    @Override
    public void contentSet(HdlModel source) {
      // ((InstanceState)
      // instance).getProject().getSimulator().getVhdlSimulator().fireInvalidated();
      instance.fireInvalidated();
      instance.recomputeBounds();
    }
  }

  static final Logger logger = LoggerFactory.getLogger(VhdlEntityComponent.class);

  public static final Attribute<VhdlContentComponent> CONTENT_ATTR = new ContentAttribute();
  static final int WIDTH = 140;
  static final int HEIGHT = 40;
  static final int PORT_GAP = 10;

  static final int X_PADDING = 5;

  private final WeakHashMap<Instance, VhdlEntityListener> contentListeners;

  public VhdlEntityComponent() {
    super(_ID, S.getter("vhdlComponent"), new VhdlHdlGeneratorFactory(), true);

    this.contentListeners = new WeakHashMap<>();
    this.setIcon(new ArithmeticIcon("VHDL"));
  }

  public void setSimName(AttributeSet attrs, String SName) {
    if (attrs == null) return;
    final var atrs = (VhdlEntityAttributes) attrs;
    final var label = (!attrs.getValue(StdAttr.LABEL).equals("")) ? getHDLTopName(attrs) : SName;
    if (atrs.containsAttribute(VhdlSimConstants.SIM_NAME_ATTR))
      atrs.setValue(VhdlSimConstants.SIM_NAME_ATTR, label);
  }

  public String getSimName(AttributeSet attrs) {
    if (attrs == null) return null;
    final var atrs = (VhdlEntityAttributes) attrs;
    return atrs.getValue(VhdlSimConstants.SIM_NAME_ATTR);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    final var content = instance.getAttributeValue(CONTENT_ATTR);
    final var listener = new VhdlEntityListener(instance);

    contentListeners.put(instance, listener);
    content.addHdlModelListener(listener);

    instance.addAttributeListener();
    updatePorts(instance);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new VhdlEntityAttributes();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    return attrs.getValue(CONTENT_ATTR).getName().toLowerCase();
  }

  @Override
  public String getHDLTopName(AttributeSet attrs) {
    var label = "";
    if (!attrs.getValue(StdAttr.LABEL).equals("")) {
      label = "_" + attrs.getValue(StdAttr.LABEL).toLowerCase();
    }

    return getHDLName(attrs) + label;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var content = attrs.getValue(CONTENT_ATTR);
    final var nbInputs = content.getInputsNumber();
    final var nbOutputs = content.getOutputsNumber();

    return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs) * PORT_GAP + HEIGHT);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == CONTENT_ATTR) {
      updatePorts(instance);
      instance.recomputeBounds();
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    final var g = painter.getGraphics();
    final var content = painter.getAttributeValue(CONTENT_ATTR);
    var metric = g.getFontMetrics();

    final var bds = painter.getBounds();
    final var x0 = bds.getX() + (bds.getWidth() / 2);
    final var y0 = bds.getY() + metric.getHeight() + 12;
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(content.getName(), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    final var glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      final var font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(
          g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setColor(Color.GRAY);
    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    final var inputs = content.getInputs();
    final var outputs = content.getOutputs();

    for (var i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (var i = 0; i < outputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(outputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + WIDTH - 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_RIGHT,
          GraphicsUtil.V_CENTER);

    painter.drawBounds();
    painter.drawPorts();
  }

  /**
   * Propagate signals through the VHDL component. Logisim doesn't have a VHDL simulation tool. So
   * we need to use an external tool. We send signals to Questasim/Modelsim through a socket and a
   * tcl binder. Then, a simulation step is done and the tcl server sends the output signals back to
   * Logisim. Then we can set the VHDL component output properly.
   *
   * <p>This can be done only if Logisim could connect to the tcl server (socket). This is done in
   * Simulation.java.
   */
  @Override
  public void propagate(InstanceState state) {

    if (state.getProject().getVhdlSimulator().isEnabled()
        && state.getProject().getVhdlSimulator().isRunning()) {

      VhdlSimulatorTop vhdlSimulator = state.getProject().getVhdlSimulator();

      for (final var p : state.getInstance().getPorts()) {
        final var index = state.getPortIndex(p);
        final var val = state.getPortValue(index);

        String vhdlEntityName = getSimName(state.getAttributeSet());

        String message =
            p.getType()
                + ":"
                + vhdlEntityName
                + "_"
                + p.getToolTip()
                + ":"
                + val.toBinaryString()
                + ":"
                + index;

        vhdlSimulator.send(message);
      }

      vhdlSimulator.send("sync");

      /* Get response from tcl server */
      String server_response;
      while ((server_response = vhdlSimulator.receive()) != null
          && server_response.length() > 0
          && !server_response.equals("sync")) {

        final var parameters = server_response.split(":");

        final var busValue = parameters[1];

        final var vector_values = new Value[busValue.length()];

        var k = busValue.length() - 1;
        for (final var bit : busValue.toCharArray()) {
          try {
            switch (Character.getNumericValue(bit)) {
              case 0:
                vector_values[k] = Value.FALSE;
                break;
              case 1:
                vector_values[k] = Value.TRUE;
                break;
              default:
                vector_values[k] = Value.UNKNOWN;
                break;
            }
          } catch (NumberFormatException e) {
            vector_values[k] = Value.UNKNOWN;
          }
          k--;
        }

        state.setPort(Integer.parseInt(parameters[2]), Value.create(vector_values), 1);
      }

      /* VhdlSimulation stopped/disabled */
    } else {

      for (final var p : state.getInstance().getPorts()) {
        int index = state.getPortIndex(p);

        /* If it is an output */
        if (p.getType() == 2) {
          final var vector_values = new Value[p.getFixedBitWidth().getWidth()];
          for (var k = 0; k < p.getFixedBitWidth().getWidth(); k++) {
            vector_values[k] = Value.UNKNOWN;
          }

          state.setPort(index, Value.create(vector_values), 1);
        }
      }

      throw new UnsupportedOperationException(
          "VHDL component simulation is not supported. This could be because there is no Questasim/Modelsim simulation server running.");
    }
  }

  /**
   * Save the VHDL entity in a file. The file is used for VHDL components simulation by
   * QUestasim/Modelsim
   */
  public void saveFile(AttributeSet attrs) {

    PrintWriter writer;
    try {
      writer =
          new PrintWriter(VhdlSimConstants.SIM_SRC_PATH + getSimName(attrs) + ".vhdl",
              StandardCharsets.UTF_8);

      var content = attrs.getValue(CONTENT_ATTR).getContent()
              .replaceAll("(?i)" + getHDLName(attrs), getSimName(attrs));

      writer.print(content);
      writer.close();
    } catch (IOException e) {
      logger.error("Could not create vhdl file: {}", e.getMessage());
      e.printStackTrace();
    }
  }

  void updatePorts(Instance instance) {
    VhdlContentComponent content = instance.getAttributeValue(CONTENT_ATTR);
    instance.setPorts(content.getPorts());
  }
}
