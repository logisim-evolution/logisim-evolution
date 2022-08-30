/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.gui.main.Frame;
import com.cburch.logisim.instance.Instance;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.soc.data.SocBusInfo;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.gui.BusTransactionInsertionGui;
import com.cburch.logisim.soc.gui.ListeningFrame;
import com.cburch.logisim.soc.gui.TraceWindowTableModel;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.tools.MenuExtender;
import com.cburch.logisim.util.LineBuffer;
import com.cburch.logisim.util.StringUtil;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

public class SocBusMenuProvider implements ActionListener {

  private static final int SHOW_MEMORY_MAP = 1;
  private static final int INSERT_TRANSACTION = 2;
  private static final int SHOW_TRACES = 3;
  private final HashMap<Instance, InstanceInformation> myInfo;

  public SocBusMenuProvider() {
    myInfo = new HashMap<>();
  }

  public MenuExtender getMenu(Instance inst) {
    if (!myInfo.containsKey(inst)) {
      final var instInfo = new InstanceInformation(inst, this);
      myInfo.put(inst, instInfo);
    }
    return new MenuProvider(inst, this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    final var source = e.getSource();
    if (source instanceof InstanceMenuItem info) {
      final var inst = info.getInstance();
      if (myInfo.containsKey(inst)) {
        final var function = info.getFunction();
        switch (function) {
          case SHOW_MEMORY_MAP -> myInfo.get(inst).showMemoryMap(inst);
          case INSERT_TRANSACTION -> myInfo
              .get(inst)
              .insertTransaction(inst, info.getCircuitState(), info.getState(), info.getName());
          case SHOW_TRACES ->
              myInfo.get(inst).showTraceWindow(inst, info.getState(), info.getHierInfo());
          default ->
              throw new IllegalStateException(String.format("Unsupported function: %d", function));
        }
      }
    }
  }

  private void setParentFrame(Instance inst, Frame frame) {
    if (myInfo.containsKey(inst)) {
      myInfo.get(inst).setParentFrame(frame);
    }
  }

  public void registerBusState(SocBusStateInfo.SocBusState state, Instance instance) {
    if (!myInfo.containsKey(instance)) {
      myInfo.put(instance, new InstanceInformation(instance, this));
    }
    myInfo.get(instance).registerBusState(state);
  }

  public void deregisterBusState(SocBusStateInfo.SocBusState state, Instance instance) {
    if (myInfo.containsKey(instance)) {
      myInfo.get(instance).deregisterBusState(state);
    }
  }

  private static class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private final Instance instance;
    private final int function;
    private final SocBusStateInfo.SocBusState data;
    private final CircuitStateHolder.HierarchyInfo csh;
    private final CircuitState circuitState;

    public InstanceMenuItem(Instance inst, String label, int function, Object data,
                            CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocBusStateInfo.SocBusState) data;
      this.csh = csh;
      circuitState = null;
    }

    public InstanceMenuItem(Instance inst, String label, int function) {
      super(label);
      instance = inst;
      this.function = function;
      data = null;
      csh = null;
      circuitState = null;
    }

