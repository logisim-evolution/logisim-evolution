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
import lombok.val;

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
    val floating = new HashSet<Component>(sel.getFloatingComponents());
    val anchored = new HashSet<Component>(sel.getAnchoredComponents());
    val toDrop = new ArrayList<Component>();
    val toIgnore = new ArrayList<Component>();
    for (val comp : comps) {
      if (floating.contains(comp)) {
        toDrop.add(comp);
      } else if (anchored.contains(comp)) {
        toDrop.add(comp);
        toIgnore.add(comp);
      }
    }
    if (toDrop.size() == toIgnore.size()) {
      for (val comp : toIgnore) {
        sel.remove(null, comp);
      }
      return null;
    } else {
      val numDrop = toDrop.size() - toIgnore.size();
      return new Drop(sel, toDrop, numDrop);
    }
  }

  public static Action dropAll(Selection sel) {
    return drop(sel, sel.getComponents());
  }

  public static Action duplicate(Selection sel) {
    return new Duplicate(sel);
  }

  private static ComponentFactory findComponentFactory(ComponentFactory factory, ArrayList<Library> libs, boolean acceptNameMatch) {
    val name = factory.getName();
    for (val lib : libs) {
      for (val tool : lib.getTools()) {
        if (tool instanceof AddTool) {
          val addTool = (AddTool) tool;
          if (name.equals(addTool.getName())) {
            val fact = addTool.getFactory(true);
            if (acceptNameMatch) {
              return fact;
            } else if (fact == factory) {
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
    val replMap = new HashMap<Component, Component>();
    val file = proj.getLogisimFile();
    val libs = new ArrayList<Library>();
    libs.add(file);
    libs.addAll(file.getLibraries());

    ArrayList<String> dropped = null;
    val clip = Clipboard.get();
    val comps = clip.getComponents();
    val factoryReplacements = new HashMap<ComponentFactory, ComponentFactory>();
    for (val comp : comps) {
      if (comp instanceof Wire) continue;
      val compFactory = comp.getFactory();
      if (compFactory == Text.FACTORY) continue;

      var copyFactory = findComponentFactory(compFactory, libs, false);
      if (factoryReplacements.containsKey(compFactory)) {
        copyFactory = factoryReplacements.get(compFactory);
      } else if (copyFactory == null) {
        val candidate = findComponentFactory(compFactory, libs, true);
        if (candidate == null) {
          if (dropped == null) {
            dropped = new ArrayList<>();
          }
          dropped.add(compFactory.getDisplayName());
        } else {
          val msg = S.get("pasteCloneQuery", compFactory.getName());
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
        val copyLoc = comp.getLocation();
        val copyAttrs = (AttributeSet) comp.getAttributeSet().clone();
        val copy = copyFactory.createComponent(copyLoc, copyAttrs);
        replMap.put(comp, copy);
      }
    }

    if (dropped != null) {
      Collections.sort(dropped);
      val droppedStr = new StringBuilder();
      droppedStr.append(S.get("pasteDropMessage"));
      var curName = dropped.get(0);
      var curCount = 1;
      var lines = 1;
      for (var i = 1; i <= dropped.size(); i++) {
        String nextName = i == dropped.size() ? "" : dropped.get(i);
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
      val area = new JTextArea(lines, 60);
      area.setEditable(false);
      area.setText(droppedStr.toString());
      area.setCaretPosition(0);
      val areaPane = new JScrollPane(area);
      OptionPane.showMessageDialog(proj.getFrame(), areaPane, S.get("pasteDropTitle"), OptionPane.WARNING_MESSAGE);
    }

    return replMap;
  }

  public static Action pasteMaybe(Project proj, Selection sel) {
    val replacements = getReplacementMap(proj);
    return new Paste(sel, replacements);
  }

  public static Action translate(Selection sel, int dx, int dy, ReplacementMap repl) {
    return new Translate(sel, dx, dy, repl);
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Anchor extends Action {
    private final Selection sel;
    private final int numAnchor;
    private final SelectionSave before;
    private CircuitTransaction xnReverse;

    Anchor(Selection sel, int numAnchor) {
      this.sel = sel;
      this.before = SelectionSave.create(sel);
      this.numAnchor = numAnchor;
    }

    @Override
    public void doIt(Project proj) {
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);
      sel.dropAll(xn);
      val result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numAnchor == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      val last = (other instanceof JoinedAction) ? ((JoinedAction) other).getLastAction()
                  : other;

      SelectionSave otherAfter = null;
      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  private static class Copy extends Action {
    private final Selection sel;
    private Clipboard oldClip;

    Copy(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      oldClip = Clipboard.get();
      Clipboard.set(sel, sel.getAttributeSet());
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
  }

  private static class Cut extends Action {
    private final Selection sel;
    private final Action second;
    private Clipboard oldClip;

    Cut(Selection sel) {
      this.sel = sel;
      second = new Delete(sel);
    }

    @Override
    public void doIt(Project proj) {
      oldClip = Clipboard.get();
      Clipboard.set(sel, sel.getAttributeSet());
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
  }

  private static class Delete extends Action {
    private final Selection sel;
    private CircuitTransaction xnReverse;

    Delete(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);
      sel.deleteAllHelper(xn);
      val result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("deleteSelectionAction");
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Drop extends Action {
    private final Selection sel;
    private final Component[] drops;
    private final int numDrops;
    private final SelectionSave before;
    private CircuitTransaction xnReverse;

    Drop(Selection sel, Collection<Component> toDrop, int numDrops) {
      this.sel = sel;
      this.drops = new Component[toDrop.size()];
      toDrop.toArray(this.drops);
      this.numDrops = numDrops;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doIt(Project proj) {
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);
      for (val comp : drops) sel.remove(xn, comp);
      xnReverse = xn.execute().getReverseTransaction();
    }

    @Override
    public String getName() {
      return numDrops == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      val last = (other instanceof JoinedAction) ? ((JoinedAction) other).getLastAction() : other;
      SelectionSave otherAfter = null;
      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }

      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  private static class Duplicate extends Action {
    private final Selection sel;
    private CircuitTransaction xnReverse;
    private SelectionSave after;

    Duplicate(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);
      sel.duplicateHelper(xn);
      val result = xn.execute();
      xnReverse = result.getReverseTransaction();
      after = SelectionSave.create(sel);
    }

    @Override
    public String getName() {
      return S.get("duplicateSelectionAction");
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }

  private static class Paste extends Action {
    private final Selection sel;
    private final HashMap<Component, Component> componentReplacements;
    private CircuitTransaction xnReverse;
    private SelectionSave after;

    Paste(Selection sel, HashMap<Component, Component> replacements) {
      this.sel = sel;
      this.componentReplacements = replacements;
    }

    private Collection<Component> computeAdditions(Collection<Component> comps) {
      val replMap = componentReplacements;
      val toAdd = new ArrayList<Component>(comps.size());
      for (val comp : comps) {
        if (replMap.containsKey(comp)) {
          val repl = replMap.get(comp);
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
    public void doIt(Project proj) {
      val clip = Clipboard.get();
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);
      val comps = clip.getComponents();
      val toAdd = computeAdditions(comps);

      val canvas = proj.getFrame().getCanvas();
      val circ = canvas.getCircuit();

      /* Check if instantiated circuits are one of the parent circuits */
      for (val c : comps) {
        val factory = c.getFactory();
        if (factory instanceof SubcircuitFactory) {
          val circFact = (SubcircuitFactory) factory;
          val depends = canvas.getProject().getDependencies();
          if (!depends.canAdd(circ, circFact.getSubcircuit())) {
            canvas.setErrorMessage(com.cburch.logisim.tools.Strings.S.getter("circularError"));
            return;
          }
        }
      }

      if (toAdd.size() > 0) {
        sel.pasteHelper(xn, toAdd);
        val result = xn.execute();
        xnReverse = result.getReverseTransaction();
        after = SelectionSave.create(sel);
      } else {
        xnReverse = null;
      }
    }

    @Override
    public String getName() {
      return S.get("pasteClipboardAction");
    }

    @Override
    public void undo(Project proj) {
      if (xnReverse != null) {
        xnReverse.execute();
      }
    }
  }

  private static class Translate extends Action {
    private final Selection sel;
    private final int dx;
    private final int dy;
    private final ReplacementMap replacements;
    private final SelectionSave before;
    private CircuitTransaction xnReverse;

    Translate(Selection sel, int dx, int dy, ReplacementMap replacements) {
      this.sel = sel;
      this.dx = dx;
      this.dy = dy;
      this.replacements = replacements;
      this.before = SelectionSave.create(sel);
    }

    @Override
    public void doIt(Project proj) {
      val circuit = proj.getCurrentCircuit();
      val xn = new CircuitMutation(circuit);

      sel.translateHelper(xn, dx, dy);
      if (replacements != null) {
        xn.replace(replacements);
      }

      val result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("moveSelectionAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      val last = (other instanceof JoinedAction) ? ((JoinedAction) other).getLastAction() : other;
      SelectionSave otherAfter = null;
      if (last instanceof Paste) {
        otherAfter = ((Paste) last).after;
      } else if (last instanceof Duplicate) {
        otherAfter = ((Duplicate) last).after;
      }
      return otherAfter != null && otherAfter.equals(this.before);
    }

    @Override
    public void undo(Project proj) {
      xnReverse.execute();
    }
  }
}
