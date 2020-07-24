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


/* This file is adopted from the MIPS.jar library by
 * Martin Dybdal <dybber@dybber.dk> and
 * Anders Boesen Lindbo Larsen <abll@diku.dk>.
 * It was developed for the computer architecture class at the Department of
 * Computer Science, University of Copenhagen.
 */

package com.cburch.logisim.std.gates;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.data.AbstractAttributeSet;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Attributes;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceFactory;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.Port;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.GraphicsUtil;
import java.awt.Color;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;

class PLA extends InstanceFactory {
  static final int IN_PORT = 0;
  static final int OUT_PORT = 1;

  static final Attribute<BitWidth> ATTR_IN_WIDTH =
      Attributes.forBitWidth("in_width", S.getter("Bit Width In"));
  static final Attribute<BitWidth> ATTR_OUT_WIDTH =
      Attributes.forBitWidth("out_width", S.getter("Bit Width Out"));
  static Attribute<PLATable> ATTR_TABLE = new TruthTableAttribute();

  public static InstanceFactory FACTORY = new PLA();

  private static final Color BACKGROUND_COLOR = new Color(230, 230, 230);

  private static final List<Attribute<?>> ATTRIBUTES =
      Arrays.asList(
          new Attribute<?>[] {
            StdAttr.FACING,
            ATTR_IN_WIDTH,
            ATTR_OUT_WIDTH,
            ATTR_TABLE,
            StdAttr.LABEL,
            StdAttr.LABEL_LOC,
            StdAttr.LABEL_FONT
          });

