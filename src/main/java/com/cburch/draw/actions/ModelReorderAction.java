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
import com.cburch.draw.model.ReorderRequest;
import com.cburch.draw.util.ZOrder;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import lombok.Getter;
import lombok.val;

public class ModelReorderAction extends ModelAction {
  private final List<ReorderRequest> requests;
  @Getter private final List<CanvasObject> objects;
  private final int type;

  public ModelReorderAction(CanvasModel model, List<ReorderRequest> reqs) {
    super(model);
    requests = new ArrayList<>(reqs);
    objects = new ArrayList<>(reqs.size());
    for (ReorderRequest r : reqs) {
      objects.add(r.getObject());
    }
    int typeIndex = 0; // 0 = mixed/unknown, -1 = to greater index, 1 = to
    // smaller index
    for (val r : reqs) {
      val from = r.getFromIndex();
      val to = r.getToIndex();
      var thisType = Integer.compare(to, from);
      if (typeIndex == 2) {
        typeIndex = thisType;
      } else if (typeIndex != thisType) {
        typeIndex = 0;
        break;
      }
    }
    type = typeIndex;
  }

  public static ModelReorderAction createLower(CanvasModel model, Collection<? extends CanvasObject> objects) {
    val reqs = new ArrayList<ReorderRequest>();
    val zMap = ZOrder.getZIndex(objects, model);
    for (val entry : zMap.entrySet()) {
      val obj = entry.getKey();
      val from = entry.getValue();
      val above = ZOrder.getObjectBelow(obj, model, objects);
      if (above != null) {
        var to = ZOrder.getZIndex(above, model);
        if (objects.contains(above)) to++;
        reqs.add(new ReorderRequest(obj, from, to));
      }
    }
    if (reqs.isEmpty()) return null;

    reqs.sort(ReorderRequest.ASCENDING_FROM);
    repairRequests(reqs);
    return new ModelReorderAction(model, reqs);
  }

  public static ModelReorderAction createLowerBottom(CanvasModel model, Collection<? extends CanvasObject> objects) {
    val reqs = new ArrayList<ReorderRequest>();
    val zMap = ZOrder.getZIndex(objects, model);
    var to = 0;
    for (val entry : zMap.entrySet()) {
      val obj = entry.getKey();
      val from = entry.getValue();
      reqs.add(new ReorderRequest(obj, from, to));
    }
    if (reqs.isEmpty()) return null;
    reqs.sort(ReorderRequest.ASCENDING_FROM);
    repairRequests(reqs);
    return new ModelReorderAction(model, reqs);
  }

  public static ModelReorderAction createRaise(CanvasModel model, Collection<? extends CanvasObject> objects) {
    val reqs = new ArrayList<ReorderRequest>();
    val zmap = ZOrder.getZIndex(objects, model);
    for (val entry : zmap.entrySet()) {
      val obj = entry.getKey();
      val from = entry.getValue();
      val above = ZOrder.getObjectAbove(obj, model, objects);
      if (above != null) {
        var to = ZOrder.getZIndex(above, model);
        if (objects.contains(above)) to--;
        reqs.add(new ReorderRequest(obj, from, to));
      }
    }
    if (reqs.isEmpty()) return null;
    reqs.sort(ReorderRequest.DESCENDING_FROM);
    repairRequests(reqs);
    return new ModelReorderAction(model, reqs);
  }

  public static ModelReorderAction createRaiseTop(CanvasModel model, Collection<? extends CanvasObject> objects) {
    val reqs = new ArrayList<ReorderRequest>();
    val zmap = ZOrder.getZIndex(objects, model);
    val to = model.getObjectsFromBottom().size() - 1;
    for (val entry : zmap.entrySet()) {
      val obj = entry.getKey();
      val from = entry.getValue();
      reqs.add(new ReorderRequest(obj, from, to));
    }
    if (reqs.isEmpty()) return null;
    reqs.sort(ReorderRequest.ASCENDING_FROM);
    repairRequests(reqs);
    return new ModelReorderAction(model, reqs);
  }

  private static void repairRequests(List<ReorderRequest> reqs) {
    for (int i = 0, n = reqs.size(); i < n; i++) {
      val req = reqs.get(i);
      var from = req.getFromIndex();
      var to = req.getToIndex();
      for (var j = 0; j < i; j++) {
        val prev = reqs.get(j);
        val prevFrom = prev.getFromIndex();
        val prevTo = prev.getToIndex();
        if (prevFrom <= from && from < prevTo) {
          from--;
        } else if (prevTo <= from && from < prevFrom) {
          from++;
        }
        if (prevFrom <= to && to < prevTo) {
          to--;
        } else if (prevTo <= to && to < prevFrom) {
          to++;
        }
      }
      if (from != req.getFromIndex() || to != req.getToIndex()) {
        reqs.set(i, new ReorderRequest(req.getObject(), from, to));
      }
    }
    for (int i = reqs.size() - 1; i >= 0; i--) {
      val req = reqs.get(i);
      if (req.getFromIndex() == req.getToIndex()) reqs.remove(i);
    }
  }

  @Override
  void doSub(CanvasModel model) {
    model.reorderObjects(requests);
  }

  @Override
  public String getName() {
    if (type < 0) {
      return S.get("actionRaise", getShapesName(objects));
    } else if (type > 0) {
      return S.get("actionLower", getShapesName(objects));
    }
    return S.get("actionReorder", getShapesName(objects));
  }

  public List<ReorderRequest> getReorderRequests() {
    return Collections.unmodifiableList(requests);
  }

  @Override
  void undoSub(CanvasModel model) {
    val inv = new ArrayList<ReorderRequest>(requests.size());
    for (var i = requests.size() - 1; i >= 0; i--) {
      val r = requests.get(i);
      inv.add(new ReorderRequest(r.getObject(), r.getToIndex(), r.getFromIndex()));
    }
    model.reorderObjects(inv);
  }
}
