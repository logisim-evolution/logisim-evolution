/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.analyze.data;

import com.cburch.logisim.analyze.gui.KarnaughMapPanel;
import com.cburch.logisim.analyze.model.AnalyzerModel;
import com.cburch.logisim.analyze.model.Expression;
import com.cburch.logisim.analyze.model.Implicant;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.List;
import lombok.Getter;
import lombok.val;

public class KMapGroups {

  public static class CoverInfo {
    @Getter private final int row;  // start row
    @Getter private final int col; // start col
    @Getter private int width = 1;
    @Getter private int height = 1;

    public CoverInfo(int col, int row) {
      this.row = row;
      this.col = col;
    }

    private boolean canMerge(int col, int row) {
      /* we know that we scan left to right and top down, hence we can simplify
       * this test.
       */
      if (col >= this.col && col < (this.col + width)) {
        // the same col, two possibilities:
        // 1) either also the same row-range
        if (row >= this.row && row < (this.row + height)) return true;
        // 2) maybe one down
        return row >= this.row && row <= (this.row + height);
        // nope, distance too big
      }
      if (row >= this.row && row < (this.row + height)) {
        // same row, two possibilities:
        // 1) either also the same col-range; we do not need to check as it collides
        //    with the previous check of the same row range (see 1) above)
        // 2) maybe one to the right
        return col >= this.col && col <= (this.col + width);
      }
      return false;
    }

    public boolean merge(int col, int row) {
      if (!canMerge(col, row)) return false;
      // let's look where to merge:
      if (col >= this.col && col < (this.col + width)) {
        if (row >= this.row && row < (this.row + height))
          // 1) already inside
          return true;
        else
          // 2) to the bottom
          height++;
      } else
        // 3) to the right
        width++;
      return true;
    }
  }

  public class KMapGroupInfo {
    @Getter private final ArrayList<CoverInfo> areas;
    @Getter private final Color color;
    private final ArrayList<Implicant> singleCoveredImplicants;
    private final Expression expression;

    public KMapGroupInfo(Implicant imp, Color col) {
      this.color = col;
      areas = new ArrayList<>();
      singleCoveredImplicants = new ArrayList<>();
      val one = new ArrayList<Implicant>();
      one.add(imp);
      expression = Implicant.toExpression(format, model, one);
      build(imp);
    }

    public void removeSingleCover(Implicant imp) {
      singleCoveredImplicants.remove(imp);
    }

    public boolean containsSingleCover(Implicant imp) {
      return singleCoveredImplicants.contains(imp);
    }

