/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.soc.data;

import static com.cburch.logisim.soc.Strings.S;

import com.cburch.contracts.BaseMouseListenerContract;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.util.LocaleListener;
import com.cburch.logisim.util.LocaleManager;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.MouseEvent;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import javax.swing.BorderFactory;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableCellRenderer;

public class SocMemMapModel extends AbstractTableModel
    implements SocBusSlaveListener, LocaleListener, BaseMouseListenerContract {

  private static final long serialVersionUID = 1L;
  private static final long longMask = Long.parseUnsignedLong("FFFFFFFF", 16);

  public static class MemoryMapHeaderRenderer extends JLabel implements TableCellRenderer {
    private static final long serialVersionUID = 1L;

    public MemoryMapHeaderRenderer() {
      setForeground(Color.BLUE);
      setBorder(BorderFactory.createEtchedBorder());
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      if (column < 2)
        setHorizontalAlignment(JLabel.CENTER);
      else
        setHorizontalAlignment(JLabel.LEFT);
      setText(value.toString());
      return this;
    }
  }

  public static class SlaveInfoRenderer extends JLabel implements TableCellRenderer {

    private static final long serialVersionUID = 1L;

    public SlaveInfoRenderer() {
      setOpaque(true);
    }

    @Override
    public Component getTableCellRendererComponent(
        JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      SlaveInfo s = (SlaveInfo) value;
      setBackground(s.getColor());
      setForeground(s.getTextColor());
      switch (column) {
        case 0 -> {
          setText(String.format("0x%08X", s.getStartAddress()));
          setHorizontalAlignment(JLabel.CENTER);
        }
        case 1 -> {
          setText(String.format("0x%08X", s.getEndAddress()));
          setHorizontalAlignment(JLabel.CENTER);
        }
        default -> {
          setText(s.getName());
          setHorizontalAlignment(JLabel.LEFT);
        }
      }
      return this;
    }
  }

  public static class SlaveInfo {
    private boolean hasOverlap;
    private final SocBusSlaveInterface slave;
    private long start;
    private long end;

    public SlaveInfo(SocBusSlaveInterface s) {
      slave = s;
      hasOverlap = false;
    }

    public SlaveInfo(long start, long end) {
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
      return (slave == null) ? start : ((long) slave.getStartAddress()) & longMask;
    }

    public long getEndAddress() {
      if (slave == null)
        return end;
      long start = ((long) slave.getStartAddress()) & longMask;
      long size = ((long) slave.getMemorySize()) & longMask;
      return start + size - 1;
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

  private static class SlaveMap {
    private final LinkedList<SlaveInfo> slaves;

    public SlaveMap() {
      slaves = new LinkedList<>();
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
      for (int i = 0; i < slaves.size(); i++) {
        SlaveInfo cur = slaves.get(i);
        if (cur.contains(slave.getStartAddress()))
          slave.setOverlap();
        if (cur.getStartAddress() >= slave.getStartAddress()) {
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

  private final ArrayList<SocBusSlaveInterface> slaves;
  private final SlaveMap slaveMap;
  private final SlaveInfoRenderer slaveRenderer;
  private final MemoryMapHeaderRenderer headRenderer;
  private InstanceComponent marked;

  public SocMemMapModel() {
    super();
    LocaleManager.addLocaleListener(this);
    slaveMap = new SlaveMap();
    slaves = new ArrayList<>();
    slaveRenderer = new SlaveInfoRenderer();
    headRenderer = new MemoryMapHeaderRenderer();
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

  public List<SocBusSlaveInterface> getSlaves() {
    return slaves;
  }

  public SlaveInfoRenderer getCellRender() {
    return slaveRenderer;
  }

  public MemoryMapHeaderRenderer getHeaderRenderer() {
    return headRenderer;
  }


  @Override
  public String getColumnName(int col) {
    return switch (col) {
      case 0 -> S.get("SocMemMapStartAddress");
      case 1 -> S.get("SocMemMapEndAddress");
      case 2 -> S.get("SocMemMapSlaveName");
      default -> "";
    };
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
  public boolean isCellEditable(int row, int col) {
    return false;
  }

  @Override
  public void labelChanged() {
    rebuild();
  }

  @Override
  public void memoryMapChanged() {
    rebuild();
  }

  private void rebuild() {
    slaveMap.clear();
    if (slaves.isEmpty())
      slaveMap.add(new SlaveInfo(0, -1));
    else {
      for (SocBusSlaveInterface s : slaves)
        slaveMap.add(s);
      /* now we fill in the blanks */
      ArrayList<SlaveInfo> empties = new ArrayList<>();
      long addr = 0;
      for (int i = 0; i < slaveMap.size(); i++) {
        SlaveInfo s = slaveMap.getSlave(i);
        if (addr < s.getStartAddress()) {
          empties.add(new SlaveInfo(addr, s.getStartAddress() - 1));
        }
        addr = s.getEndAddress() + 1;
      }
      if (addr < longMask)
        empties.add(new SlaveInfo(addr, longMask));
      for (SlaveInfo s : empties)
        slaveMap.add(s);
    }
    fireTableDataChanged();
  }

  @Override
  public void localeChanged() {
    rebuild();
  }

  @Override
  public void mouseClicked(MouseEvent e) {
    if (e.getComponent() instanceof JTable table) {
      final var row = table.rowAtPoint(e.getPoint());
      if (row >= 0 && row < slaveMap.size()) {
        final var comp = slaveMap.getSlave(row).getComponent();
        if (marked != null) marked.clearMarks();
        comp.markInstance();
        comp.getInstance().fireInvalidated();
        marked = comp;
      }
    }
  }

  @Override
  public void mouseExited(MouseEvent e) {
    if (marked != null) {
      marked.clearMarks();
      marked.getInstance().fireInvalidated();
      marked = null;
    }
  }
}
