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

package com.cburch.logisim.soc.bus;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.util.HashMap;

import javax.swing.JFrame;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ScrollPaneConstants;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.data.Location;
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

public class SocBusMenuProvider  implements ActionListener {

  private final static int SHOW_MEMORY_MAP = 1;
  private final static int INSERT_TRANSACTION = 2;
  private final static int SHOW_TRACES = 3;

  private class InstanceMenuItem extends JMenuItem {
    private static final long serialVersionUID = 1L;
    private Instance instance;
    private int function;
    private SocBusStateInfo.SocBusState data;
    private CircuitStateHolder.HierarchyInfo csh;
    private CircuitState circuitState;
    
    public InstanceMenuItem(Instance inst, String label, int function, Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocBusStateInfo.SocBusState)data;
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
        
    public InstanceMenuItem(Instance inst, String label, int function, CircuitState state, Object data, CircuitStateHolder.HierarchyInfo csh) {
      super(label);
      instance = inst;
      this.function = function;
      this.data = (SocBusStateInfo.SocBusState)data;
      this.csh = csh;
      circuitState = state;
    }
          
    public Instance getInstance() { return instance; }
    public int getFunction() { return function; }
    public SocBusStateInfo.SocBusState getState() { return data; }
    public CircuitState getCircuitState() { return circuitState; }
    public CircuitStateHolder.HierarchyInfo getHierInfo() { return csh; }
  }
  
  private class MenuProvider implements MenuExtender, CircuitStateHolder {

    private Instance instance;
    private SocBusMenuProvider parrent;
    SocBusStateInfo.SocBusState data;
    CircuitState circuitState;
    HierarchyInfo hierarchy;

    public MenuProvider(Instance inst, SocBusMenuProvider parrent) {
      this.instance = inst;
      this.parrent = parrent;
      circuitState = null;
      data = null;
      hierarchy = null;
    }

    @Override
    public void configureMenu(JPopupMenu menu, Project proj) {
      setParrentFrame(instance,proj.getFrame());
      String instName = instance.getAttributeValue(StdAttr.LABEL);
      if (instName == null || instName.isEmpty()) {
        Location loc = instance.getLocation();
        instName = instance.getFactory().getHDLName(instance.getAttributeSet())+"@"+loc.getX()+","+loc.getY();
      }
      String name = circuitState != null ? instName+" : "+S.get("SocBusMemMap") : S.get("SocBusMemMap");
      menu.addSeparator();
      InstanceMenuItem memMap = new InstanceMenuItem(instance,name,SHOW_MEMORY_MAP);
      memMap.addActionListener(parrent);
      memMap.setEnabled(true);
      menu.add(memMap);
      InstanceMenuItem insertTrans;
      if (circuitState == null) {
        name = S.get("insertTrans");
        CircuitState cstate = proj.getCircuitState();
        Object istate = instance.getData(cstate);
        insertTrans = new InstanceMenuItem(instance,name,INSERT_TRANSACTION,cstate,istate,hierarchy);
      } else {
        name = instName+" : "+S.get("insertTrans");
        insertTrans = new InstanceMenuItem(instance,name,INSERT_TRANSACTION,circuitState,data,hierarchy);
      }
      insertTrans.addActionListener(parrent);
      insertTrans.setEnabled(true);
      menu.add(insertTrans);
      if (circuitState != null) {
        name = instName+" : "+S.get("SocBusTraceWindow");
        InstanceMenuItem traceWin = new InstanceMenuItem(instance,name,SHOW_TRACES,data,hierarchy);
        traceWin.addActionListener(parrent);
        traceWin.setEnabled(true);
        menu.add(traceWin);
      }
    }
    
    @Override
    public void setCircuitState(CircuitState state) {
      if (state == null)
        return;
      circuitState = state;
      data = (SocBusStateInfo.SocBusState)state.getData(instance.getComponent());
    }

    @Override
    public void setHierarchyName(HierarchyInfo csh) {
      hierarchy = csh;
    }
  }
  
  public class InstanceInformation {
    private Frame parrentFrame;
    private TraceWindowTableModel traceModel;
    private ListeningFrame myTraceFrame;
    private HashMap<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo> myTraceList;
    private HashMap<SocBusStateInfo.SocBusState,BusTransactionInsertionGui> myInsertionFrames;
	    
    public InstanceInformation(Instance inst, SocBusMenuProvider parrent) {
      parrentFrame = null;
      myTraceFrame = null;
      traceModel = null;
      myTraceList = new HashMap<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo>();
      myInsertionFrames = new HashMap<SocBusStateInfo.SocBusState,BusTransactionInsertionGui>();
    }

    public void showMemoryMap(Instance instance) {
      SocBusInfo info = instance.getAttributeValue(SocBusAttributes.SOC_BUS_ID);
      SocBusStateInfo state = info.getSocSimulationManager().getSocBusState(info.getBusId());
      if (parrentFrame != null)
        parrentFrame.addWindowListener(state);
      state.setVisible(true);
    }
    
