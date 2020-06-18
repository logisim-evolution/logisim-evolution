/**
 * This file is part of logisim-evolution.
 *
 * Logisim-evolution is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by the
 * Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * Logisim-evolution is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY
 * or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * for more details.
 *
 * You should have received a copy of the GNU General Public License along 
 * with logisim-evolution. If not, see <http://www.gnu.org/licenses/>.
 *
 * Original code by Carl Burch (http://www.cburch.com), 2011.
 * Subsequent modifications by:
 *   + College of the Holy Cross
 *     http://www.holycross.edu
 *   + Haute École Spécialisée Bernoise/Berner Fachhochschule
 *     http://www.bfh.ch
 *   + Haute École du paysage, d'ingénierie et d'architecture de Genève
 *     http://hepia.hesge.ch/
 *   + Haute École d'Ingénierie et de Gestion du Canton de Vaud
 *     http://www.heig-vd.ch/
 */

package com.cburch.logisim.gui.main;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.CircuitTransaction;
import com.cburch.logisim.circuit.CircuitTransactionResult;
import com.cburch.logisim.circuit.ReplacementMap;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.file.LogisimFile;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Dependencies;
import com.cburch.logisim.proj.JoinedAction;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlEntity;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

public class SelectionActions {
  /**
   * Code taken from Cornell's version of Logisim: http://www.cs.cornell.edu/courses/cs3410/2015sp/
   */
  private static class Anchor extends Action {

    private Selection sel;
    private int numAnchor;
    private SelectionSave before;
    private CircuitTransaction xnReverse;

    Anchor(Selection sel, int numAnchor) {
      this.sel = sel;
      this.before = SelectionSave.create(sel);
      this.numAnchor = numAnchor;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.dropAll(xn);
      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numAnchor == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction) last = ((JoinedAction) other).getLastAction();
      else last = other;

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
    private Selection sel;
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
    private Selection sel;
    private Clipboard oldClip;
    private Action second;

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
    private Selection sel;
    private CircuitTransaction xnReverse;

    Delete(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.deleteAllHelper(xn);
      CircuitTransactionResult result = xn.execute();
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

    private Selection sel;
    private Component[] drops;
    private int numDrops;
    private SelectionSave before;
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
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);

