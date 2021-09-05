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

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.LogisimVersion;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstancePainter;
import com.cburch.logisim.instance.InstanceState;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.proj.Project;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.StringTokenizer;
import java.util.WeakHashMap;
import javax.swing.JLabel;

public class Rom extends Mem {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "ROM";

  static class ContentsAttribute extends Attribute<MemContents> {
    public ContentsAttribute() {
      super("contents", S.getter("romContentsAttr"));
    }

    @Override
    public java.awt.Component getCellEditor(Window source, MemContents value) {
      if (source instanceof Frame) {
        final var proj = ((Frame) source).getProject();
        RomAttributes.register(value, proj);
      }
      final var ret = new ContentsCell(source, value);
      ret.mouseClicked(null);
      return ret;
    }

    @Override
    public MemContents parse(String value) {
      final var lineBreak = value.indexOf('\n');
      final var first = lineBreak < 0 ? value : value.substring(0, lineBreak);
      final var rest = lineBreak < 0 ? "" : value.substring(lineBreak + 1);
      final var toks = new StringTokenizer(first);
      try {
        final var header = toks.nextToken();
        if (!header.equals("addr/data:")) return null;
        final var addr = Integer.parseInt(toks.nextToken());
        final var data = Integer.parseInt(toks.nextToken());
        return HexFile.parseFromCircFile(rest, addr, data);
      } catch (IOException | NoSuchElementException | NumberFormatException e) {
        return null;
      }
    }

    @Override
    public String toDisplayString(MemContents value) {
      return S.get("romContentsValue");
    }

    @Override
    public String toStandardString(MemContents state) {
      final var addr = state.getLogLength();
      final var data = state.getWidth();
      final var contents = HexFile.saveToString(state);
      return "addr/data: " + addr + " " + data + "\n" + contents;
    }
  }

  @SuppressWarnings("serial")
  private static class ContentsCell extends JLabel implements BaseMouseListenerContract {
    final Window source;
    final MemContents contents;

    ContentsCell(Window source, MemContents contents) {
      super(S.get("romContentsValue"));
      this.source = source;
      this.contents = contents;
      addMouseListener(this);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (contents == null) return;
      final var proj = source instanceof Frame ? ((Frame) source).getProject() : null;
      final var frame = RomAttributes.getHexFrame(contents, proj, null);
      frame.setVisible(true);
      frame.toFront();
    }
  }

  public static final Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

  // The following is so that instance's MemListeners aren't freed by the
  // garbage collector until the instance itself is ready to be freed.
  private final WeakHashMap<Instance, MemListener> memListeners;

  public Rom() {
    super(_ID, S.getter("romComponent"), 0);
    setIcon(new ArithmeticIcon("ROM", 3));
    memListeners = new WeakHashMap<>();
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    final var contents = getMemContents(instance);
    final var listener = new MemListener(instance);
    memListeners.put(instance, listener);
    contents.addHexModelListener(listener);
    instance.addAttributeListener();
  }

  @Override
  void configurePorts(Instance instance) {
    RamAppearance.configurePorts(instance);
  }

  @Override
  public Object getDefaultAttributeValue(Attribute<?> attr, LogisimVersion ver) {
    return (attr.equals(StdAttr.APPEARANCE))
        ? StdAttr.APPEAR_CLASSIC
        : super.getDefaultAttributeValue(attr, ver);
  }

  @Override
  public AttributeSet createAttributeSet() {
    return new RomAttributes();
  }

  @Override
  public String getHDLName(AttributeSet attrs) {
    final var Label = CorrectLabel.getCorrectLabel(attrs.getValue(StdAttr.LABEL));
    return (Label.length() == 0) ? "ROM" : "ROMCONTENTS_" + Label;
  }

  @Override
  HexFrame getHexFrame(Project proj, Instance instance, CircuitState state) {
    return RomAttributes.getHexFrame(getMemContents(instance), proj, instance);
  }

  public static MemContents getMemContents(Instance instance) {
    return instance.getAttributeValue(CONTENTS_ATTR);
  }

  public static void closeHexFrame(Component c) {
    if (!(c instanceof InstanceComponent)) return;
    final var inst = ((InstanceComponent) c).getInstance();
    RomAttributes.closeHexFrame(getMemContents(inst));
  }

  @Override
  public Bounds getOffsetBounds(AttributeSet attrs) {
    final var len = attrs.getValue(Mem.DATA_ATTR).getWidth();
    if (attrs.getValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      return Bounds.create(0, 0, SymbolWidth + 40, 140);
    } else {
      return Bounds.create(0, 0, SymbolWidth + 40, RamAppearance.getControlHeight(attrs) + 20 * len);
    }
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
    var ret = (MemState) instance.getData(state);
    if (ret == null) {
      final var contents = getMemContents(instance);
      ret = new MemState(contents);
      instance.setData(state, ret);
    }
    return ret;
  }

  @Override
  MemState getState(InstanceState state) {
    var ret = (MemState) state.getData();
    if (ret == null) {
      final var contents = getMemContents(state.getInstance());
      ret = new MemState(contents);
      state.setData(ret);
    }
    return ret;
  }

  @Override
  public boolean HDLSupportedComponent(AttributeSet attrs) {
    if (MyHDLGenerator == null) MyHDLGenerator = new RomHDLGeneratorFactory();
    return MyHDLGenerator.HDLTargetSupported(attrs);
  }

  @Override
  protected void instanceAttributeChanged(Instance instance, Attribute<?> attr) {
    if (attr == Mem.DATA_ATTR || attr == Mem.ADDR_ATTR || attr == StdAttr.APPEARANCE || attr == Mem.LINE_ATTR) {
      instance.recomputeBounds();
      configurePorts(instance);
    }
  }

  @Override
  public void paintInstance(InstancePainter painter) {
    if (painter.getAttributeValue(StdAttr.APPEARANCE) == StdAttr.APPEAR_CLASSIC) {
      RamAppearance.DrawRamClassic(painter);
    } else {
      RamAppearance.DrawRamEvolution(painter);
    }
  }

  @Override
  public void propagate(InstanceState state) {
    final var myState = getState(state);
    final var dataBits = state.getAttributeValue(DATA_ATTR);
    final var attrs = state.getAttributeSet();

    final var addrValue = state.getPortValue(RamAppearance.getAddrIndex(0, attrs));
    final var nrDataLines = RamAppearance.getNrDataOutPorts(attrs);

    final var addr = addrValue.toLongValue();
    if (addrValue.isErrorValue() || (addrValue.isFullyDefined() && addr < 0)) {
      for (var i = 0; i < nrDataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createError(dataBits), DELAY);
      return;
    }
    if (!addrValue.isFullyDefined()) {
      for (var i = 0; i < nrDataLines; i++)
        state.setPort(RamAppearance.getDataOutIndex(i, attrs), Value.createUnknown(dataBits), DELAY);
      return;
    }
    if (addr != myState.getCurrent()) {
      myState.setCurrent(addr);
      myState.scrollToShow(addr);
    }

    boolean misaligned = addr % nrDataLines != 0;
    boolean misalignError = misaligned && !state.getAttributeValue(ALLOW_MISALIGNED);

    for (var i = 0; i < nrDataLines; i++) {
      long val = myState.getContents().get(addr + i);
      state.setPort(
          RamAppearance.getDataOutIndex(i, attrs),
          misalignError ? Value.createError(dataBits) : Value.createKnown(dataBits, val),
          DELAY);
    }
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    closeHexFrame(c);
  }

  @Override
  public boolean RequiresNonZeroLabel() {
    return true;
  }
}
