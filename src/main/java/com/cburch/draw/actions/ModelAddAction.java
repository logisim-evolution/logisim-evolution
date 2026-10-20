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
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ModelAddAction extends ModelAction {
  private final List<CanvasObject> added;
  private final int addIndex;

  public ModelAddAction(CanvasModel model, CanvasObject added) {
    this(model, Collections.singleton(added));
  }

  public ModelAddAction(CanvasModel model, Collection<CanvasObject> added) {
    super(model);
    this.added = new ArrayList<>(added);
    this.addIndex = model.getObjectsFromBottom().size();
  }

  public ModelAddAction(CanvasModel model, Collection<CanvasObject> added, int index) {
    super(model);
    this.added = new ArrayList<>(added);
    this.addIndex = index;
  }

  @Override
  void doSub(CanvasModel model) {
    model.addObjects(addIndex, added);
  }

  public int getDestinationIndex() {
    return addIndex;
  }

  @Override
  public String getName() {
    return S.get("actionAdd", getShapesName(added));
  }

  @Override
  public Collection<CanvasObject> getObjects() {
    return Collections.unmodifiableList(added);
  }

  @Override
  void undoSub(CanvasModel model) {
    model.removeObjects(added);
  }
}