    public void insertTransaction(Instance instance, CircuitState circuitState,
    		SocBusStateInfo.SocBusState state, String name) {
      if (!myInsertionFrames.containsKey(state))
        return;
      if (myInsertionFrames.get(state) == null) {
        String id = instance.getAttributeSet().getValue(SocBusAttributes.SOC_BUS_ID).getBusId();
        SocBusStateInfo busInfo = instance.getAttributeValue(SocBusAttributes.SOC_BUS_ID).getSocSimulationManager().getSocBusState(id);
        BusTransactionInsertionGui gui = new BusTransactionInsertionGui(busInfo,id,circuitState);
        parrentFrame.addWindowListener(gui);
        gui.setTitle(S.get("SocInsertTransWindowTitle")+" "+name);
        myInsertionFrames.put(state, gui);
      }
      JFrame frame = myInsertionFrames.get(state);
      frame.setVisible(true);
      int fstate = frame.getExtendedState();
      fstate &= ~Frame.ICONIFIED;
      frame.setExtendedState(fstate);
    }
    
    public void ShowTraceWindow(Instance instance, SocBusStateInfo.SocBusState state, CircuitStateHolder.HierarchyInfo name) {
      if (!myTraceList.containsKey(state))
        return;
      if (myTraceList.get(state) == null) {
        myTraceList.put(state, name);
        if (traceModel != null)
          traceModel.rebuild();
      }
      if (myTraceFrame == null) {
        traceModel = new TraceWindowTableModel(myTraceList,this);
        JTable table = new JTable(traceModel) {
          private static final long serialVersionUID = 1L;
          public TableCellRenderer getCellRenderer(int row, int column) { return traceModel.getCellRenderer(); }
          protected JTableHeader createDefaultTableHeader() {
            return new JTableHeader(columnModel) {
              private static final long serialVersionUID = 1L;
              public String getToolTipText(MouseEvent e) {
                java.awt.Point p = e.getPoint();
                int index = columnModel.getColumnIndexAtX(p.x);
                int realIndex = columnModel.getColumn(index).getModelIndex();
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
        JScrollPane scroll = new JScrollPane(table);
        scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scroll.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED);
        myTraceFrame = new ListeningFrame(S.getter("TraceWindowTitleDoubleClickOnTraceToRemoveTrace"));
        myTraceFrame.add(scroll);
        myTraceFrame.setSize(AppPreferences.getScaled(SocBusStateInfo.BlockWidth), AppPreferences.getScaled(320));
      }
      myTraceFrame.setVisible(true);
      int fstate = myTraceFrame.getExtendedState();
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

    public void setParrentFrame(Frame frame) {
      parrentFrame = frame;
    }
    
    public void registerBusState(SocBusStateInfo.SocBusState state) {
      if (!myTraceList.containsKey(state))
        myTraceList.put(state, null);
      if (!myInsertionFrames.containsKey(state))
        myInsertionFrames.put(state, null);
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
        if (myInsertionFrames.get(state) != null)
          myInsertionFrames.get(state).dispose();
        myInsertionFrames.remove(state);
      }
    }
  }
  
  private HashMap<Instance,InstanceInformation> myInfo;
  
  public SocBusMenuProvider() {
    myInfo = new HashMap<Instance,InstanceInformation>();
  }
  
  public MenuExtender getMenu(Instance inst) {
    if (!myInfo.containsKey(inst)) {
      InstanceInformation instInfo = new InstanceInformation(inst,this);
      myInfo.put(inst, instInfo);
    }
    return new MenuProvider(inst,this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object source = e.getSource();
    if (source instanceof InstanceMenuItem) {
      InstanceMenuItem info = (InstanceMenuItem) source;
      Instance inst = info.getInstance();
      if (myInfo.containsKey(inst)) {
        switch (info.getFunction()) {
          case SHOW_MEMORY_MAP    : myInfo.get(inst).showMemoryMap(inst);
                                    break;
          case INSERT_TRANSACTION : myInfo.get(inst).insertTransaction(inst,info.getCircuitState(),
                                       info.getState(),info.getName());
                                    break;
          case SHOW_TRACES        : myInfo.get(inst).ShowTraceWindow(inst, info.getState(), info.getHierInfo());
                                    break;
        }
      }
    }
  }

  private void setParrentFrame(Instance inst, Frame frame) {
    if (myInfo.containsKey(inst))
      myInfo.get(inst).setParrentFrame(frame);
  }
  
  public void registerBusState(SocBusStateInfo.SocBusState state , Instance instance) {
    if (!myInfo.containsKey(instance))
      myInfo.put(instance, new InstanceInformation(instance,this));
    myInfo.get(instance).registerBusState(state);
  }
  
  public void deregisterBusState(SocBusStateInfo.SocBusState state , Instance instance) {
    if (myInfo.containsKey(instance))
      myInfo.get(instance).deregisterBusState(state);
  }
  
}
