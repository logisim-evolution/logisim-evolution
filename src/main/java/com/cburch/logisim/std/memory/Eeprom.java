/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.BitWidth;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Value;
import com.cburch.logisim.fpga.designrulecheck.CorrectLabel;
import com.cburch.logisim.fpga.designrulecheck.netlistComponent;
import com.cburch.logisim.gui.hex.HexFile;
import com.cburch.logisim.gui.hex.HexFrame;
import com.cburch.logisim.gui.icons.ArithmeticIcon;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.InstanceLogger;
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
import java.util.function.Consumer;
import javax.swing.JLabel;

public class Eeprom extends Ram {
  /**
   * Unique identifier of the tool, used as reference in project files.
   * Do NOT change as it will prevent project files from loading.
   *
   * Identifier value must MUST be unique string among all tools.
   */
  public static final String _ID = "EEPROM";

  public static class Logger extends Ram.Logger {

    @Override
    public String getLogName(InstanceState state, Object option) {
      var label = state.getAttributeValue(StdAttr.LABEL);
      if (label.equals("")) {
        label = null;
      }
      if (option instanceof Number) {
        if (label == null) {
          final var disp = S.get("eepromComponent");
          final var loc = state.getInstance().getLocation();
          return disp + loc + "[" + option + "]";
        }
        return label + "[" + option + "]";
      } else {
        return label;
      }
    }
  }

  private static final Object[][] logOptions = new Object[9][];

  static class ContentsAttribute extends Rom.ContentsAttribute {
    public ContentsAttribute() {
      super();
    }

    @Override
    public java.awt.Component getCellEditor(Window source, MemContents value) {
      if (source instanceof Frame frame) {
        final var proj = frame.getProject();
        EepromAttributes.register(value, proj);
      }
      final var ret = new ContentsCell(source, value);
      ret.mouseClicked(null);
      return ret;
    }
  }

  @SuppressWarnings("serial")
  private static class ContentsCell extends Rom.ContentsCell implements BaseMouseListenerContract {
    ContentsCell(Window source, MemContents contents) {
      super(source, contents);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
      if (contents == null) return;
      final var proj = (source instanceof Frame frame) ? frame.getProject() : null;
      final var frame = EepromAttributes.getHexFrame(contents, proj, null);
      frame.setVisible(true);
      frame.toFront();
    }
  }

  public static final Attribute<MemContents> CONTENTS_ATTR = new ContentsAttribute();

  // The following is so that instance's MemListeners aren't freed by the
  // garbage collector until the instance itself is ready to be freed.
  private final WeakHashMap<Instance, MemListener> memListeners;

  public Eeprom() {
    super(_ID, S.getter("eepromComponent"), 0, null, true);
    setIcon(new ArithmeticIcon("EEPROM", 3));
    memListeners = new WeakHashMap<>();
    setInstanceLogger(Logger.class);
  }

  @Override
  protected void configureNewInstance(Instance instance) {
    super.configureNewInstance(instance);
    final var contents = getMemContents(instance);
    final var listener = new MemListener(instance);
    memListeners.put(instance, listener);
    contents.addHexModelListener(listener);
  }


  @Override
  public AttributeSet createAttributeSet() {
    return new EepromAttributes();
  }

  public static MemContents getMemContents(Instance instance) {
    return instance.getAttributeValue(CONTENTS_ATTR);
  }

  public static void closeHexFrame(Component c) {
    if (!(c instanceof InstanceComponent)) return;
    final var inst = ((InstanceComponent) c).getInstance();
    EepromAttributes.closeHexFrame(getMemContents(inst));
  }

  @Override
  public HexFrame getHexFrame(Project proj, Instance instance, CircuitState state) {
    return EepromAttributes.getHexFrame(getMemContents(instance), proj, instance);
  }

  @Override
  public boolean reset(CircuitState state, Instance instance) {
    // reset is not performed since it would affect data outside this state
    return true;
  }

  @Override
  MemState getState(Instance instance, CircuitState state) {
    var ret = (ClockedMemState) instance.getData(state);
    if (ret == null) {
      final var contents = getMemContents(instance);
      ret = new ClockedMemState(contents);
      instance.setData(state, ret);
    }
    return ret;
  }

  @Override
  MemState getState(InstanceState state) {
    var ret = (ClockedMemState) state.getData();
    if (ret == null) {
      final var contents = getMemContents(state.getInstance());
      ret = new ClockedMemState(contents);
      state.setData(ret);
    }
    return ret;
  }

  @Override
  public void removeComponent(Circuit circ, Component c, CircuitState state) {
    closeHexFrame(c);
  }

}
