/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.util;

import com.cburch.logisim.prefs.AppPreferences;

import java.util.Comparator;
import java.util.TreeMap;

/**
 * A linked-list priority queue of queues implementation, using values that extend type QNode.
 * This supports (approximately) a subset of the java.util.PriorityQueue API, but only enough
 * to support Propagator. Objects in the queue must be subclasses of QNode.
 */
public class QueueOfQueues<T extends QNode> implements QNodeQueue<T> {
  /*
   * There are two keys for priority in QNode: timeKey and serialNumber. This implementation
   * uses a sorted linked list map or a treeMap for the timeKey, chosen at time of construction.
   * In either case, the first timeKey is held outside the list because that makes all peek and
   * remove operations (except the one that empties the queue for this timeKey) work in constant
   * time. Furthermore, usually much more than 2/3 of the adds also hit on the first timeKey
   * since many components have a delay of 1, giving them a constant time lookup as well.
   * The linked list is searched from current time forward since even after the first entry the
   * desired timeKey is biased toward the beginning. The result is that the average number of
   * iterations of the lookup loop is often less than one per lookup. Each TimeNode contains the
   * head and tail of a linked queue of all QNodes with identical timeKey. Since the serial
   * numbers actually arrive here in order, a regular (non-priority) queue works. We don't even
   * look at the serialNumber.
   *
   * Invariants of the class:
   * 1. firstTimeNode is null if and only if size == 0.
   * 2. If a TimeNode is being used (firstTimeNode or in timeNodeMap), the TimeNode's queue is not empty.
   */
  private TimeNode firstTimeNode = null; // the smallest TimeNode
  private int size = 0; // The total number of T objects held in all TimeNodes
  private final TimeNodeMap timeNodeMap; // holds all TimeNodes but the smallest
  private TimeNode unusedTimeNodes = null; // Linked list holding currently unused TimeNodes

  public QueueOfQueues(String queueType) {
    timeNodeMap = AppPreferences.SIM_QUEUE_LIST_OF_QUEUES.equals(queueType) ? new LinkedNodeMap() : new TreeNodeMap();
  }

  @Override
  public boolean add(T node) {
    findTimeNode(node.timeKey).add(node);
    size++;
    return true;
  }

  @Override
  public void clear() {
    firstTimeNode = null;
    timeNodeMap.clear();
    size = 0;
  }

  @Override
  public boolean isEmpty() {
    return size == 0;
  }

  @Override
  public T peek() {
    return firstTimeNode == null ?  null : firstTimeNode.peek();
  }

  @Override
  public T remove() {
    if (firstTimeNode == null) return null;
    T ret = firstTimeNode.remove();
    size--;
    if (firstTimeNode.isEmpty()) {
      final var oldFirst = firstTimeNode;
      firstTimeNode = timeNodeMap.removeFirst(); // This will be null if there are no more.
      recycleTimeNode(oldFirst);
    }
    return ret;
  }

  @Override
  public int size() {
    return size;
  }

  /**
   * @return The TimeNode with the given timeKey, creating it if necessary.
   */
  TimeNode findTimeNode(int timeKey) {
    if (firstTimeNode == null) {
      firstTimeNode = getNewTimeNode(timeKey);
      return firstTimeNode;
    }
    if (firstTimeNode.timeKey == timeKey) {
      return firstTimeNode;
    }
    if (timeKey - firstTimeNode.timeKey < 0) {
      final var oldFirst = firstTimeNode;
      firstTimeNode = getNewTimeNode(timeKey);
      timeNodeMap.addFirst(oldFirst);
      return firstTimeNode;
    }
    return timeNodeMap.addIfNeeded(timeKey);
  }

