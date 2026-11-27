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
    final var attrs = path.leaf().getAttributeSet();
    final var rows = Tty.resolveRowCount(attrs);
    final var cols = Tty.resolveColumnCount(attrs);
    final var font = attrs.getValue(Tty.ATTR_FONT);

    // Follow the referenced TTY component's rows, columns, and content font, even while editing an
    // appearance without active simulation data.
    final var layout = Tty.computeBounds(bounds.getX(), bounds.getY(), rows, cols, font);
    setBounds(layout.getWidth(), layout.getHeight());

    final var data = state == null ? null : (TtyState) getData(state);
    GraphicsUtil.switchToWidth(g, 2);
    g.setColor(Color.YELLOW);
    g.fillRoundRect(
        bounds.getX(),
        bounds.getY(),
        bounds.getWidth(),
        bounds.getHeight(),
        2 * Tty.BORDER,
        2 * Tty.BORDER);
    g.setColor(Color.BLACK);
    g.drawRoundRect(
        bounds.getX(),
        bounds.getY(),
        bounds.getWidth(),
        bounds.getHeight(),
        2 * Tty.BORDER,
        2 * Tty.BORDER);
    if (data != null) {
      final var dataRows = data.getNrRows();
      final var rowData = new String[dataRows];
      synchronized (data) {
        for (var i = 0; i < dataRows; i++) rowData[i] = data.getRowString(i);
      }
      g.setFont(font);
      final var fm = g.getFontMetrics();
      final var rowHeight = Tty.getRowHeight(fm);
      final var x = bounds.getX() + Tty.BORDER;
      var y = bounds.getY() + Tty.BORDER + (rowHeight + fm.getAscent()) / 2;
      for (var i = 0; i < dataRows; i++) {
        g.drawString(rowData[i], x, y);
        y += rowHeight;
      }
    }
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {ATTR_LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR});
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
