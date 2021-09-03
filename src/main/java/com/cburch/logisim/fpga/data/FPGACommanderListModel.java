/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.fpga.data;

import com.cburch.logisim.fpga.designrulecheck.SimpleDRCContainer;
import com.cburch.logisim.fpga.gui.ListModelCellRenderer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;
import javax.swing.AbstractListModel;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListDataEvent;
import javax.swing.event.ListDataListener;

@SuppressWarnings("serial")
public class FPGACommanderListModel extends AbstractListModel<Object> {

  private final ArrayList<Object> myData;
  private final Set<ListDataListener> myListeners;
  private int count = 0;
  private final ListModelCellRenderer MyRender;

  public FPGACommanderListModel(boolean CountLines) {
    myData = new ArrayList<>();
    myListeners = new HashSet<>();
    MyRender = new ListModelCellRenderer(CountLines);
  }

  public ListCellRenderer<Object> getMyRenderer() {
    return MyRender;
  }

  public void clear() {
    myData.clear();
    count = 0;
    FireEvent(null);
  }

  public void add(Object toAdd) {
    count++;
    if (toAdd instanceof SimpleDRCContainer) {
      SimpleDRCContainer add = (SimpleDRCContainer) toAdd;
      if (add.getSupressCount()) count--;
      else add.setListNumber(count);
    }
    myData.add(toAdd);
    ListDataEvent e = new ListDataEvent(this, ListDataEvent.CONTENTS_CHANGED, 0, myData.size());
    FireEvent(e);
  }

  public int getCountNr() {
    return count;
  }

  @Override
  public int getSize() {
    return myData.size();
  }

  @Override
  public Object getElementAt(int index) {
    if (index < myData.size()) return myData.get(index);
    return null;
  }

  @Override
  public void addListDataListener(ListDataListener l) {
    myListeners.add(l);
  }

  @Override
  public void removeListDataListener(ListDataListener l) {
    myListeners.remove(l);
  }

  private void FireEvent(ListDataEvent e) {
    for (ListDataListener l : myListeners) l.contentsChanged(e);
  }
}
