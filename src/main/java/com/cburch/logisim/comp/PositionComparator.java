package com.cburch.logisim.comp;

import com.cburch.logisim.data.Location;
import java.util.Comparator;

public class PositionComparator implements Comparator<Component> {
  @Override
  public int compare(Component o1, Component o2) {
    if (o1 == o2) {
      return 0;
    }
    Location l1 = o1.getLocation();
    Location l2 = o2.getLocation();
    if (l2.getY() != l1.getY()) {
      return l1.getY() - l2.getY();
    }
    if (l2.getX() != l1.getX()) {
      return l1.getX() - l2.getX();
    }
    return -1;
  }
}
