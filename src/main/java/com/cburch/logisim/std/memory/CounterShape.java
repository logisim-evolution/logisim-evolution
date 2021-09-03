/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.std.memory;

import static com.cburch.logisim.std.Strings.S;

import com.cburch.logisim.circuit.appear.DynamicElement;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public class CounterShape extends RegisterShape {
  public CounterShape(int x, int y, DynamicElement.Path p) {
    super(x, y, p);
  }

  @Override
  public Element toSvgElement(Document doc) {
    return toSvgElement(doc.createElement("visible-counter"));
  }

  @Override
  public String getDisplayName() {
    return S.get("counterComponent");
  }

  @Override
  public String toString() {
    return "Counter:" + getBounds();
  }
}