  /**
   * @return a TimeNode with its timeKey set, taken from unusedTimeNodes if there is one.
   */
  private TimeNode getNewTimeNode(int timeKey) {
    if (unusedTimeNodes == null) {
      return new TimeNode(timeKey);
    }
    final var node = unusedTimeNodes;
    unusedTimeNodes = unusedTimeNodes.next;
    node.timeKey = timeKey;
    node.next = null;
    return node;
  }

  /**
   * Places the node on the unusedTimeNodes list and clears its queue.
   */
  private void recycleTimeNode(TimeNode node) {
    node.head = node.tail = null;
    node.next = unusedTimeNodes;
    unusedTimeNodes = node;
  }

  private class TimeNode {
    public int timeKey; // the simulation time these events should be processed
    public TimeNode next; // Link to next TimeNode if TimeNode is in a list
    private T head, tail; // The linked queue of T for this timeKey

    TimeNode(int timeKey) {
      this.timeKey = timeKey;
    }

    /**
     * Add node to the linked queue for this TimeNode
     */
    void add(T node) {
      if (head == null) {
        head = tail = node;
      } else {
        tail.right = node;
        tail = node;
      }
    }

    /**
     * @return the first value in the linked queue for this TimeNode
     */
    T peek() {
      return head == null ? null : head;
    }

    /**
     * @return the first value in the linked queue for this TimeNode while removing it from the queue.
     */
    T remove() {
      if (head == null) return null;
      T ret = head;
      @SuppressWarnings("unchecked")
      T next = (T) head.right;
      head = next;
      return ret;
    }

    /**
     * @return if the linked queue for this TimeNode is empty.
     */
    boolean isEmpty() {
      return head == null;
    }
  }

  private abstract class TimeNodeMap {
    /**
     * Adds the node as the first node of the map.
     *
     * @Precondition: node.key is smaller than any current node in the map.
     */
    public abstract void addFirst(TimeNode node);

    /**
     * @return the node in the map with the given timeKey, creating one if none was there.
     */
    public abstract TimeNode addIfNeeded(int timeKey);

    /**
     * Clear the map.
     */
    public abstract void clear();

    /**
     * @return the first TimeNode in the map and also removes it.
     */
    public abstract TimeNode removeFirst();
  }

  private class TreeNodeMap extends TimeNodeMap {
    private final TreeMap<Integer, TimeNode> treeMap = new TreeMap<>((Comparator<Integer>) (ls, rs) -> ls - rs);

    @Override
    public void addFirst(TimeNode node) {
      treeMap.put(node.timeKey, node);
    }

    @Override
    public TimeNode addIfNeeded(int timeKey) {
      return treeMap.computeIfAbsent(timeKey, QueueOfQueues.this::getNewTimeNode);
    }

    @Override
    public void clear() {
      treeMap.clear();
    }

    @Override
    public TimeNode removeFirst() {
      if (treeMap.isEmpty()) return null;
      final var entry = treeMap.firstEntry();
      treeMap.remove(entry.getKey());
      return entry.getValue();
    }
  }

  private class LinkedNodeMap extends TimeNodeMap {
    TimeNode head = null;

    @Override
    public void addFirst(TimeNode node) {
      node.next = head;
      head = node;
    }

    @Override
    public TimeNode addIfNeeded(int timeKey) {
      if (head == null) {
        head = getNewTimeNode(timeKey);
        return head;
      }
      var node = head;
      TimeNode previous = null;
      while (node != null && (node.timeKey - timeKey < 0)) {
        previous = node;
        node = node.next;
      }
      if (node != null && node.timeKey == timeKey) {
        return node;
      }
      final var newNode = getNewTimeNode(timeKey);
      newNode.next = node;
      if (previous == null) {
        head = newNode;
      } else {
        previous.next = newNode;
      }
      return newNode;
    }

    @Override
    public void clear() {
      head = null;
    }

    @Override
    public TimeNode removeFirst() {
      if (head == null) {
        return null;
      }
      final var oldHead = head;
      head = head.next;
      oldHead.next = null;
      return oldHead;
    }
  }
}
