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

package com.cburch.logisim.soc.gui;

import java.awt.Dimension;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.HashMap;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.circuit.CircuitEvent;
import com.cburch.logisim.circuit.CircuitListener;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentEvent;
import com.cburch.logisim.comp.ComponentListener;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.soc.bus.SocBusMenuProvider;
import com.cburch.logisim.soc.data.SocBusStateInfo;
import com.cburch.logisim.soc.data.SocBusStateInfo.SocBusState;
import com.cburch.logisim.tools.CircuitStateHolder;
import com.cburch.logisim.soc.data.SocBusTransaction;

public class TraceWindowTableModel extends AbstractTableModel implements MouseListener,
        SocBusStateInfo.SocBusStateListener,ComponentListener,CircuitListener {
	
  private class HeaderRenderer extends JLabel implements TableCellRenderer {
    private static final long serialVersionUID = 1L;
	@Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
      JLabel l = new JLabel(getColumnHeader(column));
      l.setBorder(BorderFactory.createEtchedBorder());
      return l;
    }
  }
  
  private class CellRenderer extends JPanel implements TableCellRenderer {
    private static final long serialVersionUID = 1L;
    @Override
    public java.awt.Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
      return (JPanel)value;
    }
  }
  
  public String getColumnHeader(int column) {
    SocBusStateInfo.SocBusState info = getInfoAtColumn(column); 
    if (info==null || myTraceList.get(info) == null)
      return "BUG";
    else {
      myTraceList.get(info).registerCircuitListener(this);
      myTraceList.get(info).registerComponentListener(this);
      return myTraceList.get(info).getName();
    }
  }

  private static final long serialVersionUID = 1L;
  private JTable table;
  private SocBusMenuProvider.InstanceInformation parrent;

  private HashMap<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo> myTraceList;
  private int BoxWidth = SocBusStateInfo.BlockWidth;
  
  public TraceWindowTableModel(HashMap<SocBusStateInfo.SocBusState, CircuitStateHolder.HierarchyInfo> traceList, 
                               SocBusMenuProvider.InstanceInformation p) {
    myTraceList = traceList;
    parrent = p;
    rebuild();
  }
  
  public void setColMod(JTable v) { table = v; table.addMouseListener(this);}
  
  public void rebuild() {
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i)!= null) i.registerListener(this); else i.deregisterListener(this);
    fireTableStructureChanged();
    if (table != null) {
      for (int i = 0 ; i < getColumnCount() ; i++)
    	table.getColumnModel().getColumn(i).setPreferredWidth(AppPreferences.getScaled(BoxWidth));
      table.setRowHeight(AppPreferences.getScaled(2*SocBusStateInfo.TraceHeight+SocBusStateInfo.TraceHeight/2));
      table.getTableHeader().setPreferredSize(new Dimension(AppPreferences.getScaled(BoxWidth),AppPreferences.getScaled(20)));
    }
  }
  
  public int getBoxWidth() { return BoxWidth; }
  public void setBoxWidth(int value) { BoxWidth = value; rebuild(); }
  
  @Override
  public int getRowCount() {
	int max = 1;
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null)
        if (i.getNrOfEntires() > max)
          max = i.getNrOfEntires();
    return max;
  }

  @Override
  public int getColumnCount() {
	int cols = 0;
	for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null)
        cols++;
    return cols;
  }
  
  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) { return false; } 

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    SocBusStateInfo.SocBusState.SocBusStateTrace info = getInfoAtColumn(columnIndex).getEntry(rowIndex, this);
    if (info != null && info.getTransaction() != null) {
      SocBusTransaction trans = info.getTransaction();
      Object master = trans.getTransactionInitiator();
      if (master != null && master instanceof Component) ((Component)master).addComponentListener(this);
      if (trans.getTransactionResponder() != null) trans.getTransactionResponder().addComponentListener(this);
    }
    return info;
  }
  
  private SocBusStateInfo.SocBusState getInfoAtColumn(int column) {
    ArrayList<SocBusStateInfo.SocBusState> sortedList = new ArrayList<SocBusStateInfo.SocBusState>();
    for (SocBusStateInfo.SocBusState i : myTraceList.keySet())
      if (myTraceList.get(i) != null) {
        if (sortedList.isEmpty())
          sortedList.add(i);
        else {
          boolean inserted = false;
          for (int j = 0 ; j < sortedList.size() ; j++) {
            if (myTraceList.get(i).getName().compareTo(myTraceList.get(sortedList.get(j)).getName())<=0) {
              sortedList.add(j, i);
              inserted = true;
              break;
            }
          }
          if (!inserted)
            sortedList.add(i);
        }
      }
    if (column < 0 || column >= sortedList.size()) return null;
    return sortedList.get(column);
  }

  @Override
  public void fireCanged(SocBusState item) {
    if (myTraceList.containsKey(item) && myTraceList.get(item)!= null)
      fireTableDataChanged();
  }
  
  public TableCellRenderer getCellRenderer() {
    return new CellRenderer();
  }
  
  public TableCellRenderer getHeaderRenderer() {
    return new HeaderRenderer();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getClickCount() > 1) {
      java.awt.Point p = e.getPoint();
      int index = table.getColumnModel().getColumnIndexAtX(p.x);
      int realIndex = table.getColumnModel().getColumn(index).getModelIndex();
      SocBusStateInfo.SocBusState i = getInfoAtColumn(realIndex);
      if (i != null && myTraceList.get(i) != null) {
    	myTraceList.get(i).deregisterCircuitListener(this);
    	myTraceList.get(i).deregisterComponentListener(this);
        myTraceList.put(i,null);
        rebuild();
        if (getColumnCount() == 0)
          parrent.destroyTraceWindow();
      }
    }
  }

  @Override
  public void mousePressed(MouseEvent e) {}

  @Override
  public void mouseReleased(MouseEvent e) {}

  @Override
  public void mouseEntered(MouseEvent e) {}

  @Override
  public void mouseExited(MouseEvent e) {}

  @Override
  public void componentInvalidated(ComponentEvent e) {}

  @Override
  public void endChanged(ComponentEvent e) {}

  @Override
  public void LabelChanged(ComponentEvent e) { rebuild();}

  @Override
  public void circuitChanged(CircuitEvent event) { 
    if (event.getAction() == CircuitEvent.ACTION_SET_NAME) rebuild();
  }

}