  private static class TruthTableAttribute extends Attribute<PLATable> {
    public TruthTableAttribute() {
      super("table", S.getter("plaProgram"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, PLATable tt) {
      PLATable.EditorDialog dialog = new PLATable.EditorDialog((Frame) source);
      dialog.setValue(tt);
      return dialog;
    }

    @Override
    public String toDisplayString(PLATable value) {
      return S.get("plaClickToEdit");
    }

    @Override
    public String toStandardString(PLATable tt) {
      return tt.toStandardString();
    }

    @Override
    public PLATable parse(String str) {
      return PLATable.parse(str);
    }
  }

  private class PLAAttributes extends AbstractAttributeSet {
    private String label = "";
    private Direction facing = Direction.EAST;
    private Font labelFont = StdAttr.DEFAULT_LABEL_FONT;
    private Object labelLoc = Direction.NORTH;
    private BitWidth widthIn = BitWidth.create(2);
    private BitWidth widthOut = BitWidth.create(2);
    private PLATable tt = new PLATable(2, 2, "PLA");

    @Override
    protected void copyInto(AbstractAttributeSet destObj) {
      PLAAttributes dest = (PLAAttributes) destObj;
      dest.label = this.label;
      dest.facing = this.facing;
      dest.labelFont = this.labelFont;
      dest.labelLoc = this.labelLoc;
      dest.widthIn = this.widthIn;
      dest.widthOut = this.widthOut;
      dest.tt = new PLATable(this.tt);
      dest.tt.setLabel(dest.label);
    }

    @Override
    public List<Attribute<?>> getAttributes() {
      return ATTRIBUTES;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <V> V getValue(Attribute<V> attr) {
      if (attr == StdAttr.FACING) return (V) facing;
      if (attr == ATTR_IN_WIDTH) return (V) widthIn;
      if (attr == ATTR_OUT_WIDTH) return (V) widthOut;
      if (attr == ATTR_TABLE) return (V) tt;
      if (attr == StdAttr.LABEL) return (V) label;
      if (attr == StdAttr.LABEL_LOC) return (V) labelLoc;
      if (attr == StdAttr.LABEL_FONT) return (V) labelFont;
      return null;
    }

    @Override
    public <V> void setValue(Attribute<V> attr, V value) {
      if (attr == StdAttr.LABEL_LOC) {
        labelLoc = value;
      } else if (attr == StdAttr.FACING) {
        facing = (Direction) value;
      } else if (attr == ATTR_IN_WIDTH) {
        widthIn = (BitWidth) value;
        tt.setInSize(widthIn.getWidth());
      } else if (attr == ATTR_OUT_WIDTH) {
        widthOut = (BitWidth) value;
        tt.setOutSize(widthOut.getWidth());
      } else if (attr == ATTR_TABLE) {
        tt = (PLATable) value;
        tt.setLabel(label);
        if (tt.inSize() != widthIn.getWidth())
          setValue(ATTR_IN_WIDTH, BitWidth.create(tt.inSize()));
        if (tt.outSize() != widthOut.getWidth())
          setValue(ATTR_OUT_WIDTH, BitWidth.create(tt.outSize()));
      } else if (attr == StdAttr.LABEL) {
        label = (String) value;
        tt.setLabel(label);
      } else if (attr == StdAttr.LABEL_FONT) {
        labelFont = (Font) value;
      }
      fireAttributeValueChanged(attr, value, null);
    }
  }

  public PLA() {
    super("PLA", S.getter("PLA"));
    setIconName("pla.gif");
    setFacingAttribute(StdAttr.FACING);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new PLAAttributes();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    PLAAttributes attributes = (PLAAttributes) instance.getAttributeSet();
    attributes.tt = new PLATable(instance.getAttributeValue(ATTR_TABLE));
    attributes.tt.setLabel(instance.getAttributeValue(StdAttr.LABEL));
    instance.addAttributeListener();
    updatePorts(instance);
    instance.computeLabelTextField(Instance.AVOID_LEFT | Instance.AVOID_RIGHT);
  }

  private void updatePorts(Instance instance) {
    Direction dir = instance.getAttributeValue(StdAttr.FACING);
    int dx = 0, dy = 0;
    if (dir == Direction.WEST) dx = -50;
    else if (dir == Direction.NORTH) dy = -50;
    else if (dir == Direction.SOUTH) dy = 50;
    else dx = 50;
    Port[] ps = {
      new Port(0, 0, Port.INPUT, ATTR_IN_WIDTH), new Port(dx, dy, Port.OUTPUT, ATTR_OUT_WIDTH)
    };
    ps[IN_PORT].setToolTip(S.getter("input"));
    ps[OUT_PORT].setToolTip(S.getter("output"));
    instance.setPorts(ps);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == StdAttr.FACING
        || attr == ATTR_IN_WIDTH
        || attr == ATTR_OUT_WIDTH
        || attr == StdAttr.LABEL
        || attr == StdAttr.LABEL_LOC) {
      instance.recomputeBounds();
      instance.computeLabelTextField(Instance.AVOID_LEFT | Instance.AVOID_RIGHT);
      updatePorts(instance);
    } else if (attr == ATTR_TABLE) {
      instance.fireInvalidated();
    }
  }

  @Override
  public void propagate(InstanceState state) {
    BitWidth outWidth = state.getAttributeValue(ATTR_OUT_WIDTH);
    PLATable tt = state.getAttributeValue(ATTR_TABLE);
    Value input = state.getPortValue(IN_PORT);
    long val = tt.valueFor(input.toLongValue());
    state.setPort(1, Value.createKnown(outWidth, val), 1);
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    Direction dir = attrs.getValue(StdAttr.FACING);
    Bounds ret = Bounds.create(0, -25, 50, 50).rotate(Direction.EAST, dir, 0, 0);
    return ret;
  }

  @Override
  public void paintGhost(InstancePainter painter) {
    paintInstance(painter, true);
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    paintInstance(painter, false);
  }

  void paintInstance(InstancePainter painter, boolean ghost) {
    Graphics g = painter.getGraphics();
    Bounds bds = painter.getBounds();
    int x = bds.getX();
    int y = bds.getY();
    int w = bds.getWidth();
    int h = bds.getHeight();

    if (!ghost && painter.shouldDrawColor()) {
      g.setColor(BACKGROUND_COLOR);
      g.fillRect(x, y, w, h);
    }

    if (!ghost) g.setColor(Color.BLACK);
    GraphicsUtil.switchToWidth(g, 2);
    g.drawRect(x, y, bds.getWidth(), bds.getHeight());

    g.setFont(painter.getAttributeValue(StdAttr.LABEL_FONT));
    GraphicsUtil.drawCenteredText(g, "PLA", x + w / 2, y + h / 3);
    if (!ghost) {
      if (painter.getShowState()) {
        PLATable tt = painter.getAttributeValue(ATTR_TABLE);
        Value input = painter.getPortValue(IN_PORT);
        String comment = tt.commentFor(input.toLongValue());
        int jj = comment.indexOf("#"); // don't display secondary comment
        if (jj >= 0) comment = comment.substring(0, jj).trim();
        GraphicsUtil.drawCenteredText(g, comment, x + w / 2, y + 2 * h / 3);
      }
      painter.drawLabel();
      painter.drawPorts();
    }
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    String Name = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    if (Name.length() == 0) return "PLA";
    else return "PLA_" + Name;
  }

  @Override
  public boolean HDLSupportedComponent(String HDLIdentifier, AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new PLAHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(HDLIdentifier, attrs);
  }

  @Override
  protected Object getInstanceFeature(Instance instance, Object key) {
    if (key == MenuExtender.class) {
      return new PLAMenu(this, instance);
    }
    return super.getInstanceFeature(instance, key);
  }

  class PLAMenu implements ActionListener, MenuExtender {
    private Instance instance;
    private Frame frame;
    private JMenuItem edit;

    PLAMenu(PLA factory, Instance instance) {
      this.instance = instance;
    }

    public void actionPerformed(ActionEvent evt) {
      Object src = evt.getSource();
      if (src == edit) doEdit();
    }

    public void configureMenu(JPopupMenu menu, Project proj) {
      this.frame = proj.getFrame();

      edit = new JMenuItem(S.get("plaEditMenuItem"));
      edit.setEnabled(true);
      edit.addActionListener(this);

      menu.addSeparator();
      menu.add(edit);
    }

    private void doEdit() {
      PLATable tt = instance.getAttributeValue(ATTR_TABLE);
      PLATable.EditorDialog dialog = new PLATable.EditorDialog(frame);
      dialog.setValue(tt);
      dialog.setVisible(true);
      dialog.toFront();
    }
  }
}