      for (Component comp : drops) {
        sel.remove(xn, comp);
      }

      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return numDrops == 1 ? S.get("dropComponentAction") : S.get("dropComponentsAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction) last = ((JoinedAction) other).getLastAction();
      else last = other;

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
    private Selection sel;
    private CircuitTransaction xnReverse;
    private SelectionSave after;

    Duplicate(Selection sel) {
      this.sel = sel;
    }

    @Override
    public void doIt(Project proj) {
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      sel.duplicateHelper(xn);

      CircuitTransactionResult result = xn.execute();
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
    private Selection sel;
    private CircuitTransaction xnReverse;
    private SelectionSave after;
    private HashMap<Component, Component> componentReplacements;

    Paste(Selection sel, HashMap<Component, Component> replacements) {
      this.sel = sel;
      this.componentReplacements = replacements;
    }

    private Collection<Component> computeAdditions(Collection<Component> comps) {
      HashMap<Component, Component> replMap = componentReplacements;
      ArrayList<Component> toAdd = new ArrayList<Component>(comps.size());
      for (Iterator<Component> it = comps.iterator(); it.hasNext(); ) {
        Component comp = it.next();
        if (replMap.containsKey(comp)) {
          Component repl = replMap.get(comp);
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
      Clipboard clip = Clipboard.get();
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);
      Collection<Component> comps = clip.getComponents();
      Collection<Component> toAdd = computeAdditions(comps);

      Canvas canvas = proj.getFrame().getCanvas();
      Circuit circ = canvas.getCircuit();

      /* Check if instantiated circuits are one of the parent circuits */
      for (Component c : comps) {
        ComponentFactory factory = c.getFactory();
        if (factory instanceof SubcircuitFactory) {
          SubcircuitFactory circFact = (SubcircuitFactory) factory;
          Dependencies depends = canvas.getProject().getDependencies();
          if (!depends.canAdd(circ, circFact.getSubcircuit())) {
            canvas.setErrorMessage(com.cburch.logisim.tools.Strings.S.getter("circularError"));
            return;
          }
        }
      }

      if (toAdd.size() > 0) {
        sel.pasteHelper(xn, toAdd);
        CircuitTransactionResult result = xn.execute();
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
    private Selection sel;
    private int dx;
    private int dy;
    private ReplacementMap replacements;
    private SelectionSave before;
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
      Circuit circuit = proj.getCurrentCircuit();
      CircuitMutation xn = new CircuitMutation(circuit);

      sel.translateHelper(xn, dx, dy);
      if (replacements != null) {
        xn.replace(replacements);
      }

      CircuitTransactionResult result = xn.execute();
      xnReverse = result.getReverseTransaction();
    }

    @Override
    public String getName() {
      return S.get("moveSelectionAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      Action last;
      if (other instanceof JoinedAction) last = ((JoinedAction) other).getLastAction();
      else last = other;

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
    HashSet<Component> floating = new HashSet<Component>(sel.getFloatingComponents());
    HashSet<Component> anchored = new HashSet<Component>(sel.getAnchoredComponents());
    ArrayList<Component> toDrop = new ArrayList<Component>();
    ArrayList<Component> toIgnore = new ArrayList<Component>();
    for (Component comp : comps) {
      if (floating.contains(comp)) {
        toDrop.add(comp);
      } else if (anchored.contains(comp)) {
        toDrop.add(comp);
        toIgnore.add(comp);
      }
    }
    if (toDrop.size() == toIgnore.size()) {
      for (Component comp : toIgnore) {
        sel.remove(null, comp);
      }
      return null;
    } else {
      int numDrop = toDrop.size() - toIgnore.size();
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
    String name = factory.getName();
    for (Library lib : libs) {
      for (Tool tool : lib.getTools()) {
        if (tool instanceof AddTool) {
          AddTool addTool = (AddTool) tool;
          if (name.equals(addTool.getName())) {
            ComponentFactory fact = addTool.getFactory(true);
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
    HashMap<Component, Component> replMap;
    replMap = new HashMap<Component, Component>();

    LogisimFile file = proj.getLogisimFile();
    ArrayList<Library> libs = new ArrayList<Library>();
    libs.add(file);
    libs.addAll(file.getLibraries());

    ArrayList<String> dropped = null;
    Clipboard clip = Clipboard.get();
    Collection<Component> comps = clip.getComponents();
    HashMap<ComponentFactory, ComponentFactory> factoryReplacements;
    factoryReplacements = new HashMap<ComponentFactory, ComponentFactory>();
    for (Component comp : comps) {
      if (comp instanceof Wire) continue;

      ComponentFactory compFactory = comp.getFactory();
      ComponentFactory copyFactory = findComponentFactory(compFactory, libs, false);
      if (factoryReplacements.containsKey(compFactory)) {
        copyFactory = factoryReplacements.get(compFactory);
      } else if (copyFactory == null) {
        ComponentFactory candidate = findComponentFactory(compFactory, libs, true);
        if (candidate == null) {
          if (dropped == null) {
            dropped = new ArrayList<String>();
          }
          dropped.add(compFactory.getDisplayName());
        } else {
          String msg = S.fmt("pasteCloneQuery", compFactory.getName());
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
        Location copyLoc = comp.getLocation();
        AttributeSet copyAttrs = (AttributeSet) comp.getAttributeSet().clone();
        Component copy = copyFactory.createComponent(copyLoc, copyAttrs);
        replMap.put(comp, copy);
      }
    }

    if (dropped != null) {
      Collections.sort(dropped);
      StringBuilder droppedStr = new StringBuilder();
      droppedStr.append(S.get("pasteDropMessage"));
      String curName = dropped.get(0);
      int curCount = 1;
      int lines = 1;
      for (int i = 1; i <= dropped.size(); i++) {
        String nextName = i == dropped.size() ? "" : dropped.get(i);
        if (nextName.equals(curName)) {
          curCount++;
        } else {
          lines++;
          droppedStr.append("\n  ");
          droppedStr.append(curName);
          if (curCount > 1) {
            droppedStr.append(" \u00d7 " + curCount);
          }

          curName = nextName;
          curCount = 1;
        }
      }

      lines = Math.max(3, Math.min(7, lines));
      JTextArea area = new JTextArea(lines, 60);
      area.setEditable(false);
      area.setText(droppedStr.toString());
      area.setCaretPosition(0);
      JScrollPane areaPane = new JScrollPane(area);
      OptionPane.showMessageDialog(
          proj.getFrame(), areaPane, S.get("pasteDropTitle"), OptionPane.WARNING_MESSAGE);
    }

    return replMap;
  }

  public static Action pasteMaybe(Project proj, Selection sel) {
    HashMap<Component, Component> replacements = getReplacementMap(proj);
    return new Paste(sel, replacements);
  }

  public static Action translate(Selection sel, int dx, int dy, ReplacementMap repl) {
    return new Translate(sel, dx, dy, repl);
  }

  private SelectionActions() {}
}
