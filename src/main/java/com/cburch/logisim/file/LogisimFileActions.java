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

package com.cburch.logisim.file;

import static com.cburch.logisim.file.Strings.S;

import com.cburch.logisim.circuit.Circuit;
import com.cburch.logisim.circuit.CircuitMutation;
import com.cburch.logisim.circuit.Wire;
import com.cburch.logisim.comp.Component;
import com.cburch.logisim.data.Attribute;
import com.cburch.logisim.data.AttributeSet;
import com.cburch.logisim.data.Location;
import com.cburch.logisim.gui.generic.OptionPane;
import com.cburch.logisim.proj.Action;
import com.cburch.logisim.proj.Project;
import com.cburch.logisim.proj.ProjectActions;
import com.cburch.logisim.std.base.Text;
import com.cburch.logisim.std.wiring.Pin;
import com.cburch.logisim.tools.AddTool;
import com.cburch.logisim.tools.Library;
import com.cburch.logisim.tools.LibraryTools;
import com.cburch.logisim.tools.Tool;
import com.cburch.logisim.vhdl.base.VhdlContent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

public class LogisimFileActions {
  private static class AddCircuit extends Action {
    private Circuit circuit;

    AddCircuit(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public void doIt(Project proj) {
      proj.getLogisimFile().addCircuit(circuit);
    }

    @Override
    public String getName() {
      return S.get("addCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().removeCircuit(circuit);
    }
  }

  private static class AddVhdl extends Action {
    private VhdlContent vhdl;

    AddVhdl(VhdlContent vhdl) {
      this.vhdl = vhdl;
    }

    @Override
    public void doIt(Project proj) {
      proj.getLogisimFile().addVhdlContent(vhdl);
    }

    @Override
    public String getName() {
      return S.get("addVhdlAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().removeVhdl(vhdl);
    }
  }

  private static class MergeFile extends Action {
    private ArrayList<Circuit> MergedCircuits = new ArrayList<Circuit>();
    private ArrayList<File> JarLibs = new ArrayList<File>();
    private ArrayList<File> LogiLibs = new ArrayList<File>();

    MergeFile(LogisimFile mergelib, LogisimFile source) {
      HashMap<String, Library> LibNames = new HashMap<String, Library>();
      HashSet<String> ToolList = new HashSet<String>();
      HashMap<String, String> Error = new HashMap<String, String>();
      boolean cancontinue = true;
      for (Library lib : source.getLibraries()) {
        LibraryTools.BuildLibraryList(lib, LibNames);
      }
      LibraryTools.BuildToolList(source, ToolList);
      LibraryTools.RemovePresentLibraries(mergelib, LibNames, false);
      if (LibraryTools.LibraryIsConform(
          mergelib, new HashSet<String>(), new HashSet<String>(), Error)) {
        /* Okay the library is now ready for merge */
        for (Library lib : mergelib.getLibraries()) {
          HashSet<String> NewToolList = new HashSet<String>();
          LibraryTools.BuildToolList(lib, NewToolList);
          ArrayList<String> ret = LibraryTools.LibraryCanBeMerged(ToolList, NewToolList);
          if (!ret.isEmpty()) {
            String Location = "";
            HashMap<String, String> ToolNames = LibraryTools.GetToolLocation(source, Location, ret);
            for (String key : ToolNames.keySet()) {
              String SolStr = S.get("LibMergeFailure2") + " a) ";
              String ErrLoc = ToolNames.get(key);
              String[] ErrParts = ErrLoc.split("->");
              if (ErrParts.length > 1) {
                SolStr = SolStr.concat(S.fmt("LibMergeFailure4", ErrParts[1]));
              } else {
                SolStr = SolStr.concat(S.fmt("LibMergeFailure3", key));
              }
              SolStr = SolStr.concat(" " + S.get("LibMergeFailure5") + " b) ");
              SolStr = SolStr.concat(S.fmt("LibMergeFailure6", lib.getName()));
              Error.put(SolStr, S.fmt("LibMergeFailure1", lib.getName(), key));
            }
            cancontinue = false;
          }
          String[] splits = mergelib.getLoader().getDescriptor(lib).split("#");
          File TheFile = mergelib.getLoader().getFileFor(splits[1], null);
          if (splits[0].equals("file")) LogiLibs.add(TheFile);
          else if (splits[0].equals("jar")) JarLibs.add(TheFile);
        }
        if (!cancontinue) {
          LibraryTools.ShowErrors(mergelib.getName(), Error);
          LogiLibs.clear();
          JarLibs.clear();
          return;
        }
        /* Okay merged the missing libraries, now add the circuits */
        for (Circuit circ : mergelib.getCircuits()) {
          String CircName = circ.getName().toUpperCase();
          if (ToolList.contains(CircName)) {
            ArrayList<String> ret = new ArrayList<String>();
            ret.add(CircName);
            HashMap<String, String> ToolNames = LibraryTools.GetToolLocation(source, "", ret);
            for (String key : ToolNames.keySet()) {
              String ErrLoc = ToolNames.get(key);
              String[] ErrParts = ErrLoc.split("->");
              if (ErrParts.length > 1) {
                String SolStr = S.get("LibMergeFailure2") + " a) ";
                SolStr = SolStr.concat(S.fmt("LibMergeFailure4", ErrParts[1]));
                SolStr = SolStr.concat(" " + S.get("LibMergeFailure5") + " b) ");
                SolStr = SolStr.concat(S.fmt("LibMergeFailure8", circ.getName()));
                Error.put(SolStr, S.fmt("LibMergeFailure7", key, ErrParts[1]));
                cancontinue = false;
              }
            }
            if (cancontinue) {
              Circuit circ1 = LibraryTools.getCircuitFromLibs(source, CircName);
              if (circ1 == null) {
                OptionPane.showMessageDialog(
                    null,
                    "Fatal internal error: Cannot find a referenced circuit",
                    "LogosimFileAction:",
                    OptionPane.ERROR_MESSAGE);
                cancontinue = false;
              } else if (!CircuitsAreEqual(circ1, circ)) {
                int Reponse =
                    OptionPane.showConfirmDialog(
                        null,
                        S.fmt("FileMergeQuestion", circ.getName()),
                        S.get("FileMergeTitle"),
                        OptionPane.YES_NO_OPTION);
                if (Reponse == OptionPane.YES_OPTION) {
                  MergedCircuits.add(circ);
                }
              }
            }
          } else {
            MergedCircuits.add(circ);
          }
        }
        if (!cancontinue) {
          LibraryTools.ShowErrors(mergelib.getName(), Error);
          LogiLibs.clear();
          JarLibs.clear();
          MergedCircuits.clear();
          return;
        }
      } else LibraryTools.ShowErrors(mergelib.getName(), Error);
    }

    private boolean CircuitsAreEqual(Circuit orig, Circuit newone) {
      HashMap<Location, Component> origcomps = new HashMap<Location, Component>();
      HashMap<Location, Component> newcomps = new HashMap<Location, Component>();
      for (Component comp : orig.getWires()) {
        origcomps.put(comp.getLocation(), comp);
      }
      for (Component comp : orig.getNonWires()) {
        origcomps.put(comp.getLocation(), comp);
      }
      for (Component comp : newone.getWires()) {
        newcomps.put(comp.getLocation(), comp);
      }
      for (Component comp : newone.getNonWires()) {
        newcomps.put(comp.getLocation(), comp);
      }
      Iterator<Location> it = newcomps.keySet().iterator();
      while (it.hasNext()) {
        Location loc = it.next();
        if (origcomps.containsKey(loc)) {
          Component comp1 = newcomps.get(loc);
          Component comp2 = newcomps.get(loc);
          if (comp1.getFactory().getName().equals(comp2.getFactory().getName())) {
            if (comp1.getFactory().getName().equals("Wire")) {
              Wire wir1 = (Wire) comp1;
              Wire wir2 = (Wire) comp2;
              if (wir1.overlaps(wir2, true)) {
                it.remove();
                origcomps.remove(loc);
              } else {
                System.out.println("No Wire Overlap");
              }
            } else {
              if (comp1.getAttributeSet().equals(comp2.getAttributeSet())) {
                it.remove();
                origcomps.remove(loc);
              } else {
                System.out.println("Different component");
              }
            }
          }
        }
      }
      return origcomps.isEmpty() & newcomps.isEmpty();
    }

    @Override
    public void doIt(Project proj) {
      Loader loader = proj.getLogisimFile().getLoader();
      /* first we are going to merge the jar libraries */
      for (int i = 0; i < JarLibs.size(); i++) {
        String className = null;
        JarFile jarFile = null;
        try {
          jarFile = new JarFile(JarLibs.get(i));
          Manifest manifest = jarFile.getManifest();
          className = manifest.getMainAttributes().getValue("Library-Class");
        } catch (IOException e) {
          // if opening the JAR file failed, do nothing
        } finally {
          if (jarFile != null) {
            try {
              jarFile.close();
            } catch (IOException e) {
            }
          }
        }
        // if the class name was not found, go back to the good old dialog
        if (className == null) {
          className =
              OptionPane.showInputDialog(
                  proj.getFrame(),
                  S.get("jarClassNamePrompt"),
                  S.get("jarClassNameTitle"),
                  OptionPane.QUESTION_MESSAGE);
          // if user canceled selection, abort
          if (className == null) continue;
        }
        Library lib = loader.loadJarLibrary(JarLibs.get(i), className);
        if (lib != null) {
          proj.doAction(LogisimFileActions.loadLibrary(lib, proj.getLogisimFile()));
        }
      }
      JarLibs.clear();
      /* next we are going to load the logisimfile  libraries */
      for (int i = 0; i < LogiLibs.size(); i++) {
        Library put = loader.loadLogisimLibrary(LogiLibs.get(i));
        if (put != null) {
          proj.doAction(LogisimFileActions.loadLibrary(put, proj.getLogisimFile()));
        }
      }
      LogiLibs.clear();
      /*this we are going to do in two steps, first add the circuits with inputs, outputs and wires */
      for (Circuit circ : MergedCircuits) {
        Circuit NewCirc = null;
        boolean replace = false;
        for (Circuit circs : proj.getLogisimFile().getCircuits())
          if (circs.getName().toUpperCase().equals(circ.getName().toUpperCase())) {
            NewCirc = circs;
            replace = true;
          }
        if (NewCirc == null) NewCirc = new Circuit(circ.getName(), proj.getLogisimFile(), proj);
        CircuitMutation result = new CircuitMutation(NewCirc);
        if (replace) result.clear();
        for (Wire wir : circ.getWires()) {
          result.add(Wire.create(wir.getEnd0(), wir.getEnd1()));
        }
        for (Component comp : circ.getNonWires()) {
          if (comp.getFactory() instanceof Pin) {
            result.add(
                Pin.FACTORY.createComponent(
                    comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone()));
          }
        }
        if (!replace) {
          result.execute();
          proj.doAction(LogisimFileActions.addCircuit(NewCirc));
        } else proj.doAction(result.toAction(S.getter("replaceCircuitAction")));
      }
      HashMap<String, AddTool> AvailableTools = new HashMap<String, AddTool>();
      LibraryTools.BuildToolList(proj.getLogisimFile(), AvailableTools);
      /* in the second step we are going to add the rest of the contents */
      for (Circuit circ : MergedCircuits) {
        Circuit NewCirc = proj.getLogisimFile().getCircuit(circ.getName());
        if (NewCirc != null) {
          CircuitMutation result = new CircuitMutation(NewCirc);
          for (Component comp : circ.getNonWires()) {
            if (!(comp.getFactory() instanceof Pin)) {
              AddTool current = AvailableTools.get(comp.getFactory().getName().toUpperCase());
              if (current != null) {
                Component NewComp =
                    current
                        .getFactory()
                        .createComponent(
                            comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone());
                result.add(NewComp);
              } else if (comp.getFactory().getName().equals("Text")) {
                Component NewComp =
                    Text.FACTORY.createComponent(
                        comp.getLocation(), (AttributeSet) comp.getAttributeSet().clone());
                result.add(NewComp);
              } else System.out.println("Not found:" + comp.getFactory().getName());
            }
          }
          result.execute();
        }
      }
      MergedCircuits.clear();
    }

    @Override
    public String getName() {
      return S.get("mergeFileAction");
    }

    @Override
    public boolean isModification() {
      return false;
    }

    @Override
    public void undo(Project proj) {}
  }

  private static class LoadLibraries extends Action {
    private ArrayList<Library> MergedLibs = new ArrayList<Library>();

    LoadLibraries(Library[] libs, LogisimFile source) {
      HashMap<String, Library> LibNames = new HashMap<String, Library>();
      HashSet<String> ToolList = new HashSet<String>();
      HashMap<String, String> Error = new HashMap<String, String>();
      for (Library lib : source.getLibraries()) {
        LibraryTools.BuildLibraryList(lib, LibNames);
      }
      LibraryTools.BuildToolList(source, ToolList);
      for (int i = 0; i < libs.length; i++) {
        if (LibNames.keySet().contains(libs[i].getName().toUpperCase())) {
          OptionPane.showMessageDialog(
              null,
              "\"" + libs[i].getName() + "\": " + S.get("LibraryAlreadyLoaded"),
              S.get("LibLoadErrors") + " " + libs[i].getName() + " !",
              OptionPane.WARNING_MESSAGE);
        } else {
          LibraryTools.RemovePresentLibraries(libs[i], LibNames, false);
          if (LibraryTools.LibraryIsConform(
              libs[i], new HashSet<String>(), new HashSet<String>(), Error)) {
            HashSet<String> AddedToolList = new HashSet<String>();
            LibraryTools.BuildToolList(libs[i], AddedToolList);
            for (String tool : AddedToolList)
              if (ToolList.contains(tool)) Error.put(tool, S.get("LibraryMultipleToolError"));
            if (Error.keySet().isEmpty()) {
              LibraryTools.BuildLibraryList(libs[i], LibNames);
              ToolList.addAll(AddedToolList);
              MergedLibs.add(libs[i]);
            } else LibraryTools.ShowErrors(libs[i].getName(), Error);
          } else LibraryTools.ShowErrors(libs[i].getName(), Error);
        }
      }
    }

    @Override
    public void doIt(Project proj) {
      for (Library lib : MergedLibs) {
        if (lib instanceof LoadedLibrary) {
          LoadedLibrary lib1 = (LoadedLibrary) lib;
          if (lib1.getBase() instanceof LogisimFile) {
            repair(proj, lib1.getBase());
          }
        } else if (lib instanceof LogisimFile) {
          repair(proj, lib);
        }
        proj.getLogisimFile().addLibrary(lib);
      }
    }

    private void repair(Project proj, Library lib) {
      HashMap<String, AddTool> AvailableTools = new HashMap<String, AddTool>();
      LibraryTools.BuildToolList(proj.getLogisimFile(), AvailableTools);
      if (lib instanceof LogisimFile) {
        LogisimFile ThisLib = (LogisimFile) lib;
        Iterator<Circuit> iter = ThisLib.getCircuits().iterator();
        ArrayList<Circuit> added = new ArrayList<Circuit>();
        while (iter.hasNext()) {
          Circuit circ = iter.next();
          Circuit NewCirc = new Circuit(circ.getName(), ThisLib, proj);
          CircuitMutation result = new CircuitMutation(NewCirc);
          for (Component tool : circ.getNonWires()) {
            if (AvailableTools.keySet().contains(tool.getFactory().getName().toUpperCase())) {
              AddTool current = AvailableTools.get(tool.getFactory().getName().toUpperCase());
              if (current != null) {
                Component NewComp =
                    current
                        .getFactory()
                        .createComponent(
                            tool.getLocation(), (AttributeSet) tool.getAttributeSet().clone());
                result.add(NewComp);
              } else if (tool.getFactory().getName().equals("Text")) {
                Component NewComp =
                    Text.FACTORY.createComponent(
                        tool.getLocation(), (AttributeSet) tool.getAttributeSet().clone());
                result.add(NewComp);
              } else System.out.println("Not found:" + tool.getFactory().getName());
            } else {
              result.add(tool);
            }
          }
          result.addAll(circ.getWires());
          result.execute();
          added.add(NewCirc);
        }
        for (int i = 0; i < added.size(); i++) {
          Circuit NewCirc = added.get(i);
          Circuit OldCirc = ThisLib.getCircuit(NewCirc.getName());
          if (OldCirc != null) {
            ThisLib.addCircuit(added.get(i));
            ThisLib.removeCircuit(OldCirc);
          } else {
            System.out.println("Horrible error");
          }
        }
      }
      for (Library libs : lib.getLibraries()) {
        repair(proj, libs);
      }
    }

    @Override
    public boolean isModification() {
      return MergedLibs.size() > 0;
    }

    @Override
    public String getName() {
      if (MergedLibs.size() <= 1) {
        return S.get("loadLibraryAction");
      } else {
        return S.get("loadLibrariesAction");
      }
    }

    @Override
    public void undo(Project proj) {
      for (Library lib : MergedLibs) proj.getLogisimFile().removeLibrary(lib);
    }
  }

  private static class MoveCircuit extends Action {
    private AddTool tool;
    private int fromIndex;
    private int toIndex;

    MoveCircuit(AddTool tool, int toIndex) {
      this.tool = tool;
      this.toIndex = toIndex;
    }

    @Override
    public Action append(Action other) {
      MoveCircuit ret = new MoveCircuit(tool, ((MoveCircuit) other).toIndex);
      ret.fromIndex = this.fromIndex;
      return ret.fromIndex == ret.toIndex ? null : ret;
    }

    @Override
    public void doIt(Project proj) {
      fromIndex = proj.getLogisimFile().getTools().indexOf(tool);
      proj.getLogisimFile().moveCircuit(tool, toIndex);
    }

    @Override
    public String getName() {
      return S.get("moveCircuitAction");
    }

    @Override
    public boolean shouldAppendTo(Action other) {
      return other instanceof MoveCircuit && ((MoveCircuit) other).tool == this.tool;
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().moveCircuit(tool, fromIndex);
    }
  }

  private static class RemoveCircuit extends Action {
    private Circuit circuit;
    private int index;

    RemoveCircuit(Circuit circuit) {
      this.circuit = circuit;
    }

    @Override
    public void doIt(Project proj) {
      index = proj.getLogisimFile().indexOfCircuit(circuit);
      proj.getLogisimFile().removeCircuit(circuit);
    }

    @Override
    public String getName() {
      return S.get("removeCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().addCircuit(circuit, index);
    }
  }

  private static class RemoveVhdl extends Action {
    private VhdlContent vhdl;
    private int index;

    RemoveVhdl(VhdlContent vhdl) {
      this.vhdl = vhdl;
    }

    @Override
    public void doIt(Project proj) {
      index = proj.getLogisimFile().indexOfVhdl(vhdl);
      proj.getLogisimFile().removeVhdl(vhdl);
    }

    @Override
    public String getName() {
      return S.get("removeVhdlAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().addVhdlContent(vhdl, index);
    }
  }

  private static class RevertAttributeValue {
    private AttributeSet attrs;
    private Attribute<Object> attr;
    private Object value;

    RevertAttributeValue(AttributeSet attrs, Attribute<Object> attr, Object value) {
      this.attrs = attrs;
      this.attr = attr;
      this.value = value;
    }
  }

  public static Action addVhdl(VhdlContent vhdl) {
    return new AddVhdl(vhdl);
  }

  private static class RevertDefaults extends Action {
    private Options oldOpts;
    private ArrayList<Library> libraries = null;
    private ArrayList<RevertAttributeValue> attrValues;

    RevertDefaults() {
      libraries = null;
      attrValues = new ArrayList<RevertAttributeValue>();
    }

    private void copyToolAttributes(Library srcLib, Library dstLib) {
      for (Tool srcTool : srcLib.getTools()) {
        AttributeSet srcAttrs = srcTool.getAttributeSet();
        Tool dstTool = dstLib.getTool(srcTool.getName());
        if (srcAttrs != null && dstTool != null) {
          AttributeSet dstAttrs = dstTool.getAttributeSet();
          for (Attribute<?> attrBase : srcAttrs.getAttributes()) {
            @SuppressWarnings("unchecked")
            Attribute<Object> attr = (Attribute<Object>) attrBase;
            Object srcValue = srcAttrs.getValue(attr);
            Object dstValue = dstAttrs.getValue(attr);
            if (!dstValue.equals(srcValue)) {
              dstAttrs.setValue(attr, srcValue);
              attrValues.add(new RevertAttributeValue(dstAttrs, attr, dstValue));
            }
          }
        }
      }
    }

    @Override
    public void doIt(Project proj) {
      LogisimFile src = ProjectActions.createNewFile(proj);
      LogisimFile dst = proj.getLogisimFile();

      copyToolAttributes(src, dst);
      for (Library srcLib : src.getLibraries()) {
        Library dstLib = dst.getLibrary(srcLib.getName());
        if (dstLib == null) {
          String desc = src.getLoader().getDescriptor(srcLib);
          dstLib = dst.getLoader().loadLibrary(desc);
          proj.getLogisimFile().addLibrary(dstLib);
          if (libraries == null) libraries = new ArrayList<Library>();
          libraries.add(dstLib);
        }
        copyToolAttributes(srcLib, dstLib);
      }

      Options newOpts = proj.getOptions();
      oldOpts = new Options();
      oldOpts.copyFrom(newOpts, dst);
      newOpts.copyFrom(src.getOptions(), dst);
    }

    @Override
    public String getName() {
      return S.get("revertDefaultsAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getOptions().copyFrom(oldOpts, proj.getLogisimFile());

      for (RevertAttributeValue attrValue : attrValues) {
        attrValue.attrs.setValue(attrValue.attr, attrValue.value);
      }

      if (libraries != null) {
        for (Library lib : libraries) {
          proj.getLogisimFile().removeLibrary(lib);
        }
      }
    }
  }

  private static class SetMainCircuit extends Action {
    private Circuit oldval;
    private Circuit newval;

    SetMainCircuit(Circuit circuit) {
      newval = circuit;
    }

    @Override
    public void doIt(Project proj) {
      oldval = proj.getLogisimFile().getMainCircuit();
      proj.getLogisimFile().setMainCircuit(newval);
    }

    @Override
    public String getName() {
      return S.get("setMainCircuitAction");
    }

    @Override
    public void undo(Project proj) {
      proj.getLogisimFile().setMainCircuit(oldval);
    }
  }

  private static class UnloadLibraries extends Action {
    private Library[] libs;

    UnloadLibraries(Library[] libs) {
      this.libs = libs;
    }

    @Override
    public void doIt(Project proj) {
      for (int i = libs.length - 1; i >= 0; i--) {
        proj.getLogisimFile().removeLibrary(libs[i]);
      }
    }

    @Override
    public String getName() {
      if (libs.length == 1) {
        return S.get("unloadLibraryAction");
      } else {
        return S.get("unloadLibrariesAction");
      }
    }

    @Override
    public void undo(Project proj) {
      for (int i = 0; i < libs.length; i++) {
        proj.getLogisimFile().addLibrary(libs[i]);
      }
    }
  }

  public static Action addCircuit(Circuit circuit) {
    return new AddCircuit(circuit);
  }

  public static Action MergeFile(LogisimFile mergelib, LogisimFile source) {
    return new MergeFile(mergelib, source);
  }

  public static Action loadLibraries(Library[] libs, LogisimFile source) {
    return new LoadLibraries(libs, source);
  }

  public static Action loadLibrary(Library lib, LogisimFile source) {
    return new LoadLibraries(new Library[] {lib}, source);
  }

  public static Action moveCircuit(AddTool tool, int toIndex) {
    return new MoveCircuit(tool, toIndex);
  }

  public static Action removeCircuit(Circuit circuit) {
    return new RemoveCircuit(circuit);
  }

  public static Action removeVhdl(VhdlContent vhdl) {
    return new RemoveVhdl(vhdl);
  }

  public static Action revertDefaults() {
    return new RevertDefaults();
  }

  public static Action setMainCircuit(Circuit circuit) {
    return new SetMainCircuit(circuit);
  }

  public static Action unloadLibraries(Library[] libs) {
    return new UnloadLibraries(libs);
  }

  public static Action unloadLibrary(Library lib) {
    return new UnloadLibraries(new Library[] {lib});
  }

  private LogisimFileActions() {}
}
