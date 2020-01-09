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

package com.cburch.draw.tools;

import com.cburch.draw.icons.DrawShapeIcon;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.DrawAttr;
import com.cburch.draw.shapes.Oval;
import com.cburch.logisim.data.Attribute;
import java.awt.Graphics;
import java.util.List;
import javax.swing.Icon;

public class OvalTool extends RectangularTool {
  private DrawingAttributeSet attrs;

  public OvalTool(DrawingAttributeSet attrs) {
    this.attrs = attrs;
  }

  @Override
  public CanvasObject createShape(int x, int y, int w, int h) {
    return attrs.applyTo(new Oval(x, y, w, h));
  }

  @Override
  public void drawShape(Graphics g, int x, int y, int w, int h) {
    g.drawOval(x, y, w, h);
  }

  @Override
  public void fillShape(Graphics g, int x, int y, int w, int h) {
    g.fillOval(x, y, w, h);
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return DrawAttr.getFillAttributes(attrs.getValue(DrawAttr.PAINT_TYPE));
  }

  @Override
  public Icon getIcon() {
    return new DrawShapeIcon(DrawShapeIcon.ELIPSE);
  }
}
