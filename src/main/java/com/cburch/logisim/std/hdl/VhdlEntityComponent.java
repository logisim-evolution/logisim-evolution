/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
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
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import com.cburch.logisim.vhdl.base.VhdlSimConstants;
import com.cburch.logisim.vhdl.sim.VhdlSimulatorTop;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Window;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.util.WeakHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class VhdlEntityComponent extends InstanceFactory {

  static class ContentAttribute extends Attribute<VhdlContentComponent> {

    public ContentAttribute() {
      super("content", S.getter("vhdlContentAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, VhdlContentComponent value) {
      Project proj = source instanceof Frame ? ((Frame) source).getProject() : null;
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

    Instance instance;

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

  private WeakHashMap<Instance, VhdlEntityListener> contentListeners;

  public VhdlEntityComponent() {
    super("VHDL Entity", S.getter("vhdlComponent"));

    this.contentListeners = new WeakHashMap<Instance, VhdlEntityListener>();
    this.setIcon(new ArithmeticIcon("VHDL"));
  }

  public void SetSimName(AttributeSet attrs, String SName) {
    if (attrs == null) return;
    VhdlEntityAttributes atrs = (VhdlEntityAttributes) attrs;
    String Label = (attrs.getValue(StdAttr.LABEL) != "") ? getHDLTopName(attrs) : SName;
    if (atrs.containsAttribute(VhdlSimConstants.SIM_NAME_ATTR))
      atrs.setValue(VhdlSimConstants.SIM_NAME_ATTR, Label);
  }

  public String GetSimName(AttributeSet attrs) {
    if (attrs == null) return null;
    VhdlEntityAttributes atrs = (VhdlEntityAttributes) attrs;
    return atrs.getValue(VhdlSimConstants.SIM_NAME_ATTR);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    VhdlContentComponent content = instance.getAttributeValue(CONTENT_ATTR);
    VhdlEntityListener listener = new VhdlEntityListener(instance);

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

    String label = "";

    if (attrs.getValue(StdAttr.LABEL) != "")
      label = "_" + attrs.getValue(StdAttr.LABEL).toLowerCase();

    return getHDLName(attrs) + label;
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    VhdlContentComponent content = attrs.getValue(CONTENT_ATTR);
    int nbInputs = content.getInputsNumber();
    int nbOutputs = content.getOutputsNumber();

    return Bounds.create(0, 0, WIDTH, Math.max(nbInputs, nbOutputs) * PORT_GAP + HEIGHT);
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new VhdlHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
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
    Graphics g = painter.getGraphics();
    VhdlContentComponent content = painter.getAttributeValue(CONTENT_ATTR);
    FontMetrics metric = g.getFontMetrics();

    Bounds bds = painter.getBounds();
    int x0 = bds.getX() + (bds.getWidth() / 2);
    int y0 = bds.getY() + metric.getHeight() + 12;
    GraphicsUtil.drawText(
        g,
        StringUtil.resizeString(content.getName(), metric, WIDTH),
        x0,
        y0,
        GraphicsUtil.H_CENTER,
        GraphicsUtil.V_BOTTOM);

    String glbLabel = painter.getAttributeValue(StdAttr.LABEL);
    if (glbLabel != null) {
      Font font = g.getFont();
      g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
      GraphicsUtil.drawCenteredText(
          g, glbLabel, bds.getX() + bds.getWidth() / 2, bds.getY() - g.getFont().getSize());
      g.setFont(font);
    }

    g.setColor(Color.GRAY);
    g.setFont(g.getFont().deriveFont((float) 10));
    metric = g.getFontMetrics();

    Port[] inputs = content.getInputs();
    Port[] outputs = content.getOutputs();

    for (int i = 0; i < inputs.length; i++)
      GraphicsUtil.drawText(
          g,
          StringUtil.resizeString(inputs[i].getToolTip(), metric, (WIDTH / 2) - X_PADDING),
          bds.getX() + 5,
          bds.getY() + HEIGHT - 2 + (i * PORT_GAP),
          GraphicsUtil.H_LEFT,
          GraphicsUtil.V_CENTER);
    for (int i = 0; i < outputs.length; i++)
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

  @Override
  /**
   * Propagate signals through the VHDL component. Logisim doesn't have a VHDL simulation tool. So
   * we need to use an external tool. We send signals to Questasim/Modelsim through a socket and a
   * tcl binder. Then, a simulation step is done and the tcl server sends the output signals back to
   * Logisim. Then we can set the VHDL component output properly.
   *
   * <p>This can be done only if Logisim could connect to the tcl server (socket). This is done in
   * Simulation.java.
   */
  public void propagate(InstanceState state) {

    if (state.getProject().getVhdlSimulator().isEnabled()
        && state.getProject().getVhdlSimulator().isRunning()) {

      VhdlSimulatorTop vhdlSimulator = state.getProject().getVhdlSimulator();

      for (Port p : state.getInstance().getPorts()) {
        int index = state.getPortIndex(p);
        Value val = state.getPortValue(index);

        String vhdlEntityName = GetSimName(state.getAttributeSet());

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

        String[] parameters = server_response.split("\\:");

        String busValue = parameters[1];

        Value vector_values[] = new Value[busValue.length()];

        int k = busValue.length() - 1;
        for (char bit : busValue.toCharArray()) {

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

      for (Port p : state.getInstance().getPorts()) {
        int index = state.getPortIndex(p);

        /* If it is an output */
        if (p.getType() == 2) {
          Value vector_values[] = new Value[p.getFixedBitWidth().getWidth()];
          for (int k = 0; k < p.getFixedBitWidth().getWidth(); k++) {
            vector_values[k] = Value.UNKNOWN;
          }

          state.setPort(index, Value.create(vector_values), 1);
        }
      }

      new UnsupportedOperationException(
          "VHDL component simulation is not supported. This could be because there is no Questasim/Modelsim simulation server running.");
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }

  /**
   * Save the VHDL entity in a file. The file is used for VHDL components simulation by
   * QUestasim/Modelsim
   */
  public void saveFile(AttributeSet attrs) {

    PrintWriter writer;
    try {
      writer =
          new PrintWriter(VhdlSimConstants.SIM_SRC_PATH + GetSimName(attrs) + ".vhdl", "UTF-8");

      String content = attrs.getValue(CONTENT_ATTR).getContent();

      content = content.replaceAll("(?i)" + getHDLName(attrs), GetSimName(attrs));

      writer.print(content);
      writer.close();
    } catch (FileNotFoundException e) {
      logger.error("Could not create vhdl file: {}", e.getMessage());
      e.printStackTrace();
      return;
    } catch (UnsupportedEncodingException e) {
      logger.error("Could not create vhdl file: {}", e.getMessage());
      e.printStackTrace();
      return;
    }
  }

  void updatePorts(Instance instance) {
    VhdlContentComponent content = instance.getAttributeValue(CONTENT_ATTR);
    instance.setPorts(content.getPorts());
  }
}
