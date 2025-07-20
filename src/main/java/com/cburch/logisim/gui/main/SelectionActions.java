/*
 * Logisim-evolution - digital logic design tool and simulator
 * Copyright by the Logisim-evolution developers
 *
 * https://github.com/logisim-evolution/
 *
 * This is free software released under GNU GPLv3 license
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.CircuitTransactionResult;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.JoinedAction;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class SelectionActions {

  private SelectionActions() {}

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  // anchors all floating elements, keeping elements in selection
  public static Action anchorAll(Selection sel) {
    int numAnchor = sel.getFloatingComponents().size();
    if (numAnchor == 0) {
      return null;
    } else {
      return new Anchor(sel, numAnchor);
    }
  }

  public static Action clear(Selection sel) {
    return new Delete(sel);
  }

  public static Action copy(Selection sel) {
    return new Copy(sel);
  }

  public static Action cut(Selection sel) {
    return new Cut(sel);
  }

  // clears the selection, anchoring all floating elements in selection
  public static Action drop(Selection sel, Collection<Component> comps) {
    final var floating = new HashSet<>(sel.getFloatingComponents());
    final var anchored = new HashSet<>(sel.getAnchoredComponents());
    final var toDrop = new ArrayList<Component>();
    final var toIgnore = new ArrayList<Component>();
    for (final var comp : comps) {
      if (floating.contains(comp)) {
        toDrop.add(comp);
      } else if (anchored.contains(comp)) {
        toDrop.add(comp);
        toIgnore.add(comp);
      }
    }
    if (toDrop.size() == toIgnore.size()) {
      for (final var comp : toIgnore) {
        sel.remove(null, comp);
      }
      return null;
    } else {
      final var numDrop = toDrop.size() - toIgnore.size();
      return new Drop(sel, toDrop, numDrop);
    }
  }

  public static Action dropAll(Selection sel) {
    return drop(sel, sel.getComponents());
  }

  public static Action duplicate(Selection sel) {
    return new Duplicate(sel);
  }

  private static ComponentFactory findComponentFactory(
      ComponentFactory factory, ArrayList<Library> libs, boolean acceptNameMatch) {
    final var name = factory.getName();
    for (final var lib : libs) {
      for (final var tool : lib.getTools()) {
        if (tool instanceof AddTool addTool) {
          if (name.equals(addTool.getName())) {
            final var fact = addTool.getFactory(true);
            if (acceptNameMatch || (fact == factory)) {
              return fact;
            } else if (fact.getClass() == factory.getClass()
                && !(fact instanceof SubcircuitFactory)
                && !(fact instanceof VhdlEntity)) {
              return fact;
            }
          }
        }
      }
    }
    return null;
  }

  private static HashMap<Component, Component> getReplacementMap(Project proj) {
    final var replMap = new HashMap<Component, Component>();

    final var file = proj.getLogisimFile();
    final var libs = new ArrayList<Library>();
    libs.add(file);
    libs.addAll(file.getLibraries());

    ArrayList<String> dropped = null;
    final var clip = Clipboard.get();
    final var comps = clip.getComponents();
    final var factoryReplacements = new HashMap<ComponentFactory, ComponentFactory>();
    for (final var comp : comps) {
      if (comp instanceof Wire) continue;

      final var compFactory = comp.getFactory();

      if (compFactory == Text.FACTORY) continue;

      var copyFactory = findComponentFactory(compFactory, libs, false);
      if (factoryReplacements.containsKey(compFactory)) {
        copyFactory = factoryReplacements.get(compFactory);
      } else if (copyFactory == null) {
        final var candidate = findComponentFactory(compFactory, libs, true);
        if (candidate == null) {
          if (dropped == null) {
            dropped = new ArrayList<>();
          }
          dropped.add(compFactory.getDisplayName());
        } else {
          final var msg = S.get("pasteCloneQuery", compFactory.getName());
          Object[] opts = {
            S.get("pasteCloneReplace"), S.get("pasteCloneIgnore"), S.get("pasteCloneCancel")
          };
          int select =
              OptionPane.showOptionDialog(
                  proj.getFrame(),
                  msg,
                  S.get("pasteCloneTitle"),
                  0,
                  OptionPane.QUESTION_MESSAGE,
                  null,
                  opts,
                  opts[0]);
          if (select == 0) {
            copyFactory = candidate;
          } else if (select == 1) {
            copyFactory = null;
          } else {
            return null;
          }
          factoryReplacements.put(compFactory, copyFactory);
        }
      }

      if (copyFactory == null) {
        replMap.put(comp, null);
      } else if (copyFactory != compFactory) {
        final var copyLoc = comp.getLocation();
        final var copyAttrs = (AttributeSet) comp.getAttributeSet().clone();
        final var copy = copyFactory.createComponent(copyLoc, copyAttrs);
        replMap.put(comp, copy);
      }
    }

    if (dropped != null) {
      Collections.sort(dropped);
      final var droppedStr = new StringBuilder();
      droppedStr.append(S.get("pasteDropMessage"));
      var curName = dropped.get(0);
      int curCount = 1;
      int lines = 1;
      for (int i = 1; i <= dropped.size(); i++) {
        var nextName = i == dropped.size() ? "" : dropped.get(i);
        if (nextName.equals(curName)) {
          curCount++;
        } else {
          lines++;
          droppedStr.append("\n  ");
          droppedStr.append(curName);
          if (curCount > 1) {
            droppedStr.append(" \u00d7 ").append(curCount);
          }

          curName = nextName;
          curCount = 1;
        }
      }

      lines = Math.max(3, Math.min(7, lines));
      final var area = new JTextArea(lines, 60);
      area.setEditable(false);
      area.setText(droppedStr.toString());
      area.setCaretPosition(0);
      final var areaPane = new JScrollPane(area);
      OptionPane.showMessageDialog(
          proj.getFrame(), areaPane, S.get("pasteDropTitle"), OptionPane.WARNING_MESSAGE);
    }

    return replMap;
  }

  public static Action pasteMaybe(Project proj, Selection sel) {
    final var replacements = getReplacementMap(proj);
    return new Paste(sel, replacements);
  }

  public static Action translate(Selection sel, int dx, int dy, ReplacementMap repl) {
    return new Translate(sel, dx, dy, repl);
  }

  private abstract static class SelectedComponentsAction extends Action {
    public CircuitTransaction xnForward;
    public CircuitTransaction xnReverse;
    private boolean hasDoneFirstTime = false;

    @Override
    public void doIt(Project proj) {
      if (hasDoneFirstTime) {
        this.redo(proj);
      } else {
        this.doItFirstTime(proj);
        hasDoneFirstTime = true;
      }
    }

    public abstract void doItFirstTime(Project proj);

    @Override
    public void undo(Project proj) {
      if (xnReverse != null) {
        xnReverse.execute();
      }
    }

    public void redo(Project proj) {
      if (xnForward != null) {
        xnForward.execute();
      }
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Anchor extends SelectedComponentsAction {
    private final Selection sel;
    private final int numAnchor;
    private final SelectionSave before;

    Anchor(Selection sel, int numAnchor) {
      this.sel = sel;
      this.before = SelectionSave.create(sel);
      this.numAnchor = numAnchor;
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      sel.dropAll(xn);
      xnForward = xn;
      final var result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numAnchor == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      final var last = (other instanceof JoinedAction)
          ? ((JoinedAction) other).getLastAction()
          : other;

      SelectionSave otherAfter = null;
      if (last instanceof Paste paste) {
        otherAfter = paste.after;
      } else if (last instanceof Duplicate dupe) {
        otherAfter = dupe.after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }
  }

  private static class Copy extends SelectedComponentsAction {
    private final Selection sel;
    private Clipboard oldClip;
    private Clipboard newClip;

    Copy(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doItFirstTime(Project proj) {
      oldClip = Clipboard.get();
      Clipboard.set(sel, sel.getAttributeSet());
      newClip = Clipboard.get();
    }

    @Override
    public String getName() {
      return S.get("copySelectionAction");
    }

    @Override
    public boolean isModification() {
      return false;
    }

    @Override
    public void undo(Project proj) {
      Clipboard.set(oldClip);
    }

    @Override
    public void redo(Project proj) {
      Clipboard.set(newClip);
    }
  }

  private static class Cut extends SelectedComponentsAction {
    private final Selection sel;
    private final Action second;
    private Clipboard oldClip;
    private Clipboard newClip;

    Cut(Selection sel) {
      this.sel = sel;
      second = new Delete(sel);
    }

    @Override
    public void doItFirstTime(Project proj) {
      oldClip = Clipboard.get();
      Clipboard.set(sel, sel.getAttributeSet());
      newClip = Clipboard.get();
      second.doIt(proj);
    }

    @Override
    public String getName() {
      return S.get("cutSelectionAction");
    }

    @Override
    public void undo(Project proj) {
      second.undo(proj);
      Clipboard.set(oldClip);
    }

    @Override
    public void redo(Project proj) {
      Clipboard.set(newClip);
      second.doIt(proj);
    }
  }

  private static class Delete extends SelectedComponentsAction {
    private final Selection sel;

    Delete(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      sel.deleteAllHelper(xn);
      xnForward = xn;
      final var result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("deleteSelectionAction");
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Drop extends SelectedComponentsAction {
    private final Selection sel;
    private final Component[] drops;
    private final int numDrops;
    private final SelectionSave before;

    Drop(Selection sel, Collection<Component> toDrop, int numDrops) {
      this.sel = sel;
      this.drops = new Component[toDrop.size()];
      toDrop.toArray(this.drops);
      this.numDrops = numDrops;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      for (Component comp : drops) {
        sel.remove(xn, comp);
      }
      xnForward = xn;
      final var result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numDrops == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      final var last = (other instanceof JoinedAction action)
          ? action.getLastAction()
          : other;

      SelectionSave otherAfter = null;

      if (last instanceof Paste paste) {
        otherAfter = paste.after;
      } else if (last instanceof Duplicate dupe) {
        otherAfter = dupe.after;
      }

      return otherAfter != null && otherAfter.equals(this.before);
    }
  }

  private static class Duplicate extends SelectedComponentsAction {
    private final Selection sel;
    private SelectionSave after;

    Duplicate(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      sel.duplicateHelper(xn);
      xnForward = xn;
      final var result = xn.execute();
      xnReverse = result.getReverseTransaction();
      after = SelectionSave.create(sel);
    }

    @Override
    public String getName() {
      return S.get("duplicateSelectionAction");
    }
  }

  private static class Paste extends SelectedComponentsAction {
    private final Selection sel;
    private final HashMap<Component, Component> componentReplacements;
    private SelectionSave after;

    Paste(Selection sel, HashMap<Component, Component> replacements) {
      this.sel = sel;
      this.componentReplacements = replacements;
    }

    private Collection<Component> computeAdditions(Collection<Component> comps) {
      final var replMap = componentReplacements;
      final var toAdd = new ArrayList<Component>(comps.size());
      for (final var comp : comps) {
        if (replMap.containsKey(comp)) {
          final var repl = replMap.get(comp);
          if (repl != null) {
            toAdd.add(repl);
          }
        } else {
          toAdd.add(comp);
        }
      }
      return toAdd;
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var clip = Clipboard.get();
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      final var comps = clip.getComponents();
      final var toAdd = computeAdditions(comps);

      final var canvas = proj.getFrame().getCanvas();
      final var circ = canvas.getCircuit();

      /* Check if instantiated circuits are one of the parent circuits */
      for (final var c : comps) {
        final var factory = c.getFactory();
        if (factory instanceof SubcircuitFactory circFact) {
          final var depends = canvas.getProject().getDependencies();
          if (!depends.canAdd(circ, circFact.getSubcircuit())) {
            canvas.setErrorMessage(S.getter("circularError"));
            return;
          }
        }
      }

      if (!toAdd.isEmpty()) {
        sel.pasteHelper(xn, toAdd);
        xnForward = xn;
        final var result = xn.execute();
        xnReverse = result.getReverseTransaction();
        after = SelectionSave.create(sel);
      } else {
        xnForward = null;
        xnReverse = null;
      }
    }

    @Override
    public String getName() {
      return S.get("pasteClipboardAction");
    }
  }

  private static class Translate extends SelectedComponentsAction {
    private final Selection sel;
    private final int dx;
    private final int dy;
    private final ReplacementMap replacements;
    private final SelectionSave before;

    Translate(Selection sel, int dx, int dy, ReplacementMap replacements) {
      this.sel = sel;
      this.dx = dx;
      this.dy = dy;
      this.replacements = replacements;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doItFirstTime(Project proj) {
      final var circuit = proj.getCurrentCircuit();
      final var xn = new CircuitMutation(circuit);
      sel.translateHelper(xn, dx, dy);
      if (replacements != null) {
        xn.replace(replacements);
      }
      xnForward = xn;
      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("moveSelectionAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      final var last = (other instanceof JoinedAction action)
          ? action.getLastAction()
          : other;

      SelectionSave otherAfter = null;
      if (last instanceof Paste paste) {
        otherAfter = paste.after;
      } else if (last instanceof Duplicate dupe) {
        otherAfter = dupe.after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }
  }
}
