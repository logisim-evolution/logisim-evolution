/*
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

package com.cburch.logisim.circuit;

import static com.cburch.logisim.circuit.Strings.S;

import com.cburch.logisim.comp.ComponentDrawContext;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.comp.ComponentUserEvent;
import com.cburch.logisim.comp.EndData;
import com.cburch.logisim.comp.ManagedComponent;
import com.cburch.logisim.data.AttributeEvent;
import com.cburch.logisim.data.AttributeListener;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.fpga.designrulecheck.Netlist;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.tools.ToolTipMaker;
import com.cburch.logisim.tools.WireRepair;
import com.cburch.logisim.tools.WireRepairData;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.StringUtil;
import javax.swing.JPopupMenu;

public class Splitter extends ManagedComponent
    implements WireRepair, ToolTipMaker, MenuExtender, AttributeListener {

  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "Splitter";

  private static void appendBuf(StringBuilder buf, int start, int end) {
    if (buf.length() > 0) buf.append(",");
    if (start == end) {
      buf.append(start);
    } else {
      buf.append(start).append("-").append(end);
    }
  }

  private boolean isMarked = false;

  public void SetMarked(boolean value) {
    isMarked = value;
  }

  public boolean isMarked() {
    return isMarked;
  }

  // basic data
  byte[] bit_thread; // how each bit maps to thread within end

  // derived data
  CircuitWires.SplitterData wire_data;

  public Splitter(Location loc, AttributeSet attrs) {
    super(loc, attrs, 3);
    configureComponent();
    attrs.addAttributeListener(this);
  }

  //
  // AttributeListener methods
  //
  @Override
  public void attributeListChanged(AttributeEvent e) {}

  @Override
  public void attributeValueChanged(AttributeEvent e) {
    configureComponent();
  }

  private synchronized void configureComponent() {
    final var attrs = (SplitterAttributes) getAttributeSet();
    final var parms = attrs.getParameters();
    final var fanout = attrs.fanout;
    final var bit_end = attrs.bit_end;

    // compute width of each end
    bit_thread = new byte[bit_end.length];
    final var end_width = new byte[fanout + 1];
    end_width[0] = (byte) bit_end.length;
    for (var i = 0; i < bit_end.length; i++) {
      final var thr = bit_end[i];
      if (thr > 0) {
        bit_thread[i] = end_width[thr];
        end_width[thr]++;
      } else {
        bit_thread[i] = -1;
      }
    }

    // compute end positions
    final var origin = getLocation();
    var x = origin.getX() + parms.getEnd0X();
    var y = origin.getY() + parms.getEnd0Y();
    final var dx = parms.getEndToEndDeltaX();
    final var dy = parms.getEndToEndDeltaY();

    final var ends = new EndData[fanout + 1];
    ends[0] = new EndData(origin, BitWidth.create(bit_end.length), EndData.INPUT_OUTPUT);
    for (var i = 0; i < fanout; i++) {
      ends[i + 1] = new EndData(Location.create(x, y), BitWidth.create(end_width[i + 1]), EndData.INPUT_OUTPUT);
      x += dx;
      y += dy;
    }
    wire_data = new CircuitWires.SplitterData(fanout);
    setEnds(ends);
    recomputeBounds();
    fireComponentInvalidated(new ComponentEvent(this));
  }

  @Override
  public void configureMenu(JPopupMenu menu, Project proj) {
    menu.addSeparator();
    menu.add(new SplitterDistributeItem(proj, this, 1));
    menu.add(new SplitterDistributeItem(proj, this, -1));
  }

  @Override
  public boolean contains(Location loc) {
    if (super.contains(loc)) {
      final var myLoc = getLocation();
      final var facing = getAttributeSet().getValue(StdAttr.FACING);
      if (facing == Direction.EAST || facing == Direction.WEST) {
        return Math.abs(loc.getX() - myLoc.getX()) > 5 || loc.manhattanDistanceTo(myLoc) <= 5;
      } else {
        return Math.abs(loc.getY() - myLoc.getY()) > 5 || loc.manhattanDistanceTo(myLoc) <= 5;
      }
    } else {
      return false;
    }
  }

  //
  // user interface methods
  //
  @Override
  public void draw(ComponentDrawContext context) {
    final var attrs = (SplitterAttributes) getAttributeSet();
    if (attrs.appear == SplitterAttributes.APPEAR_LEGACY) {
      SplitterPainter.drawLegacy(context, attrs, getLocation());
    } else {
      final var loc = getLocation();
      SplitterPainter.drawLines(context, attrs, loc);
      SplitterPainter.drawLabels(context, attrs, loc);
      context.drawPins(this);
    }
    if (isMarked) {
      final var g = context.getGraphics();
      final var bds = this.getBounds();
      g.setColor(Netlist.DRC_INSTANCE_MARK_COLOR);
      GraphicsUtil.switchToWidth(g, 2);
      g.drawRoundRect(bds.getX() - 10, bds.getY() - 10, bds.getWidth() + 20, bds.getHeight() + 20, 20, 20);
    }
  }

  public byte[] GetEndpoints() {
    return ((SplitterAttributes) getAttributeSet()).bit_end;
  }

  //
  // abstract ManagedComponent methods
  //
  @Override
  public ComponentFactory getFactory() {
    return SplitterFactory.instance;
  }

  @Override
  public void setFactory(ComponentFactory fact) {}

  @Override
  public Object getFeature(Object key) {
    if (key == WireRepair.class) return this;
    if (key == ToolTipMaker.class) return this;
    if (key == MenuExtender.class) return this;
    else return super.getFeature(key);
  }

  @Override
  public String getToolTip(ComponentUserEvent e) {
    var end = -1;
    for (var i = getEnds().size() - 1; i >= 0; i--) {
      if (getEndLocation(i).manhattanDistanceTo(e.getX(), e.getY()) < 10) {
        end = i;
        break;
      }
    }

    if (end == 0) {
      return S.get("splitterCombinedTip");
    } else if (end > 0) {
      var bits = 0;
      final var buf = new StringBuilder();
      final var attrs = (SplitterAttributes) getAttributeSet();
      final var bit_end = attrs.bit_end;
      var inString = false;
      var beginString = 0;
      for (var i = 0; i < bit_end.length; i++) {
        if (bit_end[i] == end) {
          bits++;
          if (!inString) {
            inString = true;
            beginString = i;
          }
        } else {
          if (inString) {
            appendBuf(buf, i - 1, beginString);
            inString = false;
          }
        }
      }
      if (inString) appendBuf(buf, bit_end.length - 1, beginString);
      String base;
      switch (bits) {
        case 0:
          base = S.get("splitterSplit0Tip");
          break;
        case 1:
          base = S.get("splitterSplit1Tip");
          break;
        default:
          base = S.get("splitterSplitManyTip");
          break;
      }
      return StringUtil.format(base, buf.toString());
    } else {
      return null;
    }
  }

  @Override
  public void propagate(CircuitState state) {
    // handled by CircuitWires, nothing to do
  }

  @Override
  public boolean shouldRepairWire(WireRepairData data) {
    return true;
  }
}
