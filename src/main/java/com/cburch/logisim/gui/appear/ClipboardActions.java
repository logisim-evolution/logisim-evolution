/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.draw.util.ZOrder;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.data.Direction;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import java.util.ArrayList;
import java.util.Map;

public class ClipboardActions extends Action {

  private final boolean remove;
  private final AppearanceCanvas canvas;
  private final CanvasModel canvasModel;
  private final Map<CanvasObject, Integer> affected;
  private final ClipboardContents newClipboard;
  private ClipboardContents oldClipboard;

  private ClipboardActions(boolean remove, AppearanceCanvas canvas) {
    this.remove = remove;
    this.canvas = canvas;
    this.canvasModel = canvas.getModel();

    final var contents = new ArrayList<CanvasObject>();
    Direction anchorFacing = null;
    Location anchorLocation = null;
    final var aff = new ArrayList<CanvasObject>();
    for (final var obj : canvas.getSelection().getSelected()) {
      if (obj.canRemove()) {
        aff.add(obj);
        contents.add(obj.clone());
      } else if (obj instanceof AppearanceAnchor anchor) {
        anchorFacing = anchor.getFacingDirection();
        anchorLocation = anchor.getLocation();
      }
    }
    contents.trimToSize();
    affected = ZOrder.getZIndex(aff, canvasModel);
    newClipboard = new ClipboardContents(contents, anchorLocation, anchorFacing);
  }

  public static Action copy(AppearanceCanvas canvas) {
    return new ClipboardActions(false, canvas);
  }

  public static Action cut(AppearanceCanvas canvas) {
    return new ClipboardActions(true, canvas);
  }

  @Override
  public void doIt(Project proj) {
    oldClipboard = Clipboard.get();
    Clipboard.set(newClipboard);
    if (remove) {
      canvasModel.removeObjects(affected.keySet());
    }
  }

  @Override
  public String getName() {
    return remove ? S.get("cutSelectionAction") : S.get("copySelectionAction");
  }

  @Override
  public void undo(Project proj) {
    if (remove) {
      canvasModel.addObjects(affected);
      canvas.getSelection().clearSelected();
      canvas.getSelection().setSelected(affected.keySet(), true);
    }
    Clipboard.set(oldClipboard);
  }
}
