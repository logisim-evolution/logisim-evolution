/*
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

package com.cburch.logisim.gui.appear;

import static com.cburch.logisim.gui.Strings.S;

import com.cburch.draw.actions.ModelAddAction;
import com.cburch.draw.actions.ModelRemoveAction;
import com.cburch.draw.model.CanvasModel;
import com.cburch.draw.model.CanvasObject;
import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.SubcircuitFactory;
import com.cburch.logisim.circuit.appear.AppearanceAnchor;
import com.cburch.logisim.circuit.appear.DynamicElement;
import com.cburch.logisim.circuit.appear.DynamicElementProvider;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.comp.ComponentFactory;
import com.cburch.logisim.data.Bounds;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.instance.InstanceComponent;
import com.cburch.logisim.instance.StdAttr;
import com.cburch.logisim.gui.appear.CheckBoxTree;
import org.scijava.swing.checkboxtree.CheckBoxNodeData;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

public class ShowStateDialog extends JDialog implements ActionListener {
  private static final long serialVersionUID = 1L;
  final JButton ok;
  final JButton cancel;
  RefTreeNode root;
  final CheckBoxTree tree;
  final AppearanceCanvas canvas;

  public ShowStateDialog(JFrame parent, AppearanceCanvas canvas) {
    super(parent, true);
    this.canvas = canvas;
    setDefaultCloseOperation(DISPOSE_ON_CLOSE);

    final var circuit = canvas.getCircuit();
    setTitle(S.get("showStateDialogTitle", circuit.getName()));

    root = enumerate(circuit, null);
    if (root == null) {
      root = new RefTreeNode(S.get("showStateDialogEmptyNode", circuit.getName()));
    }
    tree = new CheckBoxTree(root);
    tree.setCheckingPaths(getPaths());
    final var infoPane = new JScrollPane(tree);

    ok = new JButton(S.get("showStateDialogOkButton"));
    cancel = new JButton(S.get("showStateDialogCancelButton"));
    ok.addActionListener(this);
    cancel.addActionListener(this);
    JPanel buttonPanel = new JPanel();
    buttonPanel.add(ok);
    buttonPanel.add(cancel);

    Container contents = this.getContentPane();
    contents.setLayout(new BorderLayout());
    contents.add(infoPane, BorderLayout.CENTER);
    contents.add(buttonPanel, BorderLayout.PAGE_END);
    this.pack();

    Dimension pref = contents.getPreferredSize();
    if (pref.width > 750 || pref.height > 550 || pref.width < 200 || pref.height < 150) {
      if (pref.width > 750) pref.width = 750;
      else if (pref.width < 200) pref.width = 200;
      if (pref.height > 550) pref.height = 550;
      else if (pref.height < 200) pref.height = 200;
      this.setSize(pref);
    }
  }

  private static void pickPlacement(List<CanvasObject> avoid, DynamicElement shape, Bounds bbox) {
    while (badPosition(avoid, shape)) {
      // move down
      shape.translate(0, 10);
      Location loc = shape.getLocation();
      if (loc.getX() < bbox.getX() + bbox.getWidth()
          && loc.getY() + shape.getBounds().getHeight() >= bbox.getY() + bbox.getHeight())
        // if we are below the bounding box, move right and up
        shape.translate(10, (bbox.getY() + 9) / 10 * 10 - loc.getY());
    }
  }

  private static boolean badPosition(List<CanvasObject> avoid, CanvasObject shape) {
    for (CanvasObject s : avoid) {
      if (shape.overlaps(s) || s.overlaps(shape)) return true;
    }
    return false;
  }

  private static DynamicElement.Path toComponentPath(TreePath p) {
    Object[] o = p.getPath();
    InstanceComponent[] elt = new InstanceComponent[o.length - 1];
    for (int i = 1; i < o.length; i++) {
      Ref r = ((RefTreeNode) o[i]).refData;
      elt[i - 1] = r.ic;
    }
    return new DynamicElement.Path(elt);
  }

  private static TreePath toTreePath(RefTreeNode root, DynamicElement.Path path) {
    Object[] o = new Object[path.elt.length + 1];
    o[0] = root;
    for (int i = 1; i < o.length; i++) {
      o[i] = findChild((RefTreeNode) o[i - 1], path.elt[i - 1]);
      if (o[i] == null) return null;
    }
    return new TreePath(o);
  }

  private static RefTreeNode findChild(RefTreeNode node, InstanceComponent ic) {
    for (int i = 0; i < node.getChildCount(); i++) {
      RefTreeNode child = (RefTreeNode) node.getChildAt(i);
      Ref r = child.refData;
      if (r.ic.getLocation().equals(ic.getLocation())
          && r.ic.getFactory().getName().equals(ic.getFactory().getName())) return child;
    }
    return null;
  }

  private TreePath[] getPaths() {
    RefTreeNode root = (RefTreeNode) tree.getModel().getRoot();
    ArrayList<TreePath> paths = new ArrayList<>();
    for (CanvasObject shape : canvas.getModel().getObjectsFromBottom()) {
      if (!(shape instanceof DynamicElement)) continue;
      TreePath path = toTreePath(root, ((DynamicElement) shape).getPath());
      paths.add(path);
    }
    return paths.toArray(new TreePath[0]);
  }

  private void apply() {
    CanvasModel model = canvas.getModel();
    RefTreeNode root = (RefTreeNode) tree.getModel().getRoot();

    Bounds bbox = Bounds.EMPTY_BOUNDS;
    for (CanvasObject shape : model.getObjectsFromBottom()) {
      bbox = bbox.add(shape.getBounds());
    }
    Location loc = Location.create(((bbox.getX() + 9) / 10 * 10), ((bbox.getY() + 9) / 10 * 10));

    // TreePath[] roots = tree.getCheckingRoots();
    TreePath[] checked = tree.getCheckingPaths();
    ArrayList<TreePath> toAdd = new ArrayList<>(Arrays.asList(checked));

    // Remove existing dynamic objects that are no longer checked.
    ArrayList<CanvasObject> toRemove = new ArrayList<>();
    for (CanvasObject shape : model.getObjectsFromBottom()) {
      if (!(shape instanceof DynamicElement)) continue;
      TreePath path = toTreePath(root, ((DynamicElement) shape).getPath());
      if (path != null && tree.isPathChecked(path)) {
        toAdd.remove(path); // already present, don't need to add it again
      } else {
        toRemove.add(shape); // no longer checked, or invalid
      }
    }

    boolean dirty = true;
    if (toRemove.size() > 0) {
      canvas.doAction(new ModelRemoveAction(model, toRemove));
      dirty = true;
    }

    // sort the remaining shapes
    toAdd.sort(new CompareByLocations());

    ArrayList<CanvasObject> avoid = new ArrayList<>(model.getObjectsFromBottom());
    for (int i = avoid.size() - 1; i >= 0; i--) {
      if (avoid.get(i) instanceof AppearanceAnchor) avoid.remove(i);
    }
    ArrayList<CanvasObject> newShapes = new ArrayList<>();

    for (TreePath path : toAdd) {
      RefTreeNode node = (RefTreeNode) path.getLastPathComponent();
      Ref r = node.refData;
      if (r instanceof CircuitRef) continue;
      ComponentFactory factory = r.ic.getFactory();
      if (factory instanceof DynamicElementProvider) {
        int x = loc.getX();
        int y = loc.getY();
        DynamicElement.Path p = toComponentPath(path);
        DynamicElement shape = ((DynamicElementProvider) factory).createDynamicElement(x, y, p);
        pickPlacement(avoid, shape, bbox);
        loc = shape.getLocation();
        avoid.add(shape);
        newShapes.add(shape);
      }
    }
    if (newShapes.size() > 0) {
      canvas.doAction(new ModelAddAction(model, newShapes));
      dirty = true;
    }
    if (dirty) canvas.repaint();
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Object src = e.getSource();
    if (src == ok) {
      apply();
    }
    this.dispose();
  }

  private RefTreeNode enumerate(Circuit circuit, InstanceComponent ic) {
    RefTreeNode root = new RefTreeNode(new CircuitRef(circuit, ic));
    for (Component c : circuit.getNonWires()) {
      if (c instanceof InstanceComponent) {
        InstanceComponent child = (InstanceComponent) c;
        ComponentFactory f = child.getFactory();
        if (f instanceof DynamicElementProvider) {
          root.add(new RefTreeNode(new Ref(child)));
        } else if (f instanceof SubcircuitFactory) {
          RefTreeNode node = enumerate(((SubcircuitFactory) f).getSubcircuit(), child);
          if (node != null) root.add(node);
        }
      }
    }
    if (root.getChildCount() == 0) return null;
    else return root;
  }

  private static class CompareByLocations implements Comparator<TreePath> {
    @Override
    public int compare(TreePath a, TreePath b) {
      Object[] aa = a.getPath();
      Object[] bb = b.getPath();
      for (int i = 1; i < aa.length && i < bb.length; i++) {
        Ref ra = ((RefTreeNode) aa[i]).refData;
        Ref rb = ((RefTreeNode) bb[i]).refData;
        Location la = ra.ic.getLocation();
        Location lb = rb.ic.getLocation();
        int diff = la.compareTo(lb);
        if (diff != 0) return diff;
      }
      return 0;
    }
  }

  private static class Ref {
    final InstanceComponent ic;

    Ref(InstanceComponent ic) {
      this.ic = ic;
    }

    @Override
    public String toString() {
      String s = ic.getInstance().getAttributeValue(StdAttr.LABEL);
      Location loc = ic.getInstance().getLocation();
      if (s != null && s.length() > 0)
        return String.format("\"%s\" %s @ (%d, %d)", s, ic.getFactory(), loc.getX(), loc.getY());
      else return String.format("%s @ (%d, %d)", ic.getFactory(), loc.getX(), loc.getY());
    }
  }

  private static class CircuitRef extends Ref {
    final Circuit circuit;

    CircuitRef(Circuit c, InstanceComponent ic) {
      super(ic);
      this.circuit = c;
    }

    @Override
    public String toString() {
      return (ic == null) ? S.get("showStateDialogNodeTitle", circuit.getName()) : super.toString();
    }
  }

  private static class RefTreeNode extends DefaultMutableTreeNode {
    final Ref refData;

    RefTreeNode(Object data) {
      super(new CheckBoxNodeData(data.toString(), false));
      refData = (data instanceof Ref) ? (Ref) data : null;
    }
  }
}
