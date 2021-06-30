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

package com.cburch.logisim.util;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.LayoutManager2;
import java.util.ArrayList;

public class TableLayout implements LayoutManager2 {
  private final int colCount;
  private final ArrayList<Component[]> contents;
  private int curRow;
  private int curCol;
  private Dimension prefs;
  private int[] prefRow;
  private int[] prefCol;
  private double[] rowWeight;

  public TableLayout(int colCount) {
    this.colCount = colCount;
    this.contents = new ArrayList<>();
    this.curRow = 0;
    this.curCol = 0;
  }

  @Override
  public void addLayoutComponent(Component comp, Object constraints) {
    if (constraints instanceof TableConstraints) {
      final var con = (TableConstraints) constraints;
      if (con.getRow() >= 0) curRow = con.getRow();
      if (con.getCol() >= 0) curCol = con.getCol();
    }
    addLayoutComponent("", comp);
  }

  @Override
  public void addLayoutComponent(String name, Component comp) {
    while (curRow >= contents.size()) {
      contents.add(new Component[colCount]);
    }
    final var rowContents = contents.get(curRow);
    rowContents[curCol] = comp;
    ++curCol;
    if (curCol == colCount) {
      ++curRow;
      curCol = 0;
    }
    prefs = null;
  }

  @Override
  public float getLayoutAlignmentX(Container parent) {
    return 0.5f;
  }

  @Override
  public float getLayoutAlignmentY(Container parent) {
    return 0.5f;
  }

  @Override
  public void invalidateLayout(Container parent) {
    prefs = null;
  }

  @Override
  public void layoutContainer(Container parent) {
    final var pref = preferredLayoutSize(parent);
    int[] prefRow = this.prefRow;
    int[] prefCol = this.prefCol;
    final var size = parent.getSize();

    double y0;
    int yRemaining = size.height - pref.height;
    var rowWeightTotal = 0.0;
    if (yRemaining != 0 && rowWeight != null) {
      for (double weight : rowWeight) {
        rowWeightTotal += weight;
      }
    }
    if (rowWeightTotal == 0.0 && yRemaining > 0) {
      y0 = yRemaining / 2.0;
    } else {
      y0 = 0;
    }

    int x0 = (size.width - pref.width) / 2;
    if (x0 < 0) x0 = 0;
    double y = y0;
    int i = -1;
    for (Component[] row : contents) {
      i++;
      int yRound = (int) (y + 0.5);
      int x = x0;
      for (var j = 0; j < row.length; j++) {
        final var comp = row[j];
        if (comp != null) {
          row[j].setBounds(x, yRound, prefCol[j], prefRow[i]);
        }
        x += prefCol[j];
      }
      y += prefRow[i];
      if (rowWeightTotal > 0 && i < rowWeight.length) {
        y += yRemaining * rowWeight[i] / rowWeightTotal;
      }
    }

    // TODO Auto-generated method stub

  }

  @Override
  public Dimension maximumLayoutSize(Container parent) {
    return new Dimension(Integer.MAX_VALUE, Integer.MAX_VALUE);
  }

  @Override
  public Dimension minimumLayoutSize(Container parent) {
    return preferredLayoutSize(parent);
  }

  @Override
  public Dimension preferredLayoutSize(Container parent) {
    if (prefs == null) {
      final var prefCol = new int[colCount];
      final var prefRow = new int[contents.size()];
      var height = 0;
      for (var i = 0; i < prefRow.length; i++) {
        final var row = contents.get(i);
        var rowHeight = 0;
        for (var j = 0; j < row.length; j++) {
          if (row[j] != null) {
            final var dim = row[j].getPreferredSize();
            if (dim.height > rowHeight) rowHeight = dim.height;
            if (dim.width > prefCol[j]) prefCol[j] = dim.width;
          }
        }
        prefRow[i] = rowHeight;
        height += rowHeight;
      }
      var width = 0;
      for (var j : prefCol) {
        width += j;
      }
      this.prefs = new Dimension(width, height);
      this.prefRow = prefRow;
      this.prefCol = prefCol;
    }
    return new Dimension(prefs);
  }

  @Override
  public void removeLayoutComponent(Component comp) {
    for (final var row : contents) {
      for (var j = 0; j < row.length; j++) {
        if (row[j] == comp) {
          row[j] = null;
          return;
        }
      }
    }
    prefs = null;
  }

  public void setRowWeight(int rowIndex, double weight) {
    if (weight < 0) {
      throw new IllegalArgumentException("weight must be nonnegative");
    }
    if (rowIndex < 0) {
      throw new IllegalArgumentException("row index must be nonnegative");
    }
    if ((rowWeight == null || rowIndex >= rowWeight.length) && weight != 0.0) {
      final var a = new double[rowIndex + 10];
      if (rowWeight != null) System.arraycopy(rowWeight, 0, a, 0, rowWeight.length);
      rowWeight = a;
    }
    rowWeight[rowIndex] = weight;
  }
}
