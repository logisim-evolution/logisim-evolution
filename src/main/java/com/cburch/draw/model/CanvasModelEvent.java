/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.draw.model;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.EventObject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import lombok.Getter;
import lombok.val;

public class CanvasModelEvent extends EventObject {
  public static final int ACTION_ADDED = 0;
  public static final int ACTION_REMOVED = 1;
  public static final int ACTION_TRANSLATED = 2;
  public static final int ACTION_REORDERED = 3;
  public static final int ACTION_HANDLE_MOVED = 4;
  public static final int ACTION_HANDLE_INSERTED = 5;
  public static final int ACTION_HANDLE_DELETED = 6;
  public static final int ACTION_ATTRIBUTES_CHANGED = 7;
  public static final int ACTION_TEXT_CHANGED = 8;
  private static final long serialVersionUID = 1L;
  @Getter private final int action;
  private Collection<? extends CanvasObject> affected;
  @Getter private int deltaX;
  @Getter private int deltaY;
  @Getter private Map<AttributeMapKey, Object> oldValues;
  @Getter private Map<AttributeMapKey, Object> newValues;
  @Getter private Collection<ReorderRequest> reorderRequests;
  private Handle handle;
  @Getter private HandleGesture handleGesture;
  @Getter private String oldText;
  @Getter private String newText;

  // the boolean parameter is just because the compiler insists upon it to
  // avoid an erasure conflict with the first constructor
  private CanvasModelEvent(boolean dummy, CanvasModel source, int action, Collection<ReorderRequest> requests) {
    this(source, action, Collections.emptySet());

    val affected = new ArrayList<CanvasObject>(requests.size());
    for (val req : requests) {
      affected.add(req.getObject());
    }
    this.affected = affected;
    this.reorderRequests = Collections.unmodifiableCollection(requests);
  }

  private CanvasModelEvent(CanvasModel source, int action, Collection<? extends CanvasObject> affected) {
    super(source);

    this.action = action;
    this.affected = affected;
    this.deltaX = 0;
    this.deltaY = 0;
    this.oldValues = null;
    this.newValues = null;
    this.reorderRequests = null;
    this.handle = null;
    this.handleGesture = null;
    this.oldText = null;
    this.newText = null;
  }

  private CanvasModelEvent(CanvasModel source, int action, Collection<? extends CanvasObject> affected, int dx, int dy) {
    this(source, action, affected);
    this.deltaX = dx;
    this.deltaY = dy;
  }

  private CanvasModelEvent(CanvasModel source, int action, Collection<? extends CanvasObject> affected, String oldText, String newText) {
    this(source, action, affected);
    this.oldText = oldText;
    this.newText = newText;
  }

  private CanvasModelEvent(CanvasModel source, int action, Handle handle) {
    this(source, action, Collections.singleton(handle.getObject()));
    this.handle = handle;
  }

  private CanvasModelEvent(CanvasModel source, int action, HandleGesture handleGesture) {
    this(source, action, handleGesture.getHandle());
    this.handleGesture = handleGesture;
  }

  private CanvasModelEvent(CanvasModel source, int action, Map<AttributeMapKey, Object> oldValues, Map<AttributeMapKey, Object> newValues) {
    this(source, action, Collections.emptySet());

    val affected = new HashSet<CanvasObject>(newValues.size());
    for (val key : newValues.keySet()) {
      affected.add(key.getObject());
    }
    this.affected = affected;

    val oldValuesCopy = new HashMap<AttributeMapKey, Object>(oldValues);
    val newValuesCopy = new HashMap<AttributeMapKey, Object>(newValues);

    this.oldValues = Collections.unmodifiableMap(oldValuesCopy);
    this.newValues = Collections.unmodifiableMap(newValuesCopy);
  }

  public static CanvasModelEvent forAdd(CanvasModel source, Collection<? extends CanvasObject> affected) {
    return new CanvasModelEvent(source, ACTION_ADDED, affected);
  }

  public static CanvasModelEvent forChangeAttributes(CanvasModel source, Map<AttributeMapKey, Object> oldValues, Map<AttributeMapKey, Object> newValues) {
    return new CanvasModelEvent(source, ACTION_ATTRIBUTES_CHANGED, oldValues, newValues);
  }

  public static CanvasModelEvent forChangeText(CanvasModel source, CanvasObject obj, String oldText, String newText) {
    return new CanvasModelEvent(source, ACTION_TEXT_CHANGED, Collections.singleton(obj), oldText, newText);
  }

  public static CanvasModelEvent forDeleteHandle(CanvasModel source, Handle handle) {
    return new CanvasModelEvent(source, ACTION_HANDLE_DELETED, handle);
  }

  public static CanvasModelEvent forInsertHandle(CanvasModel source, Handle desired) {
    return new CanvasModelEvent(source, ACTION_HANDLE_INSERTED, desired);
  }

  public static CanvasModelEvent forMoveHandle(CanvasModel source, HandleGesture gesture) {
    return new CanvasModelEvent(source, ACTION_HANDLE_MOVED, gesture);
  }

  public static CanvasModelEvent forRemove(CanvasModel source, Collection<? extends CanvasObject> affected) {
    return new CanvasModelEvent(source, ACTION_REMOVED, affected);
  }

  public static CanvasModelEvent forReorder(CanvasModel source, Collection<ReorderRequest> requests) {
    return new CanvasModelEvent(true, source, ACTION_REORDERED, requests);
  }

  public static CanvasModelEvent forTranslate(CanvasModel source, Collection<? extends CanvasObject> affected, int dx, int dy) {
    return new CanvasModelEvent(source, ACTION_TRANSLATED, affected, 0, 0);
  }

  public Collection<? extends CanvasObject> getAffected() {
    if (affected == null) {
      val newVals = newValues;
      if (newVals != null) {
        val keys = new HashSet<CanvasObject>();
        for (val key : newVals.keySet()) {
          keys.add(key.getObject());
        }
        affected = Collections.unmodifiableCollection(keys);
      }
    }
    return affected;
  }
}
