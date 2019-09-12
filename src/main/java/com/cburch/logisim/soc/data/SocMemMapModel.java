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

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import java.util.LinkedList;

import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;

public class SocMemMapModel extends AbstractTableModel implements SocBusSlaveListener,LocaleListener,MouseListener {

  private static final long serialVersionUID = 1L;
  private static final long longMask = Long.parseUnsignedLong("FFFFFFFF", 16);
  
  public class memMapHeaderRenderer extends JLabel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

	public memMapHeaderRenderer() {
      setForeground(Color.BLUE);
      setBorder(BorderFactory.createEtchedBorder());
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
      if (column < 2)
        setHorizontalAlignment(JLabel.CENTER);
      else
    	setHorizontalAlignment(JLabel.LEFT);
      setText(value.toString());
      return this;
    }
  }
  
  public class SlaveInfoRenderer extends JLabel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

    public SlaveInfoRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
            int row, int column) {
    	SlaveInfo s = (SlaveInfo) value;
    	setBackground(s.getColor());
    	setForeground(s.getTextColor());
    	switch (column) {
    	  case 0 : setText(String.format("0x%08X", s.getStartAddress()));
    	           setHorizontalAlignment(JLabel.CENTER);
    	           break;
    	  case 1 : setText(String.format("0x%08X", s.getEndAddress()));
    	           setHorizontalAlignment(JLabel.CENTER);
    	           break;
    	  default: setText(s.getName());
    	           setHorizontalAlignment(JLabel.LEFT);
    	}
        return this;
    }
      
  }

  public class SlaveInfo {
    private boolean hasOverlap;
    private SocBusSlaveInterface slave;
    private long start;
    private long end;
    
    public SlaveInfo(SocBusSlaveInterface s) {
      slave = s;
      hasOverlap = false;
    }
    
    public SlaveInfo(long start , long end) {
      slave = null;
      hasOverlap = false;
      this.start = start;
      this.end = end;
    }
    
    public void setOverlap() {
      hasOverlap = true;
    }
    
    public boolean hasMemoryOverlap() {
      return hasOverlap;
    }
    
    public long getStartAddress() {
      return (slave == null) ? start : ((long)slave.getStartAddress())&longMask;
    }
    
    public long getEndAddress() {
      if (slave == null)
        return end;
      long start = ((long)slave.getStartAddress())&longMask;
      long size = ((long)slave.getMemorySize())&longMask;
      return start+size-1;
    }
    
    public String getName() {
      return slave == null ? S.get("SocMemMapEmpty") : slave.getName();
    }
    
    public boolean contains(long address) {
      boolean ret = address >= getStartAddress() && address <= getEndAddress();
      hasOverlap |= ret;
      return ret;
    }
    
    public Color getColor() {
      if (slave == null)
        return Color.LIGHT_GRAY;
      return hasOverlap ? Color.RED : Color.GREEN;
    }
    
    public Color getTextColor() {
      if (hasOverlap)
        return Color.LIGHT_GRAY;
      return Color.BLACK;
    }
    
    public InstanceComponent getComponent() {
      if (slave == null)
        return null;
      return slave.getComponent();
    }
  }
  
  private class SlaveMap {
    private LinkedList<SlaveInfo> slaves;
    
    public SlaveMap() {
      slaves = new LinkedList<SlaveInfo>();
    }
    
    public void add(SocBusSlaveInterface s) {
      SlaveInfo slave = new SlaveInfo(s);
      add(slave);
    }
    
    public void add(SlaveInfo slave) {
      if (slaves.isEmpty()) {
        slaves.add(slave);
        return;
      }
      for (int i = 0 ; i < slaves.size() ; i++) {
        SlaveInfo cur = slaves.get(i);
        if (cur.contains(slave.getStartAddress()))
          slave.setOverlap();
        if (cur.getStartAddress()>= slave.getStartAddress()) {
          if (slave.contains(cur.getStartAddress()))
            cur.setOverlap();
          slaves.add(i, slave);
          return;
        }
      }
      slaves.add(slave);
    }
    
    public SlaveInfo getSlave(int index) {
      if (index < 0 || index >= slaves.size())
        return null;
      return slaves.get(index);
    }
    
    public int size() {
      return slaves.size();
    }
    
    public void clear() {
      slaves.clear();
    }
  }
  
  private ArrayList<SocBusSlaveInterface> slaves;
  private SlaveMap slaveMap;
  private SlaveInfoRenderer slaveRenderer;
  private memMapHeaderRenderer headRenderer;
  private InstanceComponent marked;
  
  public SocMemMapModel() {
    super();
    LocaleManager.addLocaleListener(this);
    slaveMap = new SlaveMap();
    slaves = new ArrayList<SocBusSlaveInterface>();
    slaveRenderer = new SlaveInfoRenderer();
    headRenderer = new memMapHeaderRenderer();
    marked = null;
    rebuild();
  }

  public void registerSocBusSlave(SocBusSlaveInterface slave) {
    if (!slaves.contains(slave)) {
      slaves.add(slave);
      slave.registerListener(this);
      rebuild();
    }
  }
      
  public void removeSocBusSlave(SocBusSlaveInterface slave) {
    if (slaves.contains(slave)) {
      slaves.remove(slave);
      slave.removeListener(this);
      rebuild();
    }
  }
  
  public ArrayList<SocBusSlaveInterface> getSlaves() {
    return slaves;
  }
  
  public SlaveInfoRenderer getCellRender() {
    return slaveRenderer;
  }
  
  public memMapHeaderRenderer getHeaderRenderer() {
    return headRenderer;
  }
  
  
  @Override
  public String getColumnName(int col) {
    switch (col) {
      case 0 : return S.get("SocMemMapStartAddress");
      case 1 : return S.get("SocMemMapEndAddress");
      case 2 : return S.get("SocMemMapSlaveName");
    }
    return "";
  }
  
  @Override
  public int getRowCount() {
    return slaveMap.size();
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return slaveMap.getSlave(rowIndex);
  }
  
  @Override
  public boolean isCellEditable(int row, int col) { return false; }

  @Override
  public void labelChanged() { rebuild(); }

  @Override
  public void memoryMapChanged() { rebuild(); }

  private void rebuild() {
    slaveMap.clear();
    if (slaves.isEmpty())
      slaveMap.add(new SlaveInfo(0,-1));
    else {
      for (SocBusSlaveInterface s : slaves)
        slaveMap.add(s);
      /* now we fill in the blanks */
      ArrayList<SlaveInfo> empties = new ArrayList<SlaveInfo>();
      long addr = 0;
      for (int i = 0 ; i < slaveMap.size() ; i++) {
        SlaveInfo s = slaveMap.getSlave(i);
        if (addr < s.getStartAddress()) {
          empties.add(new SlaveInfo(addr,s.getStartAddress()-1));
        }
        addr = s.getEndAddress()+1;
      }
      if (addr < longMask)
        empties.add(new SlaveInfo(addr,longMask));
      for (SlaveInfo s : empties)
        slaveMap.add(s);
    }
    fireTableDataChanged();
  }

  @Override
  public void localeChanged() { rebuild(); }
  
  

  @Override
  public void mouseClicked(MouseEvent e) {
	if (e.getComponent() instanceof JTable) {
      JTable t = (JTable) e.getComponent();
      int row = t.rowAtPoint(e.getPoint());
      if (row >= 0 && row < slaveMap.size()) {
        InstanceComponent comp = slaveMap.getSlave(row).getComponent();
        if (marked != null)
          marked.clearMarks();
        comp.MarkInstance();
        comp.getInstance().fireInvalidated();
        marked = comp;
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
  public void mouseExited(MouseEvent e) { 
    if (marked != null) {
      marked.clearMarks();
      marked.getInstance().fireInvalidated();
      marked = null; 
    }
  }
}
