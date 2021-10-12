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

import com.cburch.draw.model.AttributeMapKey;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.data.Attribute;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;

public class ModelChangeAttributeAction extends ModelAction {
  private final Map<AttributeMapKey, Object> oldValues;
  private final Map<AttributeMapKey, Object> newValues;
  private Attribute<?> attr;

  public ModelChangeAttributeAction(
      CanvasModel model,
      Map<AttributeMapKey, Object> oldValues,
      Map<AttributeMapKey, Object> newValues) {
    super(model);
    this.oldValues = oldValues;
    this.newValues = newValues;
  }

  @Override
  void doSub(CanvasModel model) {
    model.setAttributeValues(newValues);
  }

  @Override
  public String getName() {
    Attribute<?> a = attr;
    if (a == null) {
      var found = false;
      for (final var key : newValues.keySet()) {
        final var at = key.getAttribute();
        if (found) {
          if (!Objects.equals(a, at)) {
            a = null;
            break;
          }
        } else {
          found = true;
          a = at;
        }
      }
      attr = a;
    }
    return (a == null)
        ? S.get("actionChangeAttributes")
        : S.get("actionChangeAttribute", a.getDisplayName());
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    final var ret = new HashSet<CanvasObject>();
    for (final var key : newValues.keySet()) {
      ret.add(key.getObject());
    }
    return ret;
  }

  @Override
  void undoSub(CanvasModel model) {
    model.setAttributeValues(oldValues);
  }
}