    public void paint(Graphics2D g, int x, int y, int cellWidth, int cellHeight, boolean highlighted, boolean colored) {
      var d = 2 * IMP_RADIUS;
      val col = g.getColor();
      if (highlighted)
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 180));
      else if (colored)
        g.setColor(new Color(color.getRed(), color.getGreen(), color.getBlue(), 128));
      else g.setColor(new Color(128, 128, 128, 128));
      for (val cover : areas) {
        g.fillRoundRect(
            x + cover.getCol() * cellWidth + IMP_INSET,
            y + cover.getRow() * cellHeight + IMP_INSET,
            cover.getWidth() * cellWidth - 2 * IMP_INSET,
            cover.getHeight() * cellHeight - 2 * IMP_INSET,
            d,
            d);
      }
      g.setColor(col);
    }

    public boolean insideCover(int col, int row) {
      val table = model.getTruthTable();
      if (table.getInputColumnCount() > KarnaughMapPanel.MAX_VARS) return false;
      val kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
      val kmapCols = 1 << KarnaughMapPanel.COL_VARS[table.getInputColumnCount()];
      for (val sq : singleCoveredImplicants) {
        val tableRow = sq.getRow();
        if (tableRow < 0) return false;
        val krow = KarnaughMapPanel.getRow(tableRow, kmapRows, kmapCols);
        val kcol = KarnaughMapPanel.getCol(tableRow, kmapRows, kmapCols);
        if (krow == row && kcol == col) return true;
      }
      return false;
    }

    private void addSingleCover(Implicant imp) {
      /* we have to make sure that only one cover contains the implicant
       * in case that multiple covers cover the implicant, so we remove
       * a multi-covered implicant from the existing covers. We could also
       * not add it to the current cover, it's a choice.
       */
      for (val other : covers) {
        if (other.containsSingleCover(imp)) {
          other.removeSingleCover(imp);
        }
      }
      singleCoveredImplicants.add(imp);
    }

    private void build(Implicant imp) {
      val table = model.getTruthTable();
      if (table.getInputColumnCount() > KarnaughMapPanel.MAX_VARS) return;
      int kmapRows = 1 << KarnaughMapPanel.ROW_VARS[table.getInputColumnCount()];
      int kmapCols = 1 << KarnaughMapPanel.COL_VARS[table.getInputColumnCount()];

      val imps = new Boolean[kmapRows][kmapCols];
      for (var row = 0; row < kmapRows; row++) {
        for (var col = 0; col < kmapCols; col++) imps[row][col] = false;
      }
      for (val sq : imp.getTerms()) {
        addSingleCover(sq);
        val tableRow = sq.getRow();
        if (tableRow < 0) return;
        val row = KarnaughMapPanel.getRow(tableRow, kmapRows, kmapCols);
        val col = KarnaughMapPanel.getCol(tableRow, kmapRows, kmapCols);
        if ((row < kmapRows) && (col < kmapCols)) imps[row][col] = true;
      }
      CoverInfo current = null;
      for (var row = 0; row < kmapRows; row++)
        for (var col = 0; col < kmapCols; col++) {
          if (imps[row][col]) {
            // we have a candidate
            if (current != null) {
              if (current.merge(col, row)) continue;
              if (!areas.contains(current)) areas.add(current);
            }
            // can we merge with an existing ?
            var found = false;
            for (val area : areas) {
              if (!found && area.merge(col, row)) {
                current = area;
                found = true;
              }
            }
            if (!found) current = new CoverInfo(col, row);
          } else {
            // no candidate
            if (current != null && !areas.contains(current)) areas.add(current);
            current = null;
          }
        }
      if (current != null && !areas.contains(current)) areas.add(current);
    }
  }

  private final AnalyzerModel model;
  private String output;
  private int format;
  @Getter private ArrayList<KMapGroupInfo> covers;
  private static final int IMP_RADIUS = 5;
  private static final int IMP_INSET = 4;
  private int highlighted;

  public KMapGroups(AnalyzerModel model) {
    this.model = model;
    highlighted = -1;
  }

  public void setformat(int format) {
    this.format = format;
    update();
  }

  public void setOutput(String name) {
    output = name;
    update();
  }

  public boolean highlight(int col, int row) {
    val oldHighlighted = highlighted;
    highlighted = -1;
    for (int nr = 0; nr < covers.size() && highlighted < 0; nr++) {
      if (covers.get(nr).insideCover(col, row)) highlighted = nr;
    }
    return oldHighlighted != highlighted;
  }

  public boolean clearHighlight() {
    val ret = highlighted >= 0;
    highlighted = -1;
    return ret;
  }

  public Expression getHighlightedExpression() {
    if (highlighted < 0 || highlighted >= covers.size()) return null;
    return covers.get(highlighted).expression;
  }

  public Color getBackgroundColor() {
    if (highlighted < 0 || highlighted >= covers.size()) return null;
    val col = covers.get(highlighted).color;
    return new Color(col.getRed(), col.getGreen(), col.getBlue(), 60);
  }

  public void update() {
    val implicants = model.getOutputExpressions().getMinimalImplicants(output);
    covers = new ArrayList<>();
    CoverColor.COVER_COLOR.reset();
    if (implicants != null) {
      for (val imp : implicants) {
        covers.add(new KMapGroupInfo(imp, CoverColor.COVER_COLOR.getNext()));
      }
    }
    highlighted = -1;
  }

  public void paint(Graphics2D g, int x, int y, int cellWidth, int cellHeight) {
    for (var cov = 0; cov < covers.size(); cov++) {
      if (cov == highlighted) continue;
      val curCov = covers.get(cov);
      curCov.paint(g, x, y, cellWidth, cellHeight, false, highlighted < 0);
    }
    if (highlighted >= 0 && highlighted < covers.size())
      covers.get(highlighted).paint(g, x, y, cellWidth, cellHeight, true, true);
  }
}
