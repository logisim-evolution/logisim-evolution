/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.actions;

import static com.cburch.draw.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.shapes.Text;
import java.util.Collection;
import java.util.Collections;

public class ModelEditTextAction extends ModelAction {
  private final Text text;
  private final String oldValue;
  private final String newValue;

  public ModelEditTextAction(CanvasModel model, Text text, String newValue) {
    super(model);
    this.text = text;
    this.oldValue = text.getText();
    this.newValue = newValue;
  }

  @Override
  void doSub(CanvasModel model) {
    model.setText(text, newValue);
  }

  @Override
  public String getName() {
    return S.get("actionEditText");
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.singleton(text);
  }

  @Override
  void undoSub(CanvasModel model) {
    model.setText(text, oldValue);
  }
}
