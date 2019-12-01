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

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.util.List;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;

public class TtyShape extends DynamicElement {

  public TtyShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 240, 130));
  }

  public void setBounds(int width , int height) {
    bounds = Bounds.create(bounds.getX(), bounds.getY(), width, height);;
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    TtyState data = state == null ? null : (TtyState) getData(state);
    if (data != null) {
      int rows = data.getNrRows();
      int cols = data.getNrCols();
      int width = 2 * Tty.BORDER + cols * Tty.COL_WIDTH;
      int height = 2 * Tty.BORDER + rows * Tty.ROW_HEIGHT;
      if (width < 30) width = 30;
      if (height < 30) height = 30;
      setBounds(width,height);
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.YELLOW);
    g.fillRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(),
    		2 * Tty.BORDER, 2 * Tty.BORDER);
    g.setColor(Color.BLACK);
    g.drawRoundRect( bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 
                     2 * Tty.BORDER, 2 * Tty.BORDER);
    if (data != null) {
      int rows = data.getNrRows();
      String[] rowData = new String[rows];
      synchronized (data) {
        for (int i = 0 ; i < rows; i++)
          rowData[i] = data.getRowString(i);
      }
      g.setFont(Tty.DEFAULT_FONT);
      FontMetrics fm = g.getFontMetrics();
      int x = bounds.getX() + Tty.BORDER;
      int y = bounds.getY() + Tty.BORDER + (Tty.ROW_HEIGHT + fm.getAscent()) / 2;
      for (int i = 0; i < rows; i++) {
        g.drawString(rowData[i], x, y);
        y += Tty.ROW_HEIGHT;
      }
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(new Attribute<?>[] {ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-tty"));
  }
  @Override
  public String getDisplayName() {
    return S.get("ttyComponent");
  }

  @Override
  public String toString() {
    return "Tty:" + getBounds();
  }
}
