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

import com.cburch.draw.shapes.DrawAttr;
import com.cburch.logisim.circuit.CircuitState;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.prefs.AppPreferences;
import com.cburch.logisim.util.UnmodifiableList;
import java.awt.Color;
import java.awt.Graphics;
import java.util.List;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class MatrixKeypadShape extends DynamicElement {

  public MatrixKeypadShape(int x, int y, DynamicElement.Path path) {
    super(path, Bounds.create(x, y, 60, 60));
  }

  @Override
  public boolean contains(Location loc, boolean assumeFilled) {
    final var boundsX = bounds.getX();
    final var boundsY = bounds.getY();
    final var width = bounds.getWidth();
    final var height = bounds.getHeight();
    final var pointX = loc.getX();
    final var pointY = loc.getY();
    return pointX >= boundsX && pointX < boundsX + width && pointY >= boundsY && pointY < boundsY + height;
  }

  @Override
  public List<Attribute<?>> getAttributes() {
    return UnmodifiableList.create(
        new Attribute<?>[] {
          DrawAttr.STROKE_WIDTH, StdAttr.LABEL, StdAttr.LABEL_FONT, StdAttr.LABEL_COLOR
        });
  }

  @Override
  @SuppressWarnings("unchecked")
  public <V> V getValue(Attribute<V> attr) {
    return super.getValue(attr);
  }

  @Override
  public void updateValue(Attribute<?> attr, Object value) {
    super.updateValue(attr, value);
  }

  @Override
  public void paintDynamic(Graphics g, CircuitState state) {
    final var boundsX = bounds.getX();
    final var boundsY = bounds.getY();
    final var width = bounds.getWidth();
    final var height = bounds.getHeight();

    g.setColor(new Color(50, 50, 50));
    g.fillRoundRect(boundsX, boundsY, width, height, 8, 8);
    g.setColor(new Color(AppPreferences.COMPONENT_COLOR.get()));
    g.drawRoundRect(boundsX, boundsY, width, height, 8, 8);

    final var keySize = 12;
    final var gap = 2;
    final var step = keySize + gap;
    final var startX = boundsX + 3;
    final var startY = boundsY + 3;

    for (int row = 0; row < 4; row++) {
      for (int column = 0; column < 4; column++) {
        final var keyX = startX + column * step;
        final var keyY = startY + row * step;
        final var isRedKey = (column == 3 || row == 3);
        g.setColor(isRedKey ? new Color(220, 40, 40) : new Color(0, 170, 220));
        g.fillRoundRect(keyX, keyY, keySize, keySize, 2, 2);
        g.setColor(Color.BLACK);
        g.drawRoundRect(keyX, keyY, keySize, keySize, 2, 2);
      }
    }

    drawLabel(g);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("matrix-keypad"));
  }

  @Override
  public String getDisplayName() {
    return S.get("matrixKeypadComponent");
  }

  @Override
  public String toString() {
    return "MatrixKeypad:" + getBounds();
  }
}
