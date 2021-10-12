/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.io;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.util.GraphicsUtil;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class TtyShape extends DynamicElement {

  public TtyShape(int x, int y, DynamicElement.Path p) {
    super(p, Bounds.create(x, y, 240, 130));
  }

  public void setBounds(int width, int height) {
    bounds = Bounds.create(bounds.getX(), bounds.getY(), width, height);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    final var data = state == null ? null : (TtyState) getData(state);
    if (data != null) {
      final var rows = data.getNrRows();
      final var cols = data.getNrCols();
      var width = 2 * Tty.BORDER + cols * Tty.COL_WIDTH;
      if (width < 30) width = 30;
      var height = 2 * Tty.BORDER + rows * Tty.ROW_HEIGHT;
      if (height < 30) height = 30;
      setBounds(width, height);
    }
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.YELLOW);
    g.fillRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 2 * Tty.BORDER, 2 * Tty.BORDER);
    g.setColor(Color.BLACK);
    g.drawRoundRect(bounds.getX(), bounds.getY(), bounds.getWidth(), bounds.getHeight(), 2 * Tty.BORDER, 2 * Tty.BORDER);
    if (data != null) {
      final var rows = data.getNrRows();
      final var rowData = new String[rows];
      synchronized (data) {
        for (var i = 0; i < rows; i++)
          rowData[i] = data.getRowString(i);
      }
      g.setFont(Tty.DEFAULT_FONT);
      final var fm = g.getFontMetrics();
      final var x = bounds.getX() + Tty.BORDER;
      var y = bounds.getY() + Tty.BORDER + (Tty.ROW_HEIGHT + fm.getAscent()) / 2;
      for (var i = 0; i < rows; i++) {
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