    public InstanceMenuItem(Instance inst, String label, int function, CircuitState state,
                            Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocBusStateInfo.SocBusState) data;
      this.csh = csh;
      circuitState = state;
    }

    public Instance getInstance() {
      return instance;
    }

    public int getFunction() {
      return function;
    }

    public SocBusStateInfo.SocBusState getState() {
      return data;
    }

    public CircuitState getCircuitState() {
      return circuitState;
    }

    public CircuitStateHolder.HierarchyInfo getHierInfo() {
      return csh;
    }
  }

  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private final Instance instance;
    private final SocBusMenuProvider parent;
    SocBusStateInfo.SocBusState data;
    CircuitState circuitState;
    HierarchyInfo hierarchy;

    public MenuProvider(Instance inst, SocBusMenuProvider parent) {
      this.instance = inst;
      this.parent = parent;
      circuitState = null;
      data = null;
      hierarchy = null;
    }

    @Override
    public void configureMenu(JPopupMenu menu, Project proj) {
      setParentFrame(instance, proj.getFrame());
      var instanceName = instance.getAttributeValue(StdAttr.LABEL);
      if (StringUtil.isNullOrEmpty(instanceName)) {
        final var loc = instance.getLocation();
        instanceName = LineBuffer.format("{{1}}@{{2}},{{3}}",
                instance.getFactory().getHDLName(instance.getAttributeSet()), loc.getX(), loc.getY());
      }

      var name = (circuitState != null) ? (instanceName + ": ") : "";
      name += S.get("SocBusMemMap");
      menu.addSeparator();
      final var memMap = new InstanceMenuItem(instance, name, SHOW_MEMORY_MAP);
      memMap.addActionListener(parent);
      memMap.setEnabled(true);
      menu.add(memMap);

      InstanceMenuItem insertTrans;
      if (circuitState == null) {
        name = S.get("insertTrans");
        final var cstate = proj.getCircuitState();
        final var istate = instance.getData(cstate);
        insertTrans = new InstanceMenuItem(instance, name, INSERT_TRANSACTION, cstate, istate, hierarchy);
      } else {
        name = instanceName + ": " + S.get("insertTrans");
        insertTrans = new InstanceMenuItem(instance, name, INSERT_TRANSACTION, circuitState, data, hierarchy);
      }
      insertTrans.addActionListener(parent);
      insertTrans.setEnabled(true);
      menu.add(insertTrans);
      if (circuitState != null) {
        name = instanceName + ": " + S.get("SocBusTraceWindow");
        final var traceWin = new InstanceMenuItem(instance, name, SHOW_TRACES, data, hierarchy);
        traceWin.addActionListener(parent);
        traceWin.setEnabled(true);
        menu.add(traceWin);
      }
    }

    @Override
    public void setCircuitState(CircuitState state) {
      if (state != null) {
        circuitState = state;
        data = (SocBusStateInfo.SocBusState) state.getData(instance.getComponent());
      }
    }

    @Override
    public void setHierarchyName(HierarchyInfo csh) {
      hierarchy = csh;
    }
  }

  public static class InstanceInformation {
    private final HashMap<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo> myTraceList;
    private final HashMap<SocBusStateInfo.SocBusState, BusTransactionInsertionGui> myInsertionFrames;
    private Frame parentFrame;
    private TraceWindowTableModel traceModel;
    private ListeningFrame myTraceFrame;

    public InstanceInformation(Instance inst, SocBusMenuProvider parent) {
      parentFrame = null;
      myTraceFrame = null;
      traceModel = null;
      myTraceList = new HashMap<>();
      myInsertionFrames = new HashMap<>();
    }

    public void showMemoryMap(Instance instance) {
      SocBusInfo info = instance.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
      SocBusStateInfo state = info.getSocSimulationManager().getSocBusState(info.getBusId());
      if (parentFrame != null) parentFrame.addWindowListener(state);
      state.setVisible(true);
    }

    public void insertTransaction(Instance instance, CircuitState circuitState,
                                  SocBusStateInfo.SocBusState state, String name) {
      if (!myInsertionFrames.containsKey(state)) return;
      if (myInsertionFrames.get(state) == null) {
        String id = instance.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID).getBusId();
        final var busInfo =
            instance
                .getAttributeValue(SocBusAttributes.SOC_BUS_ID)
                .getSocSimulationManager()
                .getSocBusState(id);
        final var gui = new BusTransactionInsertionGui(busInfo, id, circuitState);
        parentFrame.addWindowListener(gui);
        gui.setTitle(S.get("SocInsertTransWindowTitle") + " " + name);
        myInsertionFrames.put(state, gui);
      }
      final var frame = myInsertionFrames.get(state);
      frame.setVisible(true);
      var fstate = frame.getExtendedState();
      fstate &= ~Frame.ICONIFIED;
      frame.setExtendedState(fstate);
    }

    public void showTraceWindow(Instance instance, SocBusStateInfo.SocBusState state,
                                CircuitStateHolder.HierarchyInfo name) {
      if (!myTraceList.containsKey(state)) return;
      if (myTraceList.get(state) == null) {
        myTraceList.put(state, name);
        if (traceModel != null) traceModel.rebuild();
      }

      if (myTraceFrame == null) {
        traceModel = new TraceWindowTableModel(myTraceList, this);
        final var table = new JTable(traceModel) {
              private static final long serialVersionUID = 1L;

              @Override
              public TableCellRenderer getCellRenderer(int row, int column) {
                return traceModel.getCellRenderer();
              }

              @Override
              protected JTableHeader createDefaultTableHeader() {
                return new JTableHeader(columnModel) {
                  private static final long serialVersionUID = 1L;

                  @Override
                  public String getToolTipText(MouseEvent e) {
                    final var point = e.getPoint();
                    final var index = columnModel.getColumnIndexAtX(point.x);
                    final var realIndex = columnModel.getColumn(index).getModelIndex();
                    return traceModel.getColumnHeader(realIndex);
                  }
                };
              }
            };
        table.getTableHeader().setDefaultRenderer(traceModel.getHeaderRenderer());
        table.setFillsViewportHeight(true);
        table.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        traceModel.setColMod(table);
        traceModel.rebuild();
        final var scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        myTraceFrame = new ListeningFrame(S.getter("TraceWindowTitleDoubleClickOnTraceToRemoveTrace"));
        myTraceFrame.add(scroll);

        final var scaledWidth = AppPreferences.getScaled(SocBusStateInfo.BLOCK_WIDTH);
        // FIXME: arbitrary set base heigh
        final var scaledHeight = AppPreferences.getScaled(320);
        myTraceFrame.setSize(scaledWidth, scaledHeight);
      }
      myTraceFrame.setVisible(true);
      var fstate = myTraceFrame.getExtendedState();
      fstate &= ~Frame.ICONIFIED;
      myTraceFrame.setExtendedState(fstate);
    }

    public void destroyTraceWindow() {
      if (myTraceFrame != null) {
        myTraceFrame.setVisible(false);
        myTraceFrame.dispose();
        myTraceFrame = null;
        traceModel = null;
      }
    }

    public void setParentFrame(Frame frame) {
      parentFrame = frame;
    }

    public void registerBusState(SocBusStateInfo.SocBusState state) {
      if (!myTraceList.containsKey(state)) myTraceList.put(state, null);
      if (!myInsertionFrames.containsKey(state)) myInsertionFrames.put(state, null);
    }

    public void deregisterBusState(SocBusStateInfo.SocBusState state) {
      if (myTraceList.containsKey(state)) {
        myTraceList.remove(state);
        if (traceModel != null) traceModel.rebuild();
        if (traceModel != null && traceModel.getColumnCount() == 0) {
          myTraceFrame.setVisible(false);
          myTraceFrame.dispose();
          myTraceFrame = null;
          traceModel = null;
        }
      }
      if (myInsertionFrames.containsKey(state)) {
        if (myInsertionFrames.get(state) != null) myInsertionFrames.get(state).dispose();
        myInsertionFrames.remove(state);
      }
    }
  }
}
